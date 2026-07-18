// pages/add/index.js - 扫码使用洗衣机
const api = require('../../utils/api.js');
const auth = require('../../utils/auth.js');

Page({
  data: {
    scanning: false
  },

  onLoad() { },

  onShow() {
    // 设置自定义tabBar选中状态
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 1 });
    }
  },

  /**
   * 扫码使用洗衣机
   */
  scanToUse() {
    if (this.data.scanning) return;

    this.setData({ scanning: true });

    wx.scanCode({
      onlyFromCamera: false,
      scanType: ['qrCode', 'barCode'],
      success: (res) => {
        console.log('扫码结果:', res);
        const result = res.result;

        // 解析二维码内容，获取设备SN
        const deviceSn = this.parseQRCode(result);

        if (deviceSn) {
          this.checkLoginAndBind({ deviceSn });
          return;
        }

        if (result) {
          this.checkLoginAndBind({ qrCodeContent: result });
          return;
        }

        wx.showToast({ title: '无效的设备二维码', icon: 'none' });
      },
      fail: (err) => {
        console.error('扫码失败:', err);
        if (err.errMsg.indexOf('cancel') === -1) {
          wx.showToast({ title: '扫码失败', icon: 'none' });
        }
      },
      complete: () => {
        this.setData({ scanning: false });
      }
    });
  },

  /**
   * 检查用户登录状态并绑定设备
   */
  checkLoginAndBind(bindParams) {
    auth.requireLogin(() => {
      this.bindDevice(bindParams);
    });
  },

  /**
   * 绑定设备
   */
  async bindDevice(bindParams) {
    try {
      wx.showLoading({ title: '绑定设备中...' });

      const device = await api.consumer.bindDevice(bindParams || {});

      wx.hideLoading();

      wx.showToast({
        title: '设备绑定成功',
        icon: 'success',
        duration: 2000
      });

      const deviceSn = (device && device.deviceSn) || (bindParams && bindParams.deviceSn) || '';
      if (deviceSn) {
        this.navigateToDevice(deviceSn);
        return;
      }

      setTimeout(() => {
        wx.switchTab({ url: '/pages/device/index' });
      }, 500);
    } catch (err) {
      wx.hideLoading();
      console.error('绑定设备失败:', err);

      if (err && err.code === 401) {
        wx.showModal({
          title: '需要登录',
          content: err.message || '登录状态已失效，请重新登录',
          showCancel: false,
          success: () => {
            wx.navigateTo({ url: '/pages/common/login/login' });
          }
        });
        return;
      }

      const msg = (err && err.message) || (err && err.errMsg) || (err && err.msg) || '设备绑定失败，请稍后重试';

      wx.showModal({
        title: '绑定失败',
        content: msg,
        confirmText: '重试',
        cancelText: '取消',
        success: (res) => {
          if (res.confirm) {
            this.bindDevice(bindParams);
          }
        }
      });
    }
  },

  /**
   * 解析二维码内容
   */
  parseQRCode(content) {
    if (!content) return null;

    const raw = String(content).trim();
    if (!raw) return null;

    const directMatch = raw.match(/WASH_[A-Za-z0-9_]+/);
    if (directMatch && directMatch[0]) {
      return directMatch[0].trim().toUpperCase();
    }

    // 格式2: URL / 查询参数
    try {
      const queryMatch = raw.match(/(?:deviceSn|device_sn|sn)=([^&]+)/i);
      if (queryMatch && queryMatch[1]) {
        return decodeURIComponent(queryMatch[1]).trim().toUpperCase();
      }
    } catch (e) {
      console.error('解析二维码失败:', e);
    }

    // 格式3: JSON
    try {
      if (raw.startsWith('{')) {
        const json = JSON.parse(raw);
        const candidate = json && (json.deviceSn || json.sn || json.device_sn);
        if (candidate) {
          return String(candidate).trim().toUpperCase();
        }
      }
    } catch (e) {
      console.error('解析二维码失败:', e);
    }

    return null;
  },

  /**
   * 跳转到设备页面
   */
  navigateToDevice(deviceSn) {
    wx.setStorageSync('currentDeviceSn', deviceSn);

    wx.showToast({
      title: '已连接设备',
      icon: 'success',
      duration: 1500
    });

    setTimeout(() => {
      wx.switchTab({ url: '/pages/device/index' });
    }, 1000);
  },

  /**
   * 手动输入设备码
   */
  manualInput() {
    wx.showModal({
      title: '输入设备码',
      editable: true,
      placeholderText: '请输入设备上的编码',
      success: (res) => {
        if (res.confirm && res.content) {
          const deviceSn = res.content.trim().toUpperCase();
          if (deviceSn) {
            // 检查用户登录状态并绑定设备
            this.checkLoginAndBind({ deviceSn });
          }
        }
      }
    });
  }
});
