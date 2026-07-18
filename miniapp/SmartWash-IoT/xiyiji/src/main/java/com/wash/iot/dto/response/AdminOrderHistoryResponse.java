package com.wash.iot.dto.response;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 管理端订单历史响应
 * 提供完整的订单信息展示
 */
@Data
public class AdminOrderHistoryResponse {

    private Long userId;
    private String userName;
    private String userPhone;
    private String userAvatar;

    private PaginationInfo pagination;
    private List<OrderDetailItem> orders;

    private SummaryStatistics summary;

    /**
     * 订单详细信息
     */
    @Data
    public static class OrderDetailItem {
        // 基础订单信息
        private Long orderId;
        private String orderNo;
        private String status;
        private String statusDisplay;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;

        // 用户信息
        private UserInfo userInfo;

        // 设备信息
        private DeviceInfo deviceInfo;

        // 洗涤信息
        private WashInfo washInfo;

        // 时间信息
        private TimeInfo timeInfo;

        // 费用信息
        private PaymentInfo paymentInfo;

        // 完成状态
        private CompletionInfo completionInfo;

        // 操作记录
        private List<OperationRecord> operations;
    }

    /**
     * 用户信息
     */
    @Data
    public static class UserInfo {
        private Long userId;
        private String nickName;
        private String realName;
        private String phone;
        private String avatarUrl;
        private String role;
        private Integer totalOrders;
        private BigDecimal totalConsumption;
    }

    /**
     * 设备信息
     */
    @Data
    public static class DeviceInfo {
        private Long deviceId;
        private String deviceSn;
        private String deviceName;
        private String location;
        private String model;
        private String manufacturer;
        private String status;
        private String statusDisplay;
        private String ownerName;
        private Double totalDuration;
        private Integer totalOrders;
        private BigDecimal totalRevenue;
    }

    /**
     * 洗涤信息
     */
    @Data
    public static class WashInfo {
        private String washMode;
        private String washModeName;
        private String washModeDescription;
        private Integer durationMinutes;
        private Integer actualDurationMinutes;
        private String temperature;
        private String waterLevel;
        private String spinSpeed;
    }

    /**
     * 时间信息
     */
    @Data
    public static class TimeInfo {
        private LocalDateTime createTime;
        private LocalDateTime payTime;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer totalDurationSeconds;
        private String durationDisplay;
        private Boolean isTimeout;
        private Integer delayMinutes;
        private Boolean timeout;
    }

    /**
     * 支付信息
     */
    @Data
    public static class PaymentInfo {
        private BigDecimal amount;
        private String paymentMethod;
        private String paymentMethodDisplay;
        private String paymentChannel;
        private String transactionId;
        private LocalDateTime paymentTime;
        private LocalDateTime payTime;
        private BigDecimal platformFee;
        private BigDecimal ownerIncome;
        private BigDecimal refundAmount;
        private String refundReason;
        private LocalDateTime refundTime;
    }

    /**
     * 完成信息
     */
    @Data
    public static class CompletionInfo {
        private String completionStatus;
        private String completionStatusDisplay;
        private String failureReason;
        private String errorCode;
        private Boolean isNormalCompletion;
        private Boolean normalCompletion;
        private Integer qualityScore; // 洗涤质量评分
        private String userFeedback;   // 用户反馈
    }

    /**
     * 操作记录
     */
    @Data
    public static class OperationRecord {
        private String operationType;
        private String operationTypeDisplay;
        private String operator;
        private LocalDateTime operationTime;
        private String description;
        private Map<String, Object> details;
    }

    /**
     * 分页信息
     */
    @Data
    public static class PaginationInfo {
        private int currentPage;
        private int pageSize;
        private long totalElements;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }

    /**
     * 统计信息
     */
    @Data
    public static class SummaryStatistics {
        private BigDecimal totalAmount;
        private BigDecimal totalIncome;
        private BigDecimal totalPlatformFee;
        private long totalOrders;
        private long completedOrders;
        private long cancelledOrders;
        private long refundedOrders;
        private Double completionRate;
        private BigDecimal averageOrderAmount;
        private Map<String, Long> statusDistribution;
        private Map<String, Long> washModeDistribution;
        private Map<String, BigDecimal> dailyRevenue;
    }
}