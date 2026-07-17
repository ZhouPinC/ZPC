Page({
  data: {
    history: []
  },
  
  onLoad() {
    this.loadHistory();
  },
  
  onShow() {
    this.loadHistory();
  },
  
  // 加载历史记录
  loadHistory() {
    const history = wx.getStorageSync('gameHistory') || [];
    this.setData({history});
  },
  
  // 前往游戏页面
  goToGame() {
    wx.navigateBack();
  },
  
  // 清空历史记录
  clearHistory() {
    wx.showModal({
      title: '确认清空',
      content: '确定要清空所有游戏记录吗？',
      success: (res) => {
        if (res.confirm) {
          wx.setStorageSync('gameHistory', []);
          this.setData({history: []});
          wx.showToast({
            title: '已清空',
            icon: 'success',
            duration: 1500
          });
        }
      }
    });
  }
});