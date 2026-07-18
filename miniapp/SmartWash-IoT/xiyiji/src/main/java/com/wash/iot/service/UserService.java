package com.wash.iot.service;

import com.wash.iot.entity.User;
import com.wash.iot.entity.Order;
import com.wash.iot.repository.UserRepository;
import com.wash.iot.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private OrderRepository orderRepository;

    @Value("${wechat.appid:}")
    private String appId;

    @Value("${wechat.secret:}")
    private String appSecret;

    /**
     * 微信登录 - 通过code换取openId和session_key
     */
    public User wxLogin(String code, String nickName, String avatarUrl) {
        // 1. 调用微信接口获取openId（生产环境需要真实调用）
        String openId = getOpenIdFromWx(code);
        
        // 2. 查找或创建用户
        User user = userRepository.findByOpenId(openId).orElse(null);
        
        if (user == null) {
            // 新用户注册
            user = new User();
            user.setOpenId(openId);
            user.setNickName(nickName);
            user.setAvatarUrl(avatarUrl);
            user.setBalance(new BigDecimal("0.00"));
            user.setPoints(0);
            log.info("新用户注册: openId={}, nickName={}", openId, nickName);
        } else {
            // 老用户更新信息
            if (nickName != null && !nickName.isEmpty()) {
                user.setNickName(nickName);
            }
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                user.setAvatarUrl(avatarUrl);
            }
            log.info("用户登录: userId={}, nickName={}", user.getId(), user.getNickName());
        }
        
        // 3. 更新登录时间和token过期时间（半年）
        user.setLastLoginTime(LocalDateTime.now());
        user.setTokenExpireTime(LocalDateTime.now().plusMonths(6));
        
        return userRepository.save(user);
    }

    /**
     * 通过userId获取用户信息
     */
    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * 通过openId获取用户信息
     */
    public Optional<User> getUserByOpenId(String openId) {
        return userRepository.findByOpenId(openId);
    }

    /**
     * 更新用户信息
     */
    public User updateUserInfo(Long userId, String nickName, String avatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        if (nickName != null && !nickName.isEmpty()) {
            user.setNickName(nickName);
        }
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            user.setAvatarUrl(avatarUrl);
        }
        
        return userRepository.save(user);
    }
    
    /**
     * 保存用户
     */
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    /**
     * 获取用户余额
     */
    public BigDecimal getBalance(Long userId) {
        return userRepository.findById(userId)
                .map(User::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * 充值余额
     */
    public User recharge(Long userId, BigDecimal amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        user.setBalance(user.getBalance().add(amount));
        log.info("用户充值: userId={}, amount={}, newBalance={}", userId, amount, user.getBalance());
        
        return userRepository.save(user);
    }

    /**
     * 扣减余额
     */
    public boolean deductBalance(Long userId, BigDecimal amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        if (user.getBalance().compareTo(amount) < 0) {
            return false; // 余额不足
        }
        
        user.setBalance(user.getBalance().subtract(amount));
        userRepository.save(user);
        log.info("用户扣款: userId={}, amount={}, newBalance={}", userId, amount, user.getBalance());
        
        return true;
    }

    /**
     * 获取用户洗衣历史记录
     */
    public List<Order> getWashHistory(Long userId) {
        return orderRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    /**
     * 检查用户token是否有效
     */
    public boolean isTokenValid(Long userId) {
        return userRepository.findById(userId)
                .map(user -> user.getTokenExpireTime() != null 
                        && user.getTokenExpireTime().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    /**
     * 调用微信接口获取openId（简化版，生产环境需要真实实现）
     */
    private String getOpenIdFromWx(String code) {
        // 生产环境应该调用微信API:
        // https://api.weixin.qq.com/sns/jscode2session?appid=APPID&secret=SECRET&js_code=CODE&grant_type=authorization_code
        
        if (appId == null || appId.isEmpty() || appSecret == null || appSecret.isEmpty()) {
            // 开发环境：使用code作为模拟openId
            log.warn("微信配置未设置，使用模拟openId");
            return "mock_openid_" + code;
        }
        
        try {
            String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                appId, appSecret, code
            );
            
            RestTemplate restTemplate = new RestTemplate();
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey("openid")) {
                return (String) response.get("openid");
            } else {
                log.error("微信登录失败: {}", response);
                throw new RuntimeException("微信登录失败");
            }
        } catch (Exception e) {
            log.error("调用微信API失败", e);
            // 降级处理：使用模拟openId
            return "mock_openid_" + code;
        }
    }
}
