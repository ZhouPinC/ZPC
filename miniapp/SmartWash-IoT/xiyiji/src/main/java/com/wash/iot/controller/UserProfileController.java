package com.wash.iot.controller;

import com.wash.iot.common.response.ApiResponse;
import com.wash.iot.dto.request.UpdateProfileRequest;
import com.wash.iot.dto.response.UserInfoResponse;
import com.wash.iot.entity.User;
import com.wash.iot.repository.UserRepository;
import com.wash.iot.security.JwtAuthenticationFilter;
import com.wash.iot.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户资料控制器
 * 提供用户资料查看和更新的REST API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/user")
public class UserProfileController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    /**
     * 获取当前登录用户的资料
     * GET /api/v1/user/profile
     */
    @GetMapping("/profile")
    public ApiResponse<UserInfoResponse> getProfile() {
        Long userId = JwtAuthenticationFilter.getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error(401, "请先登录");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        return ApiResponse.success(buildUserInfoResponse(user));
    }

    /**
     * 更新当前登录用户的资料
     * POST /api/v1/user/profile
     */
    @PostMapping("/profile")
    public ApiResponse<UserInfoResponse> updateProfile(@RequestBody UpdateProfileRequest request) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error(401, "请先登录");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (request.getNickName() != null && !request.getNickName().trim().isEmpty()) {
            if (request.getNickName().length() > 30) {
                return ApiResponse.error(400, "昵称不能超过30个字符");
            }
            user.setNickName(request.getNickName().trim());
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        if (request.getGender() != null) {
            if (request.getGender() < 0 || request.getGender() > 2) {
                return ApiResponse.error(400, "性别参数无效");
            }
            user.setGender(request.getGender());
        }

        if (request.getRealName() != null && !request.getRealName().trim().isEmpty()) {
            user.setRealName(request.getRealName().trim());
        }

        user.setUpdateTime(LocalDateTime.now());
        user = userRepository.save(user);

        log.info("用户资料更新成功: userId={}", userId);
        return ApiResponse.success(buildUserInfoResponse(user));
    }

    @PostMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserInfoResponse> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error(401, "请先登录");
        }

        if (file == null || file.isEmpty()) {
            return ApiResponse.error(400, "请选择图片");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ApiResponse.error(400, "仅支持图片格式");
        }

        if (file.getSize() > 2L * 1024 * 1024) {
            return ApiResponse.error(400, "图片不能超过2MB");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        try {
            String originalFilename = file.getOriginalFilename();
            String ext = ".jpg";
            if (originalFilename != null) {
                int dot = originalFilename.lastIndexOf('.');
                if (dot >= 0 && dot < originalFilename.length() - 1) {
                    String candidate = originalFilename.substring(dot + 1).toLowerCase();
                    if (candidate.matches("[a-z0-9]{1,5}")) {
                        ext = "." + candidate;
                    }
                }
            }

            String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
            Path uploadRoot = Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath().normalize();
            Path userDir = uploadRoot.resolve(Paths.get("avatars", String.valueOf(userId))).normalize();
            Files.createDirectories(userDir);

            Path dest = userDir.resolve(fileName).normalize();
            file.transferTo(dest);

            String publicPath = "/uploads/avatars/" + userId + "/" + fileName;
            String scheme = request.getScheme();
            String host = request.getServerName();
            int port = request.getServerPort();
            boolean defaultPort = ("http".equalsIgnoreCase(scheme) && port == 80) || ("https".equalsIgnoreCase(scheme) && port == 443);
            String base = scheme + "://" + host + (defaultPort ? "" : ":" + port);
            String avatarUrl = base + publicPath;

            user.setAvatarUrl(avatarUrl);
            user.setUpdateTime(LocalDateTime.now());
            user = userRepository.save(user);

            return ApiResponse.success(buildUserInfoResponse(user));
        } catch (Exception e) {
            log.error("上传头像失败: userId={}", userId, e);
            return ApiResponse.error(500, "上传失败");
        }
    }

    /**
     * 构建用户信息响应
     */
    private UserInfoResponse buildUserInfoResponse(User user) {
        return UserInfoResponse.builder()
                .id(user.getId())
                .openid(user.getOpenId())
                .userIdentifier(user.getUserIdentifier())
                .nickName(user.getNickName())
                .realName(user.getRealName())
                .avatarUrl(user.getAvatarUrl())
                .phone(user.getPhone())
                .gender(user.getGender())
                .role(user.getRole())
                .status(user.getStatus())
                .balance(user.getBalance())
                .points(user.getPoints())
                .totalConsumption(user.getTotalConsumption())
                .totalOrders(user.getTotalOrders())
                .createTime(user.getCreateTime())
                .lastLoginTime(user.getLastLoginTime())
                .build();
    }
}
