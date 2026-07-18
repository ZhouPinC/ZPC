// wx_xiyiji/config/index.js
// 环境配置

// 开发环境
const DEV = {
  apiBaseUrl: 'http://localhost:8080/api',
  mqttBroker: 'ws://localhost:8083/mqtt',
  refreshInterval: 3000,
};

// 生产环境
const PROD = {
  apiBaseUrl: 'https://your-domain.com/api',
  mqttBroker: 'wss://your-domain.com/mqtt',
  refreshInterval: 3000,
};

// 当前环境 (可通过条件编译或环境变量切换)
const ENV = 'DEV'; // 'DEV' | 'PROD'

const config = ENV === 'PROD' ? PROD : DEV;

module.exports = {
  ...config,
  ENV,
  // API版本
  API_VERSION: 'v1',
  // Token存储key
  TOKEN_KEY: 'auth_token',
  USER_KEY: 'user_info',
  LOGIN_TIME_KEY: 'login_time',
  // Token过期时间 (180天)
  TOKEN_EXPIRE_DAYS: 180,
};
