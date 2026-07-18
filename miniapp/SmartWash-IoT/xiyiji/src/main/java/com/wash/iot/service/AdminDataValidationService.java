package com.wash.iot.service;

import com.wash.iot.entity.*;
import com.wash.iot.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理端数据校验服务
 * 确保管理界面数据的准确性和完整性
 */
@Slf4j
@Service
@Transactional
public class AdminDataValidationService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentTxnRepository paymentTxnRepository;

    @Autowired
    private IncomeRecordRepository incomeRecordRepository;

    @Autowired
    private DeviceStatusHistoryRepository deviceStatusHistoryRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * 定时数据校验
     */
    @Scheduled(cron = "0 15 4 * * ?") // 每天凌晨4:15执行
    public void scheduledDataValidation() {
        try {
            log.info("开始定时数据校验");

            ValidationResult result = performFullValidation();

            // 记录校验结果
            logValidationResult(result);

            // 如果有错误，通知管理员
            if (result.hasErrors()) {
                notifyAdminValidationErrors(result);
            }

            log.info("定时数据校验完成: 错误={}, 警告={}", result.getErrorCount(), result.getWarningCount());

        } catch (Exception e) {
            log.error("定时数据校验失败", e);
        }
    }

    /**
     * 执行完整的数据校验
     */
    public ValidationResult performFullValidation() {
        ValidationResult result = new ValidationResult();

        // 1. 订单数据校验
        validateOrderData(result);

        // 2. 设备数据校验
        validateDeviceData(result);

        // 3. 支付数据校验
        validatePaymentData(result);

        // 4. 收益数据校验
        validateIncomeData(result);

        // 5. 用户数据校验
        validateUserData(result);

        // 6. 关联数据一致性校验
        validateDataConsistency(result);

        return result;
    }

    /**
     * 校验订单数据
     */
    private void validateOrderData(ValidationResult result) {
        try {
            List<Order> allOrders = orderRepository.findAll();

            for (Order order : allOrders) {
                validateSingleOrder(order, result);
            }

        } catch (Exception e) {
            result.addError("订单数据校验异常: " + e.getMessage());
        }
    }

    /**
     * 校验单个订单
     */
    private void validateSingleOrder(Order order, ValidationResult result) {
        // 1. 基础字段校验
        if (order.getOrderNo() == null || order.getOrderNo().trim().isEmpty()) {
            result.addError("订单ID为空: orderId=" + order.getId());
        }

        if (order.getUserId() == null) {
            result.addWarning("订单用户ID为空: orderNo=" + order.getOrderNo());
        }

        if (order.getDeviceId() == null) {
            result.addError("订单设备ID为空: orderNo=" + order.getOrderNo());
        }

        if (order.getAmount() == null || order.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            result.addError("订单金额异常: orderNo=" + order.getOrderNo() + ", amount=" + order.getAmount());
        }

        // 2. 时间逻辑校验
        validateOrderTimeLogic(order, result);

        // 3. 状态流转校验
        validateOrderStatusFlow(order, result);

        // 4. 关联数据校验
        validateOrderRelations(order, result);
    }

    /**
     * 校验订单时间逻辑
     */
    private void validateOrderTimeLogic(Order order, ValidationResult result) {
        if (order.getCreateTime() == null) {
            result.addError("订单创建时间为空: orderNo=" + order.getOrderNo());
            return;
        }

        // 支付时间不能早于创建时间
        if (order.getPayTime() != null && order.getPayTime().isBefore(order.getCreateTime())) {
            result.addError("支付时间早于创建时间: orderNo=" + order.getOrderNo());
        }

        // 开始时间不能早于支付时间
        if (order.getStartTime() != null && order.getPayTime() != null &&
            order.getStartTime().isBefore(order.getPayTime())) {
            result.addError("开始时间早于支付时间: orderNo=" + order.getOrderNo());
        }

        // 结束时间不能早于开始时间
        if (order.getEndTime() != null && order.getStartTime() != null &&
            order.getEndTime().isBefore(order.getStartTime())) {
            result.addError("结束时间早于开始时间: orderNo=" + order.getOrderNo());
        }

        // 工作时长合理性检查
        if (order.getStartTime() != null && order.getEndTime() != null && order.getDurationMinutes() != null) {
            long actualMinutes = java.time.Duration.between(order.getStartTime(), order.getEndTime()).toMinutes();
            if (Math.abs(actualMinutes - order.getDurationMinutes()) > order.getDurationMinutes() * 0.5) {
                result.addWarning("订单实际时长与设定时长差异过大: orderNo=" + order.getOrderNo() +
                    ", 设定=" + order.getDurationMinutes() + "分钟, 实际=" + actualMinutes + "分钟");
            }
        }
    }

    /**
     * 校验订单状态流转
     */
    private void validateOrderStatusFlow(Order order, ValidationResult result) {
        String status = order.getStatus();
        if (status == null) {
            result.addError("订单状态为空: orderNo=" + order.getOrderNo());
            return;
        }

        // 根据状态检查必需字段
        switch (status) {
            case "PAID":
                if (order.getPayTime() == null) {
                    result.addError("已支付状态但支付时间为空: orderNo=" + order.getOrderNo());
                }
                break;
            case "RUNNING":
                if (order.getStartTime() == null) {
                    result.addError("运行中状态但开始时间为空: orderNo=" + order.getOrderNo());
                }
                break;
            case "FINISHED":
                if (order.getEndTime() == null) {
                    result.addError("已完成状态但结束时间为空: orderNo=" + order.getOrderNo());
                }
                break;
            case "CANCELLED":
            case "REFUNDED":
                if (order.getEndTime() == null) {
                    result.addWarning("已取消/退款状态但结束时间为空: orderNo=" + order.getOrderNo());
                }
                break;
        }

        // 检查异常状态的完成信息
        if ("INTERRUPTED".equals(status) || "ABNORMAL".equals(status)) {
            if (order.getFailureReason() == null || order.getFailureReason().trim().isEmpty()) {
                result.addWarning("异常状态但失败原因为空: orderNo=" + order.getOrderNo());
            }
        }
    }

    /**
     * 校验订单关联数据
     */
    private void validateOrderRelations(Order order, ValidationResult result) {
        // 校验用户是否存在
        if (order.getUserId() != null) {
            if (!userRepository.existsById(order.getUserId())) {
                result.addError("订单用户不存在: orderNo=" + order.getOrderNo() + ", userId=" + order.getUserId());
            }
        }

        // 校验设备是否存在
        if (order.getDeviceId() != null) {
            if (!deviceRepository.existsById(order.getDeviceId())) {
                result.addError("订单设备不存在: orderNo=" + order.getOrderNo() + ", deviceId=" + order.getDeviceId());
            }
        }

        // 校验支付记录
        if (order.getOrderNo() != null && ("PAID".equals(order.getStatus()) || "FINISHED".equals(order.getStatus()))) {
            List<PaymentTxn> payments = paymentTxnRepository.findByOrderNo(order.getOrderNo());
            if (payments.isEmpty()) {
                result.addError("已支付/完成订单但无支付记录: orderNo=" + order.getOrderNo());
            } else {
                // 校验支付金额
                BigDecimal totalPayment = payments.stream()
                    .filter(p -> "SUCCESS".equals(p.getStatus()))
                    .map(PaymentTxn::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (order.getAmount().compareTo(totalPayment) != 0) {
                    result.addError("订单金额与支付金额不匹配: orderNo=" + order.getOrderNo() +
                        ", orderAmount=" + order.getAmount() + ", paymentAmount=" + totalPayment);
                }
            }
        }

        // 校验收益记录
        if ("FINISHED".equals(order.getStatus()) && order.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            List<IncomeRecord> incomes = incomeRecordRepository.findByOrderId(order.getId());
            if (incomes.isEmpty()) {
                result.addError("已完成订单但无收益记录: orderNo=" + order.getOrderNo());
            }
        }
    }

    /**
     * 校验设备数据
     */
    private void validateDeviceData(ValidationResult result) {
        try {
            List<Device> allDevices = deviceRepository.findAll();

            for (Device device : allDevices) {
                validateSingleDevice(device, result);
            }

        } catch (Exception e) {
            result.addError("设备数据校验异常: " + e.getMessage());
        }
    }

    /**
     * 校验单个设备
     */
    private void validateSingleDevice(Device device, ValidationResult result) {
        // 基础字段校验
        if (device.getDeviceSn() == null || device.getDeviceSn().trim().isEmpty()) {
            result.addError("设备序列号为空: deviceId=" + device.getId());
        }

        if (device.getStatus() == null) {
            result.addError("设备状态为空: deviceSn=" + device.getDeviceSn());
        }

        // 状态与当前订单的一致性
        if ("RUNNING".equals(device.getStatus()) && device.getCurrentOrderId() == null) {
            result.addError("设备运行中但无当前订单: deviceSn=" + device.getDeviceSn());
        }

        if (!"RUNNING".equals(device.getStatus()) && device.getCurrentOrderId() != null) {
            result.addWarning("设备非运行状态但有当前订单: deviceSn=" + device.getDeviceSn() +
                ", status=" + device.getStatus() + ", currentOrderId=" + device.getCurrentOrderId());
        }

        // 心跳时间检查
        if (device.getLastHeartbeat() != null && !"OFFLINE".equals(device.getStatus())) {
            LocalDateTime heartbeatThreshold = LocalDateTime.now().minusMinutes(10);
            if (device.getLastHeartbeat().isBefore(heartbeatThreshold)) {
                result.addWarning("设备心跳超时但状态不是离线: deviceSn=" + device.getDeviceSn() +
                    ", lastHeartbeat=" + device.getLastHeartbeat());
            }
        }

        // 统计数据合理性
        if (device.getTotalDuration() != null && device.getTotalDuration() < 0) {
            result.addError("设备总工作时长为负数: deviceSn=" + device.getDeviceSn());
        }

        if (device.getTotalOrders() != null && device.getTotalOrders() < 0) {
            result.addError("设备总订单数为负数: deviceSn=" + device.getDeviceSn());
        }
    }

    /**
     * 校验支付数据
     */
    private void validatePaymentData(ValidationResult result) {
        try {
            List<PaymentTxn> allPayments = paymentTxnRepository.findAll();

            for (PaymentTxn payment : allPayments) {
                validateSinglePayment(payment, result);
            }

        } catch (Exception e) {
            result.addError("支付数据校验异常: " + e.getMessage());
        }
    }

    /**
     * 校验单个支付记录
     */
    private void validateSinglePayment(PaymentTxn payment, ValidationResult result) {
        // 基础字段校验
        if (payment.getTransactionId() == null || payment.getTransactionId().trim().isEmpty()) {
            result.addError("支付交易ID为空: paymentId=" + payment.getId());
        }

        if (payment.getOrderNo() == null || payment.getOrderNo().trim().isEmpty()) {
            result.addError("支付订单号为空: transactionId=" + payment.getTransactionId());
        }

        if (payment.getAmount() == null || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            result.addError("支付金额异常: transactionId=" + payment.getTransactionId() + ", amount=" + payment.getAmount());
        }

        // 校验订单是否存在
        if (payment.getOrderNo() != null) {
            Order order = orderRepository.findByOrderNo(payment.getOrderNo()).orElse(null);
            if (order == null) {
                result.addError("支付记录关联的订单不存在: transactionId=" + payment.getTransactionId() +
                    ", orderNo=" + payment.getOrderNo());
            }
        }

        // 时间逻辑校验
        if (payment.getUpdateTime() != null && payment.getCreateTime() != null &&
            payment.getUpdateTime().isBefore(payment.getCreateTime())) {
            result.addError("支付更新时间早于创建时间: transactionId=" + payment.getTransactionId());
        }
    }

    /**
     * 校验收益数据
     */
    private void validateIncomeData(ValidationResult result) {
        try {
            List<IncomeRecord> allIncomes = incomeRecordRepository.findAll();

            for (IncomeRecord income : allIncomes) {
                validateSingleIncome(income, result);
            }

        } catch (Exception e) {
            result.addError("收益数据校验异常: " + e.getMessage());
        }
    }

    /**
     * 校验单个收益记录
     */
    private void validateSingleIncome(IncomeRecord income, ValidationResult result) {
        // 基础字段校验
        if (income.getAdminUserId() == null) {
            result.addError("收益记录管理员ID为空: incomeId=" + income.getId());
        }

        if (income.getDeviceId() == null) {
            result.addError("收益记录设备ID为空: incomeId=" + income.getId());
        }

        if (income.getOrderId() == null) {
            result.addError("收益记录订单ID为空: incomeId=" + income.getId());
        }

        // 金额校验
        if (income.getOrderAmount() == null || income.getOrderAmount().compareTo(BigDecimal.ZERO) < 0) {
            result.addError("收益记录订单金额异常: incomeId=" + income.getId());
        }

        if (income.getNetIncome() == null || income.getNetIncome().compareTo(BigDecimal.ZERO) < 0) {
            result.addError("收益记录净收益异常: incomeId=" + income.getId());
        }

        if (income.getPlatformFee() == null || income.getPlatformFee().compareTo(BigDecimal.ZERO) < 0) {
            result.addError("收益记录平台费用异常: incomeId=" + income.getId());
        }

        // 金额平衡校验
        if (income.getOrderAmount() != null && income.getNetIncome() != null && income.getPlatformFee() != null) {
            BigDecimal calculatedTotal = income.getNetIncome().add(income.getPlatformFee());
            if (income.getOrderAmount().compareTo(calculatedTotal) != 0) {
                result.addError("收益记录金额不平衡: incomeId=" + income.getId() +
                    ", orderAmount=" + income.getOrderAmount() +
                    ", netIncome+platformFee=" + calculatedTotal);
            }
        }

        // 关联数据校验
        if (income.getOrderId() != null) {
            if (!orderRepository.existsById(income.getOrderId())) {
                result.addError("收益记录关联的订单不存在: incomeId=" + income.getId() + ", orderId=" + income.getOrderId());
            }
        }
    }

    /**
     * 校验用户数据
     */
    private void validateUserData(ValidationResult result) {
        try {
            List<User> allUsers = userRepository.findAll();

            for (User user : allUsers) {
                validateSingleUser(user, result);
            }

        } catch (Exception e) {
            result.addError("用户数据校验异常: " + e.getMessage());
        }
    }

    /**
     * 校验单个用户
     */
    private void validateSingleUser(User user, ValidationResult result) {
        // 基础字段校验
        if (user.getOpenId() == null || user.getOpenId().trim().isEmpty()) {
            result.addError("用户OpenID为空: userId=" + user.getId());
        }

        // 统计数据合理性
        if (user.getTotalOrders() != null && user.getTotalOrders() < 0) {
            result.addError("用户总订单数为负数: userId=" + user.getId());
        }

        if (user.getTotalConsumption() != null && user.getTotalConsumption().compareTo(BigDecimal.ZERO) < 0) {
            result.addError("用户总消费金额为负数: userId=" + user.getId());
        }

        if (user.getBalance() != null && user.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            result.addWarning("用户余额为负数: userId=" + user.getId() + ", balance=" + user.getBalance());
        }

        if (user.getPoints() != null && user.getPoints() < 0) {
            result.addError("用户积分为负数: userId=" + user.getId());
        }
    }

    /**
     * 校验数据一致性
     */
    private void validateDataConsistency(ValidationResult result) {
        // 校验财务数据一致性
        validateFinancialConsistency(result);

        // 校验状态数据一致性
        validateStatusConsistency(result);
    }

    /**
     * 校验财务数据一致性
     */
    private void validateFinancialConsistency(ValidationResult result) {
        try {
            // 计算所有已完成订单的总金额
            BigDecimal totalOrderAmount = orderRepository.findByStatus("FINISHED").stream()
                .map(Order::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 计算所有收益记录的总净收益
            BigDecimal totalNetIncome = incomeRecordRepository.findAll().stream()
                .map(IncomeRecord::getNetIncome)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 计算所有收益记录的总平台费用
            BigDecimal totalPlatformFee = incomeRecordRepository.findAll().stream()
                .map(IncomeRecord::getPlatformFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 验证平衡
            BigDecimal calculatedTotal = totalNetIncome.add(totalPlatformFee);
            if (totalOrderAmount.compareTo(calculatedTotal) != 0) {
                result.addError("财务数据不平衡: 总订单金额=" + totalOrderAmount +
                    ", 净收益+平台费用=" + calculatedTotal +
                    ", 差额=" + totalOrderAmount.subtract(calculatedTotal));
            }

        } catch (Exception e) {
            result.addError("财务一致性校验异常: " + e.getMessage());
        }
    }

    /**
     * 校验状态数据一致性
     */
    private void validateStatusConsistency(ValidationResult result) {
        try {
            // 检查设备与订单状态的一致性
            List<Device> devices = deviceRepository.findAll();
            for (Device device : devices) {
                if (device.getCurrentOrderId() != null) {
                    Order order = orderRepository.findById(device.getCurrentOrderId()).orElse(null);
                    if (order == null) {
                        result.addError("设备当前订单不存在: deviceSn=" + device.getDeviceSn() +
                            ", currentOrderId=" + device.getCurrentOrderId());
                    } else if ("RUNNING".equals(order.getStatus()) && !"RUNNING".equals(device.getStatus())) {
                        result.addWarning("订单与设备状态不一致: orderNo=" + order.getOrderNo() +
                            ", orderStatus=" + order.getStatus() +
                            ", deviceStatus=" + device.getStatus());
                    }
                }
            }

        } catch (Exception e) {
            result.addError("状态一致性校验异常: " + e.getMessage());
        }
    }

    /**
     * 记录校验结果
     */
    private void logValidationResult(ValidationResult result) {
        if (result.hasErrors()) {
            log.error("数据校验发现错误: {}", String.join("; ", result.getErrors()));
        }

        if (result.hasWarnings()) {
            log.warn("数据校验发现警告: {}", String.join("; ", result.getWarnings()));
        }

        if (!result.hasErrors() && !result.hasWarnings()) {
            log.info("数据校验通过，未发现问题");
        }
    }

    /**
     * 通知管理员校验错误
     */
    private void notifyAdminValidationErrors(ValidationResult result) {
        try {
            StringBuilder message = new StringBuilder();
            message.append("数据校验发现问题汇总:\n\n");

            if (result.hasErrors()) {
                message.append("错误 (").append(result.getErrorCount()).append("):\n");
                for (int i = 0; i < Math.min(result.getErrors().size(), 10); i++) {
                    message.append(i + 1).append(". ").append(result.getErrors().get(i)).append("\n");
                }
                if (result.getErrors().size() > 10) {
                    message.append("... 还有 ").append(result.getErrors().size() - 10).append(" 个错误\n");
                }
                message.append("\n");
            }

            if (result.hasWarnings()) {
                message.append("警告 (").append(result.getWarningCount()).append("):\n");
                for (int i = 0; i < Math.min(result.getWarnings().size(), 5); i++) {
                    message.append(i + 1).append(". ").append(result.getWarnings().get(i)).append("\n");
                }
                if (result.getWarnings().size() > 5) {
                    message.append("... 还有 ").append(result.getWarnings().size() - 5).append(" 个警告\n");
                }
            }

            message.append("\n请及时处理数据异常问题。");

            // 发送通知给管理员
            notificationService.sendAdminNotification("数据校验报告", message.toString());

        } catch (Exception e) {
            log.error("发送校验错误通知失败", e);
        }
    }

    /**
     * 手动触发数据校验
     */
    public ValidationResult manualValidation(String validationType) {
        try {
            log.info("手动触发数据校验: type={}", validationType);

            ValidationResult result = new ValidationResult();

            switch (validationType.toLowerCase()) {
                case "order":
                    validateOrderData(result);
                    break;
                case "device":
                    validateDeviceData(result);
                    break;
                case "payment":
                    validatePaymentData(result);
                    break;
                case "income":
                    validateIncomeData(result);
                    break;
                case "user":
                    validateUserData(result);
                    break;
                case "consistency":
                    validateDataConsistency(result);
                    break;
                case "full":
                    return performFullValidation();
                default:
                    throw new RuntimeException("不支持的校验类型: " + validationType);
            }

            logValidationResult(result);
            return result;

        } catch (Exception e) {
            log.error("手动数据校验失败: type={}", validationType, e);
            throw new RuntimeException("手动数据校验失败: " + e.getMessage());
        }
    }

    /**
     * 校验结果类
     */
    public static class ValidationResult {
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public int getErrorCount() {
            return errors.size();
        }

        public int getWarningCount() {
            return warnings.size();
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }
    }
}