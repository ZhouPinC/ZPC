// 游戏状态枚举
const GameState = {
  NOT_STARTED: 'not_started',
  RUNNING: 'running',
  PAUSED: 'paused',
  ENDED: 'ended'
};

// 游戏模式枚举
const GameMode = {
  GUESS: 'guess',
  CHALLENGE: 'challenge'
};

// 游戏配置
const settings = {
  minValue: 0,
  maxValue: 100,
  mode: GameMode.GUESS,
  maxAttempts: 10,
  fontSize: 16,
  fontColor: '#333333'
};

// 游戏状态
let gameState = GameState.NOT_STARTED;
let targetNumber = null;
let attempts = 0;
let currentHistory = [];
let gameHistory = JSON.parse(localStorage.getItem('guessNumberHistory') || '[]');

// DOM 元素
const panels = {
  start: document.getElementById('start-panel'),
  game: document.getElementById('game-panel'),
  history: document.getElementById('history-panel')
};

const modals = {
  settings: document.getElementById('settings-modal'),
  pause: document.getElementById('pause-modal'),
  result: document.getElementById('result-modal')
};

const buttons = {
  start: document.getElementById('start-btn'),
  settings: document.getElementById('settings-btn'),
  history: document.getElementById('history-btn'),
  exit: document.getElementById('exit-btn'),
  submit: document.getElementById('submit-btn'),
  giveUp: document.getElementById('give-up-btn'),
  pause: document.getElementById('pause-btn'),
  back: document.getElementById('back-btn'),
  backFromHistory: document.getElementById('back-from-history'),
  clearHistory: document.getElementById('clear-history-btn'),
  saveSettings: document.getElementById('save-settings'),
  cancelSettings: document.getElementById('cancel-settings'),
  resume: document.getElementById('resume-btn'),
  playAgain: document.getElementById('play-again-btn'),
  backToStart: document.getElementById('back-to-start-btn')
};

const inputs = {
  guess: document.getElementById('guess-input'),
  minValue: document.getElementById('min-value'),
  maxValue: document.getElementById('max-value'),
  maxAttempts: document.getElementById('max-attempts'),
  fontSize: document.getElementById('font-size'),
  fontColor: document.getElementById('font-color'),
  gameMode: document.querySelectorAll('input[name="game-mode"]')
};

const displays = {
  gameModeInfo: document.getElementById('game-mode-info'),
  rangeInfo: document.getElementById('range-info'),
  statusInfo: document.getElementById('status-info'),
  guessHistory: document.getElementById('guess-history'),
  allHistory: document.getElementById('all-history'),
  pauseInfo: document.getElementById('pause-info'),
  pauseStats: document.getElementById('pause-stats'),
  resultTitle: document.getElementById('result-title'),
  resultMessage: document.getElementById('result-message')
};

// 初始化
function init() {
  loadSettings();
  updateSettingsForm();
  setupEventListeners();
  updateHistoryDisplay();
}

// 加载保存的设置
function loadSettings() {
  const savedSettings = localStorage.getItem('guessNumberSettings');
  if (savedSettings) {
      Object.assign(settings, JSON.parse(savedSettings));
      applyVisualSettings();
  }
}

// 保存设置
function saveSettings() {
  localStorage.setItem('guessNumberSettings', JSON.stringify(settings));
}

// 更新设置表单
function updateSettingsForm() {
  inputs.minValue.value = settings.minValue;
  inputs.maxValue.value = settings.maxValue;
  inputs.maxAttempts.value = settings.maxAttempts;
  inputs.fontSize.value = settings.fontSize;
  inputs.fontColor.value = settings.fontColor;
  
  inputs.gameMode.forEach(radio => {
      radio.checked = radio.value === settings.mode;
  });
  
  // 根据游戏模式显示/隐藏尝试次数设置
  document.getElementById('attempts-setting').style.display = 
      settings.mode === GameMode.CHALLENGE ? 'block' : 'none';
}

// 应用视觉设置
function applyVisualSettings() {
  document.body.style.fontSize = `${settings.fontSize}px`;
  document.body.style.color = settings.fontColor;
}

// 设置事件监听器
function setupEventListeners() {
  // 开始界面按钮
  buttons.start.addEventListener('click', startGame);
  buttons.settings.addEventListener('click', () => modals.settings.style.display = 'flex');
  buttons.history.addEventListener('click', showHistoryPanel);
  buttons.exit.addEventListener('click', () => {
      if (confirm('确认退出游戏吗？')) {
          window.close();
      }
  });
  
  // 游戏界面按钮
  buttons.submit.addEventListener('click', submitGuess);
  buttons.giveUp.addEventListener('click', giveUp);
  buttons.pause.addEventListener('click', pauseGame);
  buttons.back.addEventListener('click', backToStart);
  
  // 历史记录界面按钮
  buttons.backFromHistory.addEventListener('click', showStartPanel);
  buttons.clearHistory.addEventListener('click', clearHistory);
  
  // 设置弹窗按钮
  buttons.saveSettings.addEventListener('click', saveSettingsHandler);
  buttons.cancelSettings.addEventListener('click', () => modals.settings.style.display = 'none');
  
  // 暂停弹窗按钮
  buttons.resume.addEventListener('click', resumeGame);
  
  // 结果弹窗按钮
  buttons.playAgain.addEventListener('click', startGame);
  buttons.backToStart.addEventListener('click', backToStart);
  
  // 输入框事件
  inputs.guess.addEventListener('keypress', (e) => {
      if (e.key === 'Enter') {
          submitGuess();
      }
  });
  
  // 游戏模式切换
  inputs.gameMode.forEach(radio => {
      radio.addEventListener('change', (e) => {
          document.getElementById('attempts-setting').style.display = 
              e.target.value === GameMode.CHALLENGE ? 'block' : 'none';
      });
  });
}

