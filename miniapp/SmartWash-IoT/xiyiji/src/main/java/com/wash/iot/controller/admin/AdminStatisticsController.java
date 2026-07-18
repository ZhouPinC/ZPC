package com.wash.iot.controller.admin;

import com.wash.iot.common.response.ApiResponse;
import com.wash.iot.dto.response.AdminOverviewResponse;
import com.wash.iot.enums.UserRole;
import com.wash.iot.security.JwtAuthenticationFilter;
import com.wash.iot.security.RoleRequired;
import com.wash.iot.service.AdminStatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员统计控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/statistics")
public class AdminStatisticsController {

    @Autowired
    private AdminStatisticsService statisticsService;

    /**
     * 获取概览统计
     * GET /api/v1/admin/statistics/overview
     */
    @GetMapping("/overview")
    @RoleRequired(UserRole.ADMIN)
    public ApiResponse<AdminOverviewResponse> getOverview() {
        Long adminUserId = JwtAuthenticationFilter.getCurrentUserId();
        AdminOverviewResponse response = statisticsService.getOverview(adminUserId);
        return ApiResponse.success(response);
    }

    /**
     * 获取使用率统计
     * GET /api/v1/admin/statistics/usage
     */
    @GetMapping("/usage")
    @RoleRequired(UserRole.ADMIN)
    public ApiResponse<Object> getUsageStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        // TODO: 实现使用率统计
        return ApiResponse.success(null);
    }

    /**
     * 获取高峰时段统计
     * GET /api/v1/admin/statistics/peak-hours
     */
    @GetMapping("/peak-hours")
    @RoleRequired(UserRole.ADMIN)
    public ApiResponse<Object> getPeakHoursStats() {
        // TODO: 实现高峰时段统计
        return ApiResponse.success(null);
    }

    /**
     * 获取洗涤程序统计
     * GET /api/v1/admin/statistics/programs
     */
    @GetMapping("/programs")
    @RoleRequired(UserRole.ADMIN)
    public ApiResponse<Object> getProgramStats() {
        // TODO: 实现洗涤程序统计
        return ApiResponse.success(null);
    }
}
