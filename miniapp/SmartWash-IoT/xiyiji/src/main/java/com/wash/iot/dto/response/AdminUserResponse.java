package com.wash.iot.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理员用户列表响应
 */
@Data
@Builder
public class AdminUserResponse {
    private Long id;
    private String nickname;
    private String avatarUrl;
    private String phone;
    private BigDecimal balance;
    
    private Long deviceId;
    private String deviceSn;
    
    private String permissionType;  // UNLIMITED, TIME_RANGE, COUNT_LIMIT
    private String permissionStatus; // ACTIVE, EXPIRED, REVOKED
    
    private Integer usageCount;     // 使用次数
    private LocalDateTime lastUseTime; // 最后使用时间
    private LocalDateTime bindTime;    // 绑定时间
}
