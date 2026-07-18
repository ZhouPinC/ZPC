package com.wash.iot.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理员概览响应
 */
@Data
@Builder
public class AdminOverviewResponse {
    private DeviceStats devices;
    private TodayStats today;
    private MonthStats month;

    @Data
    @Builder
    public static class DeviceStats {
        private int total;
        private int online;
        private int running;
        private int fault;
        private int offline;
    }

    @Data
    @Builder
    public static class TodayStats {
        private int orders;
        private BigDecimal income;
        private int usageRate;
    }

    @Data
    @Builder
    public static class MonthStats {
        private int orders;
        private BigDecimal income;
        private int compareLastMonth; // 环比百分比
    }
}
