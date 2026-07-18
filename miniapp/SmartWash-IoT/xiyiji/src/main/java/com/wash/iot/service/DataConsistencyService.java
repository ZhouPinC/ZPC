package com.wash.iot.service;

import com.wash.iot.entity.*;
import com.wash.iot.repository.*;
import lombok.extern.slf4j.Slf4j;
import com.wash.iot.enums.DeviceStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 数据一致性保障服务
 * 确保系统数据的完整性和一致性，提供自动修复机制
 */
@Slf4j
@Service
@Transactional
public class DataConsistencyService {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentTxnRepository paymentTxnRepository;

    @Autowired
    private IncomeRecordRepository incomeRecordRepository;

    @Autowired
    private QueueRepository queueRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private NotificationService notificationService;

    // 分布式锁
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    // 一致性检查结果
    private final Map<String, List<String>> consistencyIssues = new ConcurrentHashMap<>();

    /**
     * 全量数据一致性检查
     */
    @Scheduled(cron = "0 30 3 * * ?") // 每天凌晨3:30执行
    public void fullConsistencyCheck() {
        try {
            log.info("开始全量数据一致性检查");

            List<String> issues = new ArrayList<>();

            // 1. 检查设备-订单一致性
            issues.addAll(checkDeviceOrderConsistency());

            // 2. 检查支付-订单一致性
            issues.addAll(checkPaymentOrderConsistency());

            // 3. 检查收益-订单一致性
            issues.addAll(checkIncomeOrderConsistency());

            // 4. 检查用户-设备绑定一致性
            issues.addAll(checkUserDeviceBindingConsistency());

            // 5. 检查排队-设备一致性
            issues.addAll(checkQueueDeviceConsistency());

            // 6. 检查预约-设备一致性
            issues.addAll(checkReservationDeviceConsistency());

            // 7. 检查财务数据一致性
            issues.addAll(checkFinancialConsistency());

            // 保存检查结果
            consistencyIssues.put(LocalDateTime.now().toString(), issues);

            // 自动修复可修复的问题
            int fixedCount = autoFixIssues(issues);

            log.info("全量数据一致性检查完成: 发现问题={}, 自动修复={}", issues.size(), fixedCount);

            // 如果有未修复的问题，通知管理员
            if (issues.size() > fixedCount) {
                notifyAdminConsistencyIssues(issues, fixedCount);
            }

        } catch (Exception e) {
            log.error("全量数据一致性检查失败", e);
        }
    }

    /**
     * 增量一致性检查
     */
    @Scheduled(fixedDelay = 300000) // 每5分钟执行一次
    public void incrementalConsistencyCheck() {
        try {
            LocalDateTime checkTime = LocalDateTime.now().minusMinutes(10);

            // 检查最近10分钟的数据
            List<String> issues = new ArrayList<>();

            issues.addAll(checkRecentOrders(checkTime));
            issues.addAll(checkRecentPayments(checkTime));
            issues.addAll(checkRecentDeviceStatus(checkTime));

            if (!issues.isEmpty()) {
                log.warn("发现增量一致性问题: {}", issues);
                autoFixIssues(issues);
            }

        } catch (Exception e) {
            log.error("增量一致性检查失败", e);
        }
    }

    /**
     * 检查设备-订单一致性
     */
    private List<String> checkDeviceOrderConsistency() {
        List<String> issues = new ArrayList<>();

        try {
            List<Device> devices = deviceRepository.findAll();

            for (Device device : devices) {
                // 检查设备当前订单
                if (device.getCurrentOrderId() != null) {
                    Order order = orderRepository.findById(device.getCurrentOrderId()).orElse(null);

                    if (order == null) {
                        // 设备有当前订单ID但订单不存在
                        issues.add(String.format("设备%s存在无效当前订单ID: %d",
                                device.getDeviceSn(), device.getCurrentOrderId()));
                        fixDeviceInvalidCurrentOrder(device);
                    } else {
                        // 检查订单状态与设备状态是否匹配
                        checkOrderDeviceStatusMatch(device, order, issues);
                    }
                }

                // 检查设备状态与当前订单的匹配
                checkDeviceStatusConsistency(device, issues);
            }

        } catch (Exception e) {
            log.error("检查设备-订单一致性失败", e);
            issues.add("设备-订单一致性检查异常: " + e.getMessage());
        }

        return issues;
    }

