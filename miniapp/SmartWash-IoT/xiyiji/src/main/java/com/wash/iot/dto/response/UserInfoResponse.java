package com.wash.iot.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户信息响应
 */
@Data
@Builder
public class UserInfoResponse {
    private Long id;
    private String openid;
    private String userIdentifier; // 用户唯一标识符
    private String nickName;
    private String realName;
    private String avatarUrl;
    private String phone;
    private Integer gender; // 性别：0-未知，1-男，2-女
    private String role;
    private String status;
    private BigDecimal balance;
    private Integer points;
    private BigDecimal totalConsumption;
    private Integer totalOrders;
    private java.time.LocalDateTime createTime;
    private java.time.LocalDateTime lastLoginTime;
}
