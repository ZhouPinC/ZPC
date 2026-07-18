package com.wash.iot.controller.admin;

import com.wash.iot.common.response.ApiResponse;
import com.wash.iot.dto.response.FinanceRecordResponse;
import com.wash.iot.dto.response.FinanceSummaryResponse;
import com.wash.iot.enums.UserRole;
import com.wash.iot.security.JwtAuthenticationFilter;
import com.wash.iot.security.RoleRequired;
import com.wash.iot.service.FinanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员财务控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/finance")
public class AdminFinanceController {

    @Autowired
    private FinanceService financeService;

    /**
     * 获取收益概览
     * GET /api/v1/admin/finance/summary
     */
    @GetMapping("/summary")
    @RoleRequired(UserRole.ADMIN)
    public ApiResponse<FinanceSummaryResponse> getSummary() {
        Long adminUserId = JwtAuthenticationFilter.getCurrentUserId();
        FinanceSummaryResponse response = financeService.getSummary(adminUserId);
        return ApiResponse.success(response);
    }

    /**
     * 获取收益明细
     * GET /api/v1/admin/finance/records
     */
    @GetMapping("/records")
    @RoleRequired(UserRole.ADMIN)
    public ApiResponse<List<FinanceRecordResponse>> getRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long adminUserId = JwtAuthenticationFilter.getCurrentUserId();
        List<FinanceRecordResponse> records = financeService.getRecords(adminUserId, page, size);
        return ApiResponse.success(records);
    }
}
