package com.wash.iot.dto.request;

import lombok.Data;

/**
 * 微信手机号登录请求
 */
@Data
public class WxPhoneLoginRequest {
    private String code;
    private String phoneCode;
    private String nickName;
    private String avatarUrl;
}