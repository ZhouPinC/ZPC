// pages/history/index.js
const api = require('../../utils/api.js');

Page({
  data: {
    list: [],
    loading: false,
    empty: false
  },

  onLoad() {
    this.loadHistory();
  },

  onShow() {
    // 每次显示页面时刷新数据
    this.loadHistory();
  },

  onPullDownRefresh() {
    this.loadHistory().then(() => {
      wx.stopPullDownRefresh();
    });
  },

  /**
   * 获取用户信息
   */
  getUser() {
    // 优先从新的存储key获取
    let user = wx.getStorageSync('user_info');
    if (user && user.id) return user;
    
    // 兼容旧的存储key
    user = wx.getStorageSync('user');
    if (user && user.id) return user;
    
    // 从全局状态获取
    const app = getApp();
    if (app && app.globalData && app.globalData.userInfo) {
      return app.globalData.userInfo;
    }
    return null;
  },

  /**
   * 加载洗衣历史记录
   */
  async loadHistory() {
    const user = this.getUser();
    
    if (!user || !user.id) {
      this.setData({ list: [], empty: true });
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }

    this.setData({ loading: true });

    try {
      console.log('加载历史记录，用户ID:', user.id);
      const result = await api.getWashHistory(user.id);
      console.log('历史记录API返回:', result);
      
      if (result && result.success && result.records) {
        const list = result.records.map(order => ({
          id: order.id,
          orderNo: order.orderNo,
          deviceSn: order.deviceSn || '未知设备',
          createTime: this.formatDateTime(order.createTime),
          payTime: this.formatDateTime(order.payTime),
          startTime: this.formatDateTime(order.startTime),
          endTime: this.formatDateTime(order.endTime),
          amount: order.amount ? parseFloat(order.amount).toFixed(2) : '0.00',
          status: order.status,
          statusText: this.getStatusText(order.status),
          durationMinutes: order.durationMinutes || 30,
          washMode: order.washMode || 'standard',
          washModeName: order.washModeName || '标准洗',
          payMethod: order.paymentMethodName || '余额支付'
        }));
        
        this.setData({
          list,
          empty: list.length === 0
        });
      } else {
        this.setData({ list: [], empty: true });
      }
    } catch (e) {
      console.error('加载历史记录失败:', e);
      this.setData({ list: [], empty: true });
      wx.showToast({ title: '加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  /**
   * 格式化日期时间
   */
  formatDateTime(dateStr) {
    if (!dateStr) return '-';
    
    try {
      const date = new Date(dateStr);
      if (isNaN(date.getTime())) return '-';
      
      const year = date.getFullYear();
      const month = (date.getMonth() + 1).toString().padStart(2, '0');
      const day = date.getDate().toString().padStart(2, '0');
      const hours = date.getHours().toString().padStart(2, '0');
      const minutes = date.getMinutes().toString().padStart(2, '0');
      
      return `${year}-${month}-${day} ${hours}:${minutes}`;
    } catch (e) {
      return '-';
    }
  },

  /**
   * 获取状态文本
   */
  getStatusText(status) {
    const statusMap = {
      'CREATED': '待支付',
      'PAID': '已支付',
      'RUNNING': '进行中',
      'PAUSED': '已暂停',
      'FINISHED': '已完成',
      'CANCELLED': '已取消',
      'FAILED': '失败',
      'REFUNDED': '已退款'
    };
    return statusMap[status] || status || '未知';
  }
});
