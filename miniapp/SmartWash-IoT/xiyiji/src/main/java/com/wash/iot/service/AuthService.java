package com.wash.iot.service;

import com.wash.iot.dto.request.BindPhoneRequest;
import com.wash.iot.dto.request.WxLoginRequest;
import com.wash.iot.dto.response.UserInfoResponse;
import com.wash.iot.dto.response.WxLoginResponse;
import com.wash.iot.entity.User;
import com.wash.iot.enums.UserRole;
import com.wash.iot.common.exception.BusinessException;
import com.wash.iot.repository.UserRepository;
import com.wash.iot.security.DataEncryptionUtil;
import com.wash.iot.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.converter.StringHttpMessageConverter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 认证服务 - OpenID核心版本
 * 核心登录方式：OpenID登录
 * OpenID获取失败则登录失败
 */
@Slf4j
@Service
public class AuthService {

    private static final String SUPER_ADMIN_OPENID = "******";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private DataEncryptionUtil dataEncryptionUtil;

    @Value("${wechat.appid:}")
    private String appId;

    @Value("${wechat.secret:}")
    private String appSecret;

    @Value("${admin.password:admin123}")
    private String adminPassword;

    /**
     * 微信OpenID登录
     * 核心登录流程：
     * 1. 前端调用wx.login获取code
     * 2. 后端使用code调用微信API获取OpenID
     * 3. 使用OpenID查找或创建用户
     * 4. 生成JWT Token返回
     * 5. OpenID获取失败则登录失败
     */
    public WxLoginResponse wxLogin(WxLoginRequest request) {
        try {
            // 1. 获取微信用户信息（OpenID），增加重试机制
            WxUserInfo wxUserInfo = getWxUserInfoWithRetry(request.getCode(), 3);

            // 2. 验证OpenID是否获取成功
            if (wxUserInfo == null || wxUserInfo.getOpenId() == null || wxUserInfo.getOpenId().isEmpty()) {
                log.error("OpenID获取失败: code={}", request.getCode());
                throw new RuntimeException("OpenID获取失败，请重试");
            }

            // 3. 使用OpenID查找或创建用户
            User user = userRepository.findByOpenId(wxUserInfo.getOpenId()).orElse(null);
            boolean isNewUser = (user == null);

            if (isNewUser) {
                // 创建新用户
                user = createNewUser(wxUserInfo);
                log.info("新用户注册: openId={}, nickName={}", wxUserInfo.getOpenId(), wxUserInfo.getNickName());
            } else {
                // 更新现有用户信息
                updateExistingUser(user, wxUserInfo);
            }

            if (SUPER_ADMIN_OPENID.equals(wxUserInfo.getOpenId())) {
                if (user.getRole() == null || !UserRole.SUPER_ADMIN.name().equals(user.getRole())) {
                    user.setRole(UserRole.SUPER_ADMIN.name());
                }
            }

            // 4. 更新登录时间和统计
            updateUserLoginStats(user);
            user = userRepository.save(user);

            // 5. 生成JWT Token
            UserRole role = user.getRole() != null ? UserRole.valueOf(user.getRole()) : UserRole.CONSUMER;
            String token = jwtTokenProvider.generateToken(user.getId(), role);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

            // 6. 构建响应
            return WxLoginResponse.builder()
                    .token(token)
                    .refreshToken(refreshToken)
                    .userInfo(buildUserInfo(user))
                    .isNewUser(isNewUser)
                    .needBindPhone(user.getPhone() == null || user.getPhone().isEmpty())
                    .role(user.getRole())
                    .build();

        } catch (Exception e) {
            log.error("微信OpenID登录失败: code={}", request.getCode(), e);
            throw new RuntimeException("登录失败: " + e.getMessage());
        }
    }

    /**
     * 绑定手机号（可选功能）
     */
    public UserInfoResponse bindPhone(Long userId, BindPhoneRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 验证手机号格式
        if (!isValidPhoneNumber(request.getPhone())) {
            throw new RuntimeException("手机号格式不正确");
        }

        // 检查手机号是否已被绑定
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("该手机号已被其他用户绑定");
        }

        // 验证短信验证码（如果启用）
        if (request.getSmsCode() != null && !request.getSmsCode().isEmpty()) {
            validateSmsCode(request.getPhone(), request.getSmsCode());
        }

