const app = getApp();
const api = require('../../../utils/api.js');
const storage = require('../../../utils/storage.js');

Page({
  data: {
    userId: null,
    avatarUrl: '',
    nickName: '',
    genderIndex: 0,
    genderOptions: ['未知', '男', '女'],
    phone: '',
    userIdentifier: '',
    createTime: '',
    originalData: {},
    saving: false,
    hasChanges: false,
    hasSavedOnce: false, // 新增：标记是否已经成功保存过一次
    pageState: 'idle',
    pageError: '',
    avatarLoading: false,
    avatarUploading: false,
  },

  onLoad: function(options) {
    this.loadUserProfile();
    this.fetchUserProfile();
    if (options.tab === 'phone') {
      this.scrollToPhoneSection();
    }
  },

  onShow: function() {
    console.log('【个人资料】onShow触发');
    const app = getApp();
    console.log('【个人资料】app.globalData.isLoggedIn:', app.globalData.isLoggedIn);
    
    // 使用全局登录状态检查
    if (app && app.globalData && app.globalData.isLoggedIn) {
      this.refreshUserData();
    }
  },

  loadUserProfile: function() {
    const userInfo = app.globalData.userInfo || storage.getUserInfo();
    if (!userInfo) {
      wx.showToast({ title: '请先登录后再使用此功能', icon: 'none' });
      wx.navigateBack();
      return;
    }

    this.setData({
      userId: userInfo.id,
      avatarUrl: userInfo.avatarUrl || '',
      nickName: userInfo.nickName || '',
      genderIndex: userInfo.gender !== undefined ? userInfo.gender : 0,
      phone: userInfo.phone || '',
      userIdentifier: userInfo.userIdentifier || '',
      createTime: userInfo.createTime || '',
      originalData: {
        avatarUrl: userInfo.avatarUrl || '',
        nickName: userInfo.nickName || '',
        gender: userInfo.gender || 0,
        phone: userInfo.phone || ''
      }
    });
  },

  async fetchUserProfile() {
    try {
      const isOnline = app && app.globalData ? app.globalData.isOnline !== false : true;
      if (!isOnline) {
        this.setData({ pageState: 'error', pageError: '当前网络不可用' });
        return;
      }

      this.setData({ pageState: 'loading', pageError: '' });
      const startAt = Date.now();
      const userInfo = await this.callWithRetry(() => api.userProfile.getInfo(), 2);
      const costMs = Date.now() - startAt;
      console.info('PERF profile_page_fetch_ms', costMs);

      if (userInfo && userInfo.id) {
        storage.setUserInfo(userInfo);
        app.globalData.userInfo = userInfo;

        this.setData({
          userId: userInfo.id,
          avatarUrl: userInfo.avatarUrl || '',
          nickName: userInfo.nickName || '',
          genderIndex: userInfo.gender !== undefined ? userInfo.gender : 0,
          phone: userInfo.phone || '',
          userIdentifier: userInfo.userIdentifier || '',
          createTime: userInfo.createTime || '',
          originalData: {
            avatarUrl: userInfo.avatarUrl || '',
            nickName: userInfo.nickName || '',
            gender: userInfo.gender || 0,
            phone: userInfo.phone || ''
          },
          pageState: 'success'
        });
      } else {
        this.setData({ pageState: 'error', pageError: '数据格式异常' });
      }
    } catch (e) {
      console.error('获取用户资料失败:', e);
      this.setData({ pageState: 'error', pageError: '网络连接异常' });
    }
  },

  refreshUserData: function() {
    const userInfo = app.globalData.userInfo;
    if (userInfo) {
      this.setData({
        avatarUrl: userInfo.avatarUrl || this.data.avatarUrl,
        nickName: userInfo.nickName || this.data.nickName,
        phone: userInfo.phone || this.data.phone,
        genderIndex: userInfo.gender !== undefined ? userInfo.gender : 0
      });
    }
  },

  scrollToPhoneSection: function() {
    setTimeout(() => {
      wx.pageScrollTo({
        selector: '.phone-section',
        duration: 300
      });
    }, 100);
  },

  onChooseAvatar: function(e) {
    const avatarUrl = e.detail.avatarUrl;
    if (!avatarUrl) return;
    this.setData({ avatarUrl, avatarLoading: true });
    this.uploadAvatar(avatarUrl);
  },

  async uploadAvatar(filePath) {
    const token = wx.getStorageSync('auth_token');
    if (!token) {
      this.setData({ avatarLoading: false });
      wx.showToast({ title: '登录已过期，请重新登录', icon: 'none' });
      return;
    }

    this.setData({ avatarUploading: true });
    const startAt = Date.now();
    try {
      const res = await new Promise((resolve, reject) => {
        wx.uploadFile({
          url: api.BASE_URL + '/v1/user/profile/avatar',
          filePath,
          name: 'file',
          header: {
            'Authorization': 'Bearer ' + token
          },
          timeout: 20000,
          success: resolve,
          fail: reject
        });
      });

      const costMs = Date.now() - startAt;
      console.info('PERF avatar_upload_ms', costMs);

      let body = null;
      if (res && res.data) {
        try {
          body = typeof res.data === 'string' ? JSON.parse(res.data) : res.data;
        } catch (e) {
          body = null;
        }
      }
      if (body && (body.code === 200 || body.code === 0) && body.data) {
        const userInfo = body.data;
        storage.setUserInfo(userInfo);
        app.globalData.userInfo = userInfo;

        this.setData({
          avatarUrl: userInfo.avatarUrl || this.data.avatarUrl,
          originalData: {
            ...this.data.originalData,
            avatarUrl: userInfo.avatarUrl || this.data.originalData.avatarUrl
          },
          avatarLoading: false,
          avatarUploading: false,
          hasChanges: this.calculateChanges && Object.keys(this.calculateChanges()).length > 0
        });

        wx.showToast({ title: '头像已更新', icon: 'success' });
      } else {
        throw new Error((body && body.message) || '上传失败');
      }
    } catch (e) {
      console.error('上传头像失败:', e);
      this.setData({ avatarLoading: false, avatarUploading: false });
      wx.showToast({ title: e.message || '上传失败', icon: 'none' });
    }
  },

  onNickNameInput: function(e) {
    this.setData({
      nickName: e.detail.value,
      hasChanges: true
    });
  },

  onGenderChange: function(e) {
    this.setData({
      genderIndex: parseInt(e.detail.value),
      hasChanges: true
    });
  },

  /**
   * 获取手机号（可选功能）
   * 手机号现在是可选的，用户可以选择性绑定
   */
  async onGetPhoneNumber(e) {
    console.log('getPhoneNumber:', e);
    
    // 检查用户是否授权
    if (e.detail.errMsg !== 'getPhoneNumber:ok') {
      wx.showToast({ 
        title: '已取消授权', 
        icon: 'none',
        duration: 2000
      });
      return;
    }

    try {
      wx.showLoading({ title: '绑定中...' });

      const userId = this.data.userId;
      const code = e.detail.code;

      // 调用后端接口
      const result = await new Promise((resolve, reject) => {
        wx.request({
          url: api.BASE_URL + '/v1/auth/bind-phone',
          method: 'POST',
          header: {
            'Authorization': 'Bearer ' + wx.getStorageSync('auth_token'),
            'Content-Type': 'application/json'
          },
          data: {
            phone: code // 直接传递code，后端会处理
          },
          success: (res) => {
            if (res.data && res.data.code === 200) {
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
        const phone = result.data.phone;
        this.setData({
          phone: phone,
          hasChanges: true
        });
        // 更新全局用户信息
        const app = getApp();
        if (app && app.globalData) {
          app.globalData.userInfo.phone = phone;
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
            // 用户点击重试，可以在这里添加引导逻辑
            console.log('用户选择重试');
          }
        }
      });
    }
  },

  saveProfile: function() {
    if (this.data.saving) return;

    const changes = this.calculateChanges();
    if (Object.keys(changes).length === 0) {
      wx.showToast({ title: '您当前没有修改任何信息', icon: 'none' });
      return;
    }

    this.setData({ saving: true });
    
    // 使用storage工具获取token，确保key正确
    const token = wx.getStorageSync('auth_token');
    
    if (!token) {
      this.setData({ saving: false });
      wx.showToast({ title: '登录已过期，请重新登录', icon: 'none' });
      return;
    }
    
    wx.showLoading({ title: '保存中...' });

    api.userProfile.update(changes).then((userInfo) => {
      wx.hideLoading();
      if (userInfo && userInfo.id) {
        storage.setUserInfo(userInfo);
        app.globalData.userInfo = userInfo;

        this.setData({
          originalData: {
            avatarUrl: userInfo.avatarUrl || this.data.avatarUrl,
            nickName: userInfo.nickName || this.data.nickName,
            gender: userInfo.gender !== undefined ? userInfo.gender : this.data.genderIndex,
            phone: userInfo.phone || this.data.phone
          },
          hasChanges: false,
          saving: false,
          hasSavedOnce: true,
        });

        wx.showToast({ title: '保存成功', icon: 'success' });
        setTimeout(() => {
          wx.navigateBack();
        }, 1500);
      } else {
        this.setData({ saving: false });
        wx.showToast({ title: '保存失败', icon: 'none' });
      }
    }).catch((err) => {
      this.setData({ saving: false });
      wx.hideLoading();
      console.error('Save profile error:', err);
      wx.showToast({ title: err.message || '网络错误，请重试', icon: 'none' });
    });
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

  calculateChanges: function() {
    const changes = {};
    const original = this.data.originalData;

    if (this.data.avatarUrl !== original.avatarUrl) {
      changes.avatarUrl = this.data.avatarUrl;
    }
    if (this.data.nickName !== original.nickName) {
      changes.nickName = this.data.nickName;
    }
    if (this.data.genderIndex !== original.gender) {
      changes.gender = this.data.genderIndex;
    }

    return changes;
  },

  onUnload: function() {
    // 只有当有未保存的修改且未成功保存过时才显示提示
    if (this.data.hasChanges && !this.data.hasSavedOnce) {
      wx.showModal({
        title: '提示',
        content: '有未保存的修改，是否离开？',
        success: (res) => {
          if (res.confirm) {
            wx.navigateBack();
          }
        }
      });
    }
  }
});
