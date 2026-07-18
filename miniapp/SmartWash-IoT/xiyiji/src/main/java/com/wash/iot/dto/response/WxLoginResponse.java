package com.wash.iot.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 微信登录响应
 */
@Data
@Builder
public class WxLoginResponse {
    private String token;
    private String refreshToken;
    private String role;
    private UserInfoResponse userInfo;
    private boolean isNewUser;
    private boolean needBindPhone;
}
