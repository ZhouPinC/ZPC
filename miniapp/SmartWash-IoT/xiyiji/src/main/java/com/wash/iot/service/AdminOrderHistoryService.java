package com.wash.iot.service;

import com.wash.iot.dto.response.AdminOrderHistoryResponse;
import com.wash.iot.entity.*;
import com.wash.iot.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理端订单历史服务
 * 提供完整的订单历史查询和统计功能
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class AdminOrderHistoryService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private PaymentTxnRepository paymentTxnRepository;

    @Autowired
    private IncomeRecordRepository incomeRecordRepository;

    @Autowired
    private DeviceStatusHistoryRepository deviceStatusHistoryRepository;

    @Autowired
    private AdminDeviceBindingRepository adminDeviceBindingRepository;

    /**
     * 获取管理员订单历史记录
     */
    public AdminOrderHistoryResponse getAdminOrderHistory(Long adminId, OrderQueryParams params) {
        try {
            // 1. 获取管理员绑定的设备列表
            List<Long> deviceIds = getAdminDeviceIds(adminId);
            if (deviceIds.isEmpty()) {
                return createEmptyResponse(params);
            }

            // 2. 构建查询条件
            Pageable pageable = PageRequest.of(
                params.getPage(),
                params.getSize(),
                Sort.by(Sort.Direction.DESC, "createTime")
            );

            // 3. 查询订单
            Page<Order> orderPage = orderRepository.findByDeviceIdsWithFilters(
                deviceIds,
                params.getStatus(),
                params.getStartTime(),
                params.getEndTime(),
                params.getUserId(),
                pageable
            );

            // 4. 转换为详细信息
            List<AdminOrderHistoryResponse.OrderDetailItem> orderDetails = orderPage.getContent()
                .stream()
                .map(this::convertToOrderDetail)
                .collect(Collectors.toList());

            // 5. 构建响应
            AdminOrderHistoryResponse response = new AdminOrderHistoryResponse();
            response.setUserId(adminId);
            response.setOrders(orderDetails);

            // 6. 分页信息
            AdminOrderHistoryResponse.PaginationInfo pagination = new AdminOrderHistoryResponse.PaginationInfo();
            pagination.setCurrentPage(params.getPage());
            pagination.setPageSize(params.getSize());
            pagination.setTotalElements(orderPage.getTotalElements());
            pagination.setTotalPages(orderPage.getTotalPages());
            pagination.setHasNext(orderPage.hasNext());
            pagination.setHasPrevious(orderPage.hasPrevious());
            response.setPagination(pagination);

            // 7. 统计信息
            AdminOrderHistoryResponse.SummaryStatistics summary = calculateSummaryStatistics(orderPage.getContent());
            response.setSummary(summary);

            log.info("管理员订单历史查询成功: adminId={}, totalElements={}", adminId, orderPage.getTotalElements());
            return response;

        } catch (Exception e) {
            log.error("获取管理员订单历史失败: adminId={}", adminId, e);
            throw new RuntimeException("获取订单历史失败: " + e.getMessage());
        }
    }

    /**
     * 获取订单详细信息
     */
    public AdminOrderHistoryResponse.OrderDetailItem getOrderDetail(String orderNo) {
        try {
            Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

            return convertToOrderDetail(order);

        } catch (Exception e) {
            log.error("获取订单详情失败: orderNo={}", orderNo, e);
            throw new RuntimeException("获取订单详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取订单统计数据
     */
    public AdminOrderHistoryResponse.SummaryStatistics getOrderStatistics(Long adminId, StatisticsParams params) {
        try {
            List<Long> deviceIds = getAdminDeviceIds(adminId);
            if (deviceIds.isEmpty()) {
                return createEmptyStatistics();
            }

            List<Order> orders = orderRepository.findByDeviceIdsAndTimeRange(
                deviceIds,
                params.getStartTime(),
                params.getEndTime()
            );

            return calculateSummaryStatistics(orders);

        } catch (Exception e) {
            log.error("获取订单统计失败: adminId={}", adminId, e);
            throw new RuntimeException("获取订单统计失败: " + e.getMessage());
        }
    }

    /**
     * 转换订单为详细信息
     */
    private AdminOrderHistoryResponse.OrderDetailItem convertToOrderDetail(Order order) {
        AdminOrderHistoryResponse.OrderDetailItem item = new AdminOrderHistoryResponse.OrderDetailItem();

        // 基础信息
        item.setOrderId(order.getId());
        item.setOrderNo(order.getOrderNo());
        item.setStatus(order.getStatus());
        item.setStatusDisplay(getStatusDisplay(order.getStatus()));
        item.setCreateTime(order.getCreateTime());
        item.setUpdateTime(order.getUpdateTime());

        // 用户信息
        item.setUserInfo(buildUserInfo(order.getUserId()));

        // 设备信息
        item.setDeviceInfo(buildDeviceInfo(order.getDeviceId()));

        // 洗涤信息
        item.setWashInfo(buildWashInfo(order));

        // 时间信息
        item.setTimeInfo(buildTimeInfo(order));

        // 支付信息
        item.setPaymentInfo(buildPaymentInfo(order));

        // 完成信息
        item.setCompletionInfo(buildCompletionInfo(order));

        // 操作记录
        item.setOperations(buildOperationRecords(order));

        return item;
    }

    /**
     * 构建用户信息
     */
    private AdminOrderHistoryResponse.UserInfo buildUserInfo(Long userId) {
        AdminOrderHistoryResponse.UserInfo userInfo = new AdminOrderHistoryResponse.UserInfo();

        if (userId != null) {
            userRepository.findById(userId).ifPresent(user -> {
                userInfo.setUserId(user.getId());
                userInfo.setNickName(user.getNickName() != null ? user.getNickName() : "未设置昵称");
                userInfo.setRealName(user.getRealName() != null ? maskRealName(user.getRealName()) : "未实名");
                userInfo.setPhone(user.getPhone() != null ? maskPhone(user.getPhone()) : "未绑定");
                userInfo.setAvatarUrl(user.getAvatarUrl());
                userInfo.setRole(user.getRole());
                userInfo.setTotalOrders(user.getTotalOrders() != null ? user.getTotalOrders() : 0);
                userInfo.setTotalConsumption(user.getTotalConsumption() != null ? user.getTotalConsumption() : BigDecimal.ZERO);
            });
        }

        return userInfo;
    }

    /**
     * 构建设备信息
     */
    private AdminOrderHistoryResponse.DeviceInfo buildDeviceInfo(Long deviceId) {
        AdminOrderHistoryResponse.DeviceInfo deviceInfo = new AdminOrderHistoryResponse.DeviceInfo();

        if (deviceId != null) {
            deviceRepository.findById(deviceId).ifPresent(device -> {
                deviceInfo.setDeviceId(device.getId());
                deviceInfo.setDeviceSn(device.getDeviceSn());
                deviceInfo.setDeviceName(getDeviceDisplayName(device));
                deviceInfo.setLocation(device.getLocation() != null ? device.getLocation() : "未设置位置");
                deviceInfo.setModel(device.getModel() != null ? device.getModel() : "未知型号");
                deviceInfo.setManufacturer(device.getManufacturer() != null ? device.getManufacturer() : "未知厂商");
                deviceInfo.setStatus(device.getStatus());
                deviceInfo.setStatusDisplay(getDeviceStatusDisplay(device.getStatus()));
                deviceInfo.setTotalDuration(device.getTotalDuration() != null ? device.getTotalDuration().doubleValue() : 0.0);
                deviceInfo.setTotalOrders(device.getTotalOrders() != null ? device.getTotalOrders() : 0);

                // 设备所有者信息
                if (device.getOwnerId() != null) {
                    userRepository.findById(device.getOwnerId()).ifPresent(owner -> {
                        deviceInfo.setOwnerName(owner.getNickName() != null ? owner.getNickName() : "未知管理员");
                    });
                }

                // 设备总收益
                BigDecimal totalRevenue = incomeRecordRepository.findByDeviceId(deviceId)
                    .stream()
                    .map(IncomeRecord::getNetIncome)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                deviceInfo.setTotalRevenue(totalRevenue);
            });
        }

        return deviceInfo;
    }

    /**
     * 构建洗涤信息
     */
    private AdminOrderHistoryResponse.WashInfo buildWashInfo(Order order) {
        AdminOrderHistoryResponse.WashInfo washInfo = new AdminOrderHistoryResponse.WashInfo();

        washInfo.setWashMode(order.getWashMode());
        washInfo.setWashModeName(order.getWashModeName() != null ? order.getWashModeName() : "标准洗");
        washInfo.setWashModeDescription(getWashModeDescription(order.getWashMode()));
        washInfo.setDurationMinutes(order.getDurationMinutes());

        // 计算实际工作时长
        if (order.getStartTime() != null && order.getEndTime() != null) {
            long actualMinutes = ChronoUnit.MINUTES.between(order.getStartTime(), order.getEndTime());
            washInfo.setActualDurationMinutes((int) actualMinutes);
        }

        // 获取设备状态历史中的温度等信息
        if (order.getOrderNo() != null) {
            List<DeviceStatusHistory> statusHistory = deviceStatusHistoryRepository
                .findByOrderNo(order.getOrderNo());

            if (!statusHistory.isEmpty()) {
                DeviceStatusHistory history = statusHistory.get(0);
                washInfo.setTemperature(history.getTemperature() != null ?
                    history.getTemperature() + "°C" : "未知");
            }
        }

        washInfo.setWaterLevel("标准");
        washInfo.setSpinSpeed(getSpinSpeedByMode(order.getWashMode()));

        return washInfo;
    }

    /**
     * 构建时间信息
     */
    private AdminOrderHistoryResponse.TimeInfo buildTimeInfo(Order order) {
        AdminOrderHistoryResponse.TimeInfo timeInfo = new AdminOrderHistoryResponse.TimeInfo();

        timeInfo.setCreateTime(order.getCreateTime());
        timeInfo.setPayTime(order.getPayTime());
        timeInfo.setStartTime(order.getStartTime());
        timeInfo.setEndTime(order.getEndTime());

        if (order.getStartTime() != null && order.getEndTime() != null) {
            long seconds = ChronoUnit.SECONDS.between(order.getStartTime(), order.getEndTime());
            timeInfo.setTotalDurationSeconds((int) seconds);
            timeInfo.setDurationDisplay(formatDuration(seconds));

            // 检查是否超时
            long expectedSeconds = order.getDurationMinutes() * 60L;
            timeInfo.setTimeout(seconds > expectedSeconds * 1.2); // 超过20%算超时
            timeInfo.setDelayMinutes((int) ((seconds - expectedSeconds) / 60));
        }

        return timeInfo;
    }

    /**
     * 构建支付信息
     */
    private AdminOrderHistoryResponse.PaymentInfo buildPaymentInfo(Order order) {
        AdminOrderHistoryResponse.PaymentInfo paymentInfo = new AdminOrderHistoryResponse.PaymentInfo();

        paymentInfo.setAmount(order.getAmount());
        paymentInfo.setPaymentMethod(order.getPaymentMethod());
        paymentInfo.setPaymentMethodDisplay(getPaymentMethodDisplay(order.getPaymentMethod()));
        paymentInfo.setPaymentChannel(order.getPaymentChannel());
        paymentInfo.setPayTime(order.getPayTime());
        paymentInfo.setPlatformFee(order.getPlatformFee() != null ? order.getPlatformFee() : BigDecimal.ZERO);
        paymentInfo.setOwnerIncome(order.getOwnerIncome() != null ? order.getOwnerIncome() : BigDecimal.ZERO);
        paymentInfo.setRefundAmount(order.getRefundAmount() != null ? order.getRefundAmount() : BigDecimal.ZERO);
        paymentInfo.setRefundReason(order.getRefundReason());
        paymentInfo.setRefundTime(order.getRefundTime());

        // 获取交易ID
        if (order.getOrderNo() != null) {
            List<PaymentTxn> payments = paymentTxnRepository.findByOrderNo(order.getOrderNo());
            if (!payments.isEmpty()) {
                paymentInfo.setTransactionId(payments.get(0).getTransactionId());
            }
        }

        return paymentInfo;
    }

    /**
     * 构建完成信息
     */
    private AdminOrderHistoryResponse.CompletionInfo buildCompletionInfo(Order order) {
        AdminOrderHistoryResponse.CompletionInfo completionInfo = new AdminOrderHistoryResponse.CompletionInfo();

        completionInfo.setCompletionStatus(order.getCompletionStatus());
        completionInfo.setCompletionStatusDisplay(getCompletionStatusDisplay(order.getCompletionStatus()));
        completionInfo.setFailureReason(order.getFailureReason());
        completionInfo.setNormalCompletion("SUCCESS".equals(order.getCompletionStatus()));

        // 获取设备状态历史中的错误代码
        if (order.getOrderNo() != null) {
            List<DeviceStatusHistory> statusHistory = deviceStatusHistoryRepository
                .findByOrderNo(order.getOrderNo());

            Optional<DeviceStatusHistory> errorHistory = statusHistory.stream()
                .filter(h -> h.getErrorCode() != null)
                .findFirst();

            if (errorHistory.isPresent()) {
                completionInfo.setErrorCode(errorHistory.get().getErrorCode());
            }
        }

        completionInfo.setQualityScore(5); // 默认评分
        completionInfo.setUserFeedback(""); // 用户反馈

        return completionInfo;
    }

    /**
     * 构建操作记录
     */
    private List<AdminOrderHistoryResponse.OperationRecord> buildOperationRecords(Order order) {
        List<AdminOrderHistoryResponse.OperationRecord> operations = new ArrayList<>();

        // 创建记录
        AdminOrderHistoryResponse.OperationRecord createRecord = new AdminOrderHistoryResponse.OperationRecord();
        createRecord.setOperationType("CREATE");
        createRecord.setOperationTypeDisplay("订单创建");
        createRecord.setOperator("系统");
        createRecord.setOperationTime(order.getCreateTime());
        createRecord.setDescription("用户创建订单");
        operations.add(createRecord);

        // 支付记录
        if (order.getPayTime() != null) {
            AdminOrderHistoryResponse.OperationRecord payRecord = new AdminOrderHistoryResponse.OperationRecord();
            payRecord.setOperationType("PAY");
            payRecord.setOperationTypeDisplay("支付完成");
            payRecord.setOperator("用户");
            payRecord.setOperationTime(order.getPayTime());
            payRecord.setDescription("用户完成支付，金额：" + order.getAmount() + "元");
            operations.add(payRecord);
        }

        // 开始记录
        if (order.getStartTime() != null) {
            AdminOrderHistoryResponse.OperationRecord startRecord = new AdminOrderHistoryResponse.OperationRecord();
            startRecord.setOperationType("START");
            startRecord.setOperationTypeDisplay("开始洗涤");
            startRecord.setOperator("设备");
            startRecord.setOperationTime(order.getStartTime());
            startRecord.setDescription("设备开始工作，模式：" + order.getWashModeName());
            operations.add(startRecord);
        }

        // 完成记录
        if (order.getEndTime() != null) {
            AdminOrderHistoryResponse.OperationRecord endRecord = new AdminOrderHistoryResponse.OperationRecord();
            endRecord.setOperationType("COMPLETE");
            endRecord.setOperationTypeDisplay("洗涤完成");
            endRecord.setOperator("设备");
            endRecord.setOperationTime(order.getEndTime());
            endRecord.setDescription("设备完成工作");
            operations.add(endRecord);
        }

        return operations;
    }

    /**
     * 计算统计信息
     */
    private AdminOrderHistoryResponse.SummaryStatistics calculateSummaryStatistics(List<Order> orders) {
        AdminOrderHistoryResponse.SummaryStatistics summary = new AdminOrderHistoryResponse.SummaryStatistics();

        // 基础统计
        summary.setTotalOrders(orders.size());

        BigDecimal totalAmount = orders.stream()
            .filter(o -> o.getAmount() != null)
            .map(Order::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalAmount(totalAmount);

        // 状态分布
        Map<String, Long> statusDistribution = orders.stream()
            .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));
        summary.setStatusDistribution(statusDistribution);

        summary.setCompletedOrders(statusDistribution.getOrDefault("FINISHED", 0L));
        summary.setCancelledOrders(statusDistribution.getOrDefault("CANCELLED", 0L));
        summary.setRefundedOrders(statusDistribution.getOrDefault("REFUNDED", 0L));

        // 完成率
        if (orders.size() > 0) {
            summary.setCompletionRate((double) summary.getCompletedOrders() / orders.size() * 100);
        }

        // 平均订单金额
        if (summary.getCompletedOrders() > 0) {
            summary.setAverageOrderAmount(totalAmount.divide(new BigDecimal(summary.getCompletedOrders()), 2, BigDecimal.ROUND_HALF_UP));
        }

        // 洗涤模式分布
        Map<String, Long> washModeDistribution = orders.stream()
            .filter(o -> o.getWashMode() != null)
            .collect(Collectors.groupingBy(Order::getWashMode, Collectors.counting()));
        summary.setWashModeDistribution(washModeDistribution);

        // 每日收益
        Map<String, BigDecimal> dailyRevenue = orders.stream()
            .filter(o -> "FINISHED".equals(o.getStatus()) && o.getAmount() != null)
            .collect(Collectors.groupingBy(
                o -> o.getCreateTime().toLocalDate().toString(),
                Collectors.mapping(Order::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
            ));
        summary.setDailyRevenue(dailyRevenue);

        // 收益统计
        BigDecimal totalIncome = orders.stream()
            .filter(o -> o.getOwnerIncome() != null)
            .map(Order::getOwnerIncome)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalIncome(totalIncome);

        BigDecimal totalPlatformFee = orders.stream()
            .filter(o -> o.getPlatformFee() != null)
            .map(Order::getPlatformFee)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalPlatformFee(totalPlatformFee);

        return summary;
    }

    // 辅助方法
    private List<Long> getAdminDeviceIds(Long adminId) {
        return adminDeviceBindingRepository.findByAdminUserId(adminId)
            .stream()
            .map(AdminDeviceBinding::getDeviceId)
            .collect(Collectors.toList());
    }

    private AdminOrderHistoryResponse createEmptyResponse(OrderQueryParams params) {
        AdminOrderHistoryResponse response = new AdminOrderHistoryResponse();
        response.setUserId(params.getAdminId());
        response.setOrders(new ArrayList<>());

        AdminOrderHistoryResponse.PaginationInfo pagination = new AdminOrderHistoryResponse.PaginationInfo();
        pagination.setCurrentPage(params.getPage());
        pagination.setPageSize(params.getSize());
        pagination.setTotalElements(0);
        pagination.setTotalPages(0);
        pagination.setHasNext(false);
        pagination.setHasPrevious(false);
        response.setPagination(pagination);

        response.setSummary(createEmptyStatistics());
        return response;
    }

    private AdminOrderHistoryResponse.SummaryStatistics createEmptyStatistics() {
        AdminOrderHistoryResponse.SummaryStatistics summary = new AdminOrderHistoryResponse.SummaryStatistics();
        summary.setTotalOrders(0L);
        summary.setTotalAmount(BigDecimal.ZERO);
        summary.setTotalIncome(BigDecimal.ZERO);
        summary.setTotalPlatformFee(BigDecimal.ZERO);
        summary.setCompletedOrders(0L);
        summary.setCancelledOrders(0L);
        summary.setRefundedOrders(0L);
        summary.setCompletionRate(0.0);
        summary.setAverageOrderAmount(BigDecimal.ZERO);
        summary.setStatusDistribution(new HashMap<>());
        summary.setWashModeDistribution(new HashMap<>());
        summary.setDailyRevenue(new HashMap<>());
        return summary;
    }

    // 显示方法
    private String getStatusDisplay(String status) {
        switch (status) {
            case "CREATED": return "待支付";
            case "PAID": return "已支付";
            case "RUNNING": return "洗涤中";
            case "FINISHED": return "已完成";
            case "CANCELLED": return "已取消";
            case "REFUNDED": return "已退款";
            case "INTERRUPTED": return "已中断";
            case "ABNORMAL": return "异常";
            default: return status;
        }
    }

    private String getDeviceStatusDisplay(String status) {
        switch (status) {
            case "IDLE": return "空闲";
            case "RUNNING": return "工作中";
            case "FINISHED": return "已完成";
            case "FAULT": return "故障";
            case "OFFLINE": return "离线";
            case "RESERVED": return "已预约";
            default: return status;
        }
    }

    private String getPaymentMethodDisplay(String paymentMethod) {
        switch (paymentMethod) {
            case "WECHAT": return "微信支付";
            case "BALANCE": return "余额支付";
            case "ALIPAY": return "支付宝";
            default: return paymentMethod;
        }
    }

    private String getCompletionStatusDisplay(String completionStatus) {
        switch (completionStatus) {
            case "SUCCESS": return "正常完成";
            case "FAILED": return "失败";
            case "INTERRUPTED": return "中断";
            case "TIMEOUT": return "超时";
            default: return completionStatus;
        }
    }

    private String getDeviceDisplayName(Device device) {
        if (device.getLocation() != null) {
            return device.getLocation() + " - " + device.getDeviceSn();
        }
        return device.getDeviceSn();
    }

    private String getWashModeDescription(String washMode) {
        switch (washMode) {
            case "standard": return "标准洗涤程序，适合日常衣物";
            case "quick": return "快速洗涤，节省时间";
            case "spin": return "单脱水程序";
            default: return "自定义洗涤程序";
        }
    }

    private String getSpinSpeedByMode(String washMode) {
        switch (washMode) {
            case "standard": return "800转/分钟";
            case "quick": return "600转/分钟";
            case "spin": return "1200转/分钟";
            default: return "标准转速";
        }
    }

    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%d小时%d分钟%d秒", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%d分钟%d秒", minutes, secs);
        } else {
            return String.format("%d秒", secs);
        }
    }

    private String maskRealName(String realName) {
        if (realName == null || realName.length() <= 2) {
            return realName;
        }
        return realName.substring(0, 1) + "**" + realName.substring(realName.length() - 1);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    // 查询参数类
    public static class OrderQueryParams {
        private Long adminId;
        private int page = 0;
        private int size = 20;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Long userId;

        // getters and setters
        public Long getAdminId() { return adminId; }
        public void setAdminId(Long adminId) { this.adminId = adminId; }
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }

    public static class StatisticsParams {
        private LocalDateTime startTime;
        private LocalDateTime endTime;

        // getters and setters
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    }
}