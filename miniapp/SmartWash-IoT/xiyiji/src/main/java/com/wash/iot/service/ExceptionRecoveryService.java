package com.wash.iot.service;

import com.wash.iot.entity.*;
import com.wash.iot.repository.*;
import lombok.extern.slf4j.Slf4j;
import com.wash.iot.enums.DeviceStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 异常恢复服务
 * 处理系统异常情况，自动恢复和人工干预机制
 */
@Slf4j
@Service
@Transactional
public class ExceptionRecoveryService {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentTxnRepository paymentTxnRepository;

    @Autowired
    private QueueRepository queueRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private IncomeRecordRepository incomeRecordRepository;

    @Autowired
    private DeviceStateManager deviceStateManager;

    @Autowired
    private OrderLifecycleService orderLifecycleService;

    @Autowired
    private RefundService refundService;

    @Autowired
    private NotificationService notificationService;

    // 异常计数器
    private final Map<String, Integer> exceptionCounters = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastExceptionTime = new ConcurrentHashMap<>();

    // 异常阈值配置
    private static final int MAX_EXCEPTION_COUNT = 3;
    private static final int RECOVERY_COOLDOWN_MINUTES = 30;
    private static final int DEVICE_OFFLINE_MINUTES = 30; // 开发环境调大阈值，防止频繁离线
    private static final int ORDER_STUCK_MINUTES = 60;

    /**
     * 处理MQTT连接异常
     */
    public void handleMqttConnectionException(String deviceSn, String errorMessage) {
        try {
            String key = "mqtt_" + deviceSn;
            incrementExceptionCounter(key);

            if (shouldTriggerRecovery(key)) {
                log.warn("MQTT连接异常次数过多，触发恢复机制: deviceSn={}, count={}",
                        deviceSn, exceptionCounters.get(key));

                // 标记设备离线
                markDeviceOffline(deviceSn, "MQTT连接异常: " + errorMessage);

                // 检查相关订单
                checkRelatedOrders(deviceSn);

                // 通知管理员
                notifyAdmin("MQTT连接异常", "设备 " + deviceSn + " MQTT连接异常: " + errorMessage);
            }

        } catch (Exception e) {
            log.error("处理MQTT连接异常失败: deviceSn={}", deviceSn, e);
        }
    }

    /**
     * 处理支付异常
     */
    public void handlePaymentException(String orderNo, String errorMessage) {
        try {
            String key = "payment_" + orderNo;
            incrementExceptionCounter(key);

            if (shouldTriggerRecovery(key)) {
                log.warn("支付异常次数过多，触发恢复机制: orderNo={}, count={}",
                        orderNo, exceptionCounters.get(key));

                Order order = orderRepository.findByOrderNo(orderNo).orElse(null);
                if (order != null) {
                    // 取消订单
                    orderLifecycleService.cancelOrder(order, "支付异常自动取消");

                    // 通知用户
                    notificationService.sendPaymentExceptionNotification(order, errorMessage);
                }

                // 通知管理员
                notifyAdmin("支付异常", "订单 " + orderNo + " 支付异常: " + errorMessage);
            }

        } catch (Exception e) {
            log.error("处理支付异常失败: orderNo={}", orderNo, e);
        }
    }

    /**
     * 处理设备状态异常
     */
    public void handleDeviceStatusException(String deviceSn, String errorMessage) {
        try {
            String key = "device_status_" + deviceSn;
            incrementExceptionCounter(key);

            if (shouldTriggerRecovery(key)) {
                log.warn("设备状态异常次数过多，触发恢复机制: deviceSn={}, count={}",
                        deviceSn, exceptionCounters.get(key));

                // 重置设备状态
                deviceStateManager.resetDeviceStatus(deviceSn, "状态异常自动恢复");

                // 检查相关订单
                checkRelatedOrders(deviceSn);

                // 通知管理员
                notifyAdmin("设备状态异常", "设备 " + deviceSn + " 状态异常: " + errorMessage);
            }

        } catch (Exception e) {
            log.error("处理设备状态异常失败: deviceSn={}", deviceSn, e);
        }
    }

    /**
     * 定时检查设备离线
     */
    @Scheduled(fixedDelay = 300000) // 每5分钟执行一次
    public void checkOfflineDevices() {
        try {
            LocalDateTime offlineThreshold = LocalDateTime.now().minusMinutes(DEVICE_OFFLINE_MINUTES);
            List<Device> offlineDevices = deviceRepository.findOfflineDevices(offlineThreshold);

            for (Device device : offlineDevices) {
                if (!DeviceStatus.OFFLINE.name().equals(device.getStatus())) {
                    log.warn("检测到设备离线: deviceSn={}, lastHeartbeat={}",
                            device.getDeviceSn(), device.getLastHeartbeat());

                    // 标记设备离线
                    markDeviceOffline(device.getDeviceSn(), "心跳超时自动离线");

                    // 检查相关订单
                    checkRelatedOrders(device.getDeviceSn());
                }
            }

        } catch (Exception e) {
            log.error("检查离线设备失败", e);
        }
    }

