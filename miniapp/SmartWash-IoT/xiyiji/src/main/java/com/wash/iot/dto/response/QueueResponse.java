package com.wash.iot.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 排队响应
 */
@Data
@Builder
public class QueueResponse {
    private Long id;
    private Long deviceId;
    private String deviceSn;
    private String deviceLocation;
    
    private Integer position;
    private Integer estimatedWaitMinutes;
    
    private String status;
    private String statusText;
    
    private LocalDateTime joinTime;
    private LocalDateTime notifyTime;
    private LocalDateTime expireTime;
}