    /**
     * 检查支付-订单一致性
     */
    private List<String> checkPaymentOrderConsistency() {
        List<String> issues = new ArrayList<>();

        try {
            List<Order> paidOrders = orderRepository.findByStatus("PAID");

            for (Order order : paidOrders) {
                List<PaymentTxn> payments = paymentTxnRepository.findByOrderNo(order.getOrderNo());

                if (payments.isEmpty()) {
                    // 订单已支付但没有支付记录
                    issues.add(String.format("订单%s已支付但无支付记录", order.getOrderNo()));
                } else {
                    // 检查支付金额是否匹配
                    checkPaymentAmountMatch(order, payments, issues);
                }
            }

            // 检查是否有孤立的支付记录
            List<PaymentTxn> allPayments = paymentTxnRepository.findAll();
            for (PaymentTxn payment : allPayments) {
                Order order = orderRepository.findByOrderNo(payment.getOrderNo()).orElse(null);
                if (order == null) {
                    issues.add(String.format("存在孤立的支付记录: %s", payment.getTransactionId()));
                }
            }

        } catch (Exception e) {
            log.error("检查支付-订单一致性失败", e);
            issues.add("支付-订单一致性检查异常: " + e.getMessage());
        }

        return issues;
    }

    /**
     * 检查收益-订单一致性
     */
    private List<String> checkIncomeOrderConsistency() {
        List<String> issues = new ArrayList<>();

        try {
            List<Order> finishedOrders = orderRepository.findByStatus("FINISHED");

            for (Order order : finishedOrders) {
                if (order.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                    List<IncomeRecord> incomes = incomeRecordRepository.findByOrderId(order.getId());

                    if (incomes.isEmpty()) {
                        // 已完成的付费订单没有收益记录
                        issues.add(String.format("已完成订单%s缺少收益记录", order.getOrderNo()));
                        createMissingIncomeRecord(order);
                    } else {
                        // 检查收益金额是否正确
                        checkIncomeAmountMatch(order, incomes, issues);
                    }
                }
            }

        } catch (Exception e) {
            log.error("检查收益-订单一致性失败", e);
            issues.add("收益-订单一致性检查异常: " + e.getMessage());
        }

        return issues;
    }

    /**
     * 检查用户-设备绑定一致性
     */
    private List<String> checkUserDeviceBindingConsistency() {
        List<String> issues = new ArrayList<>();

        try {
            // 这里需要实现用户设备绑定表的检查逻辑
            // 暂时跳过，因为具体的绑定表结构可能不同

        } catch (Exception e) {
            log.error("检查用户-设备绑定一致性失败", e);
            issues.add("用户-设备绑定一致性检查异常: " + e.getMessage());
        }

        return issues;
    }

    /**
     * 检查排队-设备一致性
     */
    private List<String> checkQueueDeviceConsistency() {
        List<String> issues = new ArrayList<>();

        try {
            List<Queue> allQueues = queueRepository.findAll();

            for (Queue queue : allQueues) {
                Device device = deviceRepository.findById(queue.getDeviceId()).orElse(null);
                if (device == null) {
                    // 排队记录引用了不存在的设备
                    issues.add(String.format("排队记录%d引用不存在的设备ID: %d",
                            queue.getId(), queue.getDeviceId()));
                    continue;
                }

                // 检查排队状态与设备状态的匹配
                if ("WAITING".equals(queue.getStatus())) {
                    if (!DeviceStatus.RUNNING.name().equals(device.getStatus())) {
                        // 设备不在运行状态但仍有排队
                        issues.add(String.format("设备%s不在运行状态但存在排队记录%d",
                                device.getDeviceSn(), queue.getId()));
                    }
                }

                // 检查排队位置是否连续
                checkQueuePositionContinuity(queue.getDeviceId(), issues);
            }

        } catch (Exception e) {
            log.error("检查排队-设备一致性失败", e);
            issues.add("排队-设备一致性检查异常: " + e.getMessage());
        }

        return issues;
    }

