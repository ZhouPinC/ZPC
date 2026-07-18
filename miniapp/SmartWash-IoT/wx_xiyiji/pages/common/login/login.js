// pages/common/login/login.js
const api = require('../../../utils/api.js');
const auth = require('../../../utils/auth.js');

Page({
  data: {
    loading: false,
    hasUserInfo: false,
    userInfo: null,
  },

  onLoad(options) {
    // 不自动跳转，让用户手动登录
    // 避免死循环
  },

  /**
   * 微信OpenID登录
   * 核心登录流程：
   * 1. 用户点击"微信一键登录"按钮
   * 2. 获取微信登录code
   * 3. 调用微信接口获取OpenID
   * 4. 服务器状态校验
   * 5. 使用OpenID进行登录
   * 6. 保存登录信息并跳转
   */
  async handleWxLogin() {
    if (this.data.loading) {
      console.log('登录正在进行中，忽略重复点击');
      return;
    }
    
    console.log('【微信登录流程】开始登录流程');
    
    // 步骤1：检查网络状态
    const networkType = await this.checkNetworkStatus();
    if (!networkType) {
      console.log('【微信登录流程】网络检查失败');
      this.showNetworkErrorModal();
      return;
    }
    console.log('【微信登录流程】网络状态正常:', networkType);
    
    this.setData({ loading: true });
    
    try {
      // 步骤2：获取微信登录code，增加重试机制
      console.log('【微信登录流程】开始获取微信登录code');
      const code = await this.getWxLoginCodeWithRetry();
      console.log('【微信登录流程】获取登录code成功:', code.substring(0, 10) + '...');
      
      // 步骤3：获取用户信息（可选）
      let userProfile = null;
      try {
        console.log('【微信登录流程】尝试获取用户信息');
        userProfile = await auth.getUserProfile();
        console.log('【微信登录流程】获取用户信息成功:', userProfile.nickName);
      } catch (err) {
        console.log('【微信登录流程】用户拒绝授权用户信息，将使用默认信息');
      }
      
      // 步骤4：服务器状态校验 - 在调用登录接口前先检查服务器连通性
      console.log('【微信登录流程】开始服务器状态校验...');
      const isServerHealthy = await this.checkServerHealth();
      if (!isServerHealthy) {
        console.log('【微信登录流程】服务器健康检查失败');
        throw new Error('SERVER_UNAVAILABLE');
      }
      console.log('【微信登录流程】服务器健康检查通过');
      
      // 步骤5：调用后端OpenID登录接口，增加重试机制
      console.log('【微信登录流程】开始调用后端OpenID登录接口');
      const result = await this.callWxLoginWithRetry(code, userProfile);
      
      // 步骤6：验证登录结果
      if (!result || !result.token || !result.userInfo) {
        console.error('【微信登录流程】登录结果异常:', result);
        throw new Error('登录返回数据异常');
      }
      
      console.log('【微信登录-OpenID】：', result.userInfo.openid || '未返回');
      console.log('【微信登录-用户ID】：', result.userInfo.id || '未返回');
      console.log('【微信登录-用户标识】：', result.userInfo.userIdentifier || '未返回');
      console.log('【微信登录-用户昵称】：', result.userInfo.nickName || '未返回');
      console.log('【微信登录-用户角色】：', result.userInfo.role || '未返回');
      console.log('【微信登录-Token】：', result.token.substring(0, 20) + '...');
      console.log('【微信登录-是否新用户】：', result.isNewUser ? '是' : '否');
      
      // 步骤7：保存登录信息
      console.log('【微信登录流程】开始保存登录信息');
      auth.saveLoginInfo(result.token, result.userInfo);
      
      // 更新全局登录状态，通知所有页面
      const app = getApp();
      if (app && app.notifyLoginStateChange) {
        app.notifyLoginStateChange();
      }
      console.log('【微信登录流程】登录信息保存成功，已通知所有页面');
      
      // 步骤8：提示并跳转
      wx.showToast({
        title: result.isNewUser ? '注册成功' : '登录成功',
        icon: 'success',
        duration: 1500
      });
      
      console.log('【微信登录流程】登录流程完成，准备跳转');
      setTimeout(() => {
        this.navigateBack();
      }, 1500);
      
    } catch (error) {
      console.error('【微信登录流程】登录失败:', error);
      this.handleLoginError(error);
    } finally {
      this.setData({ loading: false });
    }
  },

  /**
   * 检查服务器健康状态
   * 在登录前进行服务器连通性检测，确保服务器正常运行
   */
  async checkServerHealth() {
    try {
      console.log('【服务器健康检查】开始检查服务器状态');
      // 尝试调用后端健康检查接口 (注意: /health 接口在 /api/devices 路径下)
      const healthCheckUrl = api.BASE_URL + '/devices/health';
      console.log('【服务器健康检查】健康检查URL:', healthCheckUrl);
      
      const result = await new Promise((resolve, reject) => {
        // 设置较短的超时时间，快速检测服务器状态
        wx.request({
          url: healthCheckUrl,
          method: 'GET',
          timeout: 3000, // 3秒超时
          success: (res) => {
            console.log('【服务器健康检查】服务器响应:', res);
            if (res.statusCode === 200) {
              console.log('【服务器健康检查】服务器状态正常');
              resolve(true);
            } else {
              console.error('【服务器健康检查】服务器返回异常状态码:', res.statusCode);
              reject(new Error('服务器返回异常状态码: ' + res.statusCode));
            }
          },
          fail: (err) => {
            // 网络请求失败，说明服务器不可达
            console.error('【服务器健康检查】网络请求失败:', err);
            reject(err);
          }
        });
      });
      
      console.log('【服务器健康检查】健康检查通过');
      return true;
      
    } catch (error) {
      console.error('【服务器健康检查】健康检查失败:', error);
      return false;
    }
  },

  /**
   * 返回上一页或首页
   */
  navigateBack() {
    const pages = getCurrentPages();
    if (pages.length > 1) {
      wx.navigateBack();
    } else {
      wx.switchTab({ url: '/pages/device/index' });
    }
  },

  /**
   * 查看用户协议
   */
  onViewAgreement() {
    wx.navigateTo({
      url: '/pages/common/webview/webview?url=agreement',
    });
  },

  /**
   * 查看隐私政策
   */
  onViewPrivacy() {
    wx.navigateTo({
      url: '/pages/common/webview/webview?url=privacy',
    });
  },

  /**
   * 查看帮助信息
   */
  onViewHelp() {
    wx.showModal({
      title: '登录帮助',
      content: '1. 点击"微信一键登录"按钮\n2. 系统将自动获取OpenID并完成登录\n3. 登录成功后跳转到首页\n\n如遇问题，请联系客服',
      showCancel: false,
      confirmText: '我知道了'
    });
  },

  /**
   * 检查网络状态
   */
  checkNetworkStatus() {
    return new Promise((resolve) => {
      wx.getNetworkType({
        success: (res) => {
          if (res.networkType === 'none') {
            resolve(false);
          } else {
            resolve(res.networkType);
          }
        },
        fail: () => {
          resolve(false);
        }
      });
    });
  },

  /**
   * 显示网络错误提示
   */
  showNetworkErrorModal() {
    wx.showModal({
      title: '网络连接失败',
      content: '请检查网络连接后重试',
      confirmText: '重试',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          // 用户点击重试，重新检查网络状态
          this.checkNetworkStatus().then(networkType => {
            if (networkType) {
              wx.showToast({
                title: '网络已连接',
                icon: 'success'
              });
            } else {
              this.showNetworkErrorModal();
            }
          });
        }
      }
    });
  },

  /**
   * 带重试机制的微信登录code获取
   */
  async getWxLoginCodeWithRetry(maxRetries = 3) {
    let retryCount = 0;
    
    while (retryCount < maxRetries) {
      try {
        const code = await auth.wxLogin();
        return code;
      } catch (error) {
        retryCount++;
        console.error(`获取微信登录code失败，第${retryCount}次重试:`, error);
        
        if (retryCount >= maxRetries) {
          throw new Error('获取登录凭证失败，请检查网络后重试');
        }
        
        // 等待一段时间后重试
        await this.delay(1000 * retryCount);
      }
    }
  },

  /**
   * 带重试机制的微信OpenID登录
   */
  async callWxLoginWithRetry(code, userProfile, maxRetries = 2) {
    let retryCount = 0;
    
    while (retryCount < maxRetries) {
      try {
        const result = await api.auth.wxLogin(code, {
          nickName: userProfile ? userProfile.nickName : '',
          avatarUrl: userProfile ? userProfile.avatarUrl : ''
        });
        return result;
      } catch (error) {
        retryCount++;
        console.error(`OpenID登录失败，第${retryCount}次重试:`, error);
        
        if (retryCount >= maxRetries) {
          throw error;
        }
        
        // 等待一段时间后重试
        await this.delay(1000 * retryCount);
      }
    }
  },

  /**
   * 处理登录错误
   * 提供详细的错误提示和用户友好的错误信息
   */
  handleLoginError(error) {
    console.error('【微信登录流程】错误详情:', {
      message: error.message,
      code: error.code,
      statusCode: error.statusCode,
      errMsg: error.errMsg,
      stack: error.stack
    });
    
    let title = '登录失败';
    let content = '登录过程中发生错误，请重试';
    let showRetry = true;
    
    // 处理后端返回的500错误
    if (error.statusCode === 500 || (error.data && error.data.code === 500)) {
      title = '服务器错误';
      // 尝试从后端响应中获取详细错误信息
      if (error.data && error.data.message) {
        content = '服务器错误: ' + error.data.message;
      } else {
        content = '服务器内部错误，请检查后端日志或联系管理员';
      }
      showRetry = true;
    } else if (error.code === 401) {
      title = '登录已过期';
      content = '登录状态已过期，请重新登录';
    } else if (error.message) {
      if (error.message.includes('SERVER_UNAVAILABLE')) {
        title = '服务器连接失败';
        content = '无法连接到服务器，请确保服务器已启动并正常运行后重试';
      } else if (error.message.includes('OpenID') || error.message.includes('openid')) {
        title = 'OpenID获取失败';
        content = '无法获取微信OpenID，请重试或联系客服';
      } else if (error.message.includes('网络') || error.message.includes('timeout') || error.message.includes('request')) {
        title = '网络错误';
        content = '网络连接异常，请检查网络后重试';
      } else if (error.message.includes('登录凭证') || error.message.includes('code')) {
        title = '授权失败';
        content = '微信授权失败，请重新点击登录按钮';
      } else if (error.message.includes('登录返回数据异常')) {
        title = '登录异常';
        content = '服务器返回数据异常，请联系管理员';
        showRetry = false;
      } else {
        content = error.message;
      }
    } else if (error.statusCode) {
      if (error.statusCode === 500) {
        title = '服务器错误';
        content = '服务器内部错误，请联系管理员';
        showRetry = false;
      } else if (error.statusCode === 503) {
        title = '服务不可用';
        content = '服务暂时不可用，请稍后重试';
      }
    }
    
    wx.showModal({
      title: title,
      content: content,
      confirmText: showRetry ? '重试' : '确定',
      cancelText: showRetry ? '取消' : '',
      showCancel: showRetry,
      success: (res) => {
        if (res.confirm && showRetry) {
          console.log('【微信登录流程】用户选择重试登录');
          // 用户点击重试，自动重新执行登录流程
          this.handleWxLogin();
        } else if (res.confirm && !showRetry) {
          console.log('【微信登录流程】用户确认错误提示');
        } else {
          console.log('【微信登录流程】用户取消重试');
        }
      }
    });
  },

  /**
   * 延迟函数
   */
  delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
  },
});
