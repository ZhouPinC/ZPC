// pages/me/index.js
const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');
const storage = require('../../utils/storage.js');

Page({
  data: {
    userInfo: null,
    boundDeviceCount: 0,
    isAdmin: false,
    profileState: 'idle',
    profileError: '',
    avatarDisplayUrl: '/images/me.png',
    lastProfileLoadMs: 0,
  },

  onLoad() {
    this.checkLogin();
  },

  onShow() {
    // 设置自定义tabBar选中状态
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 2 });
    }

    this.checkLogin();
  },

  onPullDownRefresh() {
    if (!this.data.userInfo) {
      wx.stopPullDownRefresh();
      return;
    }
    this.refreshUserInfo();
  },

  /**
   * 获取用户信息（安全方式）
   */
  getUser() {
    const app = getApp();
    if (app && app.globalData && app.globalData.userInfo) {
      return app.globalData.userInfo;
    }
    return wx.getStorageSync('user') || null;
  },

  /**
   * 检查登录状态
   */
  checkLogin() {
    // 优先从新的存储key获取
    let user = wx.getStorageSync('user_info');

    // 兼容旧的存储key
    if (!user || !user.id) {
      user = wx.getStorageSync('user');
    }

    // 从全局状态获取
    if (!user || !user.id) {
      const app = getApp();
      if (app && app.globalData && app.globalData.userInfo) {
        user = app.globalData.userInfo;
      }
    }

    const boundDevice = wx.getStorageSync('boundDevice');

    if (user && user.id) {
      this.setData({
        userInfo: user,
        boundDeviceCount: boundDevice ? 1 : 0,
        isAdmin: user.role === 'ADMIN' || user.role === 'SUPER_ADMIN'
      });
      this.setAvatarDisplayUrl(user);
      this.refreshUserInfo();
    } else {
      this.setData({
        userInfo: null,
        boundDeviceCount: 0,
        isAdmin: false,
        profileState: 'idle',
        profileError: '',
        avatarDisplayUrl: '/images/me.png',
      });
    }
  },

  /**
   * 从服务器刷新用户信息
   */
  async refreshUserInfo() {
    try {
      const app = getApp();
      const isOnline = app && app.globalData ? app.globalData.isOnline !== false : true;
      if (!isOnline) {
        this.setData({ profileState: 'error', profileError: '当前网络不可用，点击重试' });
        return;
      }

      this.setData({ profileState: 'loading', profileError: '' });
      const startAt = Date.now();
      const userInfo = await this.callWithRetry(() => api.userProfile.getInfo(), 2);
      const costMs = Date.now() - startAt;
      console.info('PERF user_profile_fetch_ms', costMs);

      if (userInfo && userInfo.id) {
        storage.setUserInfo(userInfo);
        wx.setStorageSync('user', userInfo);
        wx.setStorageSync('user_profile_cache', { userInfo, time: Date.now() });

        if (app && app.globalData) {
          app.globalData.userInfo = userInfo;
        }

        this.setData({
          userInfo,
          isAdmin: userInfo.role === 'ADMIN' || userInfo.role === 'SUPER_ADMIN',
          profileState: 'success',
          lastProfileLoadMs: costMs,
        });
        this.setAvatarDisplayUrl(userInfo);
      } else {
        this.setData({ profileState: 'error', profileError: '数据格式异常，点击重试' });
      }
    } catch (e) {
      console.error('刷新用户信息失败:', e);
      this.setData({ profileState: 'error', profileError: '网络连接异常，点击重试' });
    } finally {
      wx.stopPullDownRefresh();
    }
  },

  retryLoad() {
    this.refreshUserInfo();
  },

  setAvatarDisplayUrl(userInfo) {
    const fallback = '/images/me.png';
    const cache = wx.getStorageSync('user_profile_cache');
    const cachedUser = cache && cache.userInfo ? cache.userInfo : null;
    const cachedAvatar = cachedUser && cachedUser.id === (userInfo && userInfo.id) ? cachedUser.avatarUrl : '';
    const url = (userInfo && userInfo.avatarUrl) || cachedAvatar || fallback;
    this.setData({ avatarDisplayUrl: url });
  },

  onAvatarError() {
    this.setData({ avatarDisplayUrl: '/images/me.png' });
  },

  async callWithRetry(fn, retries) {
    let lastError = null;
    for (let i = 0; i <= retries; i++) {
      try {
        return await fn();
      } catch (e) {
        lastError = e;
        if (i === retries) break;
        await new Promise(resolve => setTimeout(resolve, 300 * Math.pow(2, i)));
      }
    }
    throw lastError;
  },

  /**
   * 微信登录 - 统一跳转到login页面
   */
  handleLogin() {
    if (this.data.userInfo) return;
    wx.navigateTo({ url: '/pages/common/login/login' });
  },

  /**
   * 更换头像
   */
  async onChooseAvatar(e) {
    const { avatarUrl } = e.detail;
    console.log('选择头像:', avatarUrl);

    // 这里应该上传图片到服务器，目前直接使用临时路径演示
    // 实际需调用 wx.uploadFile

    // 更新本地显示
    this.setData({
      'userInfo.avatarUrl': avatarUrl
    });

    // 保存到后端
    await this.updateUserProfile({ avatarUrl });
  },

  /**
   * 更新昵称
   */
  async onNicknameBlur(e) {
    const nickName = e.detail.value;
    if (!nickName || nickName === this.data.userInfo.nickName) return;

    console.log('更新昵称:', nickName);
    this.setData({
      'userInfo.nickName': nickName
    });

    // 保存到后端
    await this.updateUserProfile({ nickName });
  },

  /**
   * 调用后端更新资料
   */
  async updateUserProfile(data) {
    try {
      if (!this.data.userInfo || !this.data.userInfo.id) return;
      const updated = await api.userProfile.update(data);
      if (updated && updated.id) {
        storage.setUserInfo(updated);
        wx.setStorageSync('user', updated);
        const app = getApp();
        if (app && app.globalData) {
          app.globalData.userInfo = updated;
        }
        this.setData({ userInfo: updated });
        this.setAvatarDisplayUrl(updated);
        wx.showToast({ title: '已保存', icon: 'none' });
      }
    } catch (e) {
      console.error('保存资料失败:', e);
    }
  },

  /**
   * 获取手机号
   */
  async getPhoneNumber(e) {
    console.log('getPhoneNumber:', e);
    
    // 检查用户是否授权
    if (!e.detail.code) {
      // 用户拒绝授权，提供友好提示
      wx.showModal({
        title: '提示',
        content: '绑定手机号可以更安全地管理您的设备和订单，建议您授权绑定。',
        confirmText: '重新授权',
        cancelText: '暂不绑定',
        success: (res) => {
          if (res.confirm) {
            // 用户同意重新授权，无需额外操作，系统会重新触发授权流程
          }
        }
      });
      return;
    }

    try {
      wx.showLoading({ title: '绑定中...' });

      const userId = this.data.userInfo.id;
      const code = e.detail.code;

      // 调用后端接口
      const result = await new Promise((resolve, reject) => {
        wx.request({
          url: getApp().globalData.config.apiBaseUrl + '/api/user/bindPhone',
          method: 'POST',
          header: {
            'content-type': 'application/x-www-form-urlencoded'
          },
          data: {
            userId: userId,
            code: code
          },
          success: (res) => {
            if (res.data && res.data.success) {
              resolve(res.data);
            } else {
              reject(new Error(res.data ? res.data.message : '请求失败'));
            }
          },
          fail: (error) => {
            console.error('请求失败:', error);
            reject(new Error('网络请求失败，请检查网络连接'));
          }
        });
      });

      wx.hideLoading();

      if (result.success) {
        wx.showToast({ 
          title: '绑定成功', 
          icon: 'success',
          duration: 2000
        });
        // 更新本地手机号
        const phone = result.phone;
        this.setData({
          'userInfo.phone': phone
        });

        // 更新缓存
        const user = { ...this.data.userInfo, phone };
        wx.setStorageSync('user', user);
        if (getApp().globalData) {
          getApp().globalData.userInfo = user;
        }
      }
    } catch (err) {
      wx.hideLoading();
      console.error('绑定手机号失败:', err);
      
      // 提供更友好的错误提示
      const errorMsg = err.message || '绑定失败，请稍后重试';
      wx.showModal({
        title: '绑定失败',
        content: errorMsg,
        confirmText: '重试',
        cancelText: '取消',
        success: (res) => {
          if (res.confirm) {
            // 用户点击重试，重新触发授权流程
            this.getPhoneNumber(e);
          }
        }
      });
    }
  },

  /**
   * 退出登录 - 统一清除所有登录信息
   */
  handleLogout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (res.confirm) {
          // 清除旧的存储方式
          wx.removeStorageSync('user');
          wx.removeStorageSync('loginTime');
          wx.removeStorageSync('boundDevice');
          wx.removeStorageSync('user_profile_cache');
          
          // 清除新的JWT存储方式
          auth.logout();
          
          // 清除全局状态
          const app = getApp();
          if (app && app.globalData) {
            app.globalData.userInfo = null;
            app.globalData.isLoggedIn = false;
            app.globalData.token = null;
            app.globalData.viewMode = 'consumer';
          }

          this.setData({
            userInfo: null,
            boundDeviceCount: 0,
            isAdmin: false,
            profileState: 'idle',
            profileError: '',
            avatarDisplayUrl: '/images/me.png',
            lastProfileLoadMs: 0,
          });
          wx.showToast({ title: '已退出', icon: 'none' });
        }
      }
    });
  },

  /**
   * 跳转到个人资料页面
   */
  navigateToProfile() {
    if (!this.data.userInfo) {
      // 未登录时先登录
      this.handleLogin();
      return;
    }
    wx.navigateTo({
      url: '/pages/me/profile/profile'
    });
  },

  /**
   * 绑定手机号
   */
  bindPhone() {
    if (!this.data.userInfo) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }
    wx.navigateTo({
      url: '/pages/me/profile/profile?tab=phone'
    });
  },

  navigateToHistory() {
    if (!this.data.userInfo) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }
    wx.navigateTo({ url: '/pages/history/index' });
  },

  navigateToBind() {
    wx.switchTab({ url: '/pages/add/index' });
  },

  handleImageError(e) {
    console.log('图片加载失败');
  },

  /**
   * 进入管理模式
   */
  enterAdminMode() {
    if (!this.data.userInfo) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }

    wx.showModal({
      title: '进入管理模式',
      editable: true,
      placeholderText: '请输入管理密码',
      success: async (res) => {
        if (res.confirm && res.content) {
          try {
            wx.showLoading({ title: '验证中...' });

            // 检查是否有JWT token，如果没有需要先登录获取
            let token = wx.getStorageSync('auth_token');
            if (!token) {
              // 没有JWT token，需要先通过微信登录获取
              console.log('没有JWT token，先进行微信登录...');
              const loginRes = await new Promise((resolve, reject) => {
                wx.login({ success: resolve, fail: reject });
              });

              if (loginRes.code) {
                const loginResult = await api.auth.wxLogin(loginRes.code, {
                  nickName: this.data.userInfo.nickName || '',
                  avatarUrl: this.data.userInfo.avatarUrl || ''
                });

                // 保存登录信息
                auth.saveLoginInfo(loginResult.token, loginResult.userInfo);
                token = loginResult.token;
              }
            }

            // 现在验证管理密码
            const result = await api.auth.enterAdmin(res.content);

            wx.hideLoading();

            // 保存新的token和用户信息
            auth.saveLoginInfo(result.token, result.userInfo);

            // 同时更新旧的存储方式
            const updatedUser = {
              ...this.data.userInfo,
              ...result.userInfo,
              role: 'ADMIN'
            };
            wx.setStorageSync('user', updatedUser);

            this.setData({ userInfo: updatedUser, isAdmin: true });

            // 切换视图模式
            this.switchToAdminMode();

            wx.showToast({ title: '验证成功', icon: 'success' });

            // 跳转到管理页面
            setTimeout(() => {
              wx.navigateTo({
                url: '/pages/admin/dashboard/dashboard'
              });
            }, 500);
          } catch (error) {
            wx.hideLoading();
            console.error('管理员验证失败:', error);
            wx.showToast({ title: error.message || '密码错误', icon: 'none' });
          }
        }
      }
    });
  },

  /**
   * 切换到管理模式（内部方法）
   */
  switchToAdminMode() {
    const app = getApp();
    if (!app) {
      console.warn('getApp() 返回 null');
      return;
    }

    // 方式1: 调用 app 方法
    if (typeof app.switchViewMode === 'function') {
      app.switchViewMode('admin');
      return;
    }

    // 方式2: 直接设置 globalData
    if (app.globalData) {
      app.globalData.viewMode = 'admin';
      console.log('已通过 globalData 切换到管理模式');
    }
  },

  /**
   * 直接进入管理页面（已是管理员）
   */
  goToAdmin() {
    this.switchToAdminMode();
    wx.navigateTo({
      url: '/pages/admin/dashboard/dashboard'
    });
  }
});
