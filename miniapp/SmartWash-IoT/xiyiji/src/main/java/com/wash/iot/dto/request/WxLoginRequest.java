package com.wash.iot.dto.request;

import lombok.Data;

/**
 * 微信登录请求
 */
@Data
public class WxLoginRequest {
    private String code;
    private String nickName;
    private String avatarUrl;
}
