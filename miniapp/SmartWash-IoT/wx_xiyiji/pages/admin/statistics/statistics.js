// pages/admin/statistics/statistics.js
const api = require('../../../utils/api.js');
const auth = require('../../../utils/auth.js');

Page({
  data: {
    loading: true,
    activeTab: 'overview',
    
    // 概览数据
    overview: {
      devices: { total: 0, online: 0, running: 0, fault: 0 },
      today: { orders: 0, income: 0, usageRate: 0 },
      month: { orders: 0, income: 0 }
    },
    
    // 使用率数据
    usageData: [],
    
    // 高峰时段
    peakHours: [],
    
    // 洗涤程序统计
    programStats: []
  },

  onLoad() {
    auth.requireAdmin(() => {
      this.loadStatistics();
    });
  },

  onShow() {
    if (auth.isAdmin()) {
      this.loadStatistics();
    }
  },

  onPullDownRefresh() {
    this.loadStatistics().then(() => {
      wx.stopPullDownRefresh();
    });
  },

  switchTab(e) {
    const tab = e.currentTarget.dataset.tab;
    this.setData({ activeTab: tab });
    
    if (tab === 'usage' && this.data.usageData.length === 0) {
      this.loadUsageStats();
    } else if (tab === 'peak' && this.data.peakHours.length === 0) {
      this.loadPeakHours();
    } else if (tab === 'program' && this.data.programStats.length === 0) {
      this.loadProgramStats();
    }
  },

  async loadStatistics() {
    this.setData({ loading: true });
    try {
      const overview = await api.admin.getOverview();
      if (overview) {
        this.setData({
          overview: {
            devices: overview.devices || { total: 0, online: 0, running: 0, fault: 0 },
            today: overview.today || { orders: 0, income: 0, usageRate: 0 },
            month: overview.month || { orders: 0, income: 0 }
          }
        });
      }
    } catch (error) {
      console.error('加载统计数据失败:', error);
      wx.showToast({ title: '加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  async loadUsageStats() {
    try {
      const data = await api.admin.getUsageStats({});
      this.setData({
        usageData: Array.isArray(data) ? data : []
      });
    } catch (error) {
      console.error('加载使用率失败:', error);
    }
  },

  async loadPeakHours() {
    try {
      const data = await api.admin.getPeakHoursStats({});
      this.setData({
        peakHours: Array.isArray(data) ? data : []
      });
    } catch (error) {
      console.error('加载高峰时段失败:', error);
    }
  },

  async loadProgramStats() {
    try {
      const data = await api.admin.getProgramStats({});
      this.setData({
        programStats: Array.isArray(data) ? data : []
      });
    } catch (error) {
      console.error('加载程序统计失败:', error);
    }
  }
});
