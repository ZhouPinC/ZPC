package com.wash.iot.controller.consumer;

import com.wash.iot.common.response.ApiResponse;
import com.wash.iot.dto.response.QueueResponse;
import com.wash.iot.enums.UserRole;
import com.wash.iot.security.JwtAuthenticationFilter;
import com.wash.iot.security.RoleRequired;
import com.wash.iot.service.QueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 排队控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/consumer/queue")
public class QueueController {

    @Autowired
    private QueueService queueService;

    /**
     * 加入排队
     * POST /api/v1/consumer/queue/join
     */
    @PostMapping("/join")
    @RoleRequired({UserRole.CONSUMER, UserRole.ADMIN, UserRole.SUPER_ADMIN})
    public ApiResponse<QueueResponse> joinQueue(@RequestBody Map<String, Long> request) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId();
        Long deviceId = request.get("deviceId");
        QueueResponse response = queueService.joinQueue(userId, deviceId);
        return ApiResponse.success(response);
    }

    /**
     * 获取我的排队状态
     * GET /api/v1/consumer/queue/my
     */
    @GetMapping("/my")
    @RoleRequired({UserRole.CONSUMER, UserRole.ADMIN, UserRole.SUPER_ADMIN})
    public ApiResponse<List<QueueResponse>> getMyQueues() {
        Long userId = JwtAuthenticationFilter.getCurrentUserId();
        List<QueueResponse> queues = queueService.getUserQueues(userId);
        return ApiResponse.success(queues);
    }

    /**
     * 退出排队
     * DELETE /api/v1/consumer/queue/{queueId}
     */
    @DeleteMapping("/{queueId}")
    @RoleRequired({UserRole.CONSUMER, UserRole.ADMIN, UserRole.SUPER_ADMIN})
    public ApiResponse<Void> leaveQueue(@PathVariable Long queueId) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId();
        queueService.leaveQueue(userId, queueId);
        return ApiResponse.success();
    }
}