        // 更新用户信息
        user.setPhone(request.getPhone());
        if (request.getRealName() != null && !request.getRealName().trim().isEmpty()) {
            user.setRealName(request.getRealName().trim());
        }
        user.setUpdateTime(LocalDateTime.now());
        user = userRepository.save(user);

        log.info("用户绑定手机号成功: userId={}, phone={}", userId, request.getPhone());
        return buildUserInfo(user);
    }

    /**
     * 验证管理密码，升级为管理员
     */
    public WxLoginResponse verifyAdmin(Long userId, String password) {
        if (!adminPassword.equals(password)) {
            throw new RuntimeException("密码错误");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (SUPER_ADMIN_OPENID.equals(user.getOpenId())) {
            user.setRole(UserRole.SUPER_ADMIN.name());
        } else if (user.getRole() == null || UserRole.CONSUMER.name().equals(user.getRole())) {
            user.setRole(UserRole.ADMIN.name());
        }
        user = userRepository.save(user);
        
        UserRole role = user.getRole() != null ? UserRole.valueOf(user.getRole()) : UserRole.ADMIN;
        String token = jwtTokenProvider.generateToken(user.getId(), role);
        
        return WxLoginResponse.builder()
                .token(token)
                .userInfo(buildUserInfo(user))
                .isNewUser(false)
                .needBindPhone(false)
                .build();
    }

    /**
     * 退出管理模式，降级为普通用户
     */
    public WxLoginResponse exitAdminMode(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        // 降级为普通用户
        user.setRole(UserRole.CONSUMER.name());
        user = userRepository.save(user);
        
        // 生成新的Token
        String token = jwtTokenProvider.generateToken(user.getId(), UserRole.CONSUMER);
        
        return WxLoginResponse.builder()
                .token(token)
                .userInfo(buildUserInfo(user))
                .isNewUser(false)
                .needBindPhone(false)
                .build();
    }

    /**
     * 构建用户信息响应
     */
    private UserInfoResponse buildUserInfo(User user) {
        return UserInfoResponse.builder()
                .id(user.getId())
                .openid(user.getOpenId())
                .userIdentifier(user.getUserIdentifier())
                .nickName(user.getNickName())
                .realName(user.getRealName())
                .avatarUrl(user.getAvatarUrl())
                .phone(user.getPhone())
                .gender(user.getGender())
                .role(user.getRole() != null ? user.getRole() : UserRole.CONSUMER.name())
                .status(user.getStatus())
                .balance(user.getBalance())
                .points(user.getPoints())
                .totalConsumption(user.getTotalConsumption() != null ? user.getTotalConsumption() : BigDecimal.ZERO)
                .totalOrders(user.getTotalOrders() != null ? user.getTotalOrders() : 0)
                .createTime(user.getCreateTime())
                .lastLoginTime(user.getLastLoginTime())
                .build();
    }

    /**
     * 获取微信用户信息
     */
    private WxUserInfo getWxUserInfo(String code) {
        // 严格检查配置，如果配置未设置则抛出异常
        if (appId == null || appId.isEmpty() || appSecret == null || appSecret.isEmpty()) {
            log.error("微信配置未设置，无法调用微信API");
            throw new RuntimeException("微信配置未正确设置，请联系管理员");
        }

        try {
            log.info("开始调用微信API获取OpenID，AppID={}, Code={}", appId, code);
            String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                appId, appSecret, code
            );
            log.info("微信API请求URL: {}", url);

            // 配置RestTemplate以支持text/plain响应（微信API返回Content-Type: text/plain）
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.getMessageConverters().add(new StringHttpMessageConverter(StandardCharsets.UTF_8));

            // 发送请求获取字符串响应
            String responseBody = restTemplate.getForObject(url, String.class);
            log.info("微信API响应: {}", responseBody);

            // 手动解析JSON字符串（因为微信返回text/plain类型）
            com.alibaba.fastjson2.JSONObject response = com.alibaba.fastjson2.JSONObject.parseObject(responseBody);

            if (response != null && response.containsKey("openid")) {
                WxUserInfo userInfo = new WxUserInfo();
                userInfo.setOpenId(response.getString("openid"));
                userInfo.setSessionKey(response.getString("session_key"));
                userInfo.setUnionId(response.getString("unionid"));
                return userInfo;
            } else {
                log.error("微信登录失败: {}", response);
                throw new RuntimeException("微信登录失败: " + response);
            }
        } catch (Exception e) {
            log.error("调用微信API失败，异常类型={}, 消息={}", e.getClass().getSimpleName(), e.getMessage());
            log.error("异常堆栈:", e);
            throw new RuntimeException("微信API调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 带重试机制的获取微信用户信息
     */
    private WxUserInfo getWxUserInfoWithRetry(String code, int maxRetries) {
        int retryCount = 0;
        Exception lastException = null;
        
        while (retryCount < maxRetries) {
            try {
                return getWxUserInfo(code);
            } catch (Exception e) {
                lastException = e;
                retryCount++;
                log.warn("获取微信用户信息失败，第{}次重试: {}", retryCount, e.getMessage());
                
                if (retryCount < maxRetries) {
                    try {
                        // 等待一段时间后重试，每次等待时间递增
                        Thread.sleep(500 * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new BusinessException(500, "获取微信用户信息被中断");
                    }
                }
            }
        }
        
        log.error("获取微信用户信息失败，已达到最大重试次数: {}", maxRetries);
        throw new BusinessException(500, "微信登录失败，请稍后重试", lastException);
    }

    /**
     * 创建新用户
     */
    private User createNewUser(WxUserInfo wxUserInfo) {
        User user = new User();
        user.setOpenId(wxUserInfo.getOpenId());
        user.setUnionId(wxUserInfo.getUnionId());
        user.setSessionKey(wxUserInfo.getSessionKey());
        user.setNickName("微信用户");
        user.setBalance(BigDecimal.ZERO);
        user.setPoints(0);
        user.setRole(UserRole.CONSUMER.name());
        user.setStatus("ACTIVE");
        user.setTotalConsumption(BigDecimal.ZERO);
        user.setTotalOrders(0);
        
        // 生成用户唯一标识符
        user.setUserIdentifier(generateUserIdentifier());
        
        return user;
    }

    /**
     * 更新现有用户信息
     */
    private void updateExistingUser(User user, WxUserInfo wxUserInfo) {
        user.setSessionKey(wxUserInfo.getSessionKey());
        if (wxUserInfo.getUnionId() != null) {
            user.setUnionId(wxUserInfo.getUnionId());
        }
        user.setUpdateTime(LocalDateTime.now());
    }

    /**
     * 更新用户登录统计
     */
    private void updateUserLoginStats(User user) {
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastActiveTime(LocalDateTime.now());
        user.setTokenExpireTime(LocalDateTime.now().plusMonths(6));
    }

    /**
     * 生成用户唯一标识符
     */
    private String generateUserIdentifier() {
        // 生成格式：UID + 时间戳后8位 + 4位随机数
        String timestamp = String.valueOf(System.currentTimeMillis());
        String timestampSuffix = timestamp.substring(timestamp.length() - 8);
        String randomSuffix = String.format("%04d", (int)(Math.random() * 10000));
        return "UID" + timestampSuffix + randomSuffix;
    }

    /**
     * 更新用户活跃时间
     * 可以在用户进行各种操作时调用此方法，以更新最后活跃时间
     */
    public void updateUserActiveTime(Long userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                user.setLastActiveTime(LocalDateTime.now());
                userRepository.save(user);
            }
        } catch (Exception e) {
            log.error("更新用户活跃时间失败: userId={}", userId, e);
        }
    }

    /**
     * 验证手机号格式
     */
    private boolean isValidPhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        // 中国大陆手机号正则表达式
        String regex = "^1[3-9]\\d{9}$";
        return phone.matches(regex);
    }

    /**
     * 验证短信验证码
     */
    private void validateSmsCode(String phone, String code) {
        // TODO: 实现短信验证码验证逻辑
        // 这里应该验证Redis中存储的验证码
        log.info("验证短信验证码: phone={}, code={}", phone, code);
    }

    /**
     * 微信用户信息内部类
     */
    private static class WxUserInfo {
        private String openId;
        private String sessionKey;
        private String unionId;
        private String nickName;

        // getters and setters
        public String getOpenId() { return openId; }
        public void setOpenId(String openId) { this.openId = openId; }
        public String getSessionKey() { return sessionKey; }
        public void setSessionKey(String sessionKey) { this.sessionKey = sessionKey; }
        public String getUnionId() { return unionId; }
        public void setUnionId(String unionId) { this.unionId = unionId; }
        public String getNickName() { return nickName; }
        public void setNickName(String nickName) { this.nickName = nickName; }
    }
}
