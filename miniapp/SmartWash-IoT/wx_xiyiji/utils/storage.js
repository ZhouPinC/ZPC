// wx_xiyiji/utils/storage.js
// 本地存储封装

const config = require('../config/index.js');

/**
 * 存储Token
 */
const setToken = (token) => {
  wx.setStorageSync(config.TOKEN_KEY, token);
};

/**
 * 获取Token
 */
const getToken = () => {
  return wx.getStorageSync(config.TOKEN_KEY) || '';
};

/**
 * 清除Token
 */
const removeToken = () => {
  wx.removeStorageSync(config.TOKEN_KEY);
};

/**
 * 存储用户信息
 */
const setUserInfo = (userInfo) => {
  wx.setStorageSync(config.USER_KEY, userInfo);
  wx.setStorageSync(config.LOGIN_TIME_KEY, Date.now());
};

/**
 * 获取用户信息
 */
const getUserInfo = () => {
  return wx.getStorageSync(config.USER_KEY) || null;
};

/**
 * 清除用户信息
 */
const removeUserInfo = () => {
  // 清除新的JWT存储方式
  wx.removeStorageSync(config.USER_KEY);
  wx.removeStorageSync(config.LOGIN_TIME_KEY);
  
  // 清除旧的存储方式（兼容）
  wx.removeStorageSync('user');
  wx.removeStorageSync('loginTime');
  wx.removeStorageSync('boundDevice');
};

/**
 * 检查登录是否过期
 */
const isLoginExpired = () => {
  const loginTime = wx.getStorageSync(config.LOGIN_TIME_KEY);
  if (!loginTime) return true;
  
  const expireTime = config.TOKEN_EXPIRE_DAYS * 24 * 60 * 60 * 1000;
  return (Date.now() - loginTime) > expireTime;
};

/**
 * 清除所有登录信息
 */
const clearAuth = () => {
  removeToken();
  removeUserInfo();
};

module.exports = {
  setToken,
  getToken,
  removeToken,
  setUserInfo,
  getUserInfo,
  removeUserInfo,
  isLoginExpired,
  clearAuth,
};
