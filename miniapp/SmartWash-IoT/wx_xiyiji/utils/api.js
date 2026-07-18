const BASE_URL = 'http://localhost:8080/api';

const storage = require('./storage.js');

let reloginPromise = null;

const silentRelogin = () => {
  if (reloginPromise) return reloginPromise;

  reloginPromise = new Promise((resolve, reject) => {
    wx.login({
      success: (res) => {
        if (!res || !res.code) {
          reloginPromise = null;
          reject({ code: 401, message: '获取登录凭证失败' });
          return;
        }

        request('/v1/auth/wx-login', 'POST', { code: res.code, nickName: '', avatarUrl: '' }, true, { skipRelogin: true, hasRetried: true })
          .then((loginResult) => {
            if (loginResult && loginResult.token && loginResult.userInfo) {
              storage.setToken(loginResult.token);
              storage.setUserInfo(loginResult.userInfo);
              const app = getApp && getApp();
              if (app && app.globalData) {
                app.globalData.token = loginResult.token;
                app.globalData.userInfo = loginResult.userInfo;
                app.globalData.isLoggedIn = true;
              }
              reloginPromise = null;
              resolve(true);
              return;
            }

            reloginPromise = null;
            reject({ code: 401, message: '登录返回数据异常' });
          })
          .catch((err) => {
            reloginPromise = null;
            reject(err);
          });
      },
      fail: (err) => {
        reloginPromise = null;
        reject(err);
      }
    });
  });

  return reloginPromise;
};

/**
 * 通用请求方法
 */
const request = (url, method, data, useJson = false, meta) => {
  return new Promise((resolve, reject) => {
    const hasRetried = meta && meta.hasRetried;
    const skipRelogin = meta && meta.skipRelogin;
    const token = storage.getToken();
    const headers = {
      'content-type': useJson ? 'application/json' : 'application/x-www-form-urlencoded'
    };
    if (token) {
      headers['Authorization'] = 'Bearer ' + token;
    }
    
    wx.request({
      url: BASE_URL + url,
      method: method,
      data: data,
      header: headers,
      timeout: 15000,
      success: (res) => {
        if (res.statusCode === 200) {
          // 处理 ApiResponse 包装
          if (res.data && res.data.code !== undefined) {
            if (res.data.code === 200 || res.data.code === 0) {
              resolve(res.data.data);
            } else {
              console.error('API业务错误:', res.data);
              reject(res.data);
            }
          } else {
            resolve(res.data);
          }
        } else if (res.statusCode === 401) {
          const message = (res.data && res.data.message) ? res.data.message : '未授权';

          if (!skipRelogin && !hasRetried && message !== '权限不足' && url.indexOf('/v1/auth/wx-login') !== 0) {
            silentRelogin()
              .then(() => request(url, method, data, useJson, { hasRetried: true }))
              .then(resolve)
              .catch(() => {
                storage.clearAuth();
                const app = getApp && getApp();
                if (app && app.globalData) {
                  app.globalData.token = null;
                  app.globalData.userInfo = null;
                  app.globalData.isLoggedIn = false;
                  app.globalData.viewMode = 'consumer';
                }
                reject({ code: 401, message: '登录已过期' });
              });
            return;
          }

          if (message !== '权限不足') {
            storage.clearAuth();
            const app = getApp && getApp();
            if (app && app.globalData) {
              app.globalData.token = null;
              app.globalData.userInfo = null;
              app.globalData.isLoggedIn = false;
              app.globalData.viewMode = 'consumer';
            }
          }

          reject({ code: 401, message: message });
        } else {
          console.error('API请求失败:', res);
          reject(res);
        }
      },
      fail: (err) => {
        console.error('网络请求失败:', err);
        reject(err);
      }
    });
  });
};

