// pages/admin/users/users.js
const api = require('../../../utils/api.js');
const auth = require('../../../utils/auth.js');

Page({
  data: {
    loading: true,
    users: [],
    expandedUserId: null
  },

  onLoad() {
    auth.requireAdmin(function() {
      this.loadUsers();
    }.bind(this));
  },

  onShow() {
    if (auth.isAdmin()) {
      this.loadUsers();
    }
  },

  onPullDownRefresh() {
    var that = this;
    this.loadUsers().then(function() {
      wx.stopPullDownRefresh();
    });
  },

  async loadUsers() {
    this.setData({ loading: true });
    try {
      var result = await api.admin.getUsers();
      var users = [];
      
      if (result && result.content) {
        users = result.content;
      } else if (Array.isArray(result)) {
        users = result;
      }
      
      // 为每个用户加载绑定的设备
      for (var i = 0; i < users.length; i++) {
        try {
          var deviceResult = await api.getBindDevices(users[i].id);
          users[i].bindDevices = (deviceResult && deviceResult.devices) ? deviceResult.devices : [];
        } catch (e) {
          users[i].bindDevices = [];
        }
      }
      
      this.setData({ users: users });
    } catch (error) {
      console.error('加载用户失败:', error);
      wx.showToast({ title: '加载失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  /**
   * 展开/收起用户设备列表
   */
  toggleUserDevices(e) {
    var userId = e.currentTarget.dataset.id;
    if (this.data.expandedUserId === userId) {
      this.setData({ expandedUserId: null });
    } else {
      this.setData({ expandedUserId: userId });
    }
  },

  /**
   * 解绑用户设备
   */
  async unbindUserDevice(e) {
    var userId = e.currentTarget.dataset.userid;
    var deviceId = e.currentTarget.dataset.deviceid;
    var deviceSn = e.currentTarget.dataset.devicesn;
    
    var that = this;
    wx.showModal({
      title: '确认解绑',
      content: '确定要解除该用户与设备 ' + deviceSn + ' 的绑定吗？',
      success: async function(res) {
        if (res.confirm) {
          try {
            wx.showLoading({ title: '解绑中...' });
            var result = await api.unbindDevice(userId, deviceId);
            wx.hideLoading();
            
            if (result && result.success) {
              wx.showToast({ title: '解绑成功', icon: 'success' });
              that.loadUsers();
            } else {
              wx.showToast({ title: result.message || '解绑失败', icon: 'none' });
            }
          } catch (error) {
            wx.hideLoading();
            wx.showToast({ title: '解绑失败', icon: 'none' });
          }
        }
      }
    });
  },

  viewUserDetail(e) {
    var id = e.currentTarget.dataset.id;
    wx.showToast({ title: '功能开发中', icon: 'none' });
  },

  async setPermission(e) {
    var id = e.currentTarget.dataset.id;
    var nickname = e.currentTarget.dataset.nickname;
    var that = this;
    
    wx.showActionSheet({
      itemList: ['无限制', '时间限制', '次数限制', '移除权限'],
      success: async function(res) {
        var types = ['UNLIMITED', 'TIME_RANGE', 'COUNT_LIMIT', 'REMOVE'];
        var type = types[res.tapIndex];
        
        if (type === 'REMOVE') {
          try {
            await api.admin.removeUserPermission(id);
            wx.showToast({ title: '已移除权限', icon: 'success' });
            that.loadUsers();
          } catch (error) {
            wx.showToast({ title: error.message || '操作失败', icon: 'none' });
          }
        } else {
          try {
            await api.admin.setUserPermission(id, { permissionType: type });
            wx.showToast({ title: '设置成功', icon: 'success' });
            that.loadUsers();
          } catch (error) {
            wx.showToast({ title: error.message || '操作失败', icon: 'none' });
          }
        }
      }
    });
  },

  getRoleText(role) {
    var map = {
      'CONSUMER': '普通用户',
      'ADMIN': '管理员',
      'SUPER_ADMIN': '超级管理员'
    };
    return map[role] || '未知';
  }
});
