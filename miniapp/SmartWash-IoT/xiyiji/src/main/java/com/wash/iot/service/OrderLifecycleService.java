package com.wash.iot.service;

import com.wash.iot.entity.Device;
import com.wash.iot.entity.IncomeRecord;
import com.wash.iot.entity.Order;
import com.wash.iot.entity.Queue;
import com.wash.iot.entity.Reservation;
import com.wash.iot.infrastructure.mqtt.MqttGateway;
import com.wash.iot.interfaces.mqtt.dto.DeviceReportEvent;
import com.wash.iot.repository.*;
import com.wash.iot.enums.DeviceStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 订单生命周期管理服务
 * 管理订单从创建到完成的全生命周期，确保业务逻辑的一致性
 */
@Slf4j
@Service
@Transactional
public class OrderLifecycleService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QueueRepository queueRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private IncomeRecordRepository incomeRecordRepository;

    @Autowired
    private MqttGateway mqttGateway;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private IncomeRecordService incomeRecordService;

    @Autowired
    private RefundService refundService;

    // 订单超时时间配置（分钟）
    private static final int ORDER_TIMEOUT_MINUTES = 30;
    private static final int PAYMENT_TIMEOUT_MINUTES = 15;
    private static final int START_TIMEOUT_MINUTES = 10;

    /**
     * 创建订单
     */
    public Order createOrder(Long userId, Long deviceId, String washMode, Integer durationMinutes, Long reservationId) {
        try {
            // 1. 验证设备和用户
            Device device = deviceRepository.findById(deviceId)
                    .orElseThrow(() -> new RuntimeException("设备不存在"));

            if (!DeviceStatus.IDLE.name().equals(device.getStatus())) {
                throw new RuntimeException("设备当前不可用");
            }

            // 2. 计算费用
            BigDecimal amount = calculateOrderAmount(device, washMode, durationMinutes);

            // 3. 创建订单
            Order order = new Order();
            order.setOrderNo(generateOrderNo());
            order.setUserId(userId);
            order.setDeviceId(deviceId);
            order.setAmount(amount);
            order.setWashMode(washMode);
            order.setWashModeName(getWashModeName(washMode));
            order.setDurationMinutes(durationMinutes);
            order.setStatus("CREATED");
            order.setReservationId(reservationId);
            order.setDeviceOwnerId(device.getOwnerId());

            order = orderRepository.save(order);

            // 4. 更新设备状态为预约中
            device.setStatus("RESERVED");
            device.setCurrentOrderId(order.getId());
            device.setCurrentOrderNo(order.getOrderNo());
            device.setCurrentUserId(userId);
            deviceRepository.save(device);

            // 5. 如果是预约订单，更新预约状态
            if (reservationId != null) {
                updateReservationStatus(reservationId, "COMPLETED");
            }

            log.info("订单创建成功: orderNo={}, userId={}, deviceId={}, amount={}",
                    order.getOrderNo(), userId, deviceId, amount);

            return order;

        } catch (Exception e) {
            log.error("创建订单失败: userId={}, deviceId={}", userId, deviceId, e);
            throw new RuntimeException("创建订单失败: " + e.getMessage());
        }
    }

    /**
     * 支付订单
     */
    public void payOrder(Order order, String paymentMethod, String paymentChannel) {
        try {
            if (!"CREATED".equals(order.getStatus())) {
                throw new RuntimeException("订单状态不允许支付");
            }

            // 更新订单状态
            order.setStatus("PAID");
            order.setPayTime(LocalDateTime.now());
            order.setPaymentMethod(paymentMethod);
            order.setPaymentChannel(paymentChannel);
            orderRepository.save(order);

            // 发送启动指令给设备
            sendStartCommand(order);

            // 更新设备状态
            updateDeviceForOrderStart(order);

            log.info("订单支付成功: orderNo={}, paymentMethod={}", order.getOrderNo(), paymentMethod);

        } catch (Exception e) {
            log.error("订单支付失败: orderNo={}", order.getOrderNo(), e);
            throw new RuntimeException("订单支付失败: " + e.getMessage());
        }
    }

    /**
     * 开始订单（设备确认启动）
     */
    public void startOrder(Order order, DeviceReportEvent event) {
        try {
            if (!"PAID".equals(order.getStatus())) {
                log.warn("订单状态异常，无法启动: orderNo={}, status={}", order.getOrderNo(), order.getStatus());
                return;
            }

            // 更新订单状态
            order.setStatus("RUNNING");
            order.setStartTime(LocalDateTime.now());
            if (event.getRemainSeconds() != null) {
                order.setDurationMinutes(event.getRemainSeconds() / 60);
            }
            orderRepository.save(order);

            // 更新设备状态
            Device device = deviceRepository.findById(order.getDeviceId()).orElse(null);
            if (device != null) {
                device.setStatus(DeviceStatus.RUNNING.name());
                device.setWorkStartTime(LocalDateTime.now());
                deviceRepository.save(device);
            }

            // 发送开始通知
            notificationService.sendOrderStartNotification(order);

            log.info("订单开始执行: orderNo={}", order.getOrderNo());

        } catch (Exception e) {
            log.error("订单启动失败: orderNo={}", order.getOrderNo(), e);
        }
    }

    /**
     * 完成订单
     */
    public void completeOrder(Order order, DeviceReportEvent event) {
        try {
            if (!"RUNNING".equals(order.getStatus())) {
                log.warn("订单状态异常，无法完成: orderNo={}, status={}", order.getOrderNo(), order.getStatus());
                return;
            }

            // 更新订单状态
            order.setStatus("FINISHED");
            order.setEndTime(LocalDateTime.now());
            order.setCompletionStatus("SUCCESS");
            orderRepository.save(order);

            // 更新设备状态
            updateDeviceForOrderComplete(order);

            // 处理用户统计
            updateUserStatistics(order);

            // 处理设备统计
            updateDeviceStatistics(order);

            // 记录收益
            recordIncome(order);

            // 通知用户完成
            notificationService.sendOrderCompletionNotification(order);

            // 处理排队队列
            processQueue(order.getDeviceId());

            log.info("订单完成: orderNo={}", order.getOrderNo());

        } catch (Exception e) {
            log.error("订单完成失败: orderNo={}", order.getOrderNo(), e);
        }
    }

    /**
     * 中断订单
     */
    public void interruptOrder(Order order, String reason) {
        try {
            if ("FINISHED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus()) || "REFUNDED".equals(order.getStatus())) {
                log.warn("订单已结束，无法中断: orderNo={}, status={}", order.getOrderNo(), order.getStatus());
                return;
            }

            // 更新订单状态
            order.setStatus("INTERRUPTED");
            order.setEndTime(LocalDateTime.now());
            order.setCompletionStatus("INTERRUPTED");
            order.setFailureReason(reason);
            orderRepository.save(order);

            // 更新设备状态
            updateDeviceForOrderComplete(order);

            // 自动退款
            refundService.processAutoRefund(order, "设备故障: " + reason);

            // 通知用户
            notificationService.sendOrderInterruptNotification(order, reason);

            log.info("订单中断: orderNo={}, reason={}", order.getOrderNo(), reason);

        } catch (Exception e) {
            log.error("订单中断失败: orderNo={}", order.getOrderNo(), e);
        }
    }

    /**
     * 取消订单
     */
    public void cancelOrder(Order order, String reason) {
        try {
            if (!"CREATED".equals(order.getStatus())) {
                throw new RuntimeException("只有未支付的订单可以取消");
            }

            // 更新订单状态
            order.setStatus("CANCELLED");
            order.setEndTime(LocalDateTime.now());
            orderRepository.save(order);

            // 更新设备状态
            updateDeviceForOrderComplete(order);

            log.info("订单取消: orderNo={}, reason={}", order.getOrderNo(), reason);

        } catch (Exception e) {
            log.error("订单取消失败: orderNo={}", order.getOrderNo(), e);
            throw new RuntimeException("订单取消失败: " + e.getMessage());
        }
    }

    /**
     * 标记订单异常
     */
    public void markOrderAbnormal(Order order, String reason) {
        try {
            if ("FINISHED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus()) || "REFUNDED".equals(order.getStatus())) {
                return;
            }

            // 更新订单状态
            order.setStatus("ABNORMAL");
            order.setFailureReason(reason);
            orderRepository.save(order);

            // 通知管理员
            notificationService.sendOrderAbnormalNotification(order, reason);

            log.warn("订单标记异常: orderNo={}, reason={}", order.getOrderNo(), reason);

        } catch (Exception e) {
            log.error("标记订单异常失败: orderNo={}", order.getOrderNo(), e);
        }
    }

    /**
     * 定时检查超时订单
     */
    @Scheduled(fixedDelay = 60000) // 每分钟执行一次
    public void checkTimeoutOrders() {
        try {
            // 检查支付超时
            checkPaymentTimeout();

            // 检查启动超时
            checkStartTimeout();

            // 检查工作超时
            checkWorkTimeout();

            // 根据预计结束时间自动完成
            checkEstimatedCompletion();

        } catch (Exception e) {
            log.error("检查超时订单失败", e);
        }
    }

    /**
     * 定时修复缺失的收益记录
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void fixMissingIncomeRecords() {
        try {
            // 查找已完成但没有收益记录的订单（处理过去24小时的订单）
            LocalDateTime start = LocalDateTime.now().minusDays(1);
            List<Order> finishedOrders = orderRepository.findByStatusAndEndTimeAfter("FINISHED", start);
            
            for (Order order : finishedOrders) {
                if (!incomeRecordRepository.existsByOrderId(order.getId())) {
                    log.info("发现缺失收益记录的订单，正在补录: orderNo={}", order.getOrderNo());
                    incomeRecordService.createIncomeRecordForOrder(order.getOrderNo());
                }
            }
        } catch (Exception e) {
            log.error("修复缺失收益记录失败", e);
        }
    }

    /**
     * 定时处理异常订单
     */
    @Scheduled(cron = "0 0 3 * * ?") // 每天凌晨3点执行
    public void handleAbnormalOrders() {
        try {
            // 处理超过24小时的异常订单
            LocalDateTime timeout = LocalDateTime.now().minusHours(24);
            List<Order> abnormalOrders = orderRepository.findByStatusAndUpdateTimeBefore("ABNORMAL", timeout);
            
            for (Order order : abnormalOrders) {
                log.info("自动取消长期异常订单: orderNo={}", order.getOrderNo());
                cancelOrder(order, "异常状态超时自动取消");
            }
        } catch (Exception e) {
            log.error("处理异常订单失败", e);
        }
    }

    /**
     * 检查支付超时
     */
    private void checkPaymentTimeout() {
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES);
        List<Order> timeoutOrders = orderRepository.findPaymentTimeoutOrders(timeout);

        for (Order order : timeoutOrders) {
            log.warn("订单支付超时，自动取消: orderNo={}", order.getOrderNo());
            cancelOrder(order, "支付超时自动取消");
        }
    }

    /**
     * 检查启动超时
     */
    private void checkStartTimeout() {
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(START_TIMEOUT_MINUTES);
        List<Order> timeoutOrders = orderRepository.findStartTimeoutOrders(timeout);

        for (Order order : timeoutOrders) {
            log.warn("订单启动超时: orderNo={}", order.getOrderNo());
            markOrderAbnormal(order, "设备启动超时");
        }
    }

    /**
     * 检查工作超时
     */
    private void checkWorkTimeout() {
        List<Order> runningOrders = orderRepository.findByStatus("RUNNING");

        for (Order order : runningOrders) {
            if (order.getStartTime() != null) {
                LocalDateTime expectedEndTime = order.getStartTime().plusMinutes(order.getDurationMinutes() + 10); // 允许10分钟误差
                if (LocalDateTime.now().isAfter(expectedEndTime)) {
                    log.warn("订单工作超时: orderNo={}", order.getOrderNo());
                    markOrderAbnormal(order, "工作超时");
                }
            }
        }
    }

    private void checkEstimatedCompletion() {
        LocalDateTime now = LocalDateTime.now();
        List<Device> devices = deviceRepository.findAll();
        for (Device device : devices) {
            if (!DeviceStatus.RUNNING.name().equals(device.getStatus())) {
                continue;
            }
            if (device.getEstimatedEndTime() == null) {
                continue;
            }
            if (now.isBefore(device.getEstimatedEndTime().plusMinutes(1))) {
                continue;
            }

            String orderNo = device.getCurrentOrderNo();
            device.setStatus(DeviceStatus.FINISHED.name());
            device.setRemainSeconds(0);
            deviceRepository.save(device);

            if (orderNo == null || orderNo.isBlank()) {
                continue;
            }

            Order order = orderRepository.findByOrderNo(orderNo).orElse(null);
            if (order == null) {
                continue;
            }
            if (!"FINISHED".equals(order.getStatus())) {
                order.setStatus("FINISHED");
                order.setEndTime(now);
                orderRepository.save(order);
                incomeRecordService.createIncomeRecordForDevice(device.getDeviceSn(), orderNo);
            }
        }
    }

    /**
     * 处理排队队列
     */
    private void processQueue(Long deviceId) {
        try {
            // 获取第一个排队的用户
            Optional<Queue> firstQueue = queueRepository.findFirstByDeviceIdOrderByQueuePosition(deviceId);

            if (firstQueue.isPresent()) {
                Queue queue = firstQueue.get();

                // 创建新订单
                Order newOrder = createOrder(
                    queue.getUserId(),
                    deviceId,
                    queue.getWashMode(),
                    queue.getDurationMinutes(),
                    null
                );

                // 更新排队状态
                queue.setStatus("SERVED");
                queue.setServeTime(LocalDateTime.now());
                queueRepository.save(queue);

                // 通知排队用户
                notificationService.sendQueueServeNotification(queue, newOrder);

                // 更新其他排队用户的位置
                updateQueuePositions(deviceId);

                log.info("处理排队完成: deviceId={}, queueId={}, newOrderNo={}",
                        deviceId, queue.getId(), newOrder.getOrderNo());
            }

        } catch (Exception e) {
            log.error("处理排队失败: deviceId={}", deviceId, e);
        }
    }

    /**
     * 更新排队位置
     */
    private void updateQueuePositions(Long deviceId) {
        List<Queue> waitingQueues = queueRepository.findByDeviceIdAndStatusOrderByQueuePosition(deviceId, "WAITING");

        for (int i = 0; i < waitingQueues.size(); i++) {
            Queue queue = waitingQueues.get(i);
            queue.setQueuePosition(i + 1);
            queueRepository.save(queue);
        }
    }

    // 辅助方法
    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    private BigDecimal calculateOrderAmount(Device device, String washMode, Integer durationMinutes) {
        // 根据设备计费模式和洗衣模式计算费用
        if ("PER_USE".equals(device.getPricingMode())) {
            return device.getPricePerUse();
        } else if ("PER_MINUTE".equals(device.getPricingMode())) {
            return device.getPricePerMinute().multiply(new BigDecimal(durationMinutes));
        } else {
            return BigDecimal.ZERO;
        }
    }

    private String getWashModeName(String washMode) {
        switch (washMode) {
            case "standard": return "标准洗";
            case "quick": return "快速洗";
            case "spin": return "单脱水";
            default: return "自定义模式";
        }
    }

    private void sendStartCommand(Order order) {
        try {
            Device device = deviceRepository.findById(order.getDeviceId()).orElse(null);
            if (device != null) {
                mqttGateway.sendStartCommand(device.getDeviceSn(), order.getOrderNo(), order.getDurationMinutes());
            }
        } catch (Exception e) {
            log.error("发送启动指令失败: orderNo={}", order.getOrderNo(), e);
        }
    }

    private void updateDeviceForOrderStart(Order order) {
        Device device = deviceRepository.findById(order.getDeviceId()).orElse(null);
        if (device != null) {
            device.setStatus(DeviceStatus.STARTING.name());
            device.setWorkStartTime(LocalDateTime.now());
            deviceRepository.save(device);
        }
    }

    private void updateDeviceForOrderComplete(Order order) {
        Device device = deviceRepository.findById(order.getDeviceId()).orElse(null);
        if (device != null) {
            device.setStatus(DeviceStatus.IDLE.name());
            device.setWorkStartTime(null);
            device.setCurrentOrderId(null);
            device.setCurrentOrderNo(null);
            device.setCurrentUserId(null);
            device.setRemainSeconds(0);
            device.setEstimatedEndTime(null);
            deviceRepository.save(device);
        }
    }

    private void updateUserStatistics(Order order) {
        // 更新用户统计信息
    }

    private void updateDeviceStatistics(Order order) {
        // 更新设备统计信息
    }

    private void recordIncome(Order order) {
        // 记录收益
    }

    private void updateReservationStatus(Long reservationId, String status) {
        // 更新预约状态
    }
}
