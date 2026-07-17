App({
  onLaunch() {
    // 初始化本地存储
    if (!wx.getStorageSync('settings')) {
      // 默认设置
      const defaultSettings = {
        minValue: 0,
        maxValue: 100,
        challengeMode: false,
        maxAttempts: 10,
        showIntro: true
      };
      wx.setStorageSync('settings', defaultSettings);
    }
    
    // 初始化历史记录
    if (!wx.getStorageSync('gameHistory')) {
      wx.setStorageSync('gameHistory', []);
    }
  },
  
  globalData: {
    currentGame: null,
    // WebSocket 配置（请修改为实际的服务器地址）
    wsUrl: 'ws://localhost:8080/gameSync',
    socketTask: null,
    clientId: null
  },
  
  // 初始化 WebSocket 连接
  connectWebSocket() {
    const clientId = wx.getStorageSync('clientId') || this.generateClientId();
    this.globalData.clientId = clientId;
    
    this.globalData.socketTask = wx.connectSocket({
      url: this.globalData.wsUrl,
      success: () => {
        console.log('WebSocket 连接成功');
      },
      fail: (err) => {
        console.error('WebSocket 连接失败', err);
      }
    });
    
    this.globalData.socketTask.onOpen(() => {
      console.log('WebSocket 已打开');
    });
    
    this.globalData.socketTask.onMessage((msg) => {
      console.log('收到服务器消息', msg.data);
    });
    
    this.globalData.socketTask.onError((err) => {
      console.error('WebSocket 错误', err);
    });
    
    this.globalData.socketTask.onClose(() => {
      console.log('WebSocket 已关闭');
    });
  },
  
  // 生成客户端 ID
  generateClientId() {
    const id = 'wx-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
    wx.setStorageSync('clientId', id);
    return id;
  },
  
  // 发送游戏数据到服务器
  sendGameData(gameData) {
    if (this.globalData.socketTask) {
      const data = {
        clientId: this.globalData.clientId,
        clientType: 'wechat',
        ...gameData
      };
      this.globalData.socketTask.send({
        data: JSON.stringify(data),
        success: () => {
          console.log('游戏数据已发送');
        },
        fail: (err) => {
          console.error('发送失败', err);
        }
      });
    }
  }
});