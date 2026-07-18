package com.wash.iot.controller.consumer;

import com.wash.iot.common.exception.BusinessException;
import com.wash.iot.common.response.ApiResponse;
import com.wash.iot.entity.User;
import com.wash.iot.enums.UserRole;
import com.wash.iot.repository.UserRepository;
import com.wash.iot.security.JwtAuthenticationFilter;
import com.wash.iot.security.RoleRequired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 钱包控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/consumer/wallet")
public class WalletController {

    @Autowired
    private UserRepository userRepository;

    /**
     * 获取余额
     * GET /api/v1/consumer/wallet/balance
     */
    @GetMapping("/balance")
    @RoleRequired({UserRole.CONSUMER, UserRole.ADMIN, UserRole.SUPER_ADMIN})
    public ApiResponse<Map<String, Object>> getBalance() {
        Long userId = JwtAuthenticationFilter.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        Map<String, Object> result = new HashMap<>();
        result.put("balance", user.getBalance());
        result.put("points", user.getPoints());
        return ApiResponse.success(result);
    }

    /**
     * 充值（模拟）
     * POST /api/v1/consumer/wallet/recharge
     */
    @PostMapping("/recharge")
    @RoleRequired({UserRole.CONSUMER, UserRole.ADMIN, UserRole.SUPER_ADMIN})
    public ApiResponse<Map<String, Object>> recharge(@RequestBody Map<String, Object> request) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId();
        BigDecimal amount = new BigDecimal(request.get("amount").toString());

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("充值金额必须大于0");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));

        user.setBalance(user.getBalance().add(amount));
        userRepository.save(user);

        log.info("用户充值: userId={}, amount={}, newBalance={}", userId, amount, user.getBalance());

        Map<String, Object> result = new HashMap<>();
        result.put("balance", user.getBalance());
        result.put("message", "充值成功");
        return ApiResponse.success(result);
    }

    /**
     * 获取消费记录
     * GET /api/v1/consumer/wallet/records
     */
    @GetMapping("/records")
    @RoleRequired({UserRole.CONSUMER, UserRole.ADMIN, UserRole.SUPER_ADMIN})
    public ApiResponse<Object> getRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // TODO: 实现消费记录查询
        return ApiResponse.success(java.util.List.of());
    }
}
