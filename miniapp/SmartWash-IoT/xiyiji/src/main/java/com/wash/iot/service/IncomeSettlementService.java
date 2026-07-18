package com.wash.iot.service;

import com.wash.iot.entity.*;
import com.wash.iot.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 收益结算服务
 * 处理收益分配、结算和提现等财务相关业务
 */
@Slf4j
@Service
@Transactional
public class IncomeSettlementService {

    @Autowired
    private IncomeRecordRepository incomeRecordRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private AdminDeviceBindingRepository adminDeviceBindingRepository;

    @Autowired
    private WithdrawalRecordRepository withdrawalRecordRepository;

    @Autowired
    private NotificationService notificationService;

    // 配置参数
    @Value("${income.platform-fee-rate:0.10}")
    private BigDecimal platformFeeRate;

    @Value("${income.settlement.threshold:100.00}")
    private BigDecimal settlementThreshold;

    @Value("${income.settlement.cycle.days:7}")
    private int settlementCycleDays;

    @Value("${income.withdrawal.min.amount:10.00}")
    private BigDecimal minWithdrawalAmount;

    @Value("${income.withdrawal.max.amount:10000.00}")
    private BigDecimal maxWithdrawalAmount;

    // 收益分配比例
    private static final BigDecimal DEVICE_OWNER_RATE = new BigDecimal("0.90"); // 90%给设备所有者
    private static final BigDecimal PLATFORM_RATE = new BigDecimal("0.10");    // 10%给平台

