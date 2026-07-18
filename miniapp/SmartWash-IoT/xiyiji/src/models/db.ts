import { OrderStatus, DeviceStatus } from '../types/enums';

// 用户模型
export interface User {
  id: number;
  openid: string;
  nickname?: string;
  balance?: number;
  create_time: Date;
  update_time: Date;
}

// 设备模型
export interface Device {
  id: number;
  device_sn: string;
  mac_address?: string;
  status: DeviceStatus;
  location?: string;
  last_heartbeat?: Date;
  create_time: Date;
  update_time: Date;
}

// 订单模型
export interface Order {
  id: number;
  order_no: string;
  user_id: number;
  device_id: number;
  amount: number;
  duration_minutes: number;
  status: OrderStatus;
  pay_time?: Date;
  start_time?: Date;
  end_time?: Date;
  create_time: Date;
  update_time: Date;
}
