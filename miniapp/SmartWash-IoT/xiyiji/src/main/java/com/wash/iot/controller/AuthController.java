package com.wash.iot.controller;

import com.wash.iot.common.response.ApiResponse;
import com.wash.iot.dto.request.AdminVerifyRequest;
import com.wash.iot.dto.request.BindPhoneRequest;
import com.wash.iot.dto.request.WxLoginRequest;
import com.wash.iot.dto.request.WxPhoneLoginRequest;
import com.wash.iot.dto.response.UserInfoResponse;
import com.wash.iot.dto.response.WxLoginResponse;
import com.wash.iot.security.JwtAuthenticationFilter;
import com.wash.iot.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 核心登录方式：OpenID登录
 * OpenID获取失败则登录失败
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 微信OpenID登录
     * POST /api/v1/auth/wx-login
     * 核心登录流程：
     * 1. 前端调用wx.login获取code
     * 2. 后端使用code调用微信API获取OpenID
     * 3. 使用OpenID查找或创建用户
     * 4. 生成JWT Token返回
     */
    @PostMapping("/wx-login")
    public ApiResponse<WxLoginResponse> wxLogin(@RequestBody WxLoginRequest request) {
        log.info("微信OpenID登录请求: code={}", request.getCode());
        try {
            WxLoginResponse response = authService.wxLogin(request);
            log.info("微信OpenID登录成功: userId={}, isNewUser={}", 
                response.getUserInfo() != null ? response.getUserInfo().getId() : null, 
                response.isNewUser());
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("微信OpenID登录失败: code={}, error={}", request.getCode(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 绑定手机号（可选功能）
     * POST /api/v1/auth/bind-phone
     */
    @PostMapping("/bind-phone")
    public ApiResponse<UserInfoResponse> bindPhone(@RequestBody BindPhoneRequest request) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error(401, "请先登录");
        }
        UserInfoResponse response = authService.bindPhone(userId, request);
        return ApiResponse.success(response);
    }

    /**
     * 验证管理密码（进入管理模式）
     * POST /api/v1/admin/auth/verify
     */
    @PostMapping("/admin/verify")
    public ApiResponse<WxLoginResponse> verifyAdmin(@RequestBody AdminVerifyRequest request) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error(401, "请先登录");
        }
        WxLoginResponse response = authService.verifyAdmin(userId, request.getPassword());
        return ApiResponse.success(response);
    }

    /**
     * 退出管理模式
     * POST /api/v1/admin/auth/exit
     */
    @PostMapping("/admin/exit")
    public ApiResponse<WxLoginResponse> exitAdminMode() {
        Long userId = JwtAuthenticationFilter.getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error(401, "请先登录");
        }
        WxLoginResponse response = authService.exitAdminMode(userId);
        return ApiResponse.success(response);
    }
}