    /**
     * 创建收益记录
     */
    public IncomeRecord createIncomeRecord(Order order, PaymentTxn paymentTxn) {
        try {
            // 获取设备所有者
            Device device = deviceRepository.findById(order.getDeviceId())
                    .orElseThrow(() -> new RuntimeException("设备不存在"));

            if (device.getOwnerId() == null) {
                log.warn("设备无所有者，无法创建收益记录: deviceId={}", device.getId());
                return null;
            }

            // 计算收益分配
            BigDecimal orderAmount = order.getAmount();
            BigDecimal platformFee = orderAmount.multiply(platformFeeRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal netIncome = orderAmount.subtract(platformFee);

            // 创建收益记录
            IncomeRecord incomeRecord = new IncomeRecord();
            incomeRecord.setAdminUserId(device.getOwnerId());
            incomeRecord.setDeviceId(device.getId());
            incomeRecord.setOrderId(order.getId());
            incomeRecord.setOrderAmount(orderAmount);
            incomeRecord.setPlatformFee(platformFee);
            incomeRecord.setNetIncome(netIncome);
            incomeRecord.setSettleStatus("PENDING");
            incomeRecord.setCreateTime(LocalDateTime.now());

            incomeRecord = incomeRecordRepository.save(incomeRecord);

            // 更新用户累计收益
            updateUserTotalIncome(device.getOwnerId(), netIncome);

            log.info("创建收益记录成功: orderId={}, orderAmount={}, netIncome={}",
                    order.getId(), orderAmount, netIncome);

            return incomeRecord;

        } catch (Exception e) {
            log.error("创建收益记录失败: orderId={}", order.getId(), e);
            throw new RuntimeException("创建收益记录失败: " + e.getMessage());
        }
    }

    /**
     * 处理退款时的收益调整
     */
    public void processRefund(Order order, BigDecimal refundAmount) {
        try {
            List<IncomeRecord> incomeRecords = incomeRecordRepository.findByOrderId(order.getId());

            for (IncomeRecord record : incomeRecords) {
                if ("PENDING".equals(record.getSettleStatus())) {
                    // 退款金额按比例扣除收益
                    BigDecimal refundRatio = refundAmount.divide(record.getOrderAmount(), 4, RoundingMode.HALF_UP);
                    BigDecimal refundNetIncome = record.getNetIncome().multiply(refundRatio).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal refundPlatformFee = record.getPlatformFee().multiply(refundRatio).setScale(2, RoundingMode.HALF_UP);

                    // 更新收益记录
                    record.setNetIncome(record.getNetIncome().subtract(refundNetIncome));
                    record.setPlatformFee(record.getPlatformFee().subtract(refundPlatformFee));
                    record.setSettleStatus("REFUNDED");
                    incomeRecordRepository.save(record);

                    // 更新用户累计收益
                    updateUserTotalIncome(record.getAdminUserId(), refundNetIncome.negate());

                    log.info("处理退款收益调整: orderId={}, refundNetIncome={}",
                            order.getId(), refundNetIncome);
                }
            }

        } catch (Exception e) {
            log.error("处理退款收益调整失败: orderId={}", order.getId(), e);
        }
    }

    /**
     * 自动结算收益
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void autoSettleIncome() {
        try {
            LocalDateTime settlementTime = LocalDateTime.now();
            LocalDateTime cutoffTime = settlementTime.minusDays(settlementCycleDays);

            // 获取待结算的收益记录
            List<IncomeRecord> pendingRecords = incomeRecordRepository
                    .findPendingRecordsBeforeTime(cutoffTime);

            // 按用户分组结算
            Map<Long, List<IncomeRecord>> userIncomes = pendingRecords.stream()
                    .collect(Collectors.groupingBy(IncomeRecord::getAdminUserId));

            for (Map.Entry<Long, List<IncomeRecord>> entry : userIncomes.entrySet()) {
                Long adminUserId = entry.getKey();
                List<IncomeRecord> records = entry.getValue();

                // 计算结算金额
                BigDecimal totalAmount = records.stream()
                        .map(IncomeRecord::getNetIncome)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // 检查是否达到结算门槛
                if (totalAmount.compareTo(settlementThreshold) >= 0) {
                    processUserSettlement(adminUserId, records, totalAmount, settlementTime);
                }
            }

            log.info("自动结算完成: 处理用户数={}, 记录数={}", userIncomes.size(), pendingRecords.size());

        } catch (Exception e) {
            log.error("自动结算失败", e);
        }
    }

    /**
     * 处理用户结算
     */
    private void processUserSettlement(Long adminUserId, List<IncomeRecord> records,
                                     BigDecimal totalAmount, LocalDateTime settlementTime) {
        try {
            User user = userRepository.findById(adminUserId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            // 更新收益记录状态
            for (IncomeRecord record : records) {
                record.setSettleStatus("SETTLED");
                record.setSettleTime(settlementTime);
                incomeRecordRepository.save(record);
            }

            // 更新用户余额（如果配置了自动到账）
            updateUserBalance(user, totalAmount);

            // 发送结算通知
            notificationService.sendSettlementNotification(user, records, totalAmount, settlementTime);

            log.info("用户结算完成: userId={}, amount={}, recordCount={}",
                    adminUserId, totalAmount, records.size());

        } catch (Exception e) {
            log.error("处理用户结算失败: userId={}", adminUserId, e);
        }
    }

    /**
     * 申请提现
     */
    public WithdrawalRecord requestWithdrawal(Long userId, BigDecimal amount, String withdrawMethod) {
        try {
            // 验证用户
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            // 验证提现金额
            if (amount.compareTo(minWithdrawalAmount) < 0) {
                throw new RuntimeException("提现金额不能少于 " + minWithdrawalAmount + " 元");
            }

            if (amount.compareTo(maxWithdrawalAmount) > 0) {
                throw new RuntimeException("提现金额不能超过 " + maxWithdrawalAmount + " 元");
            }

            // 检查可提现余额
            BigDecimal availableBalance = getAvailableBalance(userId);
            if (availableBalance.compareTo(amount) < 0) {
                throw new RuntimeException("可提现余额不足");
            }

            // 创建提现记录
            WithdrawalRecord withdrawal = new WithdrawalRecord();
            withdrawal.setUserId(userId);
            withdrawal.setAmount(amount);
            withdrawal.setWithdrawMethod(withdrawMethod);
            withdrawal.setStatus("PENDING");
            withdrawal.setCreateTime(LocalDateTime.now());

            withdrawal = withdrawalRecordRepository.save(withdrawal);

            // 冻结提现金额
            freezeBalance(userId, amount);

            // 发送提现申请通知
            notificationService.sendWithdrawalRequestNotification(user, withdrawal);

            log.info("用户申请提现: userId={}, amount={}, method={}",
                    userId, amount, withdrawMethod);

            return withdrawal;

        } catch (Exception e) {
            log.error("申请提现失败: userId={}, amount={}", userId, amount, e);
            throw new RuntimeException("申请提现失败: " + e.getMessage());
        }
    }

    /**
     * 处理提现
     */
    public void processWithdrawal(Long withdrawalId, String status, String remark) {
        try {
            WithdrawalRecord withdrawal = withdrawalRecordRepository.findById(withdrawalId)
                    .orElseThrow(() -> new RuntimeException("提现记录不存在"));

            User user = userRepository.findById(withdrawal.getUserId())
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            if ("APPROVED".equals(status)) {
                // 批准提现
                withdrawal.setStatus("APPROVED");
                withdrawal.setProcessTime(LocalDateTime.now());
                withdrawal.setRemark(remark);
                withdrawalRecordRepository.save(withdrawal);

                // 扣除冻结余额
                deductFrozenBalance(user.getId(), withdrawal.getAmount());

                // 发送批准通知
                notificationService.sendWithdrawalApprovedNotification(user, withdrawal);

                log.info("提现批准: withdrawalId={}, amount={}", withdrawalId, withdrawal.getAmount());

            } else if ("REJECTED".equals(status)) {
                // 拒绝提现
                withdrawal.setStatus("REJECTED");
                withdrawal.setProcessTime(LocalDateTime.now());
                withdrawal.setRemark(remark);
                withdrawalRecordRepository.save(withdrawal);

                // 解冻余额
                unfreezeBalance(user.getId(), withdrawal.getAmount());

                // 发送拒绝通知
                notificationService.sendWithdrawalRejectedNotification(user, withdrawal, remark);

                log.info("提现拒绝: withdrawalId={}, amount={}, reason={}",
                        withdrawalId, withdrawal.getAmount(), remark);

            } else {
                throw new RuntimeException("无效的提现状态: " + status);
            }

        } catch (Exception e) {
            log.error("处理提现失败: withdrawalId={}", withdrawalId, e);
            throw new RuntimeException("处理提现失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户收益统计
     */
    public IncomeStatistics getUserIncomeStatistics(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            // 获取收益记录
            List<IncomeRecord> records = incomeRecordRepository
                    .findByAdminUserIdAndCreateTimeBetween(userId, startTime, endTime);

            // 计算统计数据
            BigDecimal totalIncome = records.stream()
                    .map(IncomeRecord::getNetIncome)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalPlatformFee = records.stream()
                    .map(IncomeRecord::getPlatformFee)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long totalOrders = records.size();

            long settledOrders = records.stream()
                    .filter(r -> "SETTLED".equals(r.getSettleStatus()))
                    .count();

            long pendingOrders = records.stream()
                    .filter(r -> "PENDING".equals(r.getSettleStatus()))
                    .count();

            // 按设备分组统计
            Map<Long, BigDecimal> deviceIncomes = records.stream()
                    .collect(Collectors.groupingBy(
                            IncomeRecord::getDeviceId,
                            Collectors.mapping(IncomeRecord::getNetIncome,
                                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                    ));

            IncomeStatistics statistics = new IncomeStatistics();
            statistics.setUserId(userId);
            statistics.setTotalIncome(totalIncome);
            statistics.setTotalPlatformFee(totalPlatformFee);
            statistics.setTotalOrders(totalOrders);
            statistics.setSettledOrders(settledOrders);
            statistics.setPendingOrders(pendingOrders);
            statistics.setSettlementRate(totalOrders > 0 ? (double) settledOrders / totalOrders * 100 : 0);
            statistics.setDeviceIncomes(deviceIncomes);
            statistics.setStartTime(startTime);
            statistics.setEndTime(endTime);
            statistics.setAvailableBalance(getAvailableBalance(userId));
            statistics.setFrozenBalance(getFrozenBalance(userId));

            return statistics;

        } catch (Exception e) {
            log.error("获取用户收益统计失败: userId={}", userId, e);
            throw new RuntimeException("获取收益统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取平台收益统计
     */
    public PlatformIncomeStatistics getPlatformIncomeStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            // 获取所有收益记录
            List<IncomeRecord> records = incomeRecordRepository
                    .findByCreateTimeBetween(startTime, endTime);

            // 计算平台总收益
            BigDecimal totalPlatformIncome = records.stream()
                    .map(IncomeRecord::getPlatformFee)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalOrderAmount = records.stream()
                    .map(IncomeRecord::getOrderAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long totalOrders = records.size();

            // 按日期分组统计
            Map<String, BigDecimal> dailyIncome = records.stream()
                    .collect(Collectors.groupingBy(
                            r -> r.getCreateTime().toLocalDate().toString(),
                            Collectors.mapping(IncomeRecord::getPlatformFee,
                                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                    ));

            // 按设备分组统计
            Map<Long, BigDecimal> deviceIncome = records.stream()
                    .collect(Collectors.groupingBy(
                            IncomeRecord::getDeviceId,
                            Collectors.mapping(IncomeRecord::getPlatformFee,
                                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                    ));

            PlatformIncomeStatistics statistics = new PlatformIncomeStatistics();
            statistics.setTotalPlatformIncome(totalPlatformIncome);
            statistics.setTotalOrderAmount(totalOrderAmount);
            statistics.setTotalOrders(totalOrders);
            statistics.setPlatformFeeRate(platformFeeRate.multiply(new BigDecimal(100)));
            statistics.setDailyIncome(dailyIncome);
            statistics.setDeviceIncome(deviceIncome);
            statistics.setStartTime(startTime);
            statistics.setEndTime(endTime);

            return statistics;

        } catch (Exception e) {
            log.error("获取平台收益统计失败", e);
            throw new RuntimeException("获取平台收益统计失败: " + e.getMessage());
        }
    }

    // 辅助方法
    private void updateUserTotalIncome(Long userId, BigDecimal amount) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            // 这里应该有用户累计收益字段，暂时跳过
            userRepository.save(user);
        }
    }

    private void updateUserBalance(User user, BigDecimal amount) {
        // 更新用户余额逻辑
    }

    private BigDecimal getAvailableBalance(Long userId) {
        // 获取用户可用余额逻辑
        return BigDecimal.ZERO;
    }

    private BigDecimal getFrozenBalance(Long userId) {
        // 获取用户冻结余额逻辑
        return BigDecimal.ZERO;
    }

    private void freezeBalance(Long userId, BigDecimal amount) {
        // 冻结余额逻辑
    }

    private void unfreezeBalance(Long userId, BigDecimal amount) {
        // 解冻余额逻辑
    }

    private void deductFrozenBalance(Long userId, BigDecimal amount) {
        // 扣除冻结余额逻辑
    }

    // 数据传输对象
    public static class IncomeStatistics {
        private Long userId;
        private BigDecimal totalIncome;
        private BigDecimal totalPlatformFee;
        private Long totalOrders;
        private Long settledOrders;
        private Long pendingOrders;
        private Double settlementRate;
        private Map<Long, BigDecimal> deviceIncomes;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private BigDecimal availableBalance;
        private BigDecimal frozenBalance;

        // getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public BigDecimal getTotalIncome() { return totalIncome; }
        public void setTotalIncome(BigDecimal totalIncome) { this.totalIncome = totalIncome; }
        public BigDecimal getTotalPlatformFee() { return totalPlatformFee; }
        public void setTotalPlatformFee(BigDecimal totalPlatformFee) { this.totalPlatformFee = totalPlatformFee; }
        public Long getTotalOrders() { return totalOrders; }
        public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }
        public Long getSettledOrders() { return settledOrders; }
        public void setSettledOrders(Long settledOrders) { this.settledOrders = settledOrders; }
        public Long getPendingOrders() { return pendingOrders; }
        public void setPendingOrders(Long pendingOrders) { this.pendingOrders = pendingOrders; }
        public Double getSettlementRate() { return settlementRate; }
        public void setSettlementRate(Double settlementRate) { this.settlementRate = settlementRate; }
        public Map<Long, BigDecimal> getDeviceIncomes() { return deviceIncomes; }
        public void setDeviceIncomes(Map<Long, BigDecimal> deviceIncomes) { this.deviceIncomes = deviceIncomes; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public BigDecimal getAvailableBalance() { return availableBalance; }
        public void setAvailableBalance(BigDecimal availableBalance) { this.availableBalance = availableBalance; }
        public BigDecimal getFrozenBalance() { return frozenBalance; }
        public void setFrozenBalance(BigDecimal frozenBalance) { this.frozenBalance = frozenBalance; }
    }

    public static class PlatformIncomeStatistics {
        private BigDecimal totalPlatformIncome;
        private BigDecimal totalOrderAmount;
        private Long totalOrders;
        private BigDecimal platformFeeRate;
        private Map<String, BigDecimal> dailyIncome;
        private Map<Long, BigDecimal> deviceIncome;
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        // getters and setters
        public BigDecimal getTotalPlatformIncome() { return totalPlatformIncome; }
        public void setTotalPlatformIncome(BigDecimal totalPlatformIncome) { this.totalPlatformIncome = totalPlatformIncome; }
        public BigDecimal getTotalOrderAmount() { return totalOrderAmount; }
        public void setTotalOrderAmount(BigDecimal totalOrderAmount) { this.totalOrderAmount = totalOrderAmount; }
        public Long getTotalOrders() { return totalOrders; }
        public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }
        public BigDecimal getPlatformFeeRate() { return platformFeeRate; }
        public void setPlatformFeeRate(BigDecimal platformFeeRate) { this.platformFeeRate = platformFeeRate; }
        public Map<String, BigDecimal> getDailyIncome() { return dailyIncome; }
        public void setDailyIncome(Map<String, BigDecimal> dailyIncome) { this.dailyIncome = dailyIncome; }
        public Map<Long, BigDecimal> getDeviceIncome() { return deviceIncome; }
        public void setDeviceIncome(Map<Long, BigDecimal> deviceIncome) { this.deviceIncome = deviceIncome; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    }
}