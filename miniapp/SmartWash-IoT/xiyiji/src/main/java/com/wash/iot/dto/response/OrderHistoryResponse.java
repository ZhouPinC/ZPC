package com.wash.iot.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


/**
 * 订单历史响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderHistoryResponse {
    // 支持服务中构建分页/聚合响应的字段
    private Long userId;
    private Long deviceId;
    private List<OrderDetailResponse> orders;
    private int totalPages;
    private long totalElements;
    private int currentPage;
    private int pageSize;

    // 保留一些订单摘要字段（若需要）
    private Long id;
    private String orderNo;
    private String userName;
    private String userPhone;
    private String deviceSn;
    private String deviceLocation;
    private String status;
    private String washType;
    private Integer durationMinutes;
    private BigDecimal amount;
    private String paymentMethod;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String remark;
}