module.exports = {
  BASE_URL,
  
  // 旧版接口（兼容）
  getDeviceList: () => request('/devices/list', 'GET', {}),
  createOrder: (userId, deviceSn) => request('/orders/create', 'POST', { userId, deviceSn }),
  mockPay: (orderNo) => request('/orders/pay/mock', 'POST', { orderNo }),
  debugCommand: (deviceSn, command) => request('/devices/control', 'POST', { deviceSn, command }),
  getWashHistory: (userId) => request('/user/history?userId=' + userId, 'GET', {}),
  
  // 用户信息接口
  getUserInfo: (userId) => request('/user/info?userId=' + userId, 'GET', {}),
  wxLogin: (code, nickName, avatarUrl) => request('/user/wxLogin', 'POST', { code, nickName, avatarUrl }),
  
  // 健康检查
  healthCheck: () => request('/health', 'GET', {}),
  
  // 设备控制接口（完整工作流）
  startDevice: (deviceSn, washMode, washModeName, duration, userId, orderNo, paymentMethod) => {
    return request('/devices/start', 'POST', {
      deviceSn: deviceSn,
      washMode: washMode,
      washModeName: washModeName,
      duration: duration,
      userId: userId,
      orderNo: orderNo,
      paymentMethod: paymentMethod || 'balance'
    });
  },
  
  pauseDevice: (deviceSn) => request('/devices/pause', 'POST', { deviceSn: deviceSn }),
  
  continueDevice: (deviceSn) => request('/devices/continue', 'POST', { deviceSn: deviceSn }),
  
  endDevice: (deviceSn) => request('/devices/end', 'POST', { deviceSn: deviceSn }),
  
  pickupDevice: (deviceSn) => request('/devices/pickup', 'POST', { deviceSn: deviceSn }),
  
  resetDevice: (deviceSn) => request('/devices/reset', 'POST', { deviceSn: deviceSn }),
  
  getDeviceStatus: (deviceSn) => request('/devices/' + deviceSn + '/status', 'GET', {}),
  
  // 用户相关
  user: {
    getInfo: (userId) => request('/user/info?userId=' + userId, 'GET', {}),
    getHistory: (page, size) => request('/user/history?page=' + (page || 0) + '&size=' + (size || 20), 'GET', {}),
  },
  
  // 认证相关 - OpenID登录
  auth: {
    wxLogin: (code, userInfo) => request('/v1/auth/wx-login', 'POST', { code, ...userInfo }, true),
    bindPhone: (encryptedData, iv) => request('/v1/auth/bind-phone', 'POST', { encryptedData, iv }, true),
    enterAdmin: (password) => request('/v1/auth/admin/verify', 'POST', { password: password }, true),
    exitAdmin: () => request('/v1/auth/admin/exit', 'POST', {}, true),
  },
  
  // 用户资料相关
  userProfile: {
    update: (data) => request('/v1/user/profile', 'POST', data, true),
    getInfo: () => request('/v1/user/profile', 'GET', {}),
  },
  
  // 消费者设备相关
  consumer: {
    getDevices: () => request('/v1/consumer/devices', 'GET', {}),
    getDeviceStatus: (deviceSn) => request('/v1/consumer/devices/' + deviceSn + '/status', 'GET', {}),
    bindDevice: (data) => request('/v1/consumer/devices/binding', 'POST', data, true),
    startDevice: (deviceSn, data) => request('/v1/consumer/devices/' + deviceSn + '/start', 'POST', data, true),
    endDevice: (deviceSn) => request('/v1/consumer/devices/' + deviceSn + '/end', 'POST', {}),
  },
  
  // 消费者订单相关
  consumerOrder: {
    create: (data) => request('/v1/consumer/orders', 'POST', data, true),
    pay: (orderNo, paymentMethod) => request('/v1/consumer/orders/' + orderNo + '/pay', 'POST', { paymentMethod }),
    getDetail: (orderNo) => request('/v1/consumer/orders/' + orderNo, 'GET', {}),
  },
  
  // 管理员相关
  admin: {
    getOverview: () => request('/v1/admin/statistics/overview', 'GET', {}),
    getDevices: () => request('/v1/admin/devices', 'GET', {}),
    addDevice: (data) => request('/v1/admin/devices', 'POST', data, true),
    updateDevice: (deviceId, data) => request('/v1/admin/devices/' + deviceId, 'PUT', data, true),
    deleteDevice: (deviceId) => request('/v1/admin/devices/' + deviceId, 'DELETE', {}),
    getFinanceRecords: (params) => {
      const query = '?page=' + (params.page || 0) + '&size=' + (params.size || 20);
      return request('/v1/admin/finance/records' + query, 'GET', {});
    },
    getFinanceStats: () => request('/v1/admin/finance/summary', 'GET', {}),
    getFinanceSummary: () => request('/v1/admin/finance/summary', 'GET', {}),
    getUsers: (params) => {
      const query = '?page=' + ((params && params.page) || 0) + '&size=' + ((params && params.size) || 20);
      return request('/v1/admin/users' + query, 'GET', {});
    },
    getOrders: (params) => {
      const query = '?page=' + ((params && params.page) || 0) + '&size=' + ((params && params.size) || 20);
      return request('/v1/admin/orders/history' + query, 'GET', {});
    },
  },
  
  // 用户-设备绑定相关
  getBindDevices: (userId) => request('/user/devices?userId=' + userId, 'GET', {}),
  bindDevice: (userId, deviceSn) => request('/user/devices/bind', 'POST', { userId: userId, deviceSn: deviceSn }),
  unbindDevice: (userId, deviceId) => request('/user/devices/unbind', 'POST', { userId: userId, deviceId: deviceId }),
  
  // 用户资料相关
  updateProfile: (userId, nickName, avatarUrl) => request('/user/profile', 'POST', { userId: userId, nickName: nickName, avatarUrl: avatarUrl }),
  bindPhone: (userId, code) => request('/user/bindPhone', 'POST', { userId: userId, code: code }),
};
