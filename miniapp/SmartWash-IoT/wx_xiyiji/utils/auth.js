// wx_xiyiji/utils/auth.js
// 认证工具

const storage = require('./storage.js');
const config = require('../config/index.js');

/**
 * 检查是否已登录
 */
const isLoggedIn = () => {
  // 新的JWT登录方式
  const token = storage.getToken();
  const userInfo = storage.getUserInfo();
  if (token && userInfo && !storage.isLoginExpired()) {
    return true;
  }
  
  // 兼容旧的登录方式（使用'user'存储key）
  const legacyUser = wx.getStorageSync('user');
  if (legacyUser && legacyUser.id) {
    return true;
  }
  
  // 检查全局状态
  const app = getApp();
  if (app && app.globalData && app.globalData.userInfo && app.globalData.userInfo.id) {
    return true;
  }
  
  return false;
};

/**
 * 检查是否是管理员
 */
const isAdmin = () => {
  // 新的JWT方式
  const userInfo = storage.getUserInfo();
  if (userInfo && (userInfo.role === 'ADMIN' || userInfo.role === 'SUPER_ADMIN')) {
    return true;
  }
  
  // 兼容旧的登录方式
  const legacyUser = wx.getStorageSync('user');
  if (legacyUser && (legacyUser.role === 'ADMIN' || legacyUser.role === 'SUPER_ADMIN')) {
    return true;
  }
  
  // 检查全局状态
  const app = getApp();
  if (app && app.globalData) {
    const globalUser = app.globalData.userInfo;
    if (globalUser && (globalUser.role === 'ADMIN' || globalUser.role === 'SUPER_ADMIN')) {
      return true;
    }
    // 检查viewMode
    if (app.globalData.viewMode === 'admin') {
      return true;
    }
  }
  
  return false;
};

/**
 * 获取当前用户角色
 */
const getRole = () => {
  const userInfo = storage.getUserInfo();
  return userInfo ? userInfo.role : 'CONSUMER';
};

/**
 * 获取当前用户ID
 */
const getUserId = () => {
  const userInfo = storage.getUserInfo();
  return userInfo ? userInfo.id : null;
};

/**
 * 微信登录获取code
 */
const wxLogin = () => {
  return new Promise((resolve, reject) => {
    wx.login({
      success: (res) => {
        if (res.code) {
          resolve(res.code);
        } else {
          reject(new Error('获取登录code失败'));
        }
      },
      fail: reject,
    });
  });
};

/**
 * 获取用户信息（需要用户授权）
 */
const getUserProfile = () => {
  return new Promise((resolve, reject) => {
    wx.getUserProfile({
      desc: '用于完善用户资料',
      success: (res) => {
        resolve(res.userInfo);
      },
      fail: reject,
    });
  });
};

/**
 * 登录成功后保存信息
 */
const saveLoginInfo = (token, userInfo) => {
  storage.setToken(token);
  storage.setUserInfo(userInfo);
  
  // 更新全局状态
  const app = getApp();
  if (app) {
    app.globalData.token = token;
    app.globalData.userInfo = userInfo;
    app.globalData.isLoggedIn = true;
  }
};

/**
 * 退出登录
 */
const logout = () => {
  storage.clearAuth();
  
  // 清除全局状态
  const app = getApp();
  if (app) {
    app.globalData.token = null;
    app.globalData.userInfo = null;
    app.globalData.isLoggedIn = false;
    app.globalData.viewMode = 'consumer';
  }
};

/**
 * 需要登录的页面守卫
 */
const requireLogin = (callback) => {
  if (isLoggedIn()) {
    callback && callback();
  } else {
    wx.showModal({
      title: '提示',
      content: '请先登录',
      confirmText: '去登录',
      success: (res) => {
        if (res.confirm) {
          wx.navigateTo({ url: '/pages/common/login/login' });
        }
      },
    });
  }
};

/**
 * 需要管理员权限的页面守卫
 */
const requireAdmin = (callback) => {
  if (!isLoggedIn()) {
    wx.showModal({
      title: '提示',
      content: '请先登录',
      confirmText: '去登录',
      success: (res) => {
        if (res.confirm) {
          wx.navigateTo({ url: '/pages/common/login/login' });
        }
      },
    });
    return;
  }
  
  if (!isAdmin()) {
    wx.showToast({ title: '需要管理员权限', icon: 'none' });
    return;
  }
  
  callback && callback();
};

module.exports = {
  isLoggedIn,
  isAdmin,
  getRole,
  getUserId,
  wxLogin,
  getUserProfile,
  saveLoginInfo,
  logout,
  requireLogin,
  requireAdmin,
};
