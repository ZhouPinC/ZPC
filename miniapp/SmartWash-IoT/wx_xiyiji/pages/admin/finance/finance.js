// pages/admin/finance/finance.js
const api = require('../../../utils/api.js');
const auth = require('../../../utils/auth.js');

Page({
  data: {
    loading: true,
    summary: {
      totalIncome: 0,
      todayIncome: 0,
      monthIncome: 0,
      pendingSettle: 0
    },
    records: [],
    currentPage: 1,
    hasMore: true
  },

  onLoad() {
    auth.requireAdmin(() => {
      this.loadFinanceData();
    });
  },

  onShow() {
    if (auth.isAdmin()) {
      this.loadFinanceData();
    }
  },

  onPullDownRefresh() {
    this.setData({ currentPage: 1, hasMore: true });
    this.loadFinanceData().then(() => {
      wx.stopPullDownRefresh();
    });
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading) {
      this.loadMoreRecords();
    }
  },

  async loadFinanceData() {
    this.setData({ loading: true });
    try {
      // 加载收益概览
      const summary = await api.admin.getFinanceSummary();
      if (summary) {
        this.setData({
          summary: {
            totalIncome: summary.totalIncome || 0,
            todayIncome: summary.todayIncome || 0,
            monthIncome: summary.monthIncome || 0,
            pendingSettle: summary.pendingSettle || 0
          }
        });
      }

      // 加载收益明细
      const records = await api.admin.getFinanceRecords({ page: 1, size: 20 });
      let recordList = [];
      let recordLen = 0;
      if (Array.isArray(records)) {
        recordList = records;
        recordLen = records.length;
      } else if (records && records.list && Array.isArray(records.list)) {
        recordList = records.list;
        recordLen = records.list.length;
      }
      this.setData({
        records: recordList,
        currentPage: 1,
        hasMore: recordLen >= 20
      });
    } catch (error) {
      console.error('加载财务数据失败:', error);
      wx.showToast({ title: '加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  async loadMoreRecords() {
    const nextPage = this.data.currentPage + 1;
    try {
      const records = await api.admin.getFinanceRecords({ page: nextPage, size: 20 });
      let newRecords = [];
      if (Array.isArray(records)) {
        newRecords = records;
      } else if (records && records.list && Array.isArray(records.list)) {
        newRecords = records.list;
      }
      
      this.setData({
        records: [...this.data.records, ...newRecords],
        currentPage: nextPage,
        hasMore: newRecords.length >= 20
      });
    } catch (error) {
      console.error('加载更多失败:', error);
    }
  },

  exportReport() {
    wx.showToast({ title: '导出功能开发中', icon: 'none' });
  },

  formatDate(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`;
  }
});
