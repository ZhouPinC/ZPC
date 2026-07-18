package com.wash.iot.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 设备状态响应
 */
@Data
@Builder
public class DeviceStatusResponse {
    private Long id;
    private String deviceSn;
    private String name;
    private String location;
    private String status;
    private String statusText;

    private String qrCodeContent;
    private String qrCodeUrl;
    
    // 计费信息
    private String pricingMode;
    private BigDecimal pricePerUse;
    private BigDecimal pricePerMinute;
    
    // 当前工作信息
    private String washMode;
    private String washModeName;
    private Integer remainSeconds;
    private Integer totalDuration;
    private String estimatedEndTime;
    
    // 排队信息
    private Integer queueLength;
}
