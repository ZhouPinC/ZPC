package com.wash.iot.dto.request;

import lombok.Data;

/**
 * 更新用户资料请求
 */
@Data
public class UpdateProfileRequest {
    
    /**
     * 用户昵称
     */
    private String nickName;
    
    /**
     * 用户头像URL
     */
    private String avatarUrl;
    
    /**
     * 用户性别：0-未知，1-男，2-女
     */
    private Integer gender;
    
    /**
     * 真实姓名
     */
    private String realName;
}
