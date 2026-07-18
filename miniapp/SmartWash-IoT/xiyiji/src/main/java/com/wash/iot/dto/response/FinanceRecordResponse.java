package com.wash.iot.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 财务记录响应
 */
@Data
@Builder
public class FinanceRecordResponse {
    private Long id;
    private Long orderId;
    private String orderNo;
    private Long deviceId;
    private String deviceSn;
    
    private BigDecimal orderAmount;
    private BigDecimal platformFee;
    private BigDecimal netIncome;
    
    private String settleStatus;
    private LocalDateTime createTime;
    private LocalDateTime settleTime;
}