    /**
     * 定时检查卡单情况
     */
    @Scheduled(fixedDelay = 600000) // 每10分钟执行一次
    public void checkStuckOrders() {
        try {
            LocalDateTime stuckThreshold = LocalDateTime.now().minusMinutes(ORDER_STUCK_MINUTES);
            List<Order> stuckOrders = orderRepository.findStuckOrders(stuckThreshold);

            for (Order order : stuckOrders) {
                log.warn("检测到卡单情况: orderNo={}, status={}, createTime={}",
                        order.getOrderNo(), order.getStatus(), order.getCreateTime());

                // 根据订单状态处理
                handleStuckOrder(order);
            }

        } catch (Exception e) {
            log.error("检查卡单情况失败", e);
        }
    }

    /**
     * 定时检查数据一致性
     */
    @Scheduled(fixedDelay = 1800000) // 每30分钟执行一次
    public void checkDataConsistency() {
        try {
            checkDeviceOrderConsistency();
            checkPaymentOrderConsistency();
            checkIncomeRecordConsistency();

        } catch (Exception e) {
            log.error("数据一致性检查失败", e);
        }
    }

    /**
     * 手动触发异常恢复
     */
    public void manualRecovery(String recoveryType, String targetId, String reason) {
        try {
            log.info("手动触发异常恢复: type={}, targetId={}, reason={}", recoveryType, targetId, reason);

            switch (recoveryType.toLowerCase()) {
                case "device":
                    recoverDevice(targetId, reason);
                    break;
                case "order":
                    recoverOrder(targetId, reason);
                    break;
                case "payment":
                    recoverPayment(targetId, reason);
                    break;
                default:
                    throw new RuntimeException("不支持的恢复类型: " + recoveryType);
            }

            log.info("手动异常恢复成功: type={}, targetId={}", recoveryType, targetId);

        } catch (Exception e) {
            log.error("手动异常恢复失败: type={}, targetId={}", recoveryType, targetId, e);
            throw new RuntimeException("手动异常恢复失败: " + e.getMessage());
        }
    }

    /**
     * 标记设备离线
     */
    private void markDeviceOffline(String deviceSn, String reason) {
        try {
            Device device = deviceRepository.findByDeviceSn(deviceSn).orElse(null);
            if (device != null) {
                device.setStatus(DeviceStatus.OFFLINE.name());
                deviceRepository.save(device);

                // 记录状态历史
                recordDeviceStatusHistory(device.getId(), device.getStatus(), "OFFLINE", reason);

                log.info("设备标记离线: deviceSn={}, reason={}", deviceSn, reason);
            }
        } catch (Exception e) {
            log.error("标记设备离线失败: deviceSn={}", deviceSn, e);
        }
    }

    /**
     * 检查相关订单
     */
    private void checkRelatedOrders(String deviceSn) {
        try {
            Device device = deviceRepository.findByDeviceSn(deviceSn).orElse(null);
            if (device != null && device.getCurrentOrderId() != null) {
                Order order = orderRepository.findById(device.getCurrentOrderId()).orElse(null);
                if (order != null && ("RUNNING".equals(order.getStatus()) || "PAID".equals(order.getStatus()))) {
                    orderLifecycleService.markOrderAbnormal(order, "设备异常");
                }
            }
        } catch (Exception e) {
            log.error("检查相关订单失败: deviceSn={}", deviceSn, e);
        }
    }

    /**
     * 处理卡单情况
     */
    private void handleStuckOrder(Order order) {
        try {
            switch (order.getStatus()) {
                case "CREATED":
                    // 创建超时，自动取消
                    orderLifecycleService.cancelOrder(order, "创建超时自动取消");
                    break;
                case "PAID":
                    // 支付后未启动，标记异常
                    orderLifecycleService.markOrderAbnormal(order, "支付后未启动");
                    break;
                case "RUNNING":
                    // 运行时间过长，标记异常
                    orderLifecycleService.markOrderAbnormal(order, "运行时间过长");
                    break;
                default:
                    log.warn("未知卡单状态: orderNo={}, status={}", order.getOrderNo(), order.getStatus());
            }
        } catch (Exception e) {
            log.error("处理卡单失败: orderNo={}", order.getOrderNo(), e);
        }
    }

    /**
     * 检查设备订单一致性
     */
    private void checkDeviceOrderConsistency() {
        try {
            List<Device> devices = deviceRepository.findAll();

            for (Device device : devices) {
                // 检查设备当前订单是否一致
                if (device.getCurrentOrderId() != null) {
                    Order order = orderRepository.findById(device.getCurrentOrderId()).orElse(null);
                    if (order == null) {
                        // 清理无效的当前订单
                        device.setCurrentOrderId(null);
                        device.setCurrentOrderNo(null);
                        device.setCurrentUserId(null);
                        deviceRepository.save(device);
                        log.warn("清理设备无效订单: deviceSn={}", device.getDeviceSn());
                    }
                }

                // 检查设备状态与订单状态是否一致
                if (DeviceStatus.RUNNING.name().equals(device.getStatus()) && device.getCurrentOrderId() == null) {
                    // 设备运行中但没有当前订单，重置状态
                    device.setStatus(DeviceStatus.IDLE.name());
                    deviceRepository.save(device);
                    log.warn("重置设备状态（运行中无订单）: deviceSn={}", device.getDeviceSn());
                }
            }

        } catch (Exception e) {
            log.error("检查设备订单一致性失败", e);
        }
    }

