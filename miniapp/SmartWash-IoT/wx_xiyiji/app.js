// app.js
// 智洗 IoT 小程序 - 全局状态管理

const storage = require('./utils/storage.js');
const config = require('./config/index.js');
const api = require('./utils/api.js');

App({
  onLaunch() {
    console.log('智洗 IoT 小程序启动');

    // 初始化登录状态
    this.initLoginState();

    // 监听网络状态
    this.monitorNetwork();
  },

  onShow() {
    // 每次切回前台检查一次网络
    this.checkNetworkStatus();
  },

  onHide() { },

  onError(error) {
    console.error('小程序错误:', error);
  },

  /**
   * 监听网络状态
   */
  monitorNetwork() {
    wx.onNetworkStatusChange((res) => {
      console.log('网络状态改变:', res.isConnected);
      this.globalData.isOnline = res.isConnected;

      if (!res.isConnected) {
        this.startReconnectionLoop();
        // 通知当前页面
        const pages = getCurrentPages();
        const currentPage = pages[pages.length - 1];
        if (currentPage && currentPage.onNetworkStatusChange) {
          currentPage.onNetworkStatusChange(false);
        }
      } else {
        this.stopReconnectionLoop();
        this.initLoginState(); // 重新连接后恢复状态
        // 通知当前页面
        const pages = getCurrentPages();
        const currentPage = pages[pages.length - 1];
        if (currentPage && currentPage.onNetworkStatusChange) {
          currentPage.onNetworkStatusChange(true);
        }
      }
    });

    // 初始检查
    this.checkNetworkStatus();
  },

  /**
   * 检查网络状态
   */
  checkNetworkStatus() {
    wx.getNetworkType({
      success: (res) => {
        const isConnected = res.networkType !== 'none';
        this.globalData.isOnline = isConnected;
        if (!isConnected) {
          this.startReconnectionLoop();
        } else {
          this.stopReconnectionLoop();
        }
      }
    });
  },

  /**
   * 开始重连循环（每30秒）
   */
  startReconnectionLoop() {
    if (this.reconnectTimer) return;

    console.log('启动自动重连机制');
    this.reconnectTimer = setInterval(() => {
      console.log('尝试重新连接...');
      this.checkNetworkStatus();
    }, 30000);
  },

  /**
   * 停止重连循环
   */
  stopReconnectionLoop() {
    if (this.reconnectTimer) {
      clearInterval(this.reconnectTimer);
      this.reconnectTimer = null;
      console.log('网络已恢复，停止重连循环');
    }
  },

  /**
   * 初始化登录状态
   */
  initLoginState() {
    const token = storage.getToken();
    const userInfo = storage.getUserInfo();
    if (token && userInfo && !storage.isLoginExpired()) {
      this.globalData.token = token;
      this.globalData.userInfo = userInfo;
      this.globalData.isLoggedIn = true;
      console.log('自动恢复登录:', userInfo.nickname);
      
      // 校验 token 有效性
      api.userProfile.getInfo().catch(err => {
         console.log('Token 校验失败:', err);
         this.logout();
      });
    } else {
      storage.clearAuth();
      this.globalData.token = null;
      this.globalData.userInfo = null;
      this.globalData.isLoggedIn = false;
      console.log('需要重新登录');
    }
  },

  /**
   * 通知所有页面登录状态变化
   */
  notifyLoginStateChange() {
    const pages = getCurrentPages();
    const loginState = {
      isLoggedIn: this.globalData.isLoggedIn,
      userInfo: this.globalData.userInfo,
      token: this.globalData.token
    };
    
    pages.forEach(page => {
      if (page && typeof page.onLoginStateChange === 'function') {
        page.onLoginStateChange(loginState);
      }
    });
  },

  /**
   * 切换视图模式
   * @param {string} mode - 'consumer' | 'admin'
   */
  switchViewMode(mode) {
    if (mode !== 'consumer' && mode !== 'admin') {
      console.error('无效的视图模式:', mode);
      return;
    }

    this.globalData.viewMode = mode;
    console.log('切换视图模式:', mode);

    // 触发页面刷新事件
    const pages = getCurrentPages();
    const currentPage = pages[pages.length - 1];
    if (currentPage && currentPage.onViewModeChange) {
      currentPage.onViewModeChange(mode);
    }
  },

  /**
   * 检查登录状态
   * @returns {Promise<boolean>}
   */
  checkLogin() {
    return new Promise((resolve) => {
      if (this.globalData.isLoggedIn && this.globalData.token) {
        resolve(true);
      } else {
        resolve(false);
      }
    });
  },

  /**
   * 检查管理权限
   * @returns {boolean}
   */
  checkAdminPermission() {
    const userInfo = this.globalData.userInfo;
    return userInfo && (userInfo.role === 'ADMIN' || userInfo.role === 'SUPER_ADMIN');
  },

  /**
   * 更新用户信息
   * @param {Object} userInfo
   */
  updateUserInfo(userInfo) {
    this.globalData.userInfo = userInfo;
    storage.setUserInfo(userInfo);
  },

  /**
   * 更新Token
   * @param {string} token
   */
  updateToken(token) {
    this.globalData.token = token;
    storage.setToken(token);
  },

  /**
   * 退出登录
   */
  logout() {
    storage.clearAuth();
    this.globalData.token = null;
    this.globalData.userInfo = null;
    this.globalData.isLoggedIn = false;
    this.globalData.viewMode = 'consumer';
    this.globalData.currentDevice = null;
  },

  // 全局数据
  globalData: {
    // 用户信息
    userInfo: null,
    token: null,
    isLoggedIn: false,

    // 当前视图模式: 'consumer' | 'admin'
    viewMode: 'consumer',

    // 当前操作的设备
    currentDevice: null,

    // 设备列表缓存
    deviceList: [],

    // 系统配置
    config: {
      apiBaseUrl: config.apiBaseUrl,
      refreshInterval: config.refreshInterval,
    },

    // 网络状态
    isOnline: true
  }
});
