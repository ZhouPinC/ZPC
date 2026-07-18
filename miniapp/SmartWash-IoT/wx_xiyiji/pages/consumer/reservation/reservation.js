// pages/consumer/reservation/reservation.js
const api = require('../../../utils/api.js');
const auth = require('../../../utils/auth.js');

Page({
  data: {
    deviceId: null,
    deviceInfo: null,
    
    // 日期选择
    dateList: [],
    selectedDate: null,
    
    // 时段选择
    timeSlots: [],
    selectedSlot: null,
    
    loading: false,
    submitting: false,
  },

  onLoad(options) {
    auth.requireLogin(() => {
      this.setData({ deviceId: options.deviceId });
      this.initDateList();
      this.loadDeviceInfo();
    });
  },

  /**
   * 初始化日期列表（未来7天）
   */
  initDateList() {
    const dateList = [];
    const today = new Date();
    const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
    
    for (let i = 0; i < 7; i++) {
      const date = new Date(today);
      date.setDate(today.getDate() + i);
      
      const dateStr = this.formatDate(date);
      const dayStr = i === 0 ? '今天' : (i === 1 ? '明天' : weekDays[date.getDay()]);
      
      dateList.push({
        date: dateStr,
        display: `${date.getMonth() + 1}/${date.getDate()}`,
        day: dayStr,
      });
    }
    
    this.setData({ 
      dateList,
      selectedDate: dateList[0].date 
    });
    
    this.loadTimeSlots(dateList[0].date);
  },

  /**
   * 加载设备信息
   */
  async loadDeviceInfo() {
    try {
      // 使用旧接口获取设备信息
      const deviceStatus = await api.legacy.getDeviceStatus(this.data.deviceId);
      this.setData({ deviceInfo: deviceStatus });
    } catch (e) {
      console.error('获取设备信息失败:', e);
    }
  },

  /**
   * 加载可预约时段
   */
  async loadTimeSlots(date) {
    this.setData({ loading: true, timeSlots: [] });
    
    try {
      const slots = await api.consumer.getAvailableSlots(this.data.deviceId, date);
      this.setData({ timeSlots: slots || [] });
    } catch (e) {
      console.error('获取时段失败:', e);
      wx.showToast({ title: '获取时段失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  /**
   * 选择日期
   */
  onDateSelect(e) {
    const date = e.currentTarget.dataset.date;
    this.setData({ 
      selectedDate: date,
      selectedSlot: null 
    });
    this.loadTimeSlots(date);
  },

  /**
   * 选择时段
   */
  onSlotSelect(e) {
    const index = e.currentTarget.dataset.index;
    const slot = this.data.timeSlots[index];
    
    if (!slot.available) {
      wx.showToast({ title: slot.reason || '该时段不可预约', icon: 'none' });
      return;
    }
    
    this.setData({ selectedSlot: slot });
  },

  /**
   * 提交预约
   */
  async onSubmit() {
    if (!this.data.selectedSlot) {
      wx.showToast({ title: '请选择时段', icon: 'none' });
      return;
    }
    
    this.setData({ submitting: true });
    
    try {
      const result = await api.consumer.createReservation({
        deviceId: parseInt(this.data.deviceId),
        reservedDate: this.data.selectedDate,
        startTime: this.data.selectedSlot.startTime,
        endTime: this.data.selectedSlot.endTime,
      });
      
      wx.showToast({ title: '预约成功', icon: 'success' });
      
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    } catch (e) {
      console.error('预约失败:', e);
      wx.showToast({ title: e.message || '预约失败', icon: 'none' });
    } finally {
      this.setData({ submitting: false });
    }
  },

  /**
   * 格式化日期
   */
  formatDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  },

  /**
   * 格式化时间
   */
  formatTime(timeStr) {
    if (!timeStr) return '';
    return timeStr.substring(0, 5);
  },
});