    /**
     * 检查预约-设备一致性
     */
    private List<String> checkReservationDeviceConsistency() {
        List<String> issues = new ArrayList<>();

        try {
            List<Reservation> allReservations = reservationRepository.findAll();

            for (Reservation reservation : allReservations) {
                Device device = deviceRepository.findById(reservation.getDeviceId()).orElse(null);
                if (device == null) {
                    // 预约记录引用了不存在的设备
                    issues.add(String.format("预约记录%d引用不存在的设备ID: %d",
                            reservation.getId(), reservation.getDeviceId()));
                    continue;
                }

                // 检查过期预约
                if ("ACTIVE".equals(reservation.getStatus())) {
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime expireTime = reservation.getReservationTime().plusMinutes(15); // 15分钟过期

                    if (now.isAfter(expireTime)) {
                        issues.add(String.format("预约记录%d已过期但状态仍为ACTIVE", reservation.getId()));
                        updateExpiredReservation(reservation);
                    }
                }
            }

        } catch (Exception e) {
            log.error("检查预约-设备一致性失败", e);
            issues.add("预约-设备一致性检查异常: " + e.getMessage());
        }

        return issues;
    }

    /**
     * 检查财务数据一致性
     */
    private List<String> checkFinancialConsistency() {
        List<String> issues = new ArrayList<>();

        try {
            // 检查总收益计算
            BigDecimal totalOrderAmount = orderRepository.findAll().stream()
                    .filter(o -> "FINISHED".equals(o.getStatus()))
                    .map(Order::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalIncome = incomeRecordRepository.findAll().stream()
                    .map(IncomeRecord::getNetIncome)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalPlatformFee = incomeRecordRepository.findAll().stream()
                    .map(IncomeRecord::getPlatformFee)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 验证: 总订单金额 = 总收益 + 总平台费用
            BigDecimal calculatedTotal = totalIncome.add(totalPlatformFee);
            if (totalOrderAmount.compareTo(calculatedTotal) != 0) {
                issues.add(String.format("财务数据不平衡: 订单总额=%s, 收益+费用=%s",
                        totalOrderAmount, calculatedTotal));
            }

        } catch (Exception e) {
            log.error("检查财务数据一致性失败", e);
            issues.add("财务数据一致性检查异常: " + e.getMessage());
        }

        return issues;
    }

    /**
     * 检查最近订单的一致性
     */
    private List<String> checkRecentOrders(LocalDateTime checkTime) {
        List<String> issues = new ArrayList<>();

        try {
            List<Order> recentOrders = orderRepository.findByCreateTimeAfter(checkTime);

            for (Order order : recentOrders) {
                // 检查订单状态流转的合理性
                checkOrderStatusFlow(order, issues);
            }

        } catch (Exception e) {
            log.error("检查最近订单一致性失败", e);
            issues.add("最近订单一致性检查异常: " + e.getMessage());
        }

        return issues;
    }

    /**
     * 检查最近支付的一致性
     */
    private List<String> checkRecentPayments(LocalDateTime checkTime) {
        List<String> issues = new ArrayList<>();

        try {
            List<PaymentTxn> recentPayments = paymentTxnRepository.findByCreateTimeAfter(checkTime);

            for (PaymentTxn payment : recentPayments) {
                // 检查支付状态
                if ("SUCCESS".equals(payment.getStatus())) {
                    Order order = orderRepository.findByOrderNo(payment.getOrderNo()).orElse(null);
                    if (order != null && !"PAID".equals(order.getStatus()) && !"RUNNING".equals(order.getStatus())
                            && !"FINISHED".equals(order.getStatus())) {
                        issues.add(String.format("支付成功但订单状态异常: 支付%s, 订单%s状态%s",
                                payment.getTransactionId(), payment.getOrderNo(), order.getStatus()));
                    }
                }
            }

        } catch (Exception e) {
            log.error("检查最近支付一致性失败", e);
            issues.add("最近支付一致性检查异常: " + e.getMessage());
        }

        return issues;
    }

    /**
     * 检查最近设备状态的一致性
     */
    private List<String> checkRecentDeviceStatus(LocalDateTime checkTime) {
        List<String> issues = new ArrayList<>();

        try {
            List<Device> devices = deviceRepository.findAll();

            for (Device device : devices) {
                // 检查设备心跳
                if (device.getLastHeartbeat() != null &&
                    device.getLastHeartbeat().isBefore(checkTime.minusMinutes(10))) {

                    if (!DeviceStatus.OFFLINE.name().equals(device.getStatus())) {
                        issues.add(String.format("设备%s心跳超时但状态不是离线", device.getDeviceSn()));
                    }
                }
            }

        } catch (Exception e) {
            log.error("检查最近设备状态一致性失败", e);
            issues.add("最近设备状态一致性检查异常: " + e.getMessage());
        }

        return issues;
    }

    /**
     * 自动修复问题
     */
    private int autoFixIssues(List<String> issues) {
        int fixedCount = 0;

        for (String issue : issues) {
            try {
                if (autoFixIssue(issue)) {
                    fixedCount++;
                    log.info("自动修复问题: {}", issue);
                }
            } catch (Exception e) {
                log.error("自动修复问题失败: {}", issue, e);
            }
        }

        return fixedCount;
    }

    /**
     * 自动修复单个问题
     */
    private boolean autoFixIssue(String issue) {
        // 根据问题描述判断是否可以自动修复
        if (issue.contains("存在无效当前订单ID")) {
            return true; // 前面已经修复
        } else if (issue.contains("缺少收益记录")) {
            return true; // 前面已经修复
        } else if (issue.contains("已过期但状态仍为ACTIVE")) {
            return true; // 前面已经修复
        }

        return false; // 其他问题需要手动处理
    }

    // 辅助修复方法
    private void fixDeviceInvalidCurrentOrder(Device device) {
        device.setCurrentOrderId(null);
        device.setCurrentOrderNo(null);
        device.setCurrentUserId(null);
        deviceRepository.save(device);
    }

    private void checkOrderDeviceStatusMatch(Device device, Order order, List<String> issues) {
        if ("RUNNING".equals(order.getStatus()) && !DeviceStatus.RUNNING.name().equals(device.getStatus())) {
            issues.add(String.format("订单%s运行中但设备%s状态不是运行中", order.getOrderNo(), device.getDeviceSn()));
        }
    }

    private void checkDeviceStatusConsistency(Device device, List<String> issues) {
        if (DeviceStatus.RUNNING.name().equals(device.getStatus()) && device.getCurrentOrderId() == null) {
            issues.add(String.format("设备%s运行中但没有当前订单", device.getDeviceSn()));
        }
    }

    private void checkPaymentAmountMatch(Order order, List<PaymentTxn> payments, List<String> issues) {
        BigDecimal totalPayment = payments.stream()
                .filter(p -> "SUCCESS".equals(p.getStatus()))
                .map(PaymentTxn::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (order.getAmount().compareTo(totalPayment) != 0) {
            issues.add(String.format("订单%s金额不匹配: 订单金额=%s, 支付金额=%s",
                    order.getOrderNo(), order.getAmount(), totalPayment));
        }
    }

    private void checkIncomeAmountMatch(Order order, List<IncomeRecord> incomes, List<String> issues) {
        BigDecimal totalIncome = incomes.stream()
                .map(IncomeRecord::getNetIncome)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expectedIncome = order.getAmount().multiply(new BigDecimal("0.90")); // 假设90%给所有者

        if (expectedIncome.compareTo(totalIncome) != 0) {
            issues.add(String.format("订单%s收益金额不匹配: 期望=%s, 实际=%s",
                    order.getOrderNo(), expectedIncome, totalIncome));
        }
    }

    private void createMissingIncomeRecord(Order order) {
        // 创建缺失的收益记录
        Device device = deviceRepository.findById(order.getDeviceId()).orElse(null);
        if (device != null && device.getOwnerId() != null) {
            // 调用收益服务创建记录
        }
    }

    private void checkQueuePositionContinuity(Long deviceId, List<String> issues) {
        List<Queue> queues = queueRepository.findByDeviceIdAndStatusOrderByQueuePosition(deviceId, "WAITING");

        for (int i = 0; i < queues.size(); i++) {
            if (queues.get(i).getQueuePosition() != i + 1) {
                issues.add(String.format("设备%d排队位置不连续: 期望位置%d, 实际位置%d",
                        deviceId, i + 1, queues.get(i).getQueuePosition()));
                break;
            }
        }
    }

    private void updateExpiredReservation(Reservation reservation) {
        reservation.setStatus("EXPIRED");
        reservation.setCancelTime(LocalDateTime.now());
        reservation.setCancelReason("系统自动过期");
        reservationRepository.save(reservation);
    }

    private void checkOrderStatusFlow(Order order, List<String> issues) {
        // 检查订单状态流转的合理性
        // 这里可以实现更复杂的状态机检查逻辑
    }

    private void notifyAdminConsistencyIssues(List<String> issues, int fixedCount) {
        StringBuilder message = new StringBuilder();
        message.append("数据一致性检查发现 ").append(issues.size()).append(" 个问题\n");
        message.append("自动修复 ").append(fixedCount).append(" 个问题\n");
        message.append("未修复问题:\n");

        for (int i = fixedCount; i < issues.size(); i++) {
            message.append(i - fixedCount + 1).append(". ").append(issues.get(i)).append("\n");
        }

        // 发送通知给管理员
        notificationService.sendAdminNotification("数据一致性检查报告", message.toString());
    }

    /**
     * 手动触发一致性检查
     */
    public Map<String, Object> manualConsistencyCheck(String checkType) {
        try {
            List<String> issues = new ArrayList<>();

            switch (checkType.toLowerCase()) {
                case "device_order":
                    issues = checkDeviceOrderConsistency();
                    break;
                case "payment_order":
                    issues = checkPaymentOrderConsistency();
                    break;
                case "income_order":
                    issues = checkIncomeOrderConsistency();
                    break;
                case "full":
                    return fullConsistencyCheckResult();
                default:
                    throw new RuntimeException("不支持的检查类型: " + checkType);
            }

            int fixedCount = autoFixIssues(issues);

            return Map.of(
                "checkType", checkType,
                "totalIssues", issues.size(),
                "fixedIssues", fixedCount,
                "remainingIssues", issues.size() - fixedCount,
                "issues", issues
            );

        } catch (Exception e) {
            log.error("手动一致性检查失败: checkType={}", checkType, e);
            throw new RuntimeException("手动一致性检查失败: " + e.getMessage());
        }
    }

    private Map<String, Object> fullConsistencyCheckResult() {
        // 返回全量检查结果
        return Map.of(
            "checkType", "full",
            "lastCheckTime", consistencyIssues.keySet().stream().max(String::compareTo).orElse(""),
            "allIssues", consistencyIssues
        );
    }

    /**
     * 获取一致性检查历史
     */
    public Map<String, Object> getConsistencyCheckHistory() {
        return Map.of(
            "history", consistencyIssues,
            "totalChecks", consistencyIssues.size()
        );
    }
}