import { OrderStatus, DeviceStatus } from '../types/enums';
import { OrderStateMachine } from '../state-machine/order-state-machine';
import { DeviceStateMachine } from '../state-machine/device-state-machine';

/**
 * 状态机测试
 * 验证订单状态和设备状态的流转规则
 */

// 测试订单状态机
console.log('=== 测试订单状态机 ===');

// 测试CREATED状态流转
console.log('CREATED -> PAID:', OrderStateMachine.canTransition(OrderStatus.CREATED, OrderStatus.PAID)); // 应该为true
console.log('CREATED -> CANCELLED:', OrderStateMachine.canTransition(OrderStatus.CREATED, OrderStatus.CANCELLED)); // 应该为true
console.log('CREATED -> RUNNING:', OrderStateMachine.canTransition(OrderStatus.CREATED, OrderStatus.RUNNING)); // 应该为false

// 测试PAID状态流转
console.log('PAID -> RUNNING:', OrderStateMachine.canTransition(OrderStatus.PAID, OrderStatus.RUNNING)); // 应该为true
console.log('PAID -> REFUNDING:', OrderStateMachine.canTransition(OrderStatus.PAID, OrderStatus.REFUNDING)); // 应该为true
console.log('PAID -> CANCELLED:', OrderStateMachine.canTransition(OrderStatus.PAID, OrderStatus.CANCELLED)); // 应该为false

// 测试RUNNING状态流转
console.log('RUNNING -> FINISHED:', OrderStateMachine.canTransition(OrderStatus.RUNNING, OrderStatus.FINISHED)); // 应该为true
console.log('RUNNING -> FAILED:', OrderStateMachine.canTransition(OrderStatus.RUNNING, OrderStatus.FAILED)); // 应该为true
console.log('RUNNING -> PAID:', OrderStateMachine.canTransition(OrderStatus.RUNNING, OrderStatus.PAID)); // 应该为false

// 测试FINISHED状态流转
console.log('FINISHED -> any:', OrderStateMachine.canTransition(OrderStatus.FINISHED, OrderStatus.PAID)); // 应该为false

// 测试设备状态机
console.log('\n=== 测试设备状态机 ===');

// 测试设备是否可下单
const idleDevice = { status: DeviceStatus.IDLE } as any;
const runningDevice = { status: DeviceStatus.RUNNING } as any;
const offlineDevice = { status: DeviceStatus.OFFLINE } as any;

console.log('IDLE设备可下单:', DeviceStateMachine.isAvailableForOrder(idleDevice)); // 应该为true
console.log('RUNNING设备可下单:', DeviceStateMachine.isAvailableForOrder(runningDevice)); // 应该为false
console.log('OFFLINE设备可下单:', DeviceStateMachine.isAvailableForOrder(offlineDevice)); // 应该为false

console.log('\n=== 测试完成 ===');
