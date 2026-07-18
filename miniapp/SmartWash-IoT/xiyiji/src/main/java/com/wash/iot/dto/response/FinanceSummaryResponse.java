package com.wash.iot.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 财务概览响应
 */
@Data
@Builder
public class FinanceSummaryResponse {
    private BigDecimal totalIncome;      // 总收入
    private BigDecimal todayIncome;      // 今日收入
    private BigDecimal weekIncome;       // 本周收入
    private BigDecimal monthIncome;      // 本月收入
    
    private BigDecimal pendingSettle;    // 待结算金额
    private BigDecimal settledAmount;    // 已结算金额
    
    private int totalOrders;             // 总订单数
    private int todayOrders;             // 今日订单数
}