// 显示指定面板
function showPanel(panelName) {
  Object.keys(panels).forEach(key => {
      panels[key].classList.remove('active');
  });
  panels[panelName].classList.add('active');
}

// 显示开始面板
function showStartPanel() {
  showPanel('start');
}

// 显示游戏面板
function showGamePanel() {
  showPanel('game');
  updateGameInfo();
}

// 显示历史记录面板
function showHistoryPanel() {
  showPanel('history');
  updateHistoryDisplay();
}

// 开始新游戏
function startGame() {
  targetNumber = Math.floor(Math.random() * (settings.maxValue - settings.minValue + 1)) + settings.minValue;
  attempts = 0;
  currentHistory = [];
  gameState = GameState.RUNNING;

  modals.pause.style.display = 'none';
  modals.result.style.display = 'none';
  
  displays.guessHistory.innerHTML = '';
  inputs.guess.value = '';
  inputs.guess.focus();
  
  showGamePanel();
  updateStatus();
}

// 提交猜测
function submitGuess() {
  if (gameState !== GameState.RUNNING) return;
  
  const guessStr = inputs.guess.value.trim();
  if (!guessStr) return;
  
  const guess = parseInt(guessStr, 10);
  
  // 验证输入
  if (isNaN(guess) || guess < settings.minValue || guess > settings.maxValue) {
      alert(`请输入${settings.minValue}到${settings.maxValue}之间的整数`);
      return;
  }
  
  attempts++;
  
  // 记录猜测
  let result;
  if (guess === targetNumber) {
      result = 'correct';
      gameState = GameState.ENDED;
      saveGameResult(true);
      showResultModal(true);
  } else if (guess < targetNumber) {
      result = 'low';
  } else {
      result = 'high';
  }
  
  // 检查挑战模式是否失败
  if (settings.mode === GameMode.CHALLENGE && attempts >= settings.maxAttempts && result !== 'correct') {
      result = 'failed';
      gameState = GameState.ENDED;
      saveGameResult(false);
      showResultModal(false);
  }
  
  // 添加到历史并更新显示
  const historyItem = {
      guess,
      result,
      time: new Date().toLocaleTimeString()
  };
  currentHistory.push(historyItem);
  addHistoryItem(historyItem);
  
  // 清空输入框
  inputs.guess.value = '';
  inputs.guess.focus();
  
  // 更新状态
  updateStatus();
}

// 放弃游戏
function giveUp() {
  if (gameState !== GameState.RUNNING) return;
  
  gameState = GameState.ENDED;
  saveGameResult(false, true);
  showResultModal(false, true);
}

// 暂停游戏
function pauseGame() {
  if (gameState !== GameState.RUNNING) return;
  
  gameState = GameState.PAUSED;
  displays.pauseInfo.textContent = `当前模式：${settings.mode === GameMode.GUESS ? '猜数字模式' : '挑战模式'}`;
  displays.pauseStats.textContent = `已猜次数：${attempts}`;
  modals.pause.style.display = 'flex';
}

// 恢复游戏
function resumeGame() {
  gameState = GameState.RUNNING;
  modals.pause.style.display = 'none';
  inputs.guess.focus();
}

// 返回开始界面
function backToStart() {
  if (gameState === GameState.RUNNING) {
      if (!confirm('当前游戏正在进行中，确定要返回首页吗？')) {
          return;
      }
  }
  
  gameState = GameState.NOT_STARTED;
  modals.pause.style.display = 'none';
  modals.result.style.display = 'none';
  showStartPanel();
}

// 保存游戏结果
function saveGameResult(success, gaveUp = false) {
  const result = {
      date: new Date().toLocaleString(),
      mode: settings.mode === GameMode.GUESS ? '猜数字模式' : '挑战模式',
      range: `${settings.minValue} - ${settings.maxValue}`,
      target: targetNumber,
      attempts,
      success,
      gaveUp
  };
  
  gameHistory.unshift(result);
  localStorage.setItem('guessNumberHistory', JSON.stringify(gameHistory));
}

