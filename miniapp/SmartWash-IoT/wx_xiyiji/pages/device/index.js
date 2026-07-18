const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');
const storage = require('../../utils/storage.js');

Page({
  data: {
    // 设备状态相关
    device: { status: 'IDLE' },
    statusText: '空闲中',
    remainingTime: 30,
    remainingMinutes: 30,

    // 系统健康状态
    systemHealthy: true,
    refreshing: false,

    // 洗衣模式
    washModes: {
      standard: { name: '标准洗', duration: 30, price: 3.00 },
      quick: { name: '快洗', duration: 15, price: 2.00 },
      dehydration: { name: '脱水', duration: 10, price: 1.50 }
    },
    selectedMode: 'standard',
    estimatedTime: 30,
    currentModeName: '标准洗',

    // 工作状态
    workStartTime: null,
    estimatedEndTime: null,
    paused: false,

    // UI控制
    loading: false,
    timer: null,
    showPaymentModal: false,
    showLoginModal: false,
    isLoggedIn: false,
    showUnlinkedModal: false,
    paymentAmount: '3.00',

    // 设备信息
    currentSn: null, // 初始为空，如果没有绑定设备
    deviceList: [], // 设备列表

    // 当前订单
    currentOrderNo: null
  },

  onLoad(options) {
    // 检查是否有扫码参数
    if (options.q) {
      const q = decodeURIComponent(options.q);
      const params = this.parseQuery(q);
      if (params.deviceSn) {
        this.autoBindDevice(params.deviceSn);
        return;
      }
    }

    if (options.deviceSn) {
      this.autoBindDevice(options.deviceSn);
    }

    // 测试环境自动绑定
    const user = storage.getUserInfo() || wx.getStorageSync('user');
    if (user && user.nickName === '测试用户' && !this.data.currentSn) {
      this.autoBindDevice('WASH_001');
    }
  },

  onShow() {
    // 检查登录并获取设备列表
    this.checkLoginAndDevices();

    const selectedMode = this.data.selectedMode;
    const modeInfo = this.data.washModes[selectedMode];

    // 只有在设备空闲时才更新时间，避免覆盖正在运行设备的剩余时间
    if (this.data.device.status === 'IDLE') {
      this.setData({
        estimatedTime: modeInfo.duration,
        remainingTime: modeInfo.duration,
        currentModeName: modeInfo.name,
        paymentAmount: modeInfo.price.toFixed(2)
      });
    } else {
      // 非空闲状态只更新模式名称和金额，不更新时间
      this.setData({
        estimatedTime: modeInfo.duration,
        currentModeName: modeInfo.name,
        paymentAmount: modeInfo.price.toFixed(2)
      });
    }

    // 启动定时刷新
    this.startRefreshTimer();
  },

  onHide() {
    this.stopRefreshTimer();
  },

  /**
   * 启动定时刷新
   */
  startRefreshTimer() {
    if (this.data.timer) clearInterval(this.data.timer);
    this.refreshStatus();
    this.data.timer = setInterval(() => {
      if (this.data.currentSn) {
        this.refreshStatus();
      }
    }, 3000);
  },

  /**
   * 停止定时刷新
   */
  stopRefreshTimer() {
    if (this.data.timer) {
      clearInterval(this.data.timer);
      this.data.timer = null;
    }
  },

  /**
   * 解析URL参数
   */
  parseQuery(url) {
    const res = {};
    const queryStr = url.split('?')[1];
    if (queryStr) {
      const parts = queryStr.split('&');
      parts.forEach(part => {
        const [key, val] = part.split('=');
        res[key] = val;
      });
    }
    return res;
  },

  /**
   * 自动绑定设备
   */
  async autoBindDevice(deviceSn) {
    if (!auth.isLoggedIn()) {
      wx.navigateTo({ url: '/pages/common/login/login' });
      return;
    }

    try {
      wx.showLoading({ title: '绑定设备...' });
      await api.consumer.bindDevice({ deviceSn: deviceSn });
      wx.hideLoading();

      wx.setStorageSync('currentDeviceSn', deviceSn);
      wx.showToast({ title: '绑定成功', icon: 'success' });
      this.checkLoginAndDevices();
    } catch (e) {
      wx.hideLoading();
      console.error(e);
      if (e && e.code === 401) {
        wx.showModal({
          title: '需要登录',
          content: e.message || '登录状态已失效，请重新登录',
          showCancel: false,
          success: () => {
            wx.navigateTo({ url: '/pages/common/login/login' });
          }
        });
        return;
      }
      wx.showToast({ title: e && e.message ? e.message : '绑定失败', icon: 'none' });
    }
  },

  /**
   * 检查登录并获取设备
   */
  async checkLoginAndDevices() {
    const app = getApp();
    const loggedIn = auth.isLoggedIn();
    this.setData({ isLoggedIn: loggedIn });

    if (loggedIn && app && app.globalData && (!app.globalData.userInfo || !app.globalData.userInfo.id)) {
      const userInfo = storage.getUserInfo() || wx.getStorageSync('user');
      if (userInfo && userInfo.id) {
        app.globalData.userInfo = userInfo;
        app.globalData.isLoggedIn = true;
      }
    }
    
    if (loggedIn) {
      try {
        this.setData({ showLoginModal: false });
        const list = await api.consumer.getDevices();
        this.setData({ deviceList: list || [] });

        if (list && list.length > 0) {
          this.setData({ showUnlinkedModal: false });

          const preferred = wx.getStorageSync('currentDeviceSn');
          if (preferred && list.find(d => d.deviceSn === preferred)) {
            this.setData({ currentSn: preferred });
            wx.removeStorageSync('currentDeviceSn');
            this.refreshStatus();
            return;
          }

          if (!this.data.currentSn || !list.find(d => d.deviceSn === this.data.currentSn)) {
            this.setData({ currentSn: list[0].deviceSn });
            this.refreshStatus();
          }
        } else {
          this.setData({ currentSn: null, showUnlinkedModal: true });
        }
      } catch (e) {
        console.error('获取设备列表失败', e);
        if (e && e.code === 401) {
          if (e.message === '权限不足') {
            wx.showToast({ title: '权限不足', icon: 'none' });
            return;
          }
          wx.showModal({
            title: '需要登录',
            content: e.message || '登录状态已失效，请重新登录',
            showCancel: false,
            success: () => {
              wx.navigateTo({ url: '/pages/common/login/login' });
            }
          });
        }
      }
    } else {
      this.setData({ currentSn: null, deviceList: [], showLoginModal: false, showUnlinkedModal: false });
    }
  },

  /**
   * 选择设备
   */
  selectDevice(e) {
    const sn = e.currentTarget.dataset.sn;
    if (sn && sn !== this.data.currentSn) {
      this.setData({
        currentSn: sn,
        statusText: '加载中...',
        device: { status: 'IDLE' } // 重置显示状态避免闪烁
      });
      this.refreshStatus();
    }
  },

  /**
   * 扫码绑定
   */
  handleScan() {
    wx.scanCode({
      success: (res) => {
        console.log('扫码结果:', res);
        let deviceSn = '';

        if (res.path) {
          const match = res.path.match(/deviceSn=([^&]+)/);
          if (match) deviceSn = decodeURIComponent(match[1]);
        }

        if (!deviceSn && res.result) {
          deviceSn = this.parseQRCode(res.result);
        }

        if (!deviceSn) {
          wx.showToast({ title: '无效的二维码', icon: 'none' });
          return;
        }

        auth.requireLogin(() => {
          this.autoBindDevice(deviceSn);
        });
      }
    });
  },

  parseQRCode(content) {
    if (!content) return '';
    const text = (content || '').trim();

    if (text.startsWith('WASH_')) {
      return text;
    }

    try {
      if (text.includes('deviceSn=')) {
        const match = text.match(/deviceSn=([^&]+)/);
        if (match && match[1]) {
          return decodeURIComponent(match[1]);
        }
      }

      if (text.includes('sn=')) {
        const match = text.match(/sn=([^&]+)/);
        if (match && match[1]) {
          return decodeURIComponent(match[1]);
        }
      }

      if (text.startsWith('{')) {
        const json = JSON.parse(text);
        const sn = json.deviceSn || json.sn || json.device_sn;
        return sn ? String(sn).trim() : '';
      }
    } catch (e) {
      console.error('解析二维码失败:', e);
    }

    return '';
  },

  /**
   * 手动刷新
   */
  handleRefresh() {
    if (this.data.refreshing) return;

    this.setData({ refreshing: true });
    this.refreshStatus().then(() => {
      this.setData({ refreshing: false });
    }).catch(() => {
      this.setData({ refreshing: false });
    });
  },

  /**
   * 重新连接
   */
  async handleRetryConnect() {
    if (this.data.refreshing) return;

    this.setData({ refreshing: true });
    wx.showLoading({ title: '正在连接...' });

    try {
      await this.refreshStatus();
      if (this.data.systemHealthy) {
        wx.showToast({ title: '已重新连接', icon: 'success' });
      } else {
        wx.showToast({ title: '连接失败，请重试', icon: 'none' });
      }
    } catch (e) {
      console.error('重新连接失败:', e);
      wx.showToast({ title: '连接失败，请重试', icon: 'none' });
    } finally {
      wx.hideLoading();
      this.setData({ refreshing: false });
    }
  },

  /**
   * 从服务器获取设备状态
   */
  async refreshStatus() {
    try {
      const list = await api.getDeviceList();

      if (!list || !Array.isArray(list)) {
        this.setData({ systemHealthy: false, statusText: '网络连接异常，请点击刷新重试' });
        return;
      }

      this.setData({ systemHealthy: true });

      const target = list.find(d => d.deviceSn === this.data.currentSn);

      if (target) {
        let text = '空闲中';
        let remainingTime = this.data.estimatedTime;
        let estimatedEndTime = null;

        if (target.status === 'RUNNING') {
          text = (target.washModeName || this.data.currentModeName) + '中';

          // 使用服务器返回的剩余时间
          if (target.remainSeconds !== null && target.remainSeconds !== undefined) {
            remainingTime = Math.ceil(target.remainSeconds / 60);
          }
          if (target.estimatedEndTime) {
            estimatedEndTime = this.formatEndTime(target.estimatedEndTime);
          }

          this.setData({
            device: target,
            statusText: text,
            remainingTime: remainingTime,
            remainingMinutes: remainingTime,
            estimatedEndTime: estimatedEndTime,
            currentModeName: target.washModeName || this.data.currentModeName,
            currentOrderNo: target.currentOrderNo
          });
        } else if (target.status === 'STARTING') {
          text = '启动中';
          if (target.remainSeconds !== null && target.remainSeconds !== undefined) {
            remainingTime = Math.ceil(target.remainSeconds / 60);
          }
          if (target.estimatedEndTime) {
            estimatedEndTime = this.formatEndTime(target.estimatedEndTime);
          }
          this.setData({
            device: target,
            statusText: text,
            remainingTime: remainingTime,
            remainingMinutes: remainingTime,
            estimatedEndTime: estimatedEndTime,
            currentModeName: target.washModeName || this.data.currentModeName,
            currentOrderNo: target.currentOrderNo
          });
        } else if (target.status === 'PAUSED') {
          if (target.remainSeconds !== null && target.remainSeconds !== undefined) {
            remainingTime = Math.ceil(target.remainSeconds / 60);
          }
          this.setData({
            device: target,
            statusText: '已暂停',
            remainingTime: remainingTime,
            remainingMinutes: remainingTime,
            paused: true,
            currentOrderNo: target.currentOrderNo
          });
        } else if (target.status === 'FINISHED') {
          this.setData({
            device: target,
            statusText: '洗衣完成',
            remainingTime: 0,
            remainingMinutes: 0,
            estimatedEndTime: null,
            paused: false,
            currentOrderNo: target.currentOrderNo
          });
        } else if (target.status === 'OFFLINE') {
          this.setData({
            device: target,
            statusText: '设备离线',
            remainingTime: this.data.estimatedTime,
            estimatedEndTime: null,
            paused: false,
            currentOrderNo: target.currentOrderNo
          });
        } else if (target.status === 'FAULT') {
          this.setData({
            device: target,
            statusText: '设备故障',
            remainingTime: this.data.estimatedTime,
            estimatedEndTime: null,
            paused: false,
            currentOrderNo: target.currentOrderNo
          });
        } else {
          // IDLE 状态
          this.setData({
            device: target,
            statusText: '空闲中',
            remainingTime: this.data.estimatedTime,
            estimatedEndTime: null,
            paused: false,
            currentOrderNo: null
          });
        }
      } else {
        this.setData({
          device: { status: 'IDLE', deviceSn: this.data.currentSn },
          statusText: '空闲中',
          remainingTime: this.data.estimatedTime,
          currentOrderNo: null
        });
      }
    } catch (e) {
      console.error('刷新状态失败:', e);
      this.setData({
        systemHealthy: false,
        statusText: '网络连接异常，请点击刷新重试'
      });
    }
  },

  /**
   * 格式化结束时间
   */
  formatEndTime(dateStr) {
    if (!dateStr) return null;
    try {
      const date = new Date(dateStr);
      const hours = date.getHours().toString().padStart(2, '0');
      const minutes = date.getMinutes().toString().padStart(2, '0');
      return hours + ':' + minutes;
    } catch (e) {
      return null;
    }
  },

  /**
   * 启动按钮 - 显示支付弹窗
   */
  handleStart() {
    if (this.data.device.status !== 'IDLE') {
      return;
    }

    // 检查登录状态
    const app = getApp();
    if (!auth.isLoggedIn() || !app || !app.globalData || !app.globalData.userInfo) {
      this.setData({ showLoginModal: true });
      return;
    }

    if (!this.data.currentSn) {
      this.setData({ showUnlinkedModal: true });
      return;
    }

    // 显示支付弹窗
    const modeInfo = this.data.washModes[this.data.selectedMode];
    this.setData({
      showPaymentModal: true,
      paymentAmount: modeInfo.price.toFixed(2)
    });
  },

  /**
   * 处理支付 - 完整工作流
   */
  async handlePayment(e) {
    const paymentMethod = e.currentTarget.dataset.method;
    this.setData({ showPaymentModal: false, loading: true });

    try {
      const app = getApp();
      if (!auth.isLoggedIn() || !app || !app.globalData || !app.globalData.userInfo) {
        throw new Error('请先登录');
      }
      const user = app.globalData.userInfo;

      wx.showLoading({ title: '创建订单...' });

      // Step 1: 创建订单
      const order = await api.createOrder(user.id, this.data.currentSn);
      console.log('订单创建成功:', order);

      wx.showLoading({ title: '支付中...' });

      // Step 2: 模拟支付
      await api.mockPay(order.orderNo);
      console.log('支付成功');

      wx.showLoading({ title: '启动设备...' });

      // Step 3: 启动设备（发送完整信息到服务器）
      const modeInfo = this.data.washModes[this.data.selectedMode];
      const startResult = await api.startDevice(
        this.data.currentSn,
        this.data.selectedMode,
        modeInfo.name,
        modeInfo.duration,
        user.id,
        order.orderNo,
        paymentMethod
      );

      console.log('设备启动结果:', startResult);

      wx.hideLoading();

      if (startResult && startResult.success) {
        wx.showToast({ title: '启动成功', icon: 'success' });

        this.setData({
          device: { ...this.data.device, status: 'STARTING' },
          statusText: '启动中',
          currentOrderNo: order.orderNo,
          estimatedEndTime: this.formatEndTime(startResult.estimatedEndTime),
          loading: false
        });

        setTimeout(() => {
          this.refreshStatus();
        }, 500);
      } else {
        throw new Error(startResult ? startResult.message : '启动失败');
      }

    } catch (e) {
      wx.hideLoading();
      console.error('操作失败:', e);
      wx.showToast({ title: e.message || '操作失败', icon: 'none' });
      this.setData({ loading: false });
    }
  },

  /**
   * 模式选择
   */
  selectMode(e) {
    if (this.data.device.status !== 'IDLE') {
      return;
    }

    const mode = e.currentTarget.dataset.mode;
    const modeInfo = this.data.washModes[mode];

    this.setData({
      selectedMode: mode,
      estimatedTime: modeInfo.duration,
      currentModeName: modeInfo.name,
      remainingTime: modeInfo.duration,
      paymentAmount: modeInfo.price.toFixed(2)
    });
  },

  /**
   * 暂停
   */
  async handlePause() {
    if (this.data.device.status !== 'RUNNING') return;

    try {
      this.setData({ loading: true });
      wx.showLoading({ title: '暂停中...' });

      const result = await api.pauseDevice(this.data.currentSn);

      wx.hideLoading();

      if (result && result.success) {
        this.setData({
          paused: true,
          device: { ...this.data.device, status: 'PAUSED' },
          statusText: '已暂停',
          remainingMinutes: Math.ceil((result.remainSeconds || 0) / 60)
        });
        wx.showToast({ title: '暂停成功', icon: 'success' });
      } else {
        throw new Error(result ? result.message : '暂停失败');
      }
    } catch (e) {
      wx.hideLoading();
      wx.showToast({ title: e.message || '暂停失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  /**
   * 继续
   */
  async handleContinue() {
    if (this.data.device.status !== 'PAUSED') return;

    try {
      this.setData({ loading: true });
      wx.showLoading({ title: '继续中...' });

      const result = await api.continueDevice(this.data.currentSn);

      wx.hideLoading();

      if (result && result.success) {
        this.setData({
          paused: false,
          device: { ...this.data.device, status: 'RUNNING' },
          statusText: this.data.currentModeName + '中',
          estimatedEndTime: this.formatEndTime(result.estimatedEndTime)
        });
        wx.showToast({ title: '继续成功', icon: 'success' });
      } else {
        throw new Error(result ? result.message : '继续失败');
      }
    } catch (e) {
      wx.hideLoading();
      wx.showToast({ title: e.message || '继续失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  /**
   * 结束
   */
  async handleEnd() {
    if (this.data.device.status !== 'RUNNING' && this.data.device.status !== 'PAUSED') return;

    try {
      this.setData({ loading: true });
      wx.showLoading({ title: '结束中...' });

      const result = await api.endDevice(this.data.currentSn);

      wx.hideLoading();

      if (result && result.success) {
        this.setData({
          paused: false,
          device: { ...this.data.device, status: 'FINISHED' },
          statusText: '洗衣完成',
          remainingTime: 0,
          remainingMinutes: 0
        });
        wx.showToast({ title: '洗衣完成', icon: 'success' });
      } else {
        throw new Error(result ? result.message : '结束失败');
      }
    } catch (e) {
      wx.hideLoading();
      wx.showToast({ title: e.message || '结束失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  /**
   * 取衣
   */
  async handlePickup() {
    if (this.data.device.status !== 'FINISHED') return;

    try {
      this.setData({ loading: true });
      wx.showLoading({ title: '开门中...' });

      const result = await api.pickupDevice(this.data.currentSn);

      wx.hideLoading();

      if (result && result.success) {
        this.setData({
          device: { ...this.data.device, status: 'IDLE' },
          statusText: '空闲中',
          remainingTime: this.data.estimatedTime,
          estimatedEndTime: null,
          currentOrderNo: null
        });
        wx.showToast({ title: '门已打开，请取衣', icon: 'success' });
      } else {
        throw new Error(result ? result.message : '开门失败');
      }
    } catch (e) {
      wx.hideLoading();
      wx.showToast({ title: e.message || '开门失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  /**
   * 重置设备状态
   */
  async handleReset() {
    try {
      this.setData({ loading: true });

      const result = await api.resetDevice(this.data.currentSn);

      if (result && result.success) {
        this.setData({
          device: { status: 'IDLE', deviceSn: this.data.currentSn },
          statusText: '空闲中',
          remainingTime: this.data.estimatedTime,
          estimatedEndTime: null,
          paused: false,
          systemHealthy: true,
          currentOrderNo: null
        });
        wx.showToast({ title: '已重置', icon: 'success' });
      }
    } catch (e) {
      wx.showToast({ title: '重置失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  /**
   * 关闭支付弹窗
   */
  closePaymentModal() {
    this.setData({ showPaymentModal: false });
  },

  /**
   * 去登录
   */
  goToLogin() {
    this.setData({ showLoginModal: false });
    wx.navigateTo({ url: '/pages/common/login/login' });
  },

  closeLoginModal() {
    this.setData({ showLoginModal: false });
  },

  /**
   * 关闭未登录弹窗
   */
  closeUnlinkedModal() {
    this.setData({ showUnlinkedModal: false });
  }
});
