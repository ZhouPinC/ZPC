package com.wash.iot.controller.consumer;

import com.wash.iot.common.response.ApiResponse;
import com.wash.iot.dto.response.NotificationResponse;
import com.wash.iot.enums.UserRole;
import com.wash.iot.security.JwtAuthenticationFilter;
import com.wash.iot.security.RoleRequired;
import com.wash.iot.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 通知控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/consumer/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * 获取通知列表
     * GET /api/v1/consumer/notifications
     */
    @GetMapping
    @RoleRequired({UserRole.CONSUMER, UserRole.ADMIN, UserRole.SUPER_ADMIN})
    public ApiResponse<List<NotificationResponse>> getNotifications() {
        Long userId = JwtAuthenticationFilter.getCurrentUserId();
        List<NotificationResponse> notifications = notificationService.getUserNotifications(userId);
        return ApiResponse.success(notifications);
    }

    /**
     * 获取未读数量
     * GET /api/v1/consumer/notifications/unread-count
     */
    @GetMapping("/unread-count")
    @RoleRequired({UserRole.CONSUMER, UserRole.ADMIN, UserRole.SUPER_ADMIN})
    public ApiResponse<Integer> getUnreadCount() {
        Long userId = JwtAuthenticationFilter.getCurrentUserId();
        int count = notificationService.getUnreadCount(userId);
        return ApiResponse.success(count);
    }

    /**
     * 标记已读
     * POST /api/v1/consumer/notifications/read
     */
    @PostMapping("/read")
    @RoleRequired({UserRole.CONSUMER, UserRole.ADMIN, UserRole.SUPER_ADMIN})
    public ApiResponse<Void> markAsRead(@RequestBody Map<String, List<Long>> request) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId();
        List<Long> ids = request.get("ids");
        notificationService.markAsRead(userId, ids);
        return ApiResponse.success();
    }
}