    /**
     * 检查支付订单一致性
     */
    private void checkPaymentOrderConsistency() {
        try {
            List<Order> paidOrders = orderRepository.findByStatus("PAID");

            for (Order order : paidOrders) {
                List<PaymentTxn> payments = paymentTxnRepository.findByOrderNo(order.getOrderNo());
                if (payments.isEmpty()) {
                    // 已支付但没有支付记录，标记异常
                    orderLifecycleService.markOrderAbnormal(order, "支付记录缺失");
                    log.warn("订单支付记录缺失: orderNo={}", order.getOrderNo());
                }
            }

        } catch (Exception e) {
            log.error("检查支付订单一致性失败", e);
        }
    }

    /**
     * 检查收益记录一致性
     */
    private void checkIncomeRecordConsistency() {
        try {
            List<Order> finishedOrders = orderRepository.findByStatus("FINISHED");

            for (Order order : finishedOrders) {
                List<IncomeRecord> incomeRecords = incomeRecordRepository.findByOrderId(order.getId());
                if (incomeRecords.isEmpty() && order.getAmount().doubleValue() > 0) {
                    // 已完成但没有收益记录，补充记录
                    // 这里应该调用收益记录服务补充记录
                    log.warn("订单收益记录缺失: orderNo={}", order.getOrderNo());
                }
            }

        } catch (Exception e) {
            log.error("检查收益记录一致性失败", e);
        }
    }

    /**
     * 恢复设备
     */
    private void recoverDevice(String deviceSn, String reason) {
        try {
            deviceStateManager.resetDeviceStatus(deviceSn, "手动恢复: " + reason);
            clearExceptionCounter("device_status_" + deviceSn);
        } catch (Exception e) {
            log.error("恢复设备失败: deviceSn={}", deviceSn, e);
        }
    }

    /**
     * 恢复订单
     */
    private void recoverOrder(String orderNo, String reason) {
        try {
            Order order = orderRepository.findByOrderNo(orderNo).orElse(null);
            if (order != null) {
                orderLifecycleService.cancelOrder(order, "手动恢复: " + reason);
                clearExceptionCounter("payment_" + orderNo);
            }
        } catch (Exception e) {
            log.error("恢复订单失败: orderNo={}", orderNo, e);
        }
    }

    /**
     * 恢复支付
     */
    private void recoverPayment(String transactionId, String reason) {
        try {
            PaymentTxn payment = paymentTxnRepository.findByTransactionId(transactionId).orElse(null);
            if (payment != null) {
                payment.setStatus("FAILED");
                payment.setUpdateTime(LocalDateTime.now());
                paymentTxnRepository.save(payment);

                Order order = orderRepository.findByOrderNo(payment.getOrderNo()).orElse(null);
                if (order != null) {
                    orderLifecycleService.cancelOrder(order, "手动恢复支付: " + reason);
                }
            }
        } catch (Exception e) {
            log.error("恢复支付失败: transactionId={}", transactionId, e);
        }
    }

    /**
     * 增加异常计数
     */
    private void incrementExceptionCounter(String key) {
        exceptionCounters.merge(key, 1, Integer::sum);
        lastExceptionTime.put(key, LocalDateTime.now());
    }

    /**
     * 判断是否应该触发恢复
     */
    private boolean shouldTriggerRecovery(String key) {
        Integer count = exceptionCounters.get(key);
        LocalDateTime lastTime = lastExceptionTime.get(key);

        if (count == null || lastTime == null) {
            return false;
        }

        // 检查异常次数
        if (count < MAX_EXCEPTION_COUNT) {
            return false;
        }

        // 检查冷却时间
        LocalDateTime cooldownEnd = lastTime.plusMinutes(RECOVERY_COOLDOWN_MINUTES);
        return LocalDateTime.now().isAfter(cooldownEnd);
    }

    /**
     * 清除异常计数
     */
    private void clearExceptionCounter(String key) {
        exceptionCounters.remove(key);
        lastExceptionTime.remove(key);
    }

    /**
     * 记录设备状态历史
     */
    private void recordDeviceStatusHistory(Long deviceId, String oldStatus, String newStatus, String reason) {
        // 实现状态历史记录逻辑
    }

    /**
     * 通知管理员
     */
    private void notifyAdmin(String title, String message) {
        try {
            // 实现管理员通知逻辑
            log.info("管理员通知: {} - {}", title, message);
        } catch (Exception e) {
            log.error("发送管理员通知失败", e);
        }
    }

    /**
     * 获取异常统计信息
     */
    public Map<String, Object> getExceptionStatistics() {
        return Map.of(
            "exceptionCounters", exceptionCounters,
            "lastExceptionTimes", lastExceptionTime,
            "totalExceptionTypes", exceptionCounters.size()
        );
    }
}