// 显示结果弹窗
function showResultModal(success, gaveUp = false) {
  if (success) {
      displays.resultTitle.textContent = '恭喜你！';
      displays.resultMessage.textContent = `你猜对了！答案就是 ${targetNumber}，共用了 ${attempts} 次尝试。`;
  } else if (gaveUp) {
      displays.resultTitle.textContent = '游戏结束';
      displays.resultMessage.textContent = `你放弃了游戏。正确答案是 ${targetNumber}。`;
  } else {
      displays.resultTitle.textContent = '挑战失败';
      displays.resultMessage.textContent = `很遗憾，你没有猜对。正确答案是 ${targetNumber}。`;
  }
  
  modals.result.style.display = 'flex';
}

// 添加历史记录项
function addHistoryItem(item) {
  const div = document.createElement('div');
  div.className = `history-item ${item.result}`;
  
  let resultText;
  switch (item.result) {
      case 'correct':
          resultText = '正确！';
          break;
      case 'low':
          resultText = '偏小';
          break;
      case 'high':
          resultText = '偏大';
          break;
      case 'failed':
          resultText = '失败';
          break;
  }
  
  div.innerHTML = `
      <strong>${item.time}</strong> - 你猜的是 ${item.guess}，${resultText}
      ${settings.mode === GameMode.CHALLENGE ? `<br>已猜 ${attempts}/${settings.maxAttempts} 次` : ''}
  `;
  
  displays.guessHistory.appendChild(div);
  displays.guessHistory.scrollTop = displays.guessHistory.scrollHeight;
}

// 更新游戏信息显示
function updateGameInfo() {
  displays.gameModeInfo.textContent = `当前模式：${settings.mode === GameMode.GUESS ? '猜数字模式' : '挑战模式'}`;
  displays.rangeInfo.textContent = `区间：${settings.minValue} ～ ${settings.maxValue}${
      settings.mode === GameMode.CHALLENGE ? `（剩余 ${settings.maxAttempts} 次）` : ''
  }`;
}

// 更新状态信息
function updateStatus() {
  if (gameState === GameState.RUNNING) {
      displays.statusInfo.textContent = `状态：已猜 ${attempts} 次${
          settings.mode === GameMode.CHALLENGE ? `，剩余 ${settings.maxAttempts - attempts} 次` : ''
      }`;
  } else if (gameState === GameState.PAUSED) {
      displays.statusInfo.textContent = '状态：游戏已暂停';
  } else if (gameState === GameState.ENDED) {
      displays.statusInfo.textContent = '状态：游戏已结束';
  }
}

// 更新历史记录显示
function updateHistoryDisplay() {
  if (gameHistory.length === 0) {
      displays.allHistory.innerHTML = '<p class="empty-history">暂无历史记录</p>';
      return;
  }
  
  displays.allHistory.innerHTML = '';
  
  gameHistory.forEach((record, index) => {
      const div = document.createElement('div');
      div.className = 'game-record';
      
      div.innerHTML = `
          <p><strong>游戏 ${index + 1}：${record.date}</strong></p>
          <p>模式：${record.mode}，区间：${record.range}</p>
          <p>答案：${record.target}，尝试次数：${record.attempts}</p>
          <p>结果：${record.success ? '成功' : record.gaveUp ? '放弃' : '失败'}</p>
      `;
      
      displays.allHistory.appendChild(div);
  });
}

// 保存设置处理函数
function saveSettingsHandler() {
  const min = parseInt(inputs.minValue.value, 10);
  const max = parseInt(inputs.maxValue.value, 10);
  const attempts = parseInt(inputs.maxAttempts.value, 10);
  const fontSize = parseInt(inputs.fontSize.value, 10);
  const fontColor = inputs.fontColor.value;
  const mode = document.querySelector('input[name="game-mode"]:checked').value;
  
  // 验证区间设置
  if (isNaN(min) || isNaN(max) || min >= max || max - min < 10) {
      alert('请输入有效的数值区间，要求最大值大于最小值且区间长度至少为10');
      return;
  }
  
  // 验证尝试次数
  if (mode === GameMode.CHALLENGE && (isNaN(attempts) || attempts < 1)) {
      alert('请输入有效的限定次数，至少为1');
      return;
  }
  
  // 验证字体大小
  if (isNaN(fontSize) || fontSize < 12 || fontSize > 24) {
      alert('请输入有效的字体大小，范围12-24');
      return;
  }
  
  // 保存设置
  settings.minValue = min;
  settings.maxValue = max;
  settings.mode = mode;
  settings.maxAttempts = attempts;
  settings.fontSize = fontSize;
  settings.fontColor = fontColor;
  
  saveSettings();
  applyVisualSettings();
  
  modals.settings.style.display = 'none';
  alert('设置已保存');
}

// 清空历史记录
function clearHistory() {
  if (confirm('确定要清空所有历史记录吗？此操作不可恢复！')) {
    gameHistory = [];
    localStorage.setItem('guessNumberHistory', '[]');
    updateHistoryDisplay();
    alert('历史记录已清空');
  }
}

// 初始化游戏
document.addEventListener('DOMContentLoaded', init);