package com.wash.iot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 订单详情响应（包含服务中需要读写的字段）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponse {
    private Long orderId;
    private Long id;
    private String orderNo;

    private Long deviceId;
    private String deviceSn;
    private String deviceLocation;

    private String washMode;
    private String washModeName;
    private Integer durationMinutes;

    private BigDecimal amount;
    private String status;
    private String statusText;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime payTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // 财务字段
    private BigDecimal platformFee;
    private BigDecimal ownerIncome;
    private BigDecimal refundAmount;
    private String refundReason;
    private LocalDateTime refundTime;

    // 订单完成/失败信息
    private String completionStatus;
    private String failureReason;

    // 支付相关
    private String paymentMethod;
    private String paymentChannel;
    private LocalDateTime paymentTime;

    // 兼容服务中对集合的使用
    private List<Map<String, Object>> paymentDetails;
    private List<Map<String, Object>> statusHistory;

    // 用户/设备信息 Map
    private Map<String, Object> userInfo;
    private Map<String, Object> deviceInfo;

    // 收益相关（管理员视图）
    private BigDecimal netIncome;

    // 结算相关
    private String settleStatus;
    private LocalDateTime settleTime;

    // 支付参数（需要支付时返回）
    private PayParams payParams;

    @Data
    @Builder
    public static class PayParams {
        private String timeStamp;
        private String nonceStr;
        private String packageValue;
        private String signType;
        private String paySign;
    }
}
