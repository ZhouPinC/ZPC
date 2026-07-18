package com.wash.iot.service;

import com.wash.iot.dto.response.FinanceRecordResponse;
import com.wash.iot.dto.response.FinanceSummaryResponse;
import com.wash.iot.entity.Device;
import com.wash.iot.entity.IncomeRecord;
import com.wash.iot.entity.Order;
import com.wash.iot.repository.DeviceRepository;
import com.wash.iot.repository.IncomeRecordRepository;
import com.wash.iot.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 财务服务
 */
@Slf4j
@Service
public class FinanceService {

    @Autowired
    private IncomeRecordRepository incomeRecordRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private OrderRepository orderRepository;

    /**
     * 获取财务概览
     */
    public FinanceSummaryResponse getSummary(Long adminUserId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);
        LocalDateTime weekStart = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        // 查询各时段收入
        BigDecimal totalIncome = incomeRecordRepository.sumNetIncomeByAdminUserId(adminUserId);
        BigDecimal todayIncome = incomeRecordRepository.sumNetIncomeByAdminUserIdAndPeriod(adminUserId, todayStart, todayEnd);
        BigDecimal weekIncome = incomeRecordRepository.sumNetIncomeByAdminUserIdAndPeriod(adminUserId, weekStart, todayEnd);
        BigDecimal monthIncome = incomeRecordRepository.sumNetIncomeByAdminUserIdAndPeriod(adminUserId, monthStart, todayEnd);

        // 统计订单数
        int totalOrders = 0;
        int todayOrders = 0;
        try {
            totalOrders = orderRepository.countByOwnerIdAndPeriod(adminUserId, 
                    LocalDateTime.of(2020, 1, 1, 0, 0), todayEnd);
            todayOrders = orderRepository.countByOwnerIdAndPeriod(adminUserId, todayStart, todayEnd);
        } catch (Exception e) {
            log.error("统计订单数失败", e);
        }

        return FinanceSummaryResponse.builder()
                .totalIncome(totalIncome != null ? totalIncome : BigDecimal.ZERO)
                .todayIncome(todayIncome != null ? todayIncome : BigDecimal.ZERO)
                .weekIncome(weekIncome != null ? weekIncome : BigDecimal.ZERO)
                .monthIncome(monthIncome != null ? monthIncome : BigDecimal.ZERO)
                .pendingSettle(BigDecimal.ZERO) // TODO: 实现待结算统计
                .settledAmount(BigDecimal.ZERO) // TODO: 实现已结算统计
                .totalOrders(totalOrders)
                .todayOrders(todayOrders)
                .build();
    }

    /**
     * 获取财务记录列表
     */
    public List<FinanceRecordResponse> getRecords(Long adminUserId, int page, int size) {
        List<IncomeRecord> records = incomeRecordRepository.findByAdminUserIdOrderByCreateTimeDesc(adminUserId);

        // 简单分页
        int start = page * size;
        int end = Math.min(start + size, records.size());
        if (start >= records.size()) {
            return List.of();
        }

        return records.subList(start, end).stream()
                .map(this::buildRecordResponse)
                .collect(Collectors.toList());
    }

    /**
     * 构建记录响应
     */
    private FinanceRecordResponse buildRecordResponse(IncomeRecord record) {
        Device device = deviceRepository.findById(record.getDeviceId()).orElse(null);
        Order order = orderRepository.findById(record.getOrderId()).orElse(null);

        return FinanceRecordResponse.builder()
                .id(record.getId())
                .orderId(record.getOrderId())
                .orderNo(order != null ? order.getOrderNo() : null)
                .deviceId(record.getDeviceId())
                .deviceSn(device != null ? device.getDeviceSn() : null)
                .orderAmount(record.getOrderAmount())
                .platformFee(record.getPlatformFee())
                .netIncome(record.getNetIncome())
                .settleStatus(record.getSettleStatus())
                .createTime(record.getCreateTime())
                .settleTime(record.getSettleTime())
                .build();
    }
}
