package com.wash.iot.controller;

import com.wash.iot.common.response.ApiResponse;
import com.wash.iot.entity.User;
import com.wash.iot.repository.UserRepository;
import com.wash.iot.security.JwtAuthenticationFilter;
import com.wash.iot.service.AuthService;
import com.wash.iot.service.UserCleanupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理控制器
 * 提供用户管理相关的API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/users")
public class UserManagementController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserCleanupService userCleanupService;
    
    @Autowired
    private AuthService authService;

    /**
     * 获取用户统计信息
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getUserStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 总用户数
        long totalUsers = userRepository.count();
        stats.put("totalUsers", totalUsers);
        
        // 活跃用户数
        long activeUsers = userRepository.countByStatus("ACTIVE");
        stats.put("activeUsers", activeUsers);
        
        // 非活跃用户数
        long inactiveUsers = userRepository.countByStatus("INACTIVE");
        stats.put("inactiveUsers", inactiveUsers);
        
        // 管理员用户数
        long adminUsers = userRepository.countByRole("ADMIN") + userRepository.countByRole("SUPER_ADMIN");
        stats.put("adminUsers", adminUsers);
        
        // 最近注册用户数（最近30天）
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<User> recentUsers = userRepository.findRecentlyLoggedInUsers();
        long recentRegisteredUsers = recentUsers.stream()
            .filter(u -> u.getCreateTime() != null && u.getCreateTime().isAfter(thirtyDaysAgo))
            .count();
        stats.put("recentRegisteredUsers", recentRegisteredUsers);
        
        return ApiResponse.success(stats);
    }
    
    /**
     * 获取非活跃用户列表
     */
    @GetMapping("/inactive")
    public ApiResponse<List<User>> getInactiveUsers() {
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
        List<User> inactiveUsers = userRepository.findInactiveUsersBefore(sixMonthsAgo);
        return ApiResponse.success(inactiveUsers);
    }
    
    /**
     * 获取没有订单记录的用户列表
     */
    @GetMapping("/without-orders")
    public ApiResponse<List<User>> getUsersWithoutOrders() {
        List<User> usersWithoutOrders = userRepository.findUsersWithoutOrders();
        return ApiResponse.success(usersWithoutOrders);
    }
    
    /**
     * 手动执行用户清理
     */
    @PostMapping("/cleanup")
    public ApiResponse<String> executeCleanup() {
        try {
            userCleanupService.executeCleanupManually();
            return ApiResponse.success("用户清理任务执行成功");
        } catch (Exception e) {
            log.error("手动执行用户清理失败", e);
            return ApiResponse.error(500, "用户清理任务执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 激活用户
     */
    @PostMapping("/{userId}/activate")
    public ApiResponse<String> activateUser(@PathVariable Long userId) {
        try {
            boolean success = userCleanupService.reactivateUser(userId);
            if (success) {
                return ApiResponse.success("用户激活成功");
            } else {
                return ApiResponse.error(404, "用户不存在或状态不符合激活条件");
            }
        } catch (Exception e) {
            log.error("激活用户失败: userId={}", userId, e);
            return ApiResponse.error(500, "激活用户失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新用户活跃时间
     */
    @PostMapping("/update-active-time")
    public ApiResponse<String> updateUserActiveTime() {
        Long userId = JwtAuthenticationFilter.getCurrentUserId();
        if (userId == null) {
            return ApiResponse.error(401, "请先登录");
        }
        
        try {
            authService.updateUserActiveTime(userId);
            return ApiResponse.success("活跃时间更新成功");
        } catch (Exception e) {
            log.error("更新用户活跃时间失败: userId={}", userId, e);
            return ApiResponse.error(500, "更新活跃时间失败: " + e.getMessage());
        }
    }
}