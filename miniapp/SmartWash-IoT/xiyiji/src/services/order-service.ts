import { OrderStatus } from '../types/enums';
import { Order } from '../models/db';
import { OrderStateMachine } from '../state-machine/order-state-machine';
import { DeviceStateMachine } from '../state-machine/device-state-machine';
import { generateOrderNo } from '../utils/order-utils';
import { mqttClient } from '../mqtt/client';

/**
 * 订单服务
 * 处理订单相关的核心业务逻辑
 */
export class OrderService {
  /**
   * 用户扫码下单
   * @param userId 用户ID
   * @param deviceId 设备ID
   * @param amount 支付金额
   * @param durationMinutes 洗衣时长
   * @returns 创建的订单
   * @throws 设备不可用时抛出错误
   */
  static async createOrder(userId: number, deviceId: number, amount: number, durationMinutes: number): Promise<Order> {
    // 1. 查询设备状态（模拟数据库查询）
    const device = await this.getDeviceById(deviceId);
    
    // 2. 检查设备是否可下单
    if (!DeviceStateMachine.isAvailableForOrder(device)) {
      throw new Error('设备忙或离线');
    }
    
    // 3. 创建订单
    const order: Order = {
      id: 0, // 数据库自增
      order_no: generateOrderNo(),
      user_id: userId,
      device_id: deviceId,
      amount,
      duration_minutes: durationMinutes,
      status: OrderStatus.CREATED,
      create_time: new Date(),
      update_time: new Date()
    };
    
    // 4. 保存订单到数据库（模拟）
    const savedOrder = await this.saveOrder(order);
    
    return savedOrder;
  }
  
  /**
   * 支付成功回调
   * @param orderId 订单ID
   * @returns 更新后的订单
   */
  static async handlePaymentSuccess(orderId: number): Promise<Order> {
    // 1. 查询订单
    const order = await this.getOrderById(orderId);
    
    // 2. 更新订单状态为已支付
    const updatedOrder = OrderStateMachine.transition(order, OrderStatus.PAID);
    
    // 3. 保存订单
    await this.saveOrder(updatedOrder);
    
    // 4. 发送MQTT指令给设备
    const device = await this.getDeviceById(order.device_id);
    const mqttTopic = `cmd/${device.device_sn}`;
    const mqttPayload = {
      cmd: 'START',
      duration: order.duration_minutes,
      orderId: order.order_no
    };
    
    await mqttClient.publish(mqttTopic, JSON.stringify(mqttPayload));
    
    return updatedOrder;
  }
  
  /**
   * 处理设备状态上报
   * @param deviceSn 设备序列号
   * @param status 设备状态
   * @param orderId 关联订单ID
   */
  static async handleDeviceStatusReport(deviceSn: string, status: string, orderId?: string): Promise<void> {
    // 1. 查询设备
    const device = await this.getDeviceBySn(deviceSn);
    
    // 2. 更新设备状态
    const updatedDevice = DeviceStateMachine.transition(device, status as any);
    await this.saveDevice(updatedDevice);
    
    // 3. 如果有订单ID，更新订单状态
    if (orderId) {
      const order = await this.getOrderByOrderNo(orderId);
      
      if (status === 'RUNNING') {
        // 设备开始工作，更新订单为运行中
        const updatedOrder = OrderStateMachine.transition(order, OrderStatus.RUNNING);
        await this.saveOrder(updatedOrder);
      } else if (status === 'IDLE') {
        // 设备空闲，更新订单为完成
        const updatedOrder = OrderStateMachine.transition(order, OrderStatus.FINISHED);
        await this.saveOrder(updatedOrder);
      }
    }
  }
  
  // 模拟数据库操作
  private static async getDeviceById(deviceId: number): Promise<any> {
    // 实际项目中这里应该是数据库查询
    return { id: deviceId, device_sn: `device_${deviceId}`, status: 'IDLE' };
  }
  
  private static async getDeviceBySn(deviceSn: string): Promise<any> {
    // 实际项目中这里应该是数据库查询
    return { id: parseInt(deviceSn.split('_')[1]), device_sn: deviceSn, status: 'IDLE' };
  }
  
  private static async getOrderById(orderId: number): Promise<any> {
    // 实际项目中这里应该是数据库查询
    return { id: orderId, device_id: 1, status: 'CREATED', duration_minutes: 30, order_no: `order_${orderId}` };
  }
  
  private static async getOrderByOrderNo(orderNo: string): Promise<any> {
    // 实际项目中这里应该是数据库查询
    return { id: parseInt(orderNo.split('_')[1]), device_id: 1, status: 'PAID', duration_minutes: 30, order_no: orderNo };
  }
  
  private static async saveOrder(order: Order): Promise<Order> {
    // 实际项目中这里应该是数据库保存
    return order;
  }
  
  private static async saveDevice(device: any): Promise<any> {
    // 实际项目中这里应该是数据库保存
    return device;
  }
}
