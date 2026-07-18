package com.wash.iot.dto.request;

import lombok.Data;

/**
 * 管理员验证请求
 */
@Data
public class AdminVerifyRequest {
    private String password;
}
