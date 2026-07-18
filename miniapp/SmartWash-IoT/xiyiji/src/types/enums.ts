// 订单状态枚举
export enum OrderStatus {
  CREATED = 'CREATED',  // 订单创建（待支付）
  PAID = 'PAID',        // 支付成功（待启动）
  RUNNING = 'RUNNING',  // 洗衣中
  FINISHED = 'FINISHED',// 订单完成
  FAILED = 'FAILED',    // 异常结束（需介入）
  CANCELLED = 'CANCELLED', // 超时未支付取消
  REFUNDING = 'REFUNDING', // 退款中
  REFUNDED = 'REFUNDED'    // 已退款
}

// 设备状态枚举
export enum DeviceStatus {
  OFFLINE = 'OFFLINE',  // 离线
  IDLE = 'IDLE',        // 空闲
  RUNNING = 'RUNNING',  // 工作中
  FAULT = 'FAULT'       // 故障
}
