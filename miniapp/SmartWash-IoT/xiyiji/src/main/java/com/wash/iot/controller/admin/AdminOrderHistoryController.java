package com.wash.iot.controller.admin;

import com.wash.iot.common.response.ApiResponse;
import com.wash.iot.dto.response.AdminOrderHistoryResponse;
import com.wash.iot.security.JwtAuthenticationFilter;
import com.wash.iot.service.AdminOrderHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端订单历史控制器
 * 提供完整的订单查询和统计功能
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderHistoryController {

    @Autowired
    private AdminOrderHistoryService adminOrderHistoryService;

    /**
     * 获取订单历史列表
     */
    @GetMapping("/history")
    public ApiResponse<AdminOrderHistoryResponse> getOrderHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) Long userId) {

        try {
            Long adminId = JwtAuthenticationFilter.getCurrentUserId();
            if (adminId == null) {
                return ApiResponse.error(401, "请先登录");
            }

            AdminOrderHistoryService.OrderQueryParams params = new AdminOrderHistoryService.OrderQueryParams();
            params.setAdminId(adminId);
            params.setPage(page);
            params.setSize(size);
            params.setStatus(status);
            params.setStartTime(startTime);
            params.setEndTime(endTime);
            params.setUserId(userId);

            AdminOrderHistoryResponse response = adminOrderHistoryService.getAdminOrderHistory(adminId, params);
            return ApiResponse.success(response);

        } catch (Exception e) {
            log.error("获取订单历史失败", e);
            return ApiResponse.error(500, "获取订单历史失败: " + e.getMessage());
        }
    }

    /**
     * 获取订单详细信息
     */
    @GetMapping("/detail/{orderNo}")
    public ApiResponse<AdminOrderHistoryResponse.OrderDetailItem> getOrderDetail(@PathVariable String orderNo) {
        try {
            Long adminId = JwtAuthenticationFilter.getCurrentUserId();
            if (adminId == null) {
                return ApiResponse.error(401, "请先登录");
            }

            AdminOrderHistoryResponse.OrderDetailItem detail = adminOrderHistoryService.getOrderDetail(orderNo);
            return ApiResponse.success(detail);

        } catch (Exception e) {
            log.error("获取订单详情失败: orderNo={}", orderNo, e);
            return ApiResponse.error(500, "获取订单详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取订单统计数据
     */
    @GetMapping("/statistics")
    public ApiResponse<AdminOrderHistoryResponse.SummaryStatistics> getOrderStatistics(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {

        try {
            Long adminId = JwtAuthenticationFilter.getCurrentUserId();
            if (adminId == null) {
                return ApiResponse.error(401, "请先登录");
            }

            AdminOrderHistoryService.StatisticsParams params = new AdminOrderHistoryService.StatisticsParams();
            params.setStartTime(startTime);
            params.setEndTime(endTime);

            AdminOrderHistoryResponse.SummaryStatistics statistics = adminOrderHistoryService.getOrderStatistics(adminId, params);
            return ApiResponse.success(statistics);

        } catch (Exception e) {
            log.error("获取订单统计失败", e);
            return ApiResponse.error(500, "获取订单统计失败: " + e.getMessage());
        }
    }

    /**
     * 导出订单历史数据
     */
    @GetMapping("/export")
    public ApiResponse<String> exportOrderHistory(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(required = false) Long userId) {

        try {
            Long adminId = JwtAuthenticationFilter.getCurrentUserId();
            if (adminId == null) {
                return ApiResponse.error(401, "请先登录");
            }

            // TODO: 实现Excel导出功能
            String downloadUrl = "/api/v1/admin/orders/download/export_" + System.currentTimeMillis() + ".xlsx";

            return ApiResponse.success(downloadUrl);

        } catch (Exception e) {
            log.error("导出订单历史失败", e);
            return ApiResponse.error(500, "导出订单历史失败: " + e.getMessage());
        }
    }

    /**
     * 获取今日订单概览
     */
    @GetMapping("/today-overview")
    public ApiResponse<TodayOrderOverview> getTodayOverview() {
        try {
            Long adminId = JwtAuthenticationFilter.getCurrentUserId();
            if (adminId == null) {
                return ApiResponse.error(401, "请先登录");
            }

            LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime todayEnd = todayStart.plusDays(1);

            AdminOrderHistoryService.StatisticsParams params = new AdminOrderHistoryService.StatisticsParams();
            params.setStartTime(todayStart);
            params.setEndTime(todayEnd);

            AdminOrderHistoryResponse.SummaryStatistics statistics = adminOrderHistoryService.getOrderStatistics(adminId, params);

            TodayOrderOverview overview = new TodayOrderOverview();
            overview.setTodayOrders(statistics.getTotalOrders());
            overview.setTodayRevenue(statistics.getTotalAmount());
            overview.setTodayIncome(statistics.getTotalIncome());
            overview.setCompletedOrders(statistics.getCompletedOrders());
            overview.setRunningOrders(getRunningOrdersCount(adminId));
            overview.setPendingOrders(getPendingOrdersCount(adminId));

            return ApiResponse.success(overview);

        } catch (Exception e) {
            log.error("获取今日概览失败", e);
            return ApiResponse.error(500, "获取今日概览失败: " + e.getMessage());
        }
    }

    /**
     * 获取设备使用排行
     */
    @GetMapping("/device-ranking")
    public ApiResponse<List<DeviceRankingItem>> getDeviceRanking(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "REVENUE") String sortBy) {

        try {
            Long adminId = JwtAuthenticationFilter.getCurrentUserId();
            if (adminId == null) {
                return ApiResponse.error(401, "请先登录");
            }

            // TODO: 实现设备排行逻辑
            List<DeviceRankingItem> rankings = List.of(); // 临时空列表

            return ApiResponse.success(rankings);

        } catch (Exception e) {
            log.error("获取设备排行失败", e);
            return ApiResponse.error(500, "获取设备排行失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户使用排行
     */
    @GetMapping("/user-ranking")
    public ApiResponse<List<UserRankingItem>> getUserRanking(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "CONSUMPTION") String sortBy) {

        try {
            Long adminId = JwtAuthenticationFilter.getCurrentUserId();
            if (adminId == null) {
                return ApiResponse.error(401, "请先登录");
            }

            // TODO: 实现用户排行逻辑
            List<UserRankingItem> rankings = List.of(); // 临时空列表

            return ApiResponse.success(rankings);

        } catch (Exception e) {
            log.error("获取用户排行失败", e);
            return ApiResponse.error(500, "获取用户排行失败: " + e.getMessage());
        }
    }

    // 辅助方法
    private long getRunningOrdersCount(Long adminId) {
        // TODO: 实现获取进行中订单数量
        return 0;
    }

    private long getPendingOrdersCount(Long adminId) {
        // TODO: 实现获取待处理订单数量
        return 0;
    }

    // 数据传输对象
    public static class TodayOrderOverview {
        private long todayOrders;
        private java.math.BigDecimal todayRevenue;
        private java.math.BigDecimal todayIncome;
        private long completedOrders;
        private long runningOrders;
        private long pendingOrders;

        // getters and setters
        public long getTodayOrders() { return todayOrders; }
        public void setTodayOrders(long todayOrders) { this.todayOrders = todayOrders; }
        public java.math.BigDecimal getTodayRevenue() { return todayRevenue; }
        public void setTodayRevenue(java.math.BigDecimal todayRevenue) { this.todayRevenue = todayRevenue; }
        public java.math.BigDecimal getTodayIncome() { return todayIncome; }
        public void setTodayIncome(java.math.BigDecimal todayIncome) { this.todayIncome = todayIncome; }
        public long getCompletedOrders() { return completedOrders; }
        public void setCompletedOrders(long completedOrders) { this.completedOrders = completedOrders; }
        public long getRunningOrders() { return runningOrders; }
        public void setRunningOrders(long runningOrders) { this.runningOrders = runningOrders; }
        public long getPendingOrders() { return pendingOrders; }
        public void setPendingOrders(long pendingOrders) { this.pendingOrders = pendingOrders; }
    }

    public static class DeviceRankingItem {
        private Long deviceId;
        private String deviceName;
        private String location;
        private long orderCount;
        private java.math.BigDecimal revenue;
        private java.math.BigDecimal income;
        private Double utilizationRate;
        private int rank;

        // getters and setters
        public Long getDeviceId() { return deviceId; }
        public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
        public String getDeviceName() { return deviceName; }
        public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public long getOrderCount() { return orderCount; }
        public void setOrderCount(long orderCount) { this.orderCount = orderCount; }
        public java.math.BigDecimal getRevenue() { return revenue; }
        public void setRevenue(java.math.BigDecimal revenue) { this.revenue = revenue; }
        public java.math.BigDecimal getIncome() { return income; }
        public void setIncome(java.math.BigDecimal income) { this.income = income; }
        public Double getUtilizationRate() { return utilizationRate; }
        public void setUtilizationRate(Double utilizationRate) { this.utilizationRate = utilizationRate; }
        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }
    }

    public static class UserRankingItem {
        private Long userId;
        private String userName;
        private String phone;
        private long orderCount;
        private java.math.BigDecimal totalConsumption;
        private String lastOrderTime;
        private int rank;

        // getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public long getOrderCount() { return orderCount; }
        public void setOrderCount(long orderCount) { this.orderCount = orderCount; }
        public java.math.BigDecimal getTotalConsumption() { return totalConsumption; }
        public void setTotalConsumption(java.math.BigDecimal totalConsumption) { this.totalConsumption = totalConsumption; }
        public String getLastOrderTime() { return lastOrderTime; }
        public void setLastOrderTime(String lastOrderTime) { this.lastOrderTime = lastOrderTime; }
        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }
    }
}