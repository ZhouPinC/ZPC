package com.wash.iot.service;

import com.wash.iot.dto.response.AdminOverviewResponse;
import com.wash.iot.entity.AdminDeviceBinding;
import com.wash.iot.entity.Device;
import com.wash.iot.repository.AdminDeviceBindingRepository;
import com.wash.iot.repository.DeviceRepository;
import com.wash.iot.repository.IncomeRecordRepository;
import com.wash.iot.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员统计服务
 */
@Slf4j
@Service
public class AdminStatisticsService {

    @Autowired
    private AdminDeviceBindingRepository adminDeviceBindingRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private IncomeRecordRepository incomeRecordRepository;

    @Autowired
    private OrderRepository orderRepository;

    /**
     * 获取管理员概览数据
     */
    public AdminOverviewResponse getOverview(Long adminUserId) {
        // 获取管理员绑定的设备
        List<AdminDeviceBinding> bindings = adminDeviceBindingRepository.findByAdminUserId(adminUserId);
        List<Long> deviceIds = bindings.stream()
                .map(AdminDeviceBinding::getDeviceId)
                .collect(Collectors.toList());

        List<Device> devices;
        
        // 如果管理员没有绑定任何设备，显示所有设备（初始化场景）
        if (deviceIds.isEmpty()) {
            devices = deviceRepository.findAll();
            deviceIds = devices.stream()
                    .map(Device::getId)
                    .collect(Collectors.toList());
        } else {
            devices = deviceRepository.findAllById(deviceIds);
        }

        // 设备统计
        int total = devices.size();
        int online = 0, running = 0, fault = 0, offline = 0;
        for (Device device : devices) {
            String status = device.getStatus();
            if ("OFFLINE".equals(status)) {
                offline++;
            } else if ("RUNNING".equals(status) || "PAUSED".equals(status) || "FINISHED".equals(status)) {
                if ("RUNNING".equals(status) || "PAUSED".equals(status)) {
                    running++;
                }
                online++;
            } else if ("FAULT".equals(status)) {
                fault++;
                online++;
            } else {
                // IDLE 状态也算在线
                online++;
            }
        }

        // 今日统计
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);
        
        // 今日收入
        BigDecimal todayIncome = incomeRecordRepository.sumNetIncomeByAdminUserIdAndPeriod(
                adminUserId, todayStart, todayEnd);
        if (todayIncome == null) todayIncome = BigDecimal.ZERO;
        
        // 今日订单数 - 使用JPA查询
        int todayOrders = 0;
        if (!deviceIds.isEmpty()) {
            try {
                todayOrders = orderRepository.countByDeviceIdsAndPeriod(deviceIds, todayStart, todayEnd);
            } catch (Exception e) {
                log.error("统计今日订单数失败", e);
            }
        }

        // 本月统计
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        BigDecimal monthIncome = incomeRecordRepository.sumNetIncomeByAdminUserIdAndPeriod(
                adminUserId, monthStart, todayEnd);
        if (monthIncome == null) monthIncome = BigDecimal.ZERO;
        
        // 本月订单数 - 使用JPA查询
        int monthOrders = 0;
        if (!deviceIds.isEmpty()) {
            try {
                monthOrders = orderRepository.countByDeviceIdsAndPeriod(deviceIds, monthStart, todayEnd);
            } catch (Exception e) {
                log.error("统计本月订单数失败", e);
            }
        }

        // 使用率计算（简化：运行中设备/总设备）
        int usageRate = total > 0 ? (running * 100 / total) : 0;

        return AdminOverviewResponse.builder()
                .devices(AdminOverviewResponse.DeviceStats.builder()
                        .total(total)
                        .online(online)
                        .running(running)
                        .fault(fault)
                        .offline(offline)
                        .build())
                .today(AdminOverviewResponse.TodayStats.builder()
                        .orders(todayOrders)
                        .income(todayIncome)
                        .usageRate(usageRate)
                        .build())
                .month(AdminOverviewResponse.MonthStats.builder()
                        .orders(monthOrders)
                        .income(monthIncome)
                        .compareLastMonth(0) // TODO: 实现环比计算
                        .build())
                .build();
    }
}
