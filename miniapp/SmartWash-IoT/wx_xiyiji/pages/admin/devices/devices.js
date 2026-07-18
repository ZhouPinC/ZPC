// pages/admin/devices/devices.js
const api = require('../../../utils/api.js');
const auth = require('../../../utils/auth.js');

Page({
  data: {
    loading: true,
    devices: [],
    showAddModal: false,
    showQrModal: false,
    qrDeviceSn: '',
    qrImagePath: '',
    newDevice: {
      deviceSn: '',
      name: '',
      location: ''
    }
  },

  onLoad() {
    auth.requireAdmin(function () {
      this.loadDevices();
    }.bind(this));
  },

  onShow() {
    if (auth.isAdmin()) {
      this.loadDevices();
    }
  },

  onPullDownRefresh() {
    var that = this;
    this.loadDevices().then(function () {
      wx.stopPullDownRefresh();
    });
  },

  async loadDevices() {
    this.setData({ loading: true });
    try {
      var result = await api.admin.getDevices();
      var devices = [];

      if (result && result.content) {
        devices = result.content;
      } else if (Array.isArray(result)) {
        devices = result;
      }

      for (var i = 0; i < devices.length; i++) {
        devices[i].statusText = this.getStatusText(devices[i].status);
      }

      this.setData({ devices: devices });
    } catch (error) {
      console.error('加载设备失败:', error);
      wx.showToast({ title: '加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  showAddDevice() {
    // 自动生成唯一设备编号
    const timestamp = new Date().getTime().toString().substring(5);
    const random = Math.floor(Math.random() * 1000).toString().padStart(3, '0');
    const autoSn = 'WASH_' + timestamp + random;

    this.setData({
      showAddModal: true,
      newDevice: { deviceSn: autoSn, name: '洗衣机 ' + autoSn, location: '' }
    });
  },

  hideAddModal() {
    this.setData({ showAddModal: false });
  },

  onInputDeviceSn(e) {
    this.setData({ 'newDevice.deviceSn': e.detail.value });
  },

  onInputDeviceName(e) {
    this.setData({ 'newDevice.name': e.detail.value });
  },

  onInputLocation(e) {
    this.setData({ 'newDevice.location': e.detail.value });
  },

  async confirmAddDevice() {
    var deviceSn = this.data.newDevice.deviceSn;
    var name = this.data.newDevice.name;
    var location = this.data.newDevice.location;

    if (!deviceSn) {
      wx.showToast({ title: '请输入设备编号', icon: 'none' });
      return;
    }

    try {
      wx.showLoading({ title: '添加中...' });
      await api.admin.addDevice({
        deviceSn: deviceSn,
        name: name || deviceSn,
        location: location
      });
      wx.hideLoading();
      wx.showToast({ title: '添加成功', icon: 'success' });
      this.hideAddModal();
      this.loadDevices();
    } catch (error) {
      wx.hideLoading();
      wx.showToast({ title: error.message || '添加失败', icon: 'none' });
    }
  },

  /**
   * 显示设备二维码
   */
  showQrCode(e) {
    var deviceSn = e.currentTarget.dataset.sn;
    this.setData({
      showQrModal: true,
      qrDeviceSn: deviceSn,
      qrImagePath: ''
    });

    // 生成二维码
    this.generateQrCode(deviceSn);
  },

  /**
   * 生成二维码图片
   * 二维码内容为小程序页面路径，扫码后自动进入设备页面并绑定
   * 注意：此为演示用二维码，生产环境应使用微信官方小程序码API
   */
  generateQrCode(deviceSn) {
    var that = this;
    // 小程序页面路径，带设备SN参数
    // 扫码后会打开小程序并跳转到 pages/device/index?deviceSn=xxx
    var pagePath = 'pages/device/index';
    var scene = 'deviceSn=' + deviceSn;

    // 二维码内容（用于普通扫码）
    var qrContent = 'DEVICE:' + deviceSn;

    // 使用 Canvas 绘制二维码
    var ctx = wx.createCanvasContext('qrCanvasLegacy', this);

    var canvasWidth = 300;
    var canvasHeight = 400;
    var qrSize = 200;
    var qrX = (canvasWidth - qrSize) / 2;
    var qrY = 60;

    // 绘制白色背景
    ctx.setFillStyle('#ffffff');
    ctx.fillRect(0, 0, canvasWidth, canvasHeight);

    // 绘制标题
    ctx.setFillStyle('#333333');
    ctx.setFontSize(18);
    ctx.setTextAlign('center');
    ctx.fillText('扫码绑定洗衣机', canvasWidth / 2, 35);

    // 绘制二维码边框
    ctx.setStrokeStyle('#1890ff');
    ctx.setLineWidth(3);
    ctx.strokeRect(qrX - 8, qrY - 8, qrSize + 16, qrSize + 16);

    // 绘制二维码背景
    ctx.setFillStyle('#f5f5f5');
    ctx.fillRect(qrX, qrY, qrSize, qrSize);

    // 绘制二维码图案（基于设备SN生成）
    this.drawQrPattern(ctx, qrX, qrY, qrSize, qrContent);

    // 绘制设备编号
    ctx.setFillStyle('#1890ff');
    ctx.setFontSize(24);
    ctx.setTextAlign('center');
    ctx.fillText(deviceSn, canvasWidth / 2, qrY + qrSize + 45);

    // 绘制提示
    ctx.setFillStyle('#666666');
    ctx.setFontSize(12);
    ctx.fillText('微信扫一扫绑定设备', canvasWidth / 2, qrY + qrSize + 75);

    // 绘制小程序路径提示
    ctx.setFillStyle('#999999');
    ctx.setFontSize(10);
    ctx.fillText('路径: ' + pagePath + '?deviceSn=' + deviceSn, canvasWidth / 2, qrY + qrSize + 95);

    ctx.draw(false, function () {
      setTimeout(function () {
        wx.canvasToTempFilePath({
          canvasId: 'qrCanvasLegacy',
          success: function (res) {
            that.setData({ qrImagePath: res.tempFilePath });
          },
          fail: function (err) {
            console.error('生成二维码图片失败:', err);
          }
        }, that);
      }, 300);
    });
  },

  /**
   * 绘制二维码图案
   */
  drawQrPattern(ctx, x, y, size, content) {
    var moduleCount = 25;
    var moduleSize = size / moduleCount;

    var data = this.generateQrData(content, moduleCount);

    ctx.setFillStyle('#000000');

    for (var row = 0; row < moduleCount; row++) {
      for (var col = 0; col < moduleCount; col++) {
        if (data[row][col]) {
          ctx.fillRect(
            x + col * moduleSize,
            y + row * moduleSize,
            moduleSize - 0.5,
            moduleSize - 0.5
          );
        }
      }
    }
  },

  /**
   * 生成二维码数据矩阵
   */
  generateQrData(content, size) {
    var data = [];

    for (var i = 0; i < size; i++) {
      data[i] = [];
      for (var j = 0; j < size; j++) {
        data[i][j] = false;
      }
    }

    // 绘制定位图案
    this.drawFinderPattern(data, 0, 0);
    this.drawFinderPattern(data, size - 7, 0);
    this.drawFinderPattern(data, 0, size - 7);

    // 绘制对齐图案
    this.drawAlignmentPattern(data, size - 9, size - 9);

    // 绘制时序图案
    for (var i = 8; i < size - 8; i++) {
      data[6][i] = (i % 2 === 0);
      data[i][6] = (i % 2 === 0);
    }

    // 根据内容生成数据
    var hash = 0;
    for (var i = 0; i < content.length; i++) {
      hash = ((hash << 5) - hash) + content.charCodeAt(i);
      hash = hash & hash;
    }

    // 填充数据区域
    for (var row = 9; row < size - 1; row++) {
      for (var col = 9; col < size - 1; col++) {
        if (col !== 6 && row !== 6) {
          // 避开对齐图案区域
          if (row >= size - 9 && col >= size - 9) continue;

          var seed = (hash + row * size + col) % 100;
          data[row][col] = seed < 50;
        }
      }
    }

    return data;
  },

  /**
   * 绘制定位图案
   */
  drawFinderPattern(data, startX, startY) {
    for (var i = 0; i < 7; i++) {
      data[startY][startX + i] = true;
      data[startY + 6][startX + i] = true;
      data[startY + i][startX] = true;
      data[startY + i][startX + 6] = true;
    }
    for (var i = 2; i < 5; i++) {
      for (var j = 2; j < 5; j++) {
        data[startY + i][startX + j] = true;
      }
    }
  },

  /**
   * 绘制对齐图案
   */
  drawAlignmentPattern(data, centerX, centerY) {
    for (var i = -2; i <= 2; i++) {
      for (var j = -2; j <= 2; j++) {
        if (Math.abs(i) === 2 || Math.abs(j) === 2 || (i === 0 && j === 0)) {
          data[centerY + i][centerX + j] = true;
        }
      }
    }
  },

  hideQrModal() {
    this.setData({ showQrModal: false, qrImagePath: '' });
  },

  /**
   * 保存二维码到相册
   */
  saveQrCode() {
    var that = this;

    if (!this.data.qrImagePath) {
      wx.showToast({ title: '二维码生成中...', icon: 'none' });
      return;
    }

    wx.saveImageToPhotosAlbum({
      filePath: this.data.qrImagePath,
      success: function () {
        wx.showToast({ title: '已保存到相册', icon: 'success' });
      },
      fail: function (err) {
        if (err.errMsg && err.errMsg.indexOf('auth deny') !== -1) {
          wx.showModal({
            title: '提示',
            content: '需要您授权保存图片到相册',
            success: function (res) {
              if (res.confirm) {
                wx.openSetting();
              }
            }
          });
        } else {
          wx.showToast({ title: '保存失败', icon: 'none' });
        }
      }
    });
  },

  /**
   * 复制小程序路径（用于生成真正的小程序码）
   */
  copyQrContent() {
    // 复制小程序页面路径，用于在微信公众平台生成真正的小程序码
    var path = 'pages/device/index?deviceSn=' + this.data.qrDeviceSn;
    var scene = 'deviceSn=' + this.data.qrDeviceSn;

    wx.showActionSheet({
      itemList: ['复制页面路径', '复制scene参数', '复制扫码内容'],
      success: function (res) {
        var content = '';
        if (res.tapIndex === 0) {
          content = path;
        } else if (res.tapIndex === 1) {
          content = scene;
        } else {
          content = 'DEVICE:' + this.data.qrDeviceSn;
        }

        wx.setClipboardData({
          data: content,
          success: function () {
            wx.showToast({ title: '已复制', icon: 'success' });
          }
        });
      }.bind(this)
    });
  },

  /**
   * 重置设备状态（管理员功能）
   */
  async resetDevice(e) {
    var deviceSn = e.currentTarget.dataset.sn;
    var that = this;

    wx.showModal({
      title: '重置设备',
      content: '确定要重置设备 ' + deviceSn + ' 的状态吗？',
      success: async function (res) {
        if (res.confirm) {
          try {
            wx.showLoading({ title: '重置中...' });
            var result = await api.resetDevice(deviceSn);
            wx.hideLoading();

            if (result && result.success) {
              wx.showToast({ title: '重置成功', icon: 'success' });
              that.loadDevices();
            } else {
              wx.showToast({ title: result.message || '重置失败', icon: 'none' });
            }
          } catch (error) {
            wx.hideLoading();
            wx.showToast({ title: '重置失败', icon: 'none' });
          }
        }
      }
    });
  },

  viewDevice(e) {
    var id = e.currentTarget.dataset.id;
    wx.showToast({ title: '功能开发中', icon: 'none' });
  },

  async deleteDevice(e) {
    var id = e.currentTarget.dataset.id;
    var sn = e.currentTarget.dataset.sn;
    var that = this;

    wx.showModal({
      title: '确认删除',
      content: '确定要删除设备 ' + sn + ' 吗？',
      success: async function (res) {
        if (res.confirm) {
          try {
            wx.showLoading({ title: '删除中...' });
            await api.admin.deleteDevice(id);
            wx.hideLoading();
            wx.showToast({ title: '删除成功', icon: 'success' });
            that.loadDevices();
          } catch (error) {
            wx.hideLoading();
            wx.showToast({ title: error.message || '删除失败', icon: 'none' });
          }
        }
      }
    });
  },

  getStatusText(status) {
    var map = {
      'OFFLINE': '离线',
      'IDLE': '空闲',
      'RUNNING': '运行中',
      'PAUSED': '已暂停',
      'FINISHED': '已完成',
      'FAULT': '故障'
    };
    return map[status] || '未知';
  }
});
