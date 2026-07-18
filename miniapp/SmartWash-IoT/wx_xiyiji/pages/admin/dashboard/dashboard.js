// pages/admin/dashboard/dashboard.js
const api = require('../../../utils/api.js');
const auth = require('../../../utils/auth.js');

Page({
  data: {
    loading: true,
    
    // 概览数据
    overview: {
      totalDevices: 0,
      onlineDevices: 0,
      runningDevices: 0,
      faultDevices: 0,
    },
    
    // 今日数据
    todayStats: {
      orderCount: 0,
      income: 0,
      usageRate: 0,
    },
    
    // 本月数据
    monthStats: {
      orderCount: 0,
      income: 0,
    },
    
    // 设备列表
    deviceList: [],
    
    // 最近订单
    recentOrders: [],
  },

  onLoad() {
    // 检查管理员权限
    this.checkAndLoadData();
  },

  onShow() {
    this.checkAndLoadData();
  },
  
  /**
   * 检查权限并加载数据
   */
  checkAndLoadData() {
    // 检查是否有token或用户信息
    const token = wx.getStorageSync('auth_token');
    const userInfo = wx.getStorageSync('user_info') || wx.getStorageSync('user');
    
    console.log('Dashboard - token:', token ? '存在' : '不存在');
    console.log('Dashboard - userInfo:', userInfo);
    
    // 只要有用户信息就尝试加载数据
    if (token || (userInfo && userInfo.id)) {
      this.loadDashboardData();
      return;
    }
    
    // 没有任何登录信息，返回上一页
    console.log('没有登录信息，返回上一页');
    wx.navigateBack();
  },

  onPullDownRefresh() {
    this.loadDashboardData().then(() => {
      wx.stopPullDownRefresh();
    });
  },

  /**
   * 加载仪表盘数据
   */
  async loadDashboardData() {
    this.setData({ loading: true });
    
    try {
      console.log('开始加载管理中心数据...');
      
      // 并行加载数据
      const [overview, devices, financeRecords] = await Promise.all([
        api.admin.getOverview().catch(e => { 
          console.error('加载概览失败:', e); 
          return null; 
        }),
        api.admin.getDevices().catch(e => { 
          console.error('加载设备失败:', e); 
          return []; 
        }),
        api.admin.getFinanceRecords({ page: 0, size: 5 }).catch(e => { 
          console.error('加载订单失败:', e); 
          return []; 
        })
      ]);
      
      console.log('API返回 - overview:', overview);
      console.log('API返回 - devices:', devices);
      console.log('API返回 - financeRecords:', financeRecords);
      
      // 设置概览数据
      if (overview) {
        const devicesData = overview.devices || {};
        const todayData = overview.today || {};
        const monthData = overview.month || {};
        
        this.setData({
          overview: {
            totalDevices: devicesData.total || 0,
            onlineDevices: devicesData.online || 0,
            runningDevices: devicesData.running || 0,
            faultDevices: devicesData.fault || 0,
          },
          todayStats: {
            orderCount: todayData.orders || 0,
            income: todayData.income || 0,
            usageRate: todayData.usageRate || 0,
          },
          monthStats: {
            orderCount: monthData.orders || 0,
            income: monthData.income || 0,
          },
        });
      }
      
      // 设置设备列表 - 处理可能的数组或对象包装
      let deviceList = [];
      if (devices) {
        if (Array.isArray(devices)) {
          deviceList = devices;
        } else if (devices.list && Array.isArray(devices.list)) {
          deviceList = devices.list;
        } else if (devices.data && Array.isArray(devices.data)) {
          deviceList = devices.data;
        }
      }
      
      if (deviceList.length > 0) {
        this.setData({
          deviceList: deviceList.slice(0, 5).map(d => ({
            id: d.id,
            deviceSn: d.deviceSn,
            location: d.location || '未设置位置',
            status: d.status,
            statusText: d.statusText || this.getStatusText(d.status)
          }))
        });
      }
      
      // 设置最近订单 - 处理可能的数组或对象包装
      let recordList = [];
      if (financeRecords) {
        if (Array.isArray(financeRecords)) {
          recordList = financeRecords;
        } else if (financeRecords.list && Array.isArray(financeRecords.list)) {
          recordList = financeRecords.list;
        } else if (financeRecords.data && Array.isArray(financeRecords.data)) {
          recordList = financeRecords.data;
        }
      }
      
      this.setData({
        recentOrders: recordList.slice(0, 5).map(r => ({
          id: r.id,
          deviceSn: r.deviceSn || '设备',
          amount: r.netIncome || r.orderAmount || 0,
          createTime: this.formatTime(r.createTime)
        }))
      });
      
    } catch (error) {
      console.error('加载数据失败:', error);
      wx.showToast({ title: '加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },
  
  /**
   * 获取状态文本
   */
  getStatusText(status) {
    const map = {
      'OFFLINE': '离线',
      'IDLE': '空闲',
      'RUNNING': '运行中',
      'PAUSED': '已暂停',
      'FINISHED': '已完成',
      'FAULT': '故障'
    };
    return map[status] || '未知';
  },
  
  /**
   * 格式化时间
   */
  formatTime(dateStr) {
    if (!dateStr) return '';
    try {
      const date = new Date(dateStr);
      const month = date.getMonth() + 1;
      const day = date.getDate();
      const hour = String(date.getHours()).padStart(2, '0');
      const minute = String(date.getMinutes()).padStart(2, '0');
      return `${month}/${day} ${hour}:${minute}`;
    } catch (e) {
      return dateStr;
    }
  },

  /**
   * 导航到设备管理
   */
  navigateToDevices() {
    wx.navigateTo({ url: '/pages/admin/devices/devices' });
  },

  /**
   * 导航到用户管理
   */
  navigateToUsers() {
    wx.navigateTo({ url: '/pages/admin/users/users' });
  },

  /**
   * 导航到财务管理
   */
  navigateToFinance() {
    wx.navigateTo({ url: '/pages/admin/finance/finance' });
  },

  /**
   * 导航到数据统计
   */
  navigateToStatistics() {
    wx.navigateTo({ url: '/pages/admin/statistics/statistics' });
  },

  /**
   * 退出管理模式
   */
  async exitAdminMode() {
    wx.showModal({
      title: '提示',
      content: '确定退出管理模式？',
      success: async (res) => {
        if (res.confirm) {
          try {
            const result = await api.auth.exitAdmin();
            auth.saveLoginInfo(result.token, result.userInfo);
            
            // 切换视图模式（防御性检查）
            const app = getApp();
            if (app && app.switchViewMode) {
              app.switchViewMode('consumer');
            } else if (app && app.globalData) {
              app.globalData.viewMode = 'consumer';
            }
            
            wx.reLaunch({ url: '/pages/device/index' });
          } catch (error) {
            console.error('退出管理模式失败:', error);
            // 即使API失败也尝试退出
            const app = getApp();
            if (app && app.globalData) {
              app.globalData.viewMode = 'consumer';
            }
            wx.reLaunch({ url: '/pages/device/index' });
          }
        }
      },
    });
  },
});
