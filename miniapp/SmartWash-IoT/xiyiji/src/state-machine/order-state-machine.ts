import { OrderStatus } from '../types/enums';
import { Order } from '../models/db';

/**
 * 订单状态机
 * 管理订单状态的合法流转
 */
export class OrderStateMachine {
  /**
   * 检查状态流转是否合法
   * @param currentStatus 当前状态
   * @param targetStatus 目标状态
   * @returns 是否允许流转
   */
  static canTransition(currentStatus: OrderStatus, targetStatus: OrderStatus): boolean {
    const allowedTransitions: Record<OrderStatus, OrderStatus[]> = {
      [OrderStatus.CREATED]: [OrderStatus.PAID, OrderStatus.CANCELLED],
      [OrderStatus.PAID]: [OrderStatus.RUNNING, OrderStatus.REFUNDING],
      [OrderStatus.RUNNING]: [OrderStatus.FINISHED, OrderStatus.FAILED],
      [OrderStatus.FINISHED]: [],
      [OrderStatus.FAILED]: [OrderStatus.REFUNDED],
      [OrderStatus.CANCELLED]: [],
      [OrderStatus.REFUNDING]: [OrderStatus.REFUNDED],
      [OrderStatus.REFUNDED]: []
    };

    return allowedTransitions[currentStatus]?.includes(targetStatus) || false;
  }

  /**
   * 执行状态流转
   * @param order 订单对象
   * @param targetStatus 目标状态
   * @returns 更新后的订单对象
   * @throws 不合法的状态流转时抛出错误
   */
  static transition(order: Order, targetStatus: OrderStatus): Order {
    if (!this.canTransition(order.status, targetStatus)) {
      throw new Error(`Invalid state transition: ${order.status} -> ${targetStatus}`);
    }

    const updatedOrder = { ...order, status: targetStatus, update_time: new Date() };

    // 根据目标状态更新相应字段
    switch (targetStatus) {
      case OrderStatus.PAID:
        updatedOrder.pay_time = new Date();
        break;
      case OrderStatus.RUNNING:
        updatedOrder.start_time = new Date();
        break;
      case OrderStatus.FINISHED:
      case OrderStatus.FAILED:
        updatedOrder.end_time = new Date();
        break;
      default:
        break;
    }

    return updatedOrder;
  }
}
