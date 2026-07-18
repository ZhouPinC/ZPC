package com.wash.iot.service;

import com.wash.iot.dto.response.OrderHistoryResponse;
import com.wash.iot.dto.response.OrderDetailResponse;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单历史服务类
 * 提供详细的订单历史记录查询功能
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class OrderHistoryService {

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

    /**
     * 获取用户订单历史记录
     */
    public OrderHistoryResponse getUserOrderHistory(Long userId, int page, int size) {
        try {
            // 分页查询用户订单
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
            Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);

            // 转换为响应对象
            List<OrderDetailResponse> orderDetails = orderPage.getContent().stream()
                    .map(this::convertToOrderDetail)
                    .collect(Collectors.toList());

            // 构建响应
            OrderHistoryResponse response = new OrderHistoryResponse();
            response.setUserId(userId);
            response.setOrders(orderDetails);
            response.setTotalPages(orderPage.getTotalPages());
            response.setTotalElements(orderPage.getTotalElements());
            response.setCurrentPage(page);
            response.setPageSize(size);

            log.info("查询用户订单历史成功: userId={}, totalElements={}", userId, orderPage.getTotalElements());
            return response;

        } catch (Exception e) {
            log.error("查询用户订单历史失败: userId={}", userId, e);
            throw new RuntimeException("查询订单历史失败: " + e.getMessage());
        }
    }

    /**
     * 获取管理员收益记录
     */
    public OrderHistoryResponse getAdminIncomeHistory(Long adminId, int page, int size) {
        try {
            // 分页查询收益记录
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
            Page<IncomeRecord> incomePage = incomeRecordRepository.findByAdminUserId(adminId, pageable);

            // 转换为订单详情响应
            List<OrderDetailResponse> orderDetails = incomePage.getContent().stream()
                    .map(this::convertIncomeToOrderDetail)
                    .collect(Collectors.toList());

            // 构建响应
            OrderHistoryResponse response = new OrderHistoryResponse();
            response.setUserId(adminId);
            response.setOrders(orderDetails);
            response.setTotalPages(incomePage.getTotalPages());
            response.setTotalElements(incomePage.getTotalElements());
            response.setCurrentPage(page);
            response.setPageSize(size);

            log.info("查询管理员收益历史成功: adminId={}, totalElements={}", adminId, incomePage.getTotalElements());
            return response;

        } catch (Exception e) {
            log.error("查询管理员收益历史失败: adminId={}", adminId, e);
            throw new RuntimeException("查询收益历史失败: " + e.getMessage());
        }
    }

    /**
     * 获取设备使用历史记录
     */
    public OrderHistoryResponse getDeviceUsageHistory(Long deviceId, int page, int size) {
        try {
            // 分页查询设备订单
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
            Page<Order> orderPage = orderRepository.findByDeviceId(deviceId, pageable);

            // 转换为响应对象
            List<OrderDetailResponse> orderDetails = orderPage.getContent().stream()
                    .map(this::convertToOrderDetail)
                    .collect(Collectors.toList());

            // 构建响应
            OrderHistoryResponse response = new OrderHistoryResponse();
            response.setDeviceId(deviceId);
            response.setOrders(orderDetails);
            response.setTotalPages(orderPage.getTotalPages());
            response.setTotalElements(orderPage.getTotalElements());
            response.setCurrentPage(page);
            response.setPageSize(size);

            log.info("查询设备使用历史成功: deviceId={}, totalElements={}", deviceId, orderPage.getTotalElements());
            return response;

        } catch (Exception e) {
            log.error("查询设备使用历史失败: deviceId={}", deviceId, e);
            throw new RuntimeException("查询设备使用历史失败: " + e.getMessage());
        }
    }

    /**
     * 获取订单详细信息
     */
    public OrderDetailResponse getOrderDetail(String orderNo) {
        try {
            Order order = orderRepository.findByOrderNo(orderNo)
                    .orElseThrow(() -> new RuntimeException("订单不存在"));

            OrderDetailResponse detail = convertToOrderDetail(order);

            // 查询支付信息
            List<PaymentTxn> paymentTxns = paymentTxnRepository.findByOrderNo(orderNo);
            detail.setPaymentDetails(paymentTxns.stream()
                    .map(this::convertToPaymentDetail)
                    .collect(Collectors.toList()));

            // 查询设备状态历史
            List<DeviceStatusHistory> statusHistory = deviceStatusHistoryRepository.findByOrderNo(orderNo);
            detail.setStatusHistory(statusHistory.stream()
                    .map(this::convertToStatusHistory)
                    .collect(Collectors.toList()));

            log.info("查询订单详情成功: orderNo={}", orderNo);
            return detail;

        } catch (Exception e) {
            log.error("查询订单详情失败: orderNo={}", orderNo, e);
            throw new RuntimeException("查询订单详情失败: " + e.getMessage());
        }
    }

    /**
     * 转换订单为详细信息响应
     */
    private OrderDetailResponse convertToOrderDetail(Order order) {
        OrderDetailResponse response = new OrderDetailResponse();

        // 基础订单信息
        response.setOrderId(order.getId());
        response.setOrderNo(order.getOrderNo());
        response.setAmount(order.getAmount());
        response.setStatus(order.getStatus());
        response.setCreateTime(order.getCreateTime());
        response.setUpdateTime(order.getUpdateTime());

        // 洗衣信息
        response.setWashMode(order.getWashMode());
        response.setWashModeName(order.getWashModeName());
        response.setDurationMinutes(order.getDurationMinutes());

        // 时间信息
        response.setPayTime(order.getPayTime());
        response.setStartTime(order.getStartTime());
        response.setEndTime(order.getEndTime());

        // 费用信息
        response.setPlatformFee(order.getPlatformFee());
        response.setOwnerIncome(order.getOwnerIncome());
        response.setRefundAmount(order.getRefundAmount());
        response.setRefundReason(order.getRefundReason());
        response.setRefundTime(order.getRefundTime());

        // 完成状态
        response.setCompletionStatus(order.getCompletionStatus());
        response.setFailureReason(order.getFailureReason());

        // 获取用户信息
        if (order.getUserId() != null) {
            userRepository.findById(order.getUserId()).ifPresent(user -> {
                response.setUserInfo(Map.of(
                    "userId", user.getId(),
                    "nickName", user.getNickName() != null ? user.getNickName() : "未知用户",
                    "phone", user.getPhone() != null ? maskPhone(user.getPhone()) : "未绑定"
                ));
            });
        }

        // 获取设备信息
        if (order.getDeviceId() != null) {
            deviceRepository.findById(order.getDeviceId()).ifPresent(device -> {
                response.setDeviceInfo(Map.of(
                    "deviceId", device.getId(),
                    "deviceSn", device.getDeviceSn(),
                    "location", device.getLocation() != null ? device.getLocation() : "未知位置",
                    "model", device.getModel() != null ? device.getModel() : "未知型号"
                ));
            });
        }

        // 获取支付方式信息
        List<PaymentTxn> payments = paymentTxnRepository.findByOrderNo(order.getOrderNo());
        if (!payments.isEmpty()) {
            PaymentTxn payment = payments.get(0); // 取第一个支付记录
            response.setPaymentMethod(payment.getPaymentMethod());
            response.setPaymentChannel(payment.getPaymentChannel());
            response.setPaymentTime(payment.getUpdateTime());
        }

        return response;
    }

    /**
     * 转换收益记录为订单详情
     */
    private OrderDetailResponse convertIncomeToOrderDetail(IncomeRecord income) {
        OrderDetailResponse response = new OrderDetailResponse();

        // 基础信息
        response.setOrderId(income.getOrderId());
        response.setAmount(income.getOrderAmount());
        response.setCreateTime(income.getCreateTime());

        // 收益信息
        response.setNetIncome(income.getNetIncome());
        response.setPlatformFee(income.getPlatformFee());
        response.setSettleStatus(income.getSettleStatus());
        response.setSettleTime(income.getSettleTime());

        // 获取关联订单信息
        orderRepository.findById(income.getOrderId()).ifPresent(order -> {
            response.setOrderNo(order.getOrderNo());
            response.setStatus(order.getStatus());
            response.setWashModeName(order.getWashModeName());
            response.setDurationMinutes(order.getDurationMinutes());
        });

        // 获取设备信息
        deviceRepository.findById(income.getDeviceId()).ifPresent(device -> {
            response.setDeviceInfo(Map.of(
                "deviceId", device.getId(),
                "deviceSn", device.getDeviceSn(),
                "location", device.getLocation() != null ? device.getLocation() : "未知位置"
            ));
        });

        return response;
    }

    /**
     * 转换支付详情
     */
    private Map<String, Object> convertToPaymentDetail(PaymentTxn payment) {
        return Map.of(
            "transactionId", payment.getTransactionId(),
            "amount", payment.getAmount(),
            "paymentMethod", payment.getPaymentMethod(),
            "paymentChannel", payment.getPaymentChannel(),
            "status", payment.getStatus(),
            "createTime", payment.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }

    /**
     * 转换状态历史
     */
    private Map<String, Object> convertToStatusHistory(DeviceStatusHistory history) {
        return Map.of(
            "oldStatus", history.getOldStatus(),
            "newStatus", history.getNewStatus(),
            "statusType", history.getStatusType(),
            "temperature", history.getTemperature(),
            "remainSeconds", history.getRemainSeconds(),
            "errorCode", history.getErrorCode(),
            "message", history.getMessage(),
            "changeTime", history.getChangeTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }

    /**
     * 手机号脱敏
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /**
     * 获取订单统计信息
     */
    public Map<String, Object> getOrderStatistics(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            // 查询指定时间范围内的订单统计
            List<Order> orders = orderRepository.findByUserIdAndCreateTimeBetween(userId, startTime, endTime);

            // 计算统计数据
            long totalOrders = orders.size();
            BigDecimal totalAmount = orders.stream()
                    .filter(order -> "PAID".equals(order.getStatus()) || "FINISHED".equals(order.getStatus()))
                    .map(Order::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long completedOrders = orders.stream()
                    .filter(order -> "FINISHED".equals(order.getStatus()))
                    .count();

            long cancelledOrders = orders.stream()
                    .filter(order -> "CANCELLED".equals(order.getStatus()) || "REFUNDED".equals(order.getStatus()))
                    .count();

            // 按洗衣模式统计
            Map<String, Long> modeStatistics = orders.stream()
                    .filter(order -> order.getWashMode() != null)
                    .collect(Collectors.groupingBy(Order::getWashMode, Collectors.counting()));

            return Map.of(
                "totalOrders", totalOrders,
                "totalAmount", totalAmount,
                "completedOrders", completedOrders,
                "cancelledOrders", cancelledOrders,
                "completionRate", totalOrders > 0 ? (double) completedOrders / totalOrders * 100 : 0,
                "modeStatistics", modeStatistics,
                "startTime", startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "endTime", endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );

        } catch (Exception e) {
            log.error("获取订单统计失败: userId={}", userId, e);
            throw new RuntimeException("获取订单统计失败: " + e.getMessage());
        }
    }
}