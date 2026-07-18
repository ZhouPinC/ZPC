package com.wash.iot.dto.request;

import lombok.Data;

/**
 * 绑定手机号请求
 */
@Data
public class BindPhoneRequest {
    private String encryptedData;
    private String iv;
    // 或者直接传手机号（需要验证码）
    private String phone;
    private String verifyCode;
    private String smsCode;
    private String realName;
}
