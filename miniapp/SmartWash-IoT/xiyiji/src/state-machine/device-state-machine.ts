import { DeviceStatus } from '../types/enums';
import { Device } from '../models/db';

/**
 * 设备状态机
 * 管理设备状态的合法流转
 */
export class DeviceStateMachine {
  /**
   * 检查状态流转是否合法
   * @param currentStatus 当前状态
   * @param targetStatus 目标状态
   * @returns 是否允许流转
   */
  static canTransition(currentStatus: DeviceStatus, targetStatus: DeviceStatus): boolean {
    // 设备状态流转相对灵活，主要基于设备上报
    // 基本规则：离线状态可以流转到任何在线状态，在线状态之间可以相互流转
    return true;
  }

  /**
   * 执行状态流转
   * @param device 设备对象
   * @param targetStatus 目标状态
   * @returns 更新后的设备对象
   * @throws 不合法的状态流转时抛出错误
   */
  static transition(device: Device, targetStatus: DeviceStatus): Device {
    if (!this.canTransition(device.status, targetStatus)) {
      throw new Error(`Invalid device state transition: ${device.status} -> ${targetStatus}`);
    }

    return {
      ...device,
      status: targetStatus,
      update_time: new Date()
    };
  }

  /**
   * 检查设备是否可下单
   * @param device 设备对象
   * @returns 是否可下单
   */
  static isAvailableForOrder(device: Device): boolean {
    return device.status === DeviceStatus.IDLE;
  }
}
