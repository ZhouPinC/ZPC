const app = getApp();

Page({
  data: {
    // 游戏状态：NOT_STARTED, RUNNING, PAUSED, ENDED
    gameState: 'NOT_STARTED',
    
    // 设置相关
    settings: {},
    tempSettings: {},
    showSettingsModal: false,
    
    // 游戏数据
    userInput: '',
    targetNumber: null,
    attempts: 0,
    guessHistory: [],
    gameResult: {},
    
    // 弹窗控制
    showResultModal: false,
    resultTitle: '',
    resultMessage: ''
  },
  
  onLoad() {
    // 加载设置
    const settings = wx.getStorageSync('settings');
    this.setData({
      settings,
      tempSettings: {...settings}
    });
    
    // 首次启动显示说明
    if (settings.showIntro) {
      this.showIntro();
    }
  },
  
  // 显示游戏说明
  showIntro() {
    wx.showModal({
      title: '游戏说明',
      content: '欢迎来到猜数字小游戏！\n\n规则：在设置的整数区间内随机生成一个数字，你需要猜出这个数字。\n有两种模式：\n- 猜数字模式：无限次尝试\n- 挑战模式：限定次数内猜出',
      showCancel: false,
      confirmText: '知道了',
      success: (res) => {
        if (res.confirm) {
          const newSettings = {...this.data.settings, showIntro: false};
          this.setData({settings: newSettings});
          wx.setStorageSync('settings', newSettings);
        }
      }
    });
  },
  
  // 开始游戏
  startGame() {
    // 验证设置是否有效
    if (!this.validateSettings(this.data.settings)) {
      return;
    }
    
    // 生成目标数字
    const {minValue, maxValue} = this.data.settings;
    const targetNumber = Math.floor(Math.random() * (maxValue - minValue + 1)) + minValue;
    
    this.setData({
      gameState: 'RUNNING',
      targetNumber,
      attempts: 0,
      guessHistory: [],
      userInput: ''
    });
  },
  
  // 验证设置是否有效
  validateSettings(settings) {
    const {minValue, maxValue, challengeMode, maxAttempts} = settings;
    
    // 检查区间是否有效
    if (maxValue - minValue < 10) {
      wx.showToast({
        title: '区间长度至少为10',
        icon: 'none',
        duration: 2000
      });
      return false;
    }
    
    if (minValue >= maxValue) {
      wx.showToast({
        title: '最小值必须小于最大值',
        icon: 'none',
        duration: 2000
      });
      return false;
    }
    
    // 检查挑战模式设置
    if (challengeMode && (maxAttempts < 1 || !Number.isInteger(maxAttempts))) {
      wx.showToast({
        title: '挑战次数必须至少为1',
        icon: 'none',
        duration: 2000
      });
      return false;
    }
    
    return true;
  },
  
  // 打开设置
  openSettings() {
    this.setData({
      tempSettings: {...this.data.settings},
      showSettingsModal: true
    });
  },
  
  // 关闭设置
  closeSettings() {
    this.setData({showSettingsModal: false});
  },
  
  // 取消设置
  cancelSettings() {
    this.setData({
      tempSettings: {...this.data.settings},
      showSettingsModal: false
    });
  },
  
  // 保存设置
  saveSettings() {
    if (this.validateSettings(this.data.tempSettings)) {
      this.setData({
        settings: {...this.data.tempSettings},
        showSettingsModal: false
      });
      wx.setStorageSync('settings', this.data.tempSettings);
      wx.showToast({
        title: '设置已保存',
        icon: 'success',
        duration: 1000
      });
    }
  },
  
  // 输入变化事件
  onInputChange(e) {
    this.setData({userInput: e.detail.value});
  },
  
  // 最小值变化
  onMinChange(e) {
    const value = parseInt(e.detail.value) || 0;
    this.setData({
      'tempSettings.minValue': value
    });
  },
  
  // 最大值变化
  onMaxChange(e) {
    const value = parseInt(e.detail.value) || 0;
    this.setData({
      'tempSettings.maxValue': value
    });
  },
  
  // 模式变化
  onModeChange(e) {
    this.setData({
      'tempSettings.challengeMode': e.detail.value === 'challenge'
    });
  },
  
  // 最大尝试次数变化
  onMaxAttemptsChange(e) {
    const value = parseInt(e.detail.value) || 10;
    this.setData({
      'tempSettings.maxAttempts': value
    });
  },
  
  // 提交猜测
  submitGuess() {
    const {userInput, targetNumber, attempts, guessHistory, settings, gameState} = this.data;
    
    if (gameState !== 'RUNNING') return;
    
    // 验证输入
    const guess = parseInt(userInput);
    if (isNaN(guess)) {
      wx.showToast({
        title: '请输入有效的数字',
        icon: 'none',
        duration: 1500
      });
      return;
    }
    
    if (guess < settings.minValue || guess > settings.maxValue) {
      wx.showToast({
        title: `请输入${settings.minValue}到${settings.maxValue}之间的数字`,
        icon: 'none',
        duration: 2000
      });
      return;
    }
    
    // 处理猜测
    let result, resultTitle, resultMessage;
    const newAttempts = attempts + 1;
    const newHistory = [...guessHistory];
    
    if (guess === targetNumber) {
      // 猜对了
      result = '正确';
      resultTitle = '恭喜你！';
      resultMessage = `你猜对了，共尝试了${newAttempts}次`;
      
      this.setData({
        gameState: 'ENDED',
        gameResult: {
          status: 'win',
          attempts: newAttempts,
          target: targetNumber,
          date: new Date().toLocaleString()
        },
        showResultModal: true,
        resultTitle,
        resultMessage
      });
      
      // 保存到历史记录
      this.saveGameResult('win', newAttempts, targetNumber);
    } else if (guess < targetNumber) {
      // 猜小了
      result = '偏小';
    } else {
      // 猜大了
      result = '偏大';
    }
    
    // 检查挑战模式是否失败
    if (settings.challengeMode && newAttempts >= settings.maxAttempts && guess !== targetNumber) {
      resultTitle = '挑战失败';
      resultMessage = `已达到最大尝试次数${settings.maxAttempts}次`;
      
      this.setData({
        gameState: 'ENDED',
        gameResult: {
          status: 'lose',
          attempts: newAttempts,
          target: targetNumber,
          date: new Date().toLocaleString()
        },
        showResultModal: true,
        resultTitle,
        resultMessage
      });
      
      // 保存到历史记录
      this.saveGameResult('lose', newAttempts, targetNumber);
    }
    
    // 添加到历史记录
    newHistory.push({
      number: guess,
      result
    });
    
    // 更新界面
    this.setData({
      attempts: newAttempts,
      guessHistory: newHistory,
      userInput: ''
    });
  },
  
  // 放弃游戏
  giveUp() {
    const {targetNumber, attempts} = this.data;
    
    this.setData({
      gameState: 'ENDED',
      gameResult: {
        status: 'giveup',
        attempts,
        target: targetNumber,
        date: new Date().toLocaleString()
      },
      resultTitle: '游戏结束',
      resultMessage: '你放弃了本次游戏',
      showResultModal: true
    });
    
    // 保存到历史记录
    this.saveGameResult('giveup', attempts, targetNumber);
  },
  
  // 暂停游戏
  pauseGame() {
    this.setData({gameState: 'PAUSED'});
  },
  
  // 继续游戏
  resumeGame() {
    this.setData({gameState: 'RUNNING'});
  },
  
  // 关闭结果弹窗
  closeResult() {
    this.setData({showResultModal: false});
  },
  
  // 返回开始界面
  backToStart() {
    this.setData({
      gameState: 'NOT_STARTED',
      showResultModal: false
    });
  },
  
  // 再来一局
  playAgain() {
    this.setData({showResultModal: false});
    this.startGame();
  },
  
  // 前往历史记录页面
  goToHistory() {
    wx.navigateTo({
      url: '/pages/history/history'
    });
  },
  
  // 保存游戏结果到历史记录
  saveGameResult(status, attempts, target) {
    const {settings} = this.data;
    const history = wx.getStorageSync('gameHistory') || [];
    
    const result = {
      id: Date.now(),
      date: new Date().toLocaleString(),
      status, // win, lose, giveup
      attempts,
      target,
      min: settings.minValue,
      max: settings.maxValue,
      mode: settings.challengeMode ? 'challenge' : 'normal',
      maxAttempts: settings.maxAttempts
    };
    
    history.unshift(result); // 添加到开头
    // 限制历史记录数量为100条
    if (history.length > 100) {
      history.pop();
    }
    
    wx.setStorageSync('gameHistory', history);
  },
  
  // 计算剩余尝试次数
  get remainingAttempts() {
    const {settings, attempts} = this.data;
    if (!settings.challengeMode) return 0;
    return Math.max(0, settings.maxAttempts - attempts);
  },
  
  // 获取状态文本
  get statusText() {
    const {gameState} = this.data;
    switch (gameState) {
      case 'NOT_STARTED': return '未开始';
      case 'RUNNING': return '进行中';
      case 'PAUSED': return '已暂停';
      case 'ENDED': return '已结束';
      default: return '';
    }
  }
});