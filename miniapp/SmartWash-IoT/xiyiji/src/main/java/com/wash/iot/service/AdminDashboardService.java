package com.wash.iot.service;

import com.wash.iot.dto.response.*;
import com.wash.iot.entity.*;
import com.wash.iot.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 管理后台仪表板服务
 * 提供丰富的数据可视化和交互功能
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IncomeRecordRepository incomeRecordRepository;

    @Autowired
    private QueueRepository queueRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private AdminDeviceBindingRepository adminDeviceBindingRepository;

    /**
     * 获取仪表板概览数据
     */
    public DashboardOverview getDashboardOverview(Long adminId) {
        try {
            DashboardOverview overview = new DashboardOverview();

            // 1. 实时数据
            overview.setRealtimeData(getRealtimeData(adminId));

            // 2. 今日统计
            overview.setTodayStats(getTodayStats(adminId));

            // 3. 本周统计
            overview.setWeeklyStats(getWeeklyStats(adminId));

            // 4. 设备状态
            overview.setDeviceStatus(getDeviceStatus(adminId));

            // 5. 待处理事项
            overview.setPendingItems(getPendingItems(adminId));

            // 6. 收益趋势
            overview.setRevenueTrend(getRevenueTrend(adminId, 7));

            // 7. 热门设备
            overview.setPopularDevices(getPopularDevices(adminId, 5));

            // 8. 活跃用户
            overview.setActiveUsers(getActiveUsers(adminId, 10));

            log.info("获取仪表板概览成功: adminId={}", adminId);
            return overview;

        } catch (Exception e) {
            log.error("获取仪表板概览失败: adminId={}", adminId, e);
            throw new RuntimeException("获取仪表板概览失败: " + e.getMessage());
        }
    }

    /**
     * 获取实时数据
     */
    private RealtimeData getRealtimeData(Long adminId) {
        RealtimeData realtimeData = new RealtimeData();

        List<Long> deviceIds = getAdminDeviceIds(adminId);

        // 在线设备数
        LocalDateTime onlineThreshold = LocalDateTime.now().minusMinutes(5);
        long onlineDevices = deviceRepository.countByDeviceIdsAndLastHeartbeatAfter(deviceIds, onlineThreshold);
        realtimeData.setOnlineDevices(onlineDevices);

        // 运行中设备数
        long runningDevices = deviceRepository.countByDeviceIdsAndStatus(deviceIds, "RUNNING");
        realtimeData.setRunningDevices(runningDevices);

        // 当前排队人数
        long queueCount = queueRepository.countByDeviceIdsAndStatus(deviceIds, "WAITING");
        realtimeData.setQueueCount(queueCount);

        // 今日预约数
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime todayEnd = todayStart.plusDays(1);
        long todayReservations = reservationRepository.countByDeviceIdsAndReservationTimeBetween(
            deviceIds, todayStart, todayEnd);
        realtimeData.setTodayReservations(todayReservations);

        // 当前收益（今日）
        BigDecimal todayRevenue = orderRepository.findByDeviceIdsAndCreateTimeBetween(deviceIds, todayStart, todayEnd)
            .stream()
            .filter(o -> "FINISHED".equals(o.getStatus()))
            .map(Order::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        realtimeData.setTodayRevenue(todayRevenue);

        realtimeData.setUpdateTime(LocalDateTime.now());

        return realtimeData;
    }

    /**
     * 获取今日统计
     */
    private TodayStats getTodayStats(Long adminId) {
        TodayStats todayStats = new TodayStats();

        List<Long> deviceIds = getAdminDeviceIds(adminId);
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime todayEnd = todayStart.plusDays(1);

        List<Order> todayOrders = orderRepository.findByDeviceIdsAndCreateTimeBetween(deviceIds, todayStart, todayEnd);

        // 订单统计
        todayStats.setTotalOrders(todayOrders.size());
        todayStats.setCompletedOrders((int) todayOrders.stream().filter(o -> "FINISHED".equals(o.getStatus())).count());
        todayStats.setCancelledOrders((int) todayOrders.stream().filter(o -> "CANCELLED".equals(o.getStatus())).count());

        // 收益统计
        BigDecimal totalRevenue = todayOrders.stream()
            .filter(o -> "FINISHED".equals(o.getStatus()))
            .map(Order::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        todayStats.setTotalRevenue(totalRevenue);

        BigDecimal totalIncome = todayOrders.stream()
            .filter(o -> "FINISHED".equals(o.getStatus()) && o.getOwnerIncome() != null)
            .map(Order::getOwnerIncome)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        todayStats.setTotalIncome(totalIncome);

        // 平均订单金额
        if (todayStats.getCompletedOrders() > 0) {
            todayStats.setAverageOrderAmount(totalRevenue.divide(
                new BigDecimal(todayStats.getCompletedOrders()), 2, RoundingMode.HALF_UP));
        }

        // 完成率
        if (todayStats.getTotalOrders() > 0) {
            todayStats.setCompletionRate((double) todayStats.getCompletedOrders() / todayStats.getTotalOrders() * 100);
        }

        return todayStats;
    }

    /**
     * 获取本周统计
     */
    private WeeklyStats getWeeklyStats(Long adminId) {
        WeeklyStats weeklyStats = new WeeklyStats();

        List<Long> deviceIds = getAdminDeviceIds(adminId);
        LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
        LocalDateTime weekEnd = LocalDateTime.now();

        List<Order> weekOrders = orderRepository.findByDeviceIdsAndCreateTimeBetween(deviceIds, weekStart, weekEnd);

        // 周统计
        weeklyStats.setWeeklyOrders(weekOrders.size());
        weeklyStats.setWeeklyCompletedOrders((int) weekOrders.stream().filter(o -> "FINISHED".equals(o.getStatus())).count());

        BigDecimal weeklyRevenue = weekOrders.stream()
            .filter(o -> "FINISHED".equals(o.getStatus()))
            .map(Order::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        weeklyStats.setWeeklyRevenue(weeklyRevenue);

        // 与上周对比
        LocalDateTime lastWeekStart = weekStart.minusDays(7);
        LocalDateTime lastWeekEnd = weekEnd.minusDays(7);
        List<Order> lastWeekOrders = orderRepository.findByDeviceIdsAndCreateTimeBetween(deviceIds, lastWeekStart, lastWeekEnd);

        BigDecimal lastWeekRevenue = lastWeekOrders.stream()
            .filter(o -> "FINISHED".equals(o.getStatus()))
            .map(Order::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (lastWeekRevenue.compareTo(BigDecimal.ZERO) > 0) {
            double growthRate = weeklyRevenue.subtract(lastWeekRevenue)
                .divide(lastWeekRevenue, 4, RoundingMode.HALF_UP)
                .doubleValue() * 100;
            weeklyStats.setRevenueGrowthRate(growthRate);
        }

        return weeklyStats;
    }

    /**
     * 获取设备状态
     */
    private DeviceStatusSummary getDeviceStatus(Long adminId) {
        DeviceStatusSummary deviceStatus = new DeviceStatusSummary();

        List<Long> deviceIds = getAdminDeviceIds(adminId);
        List<Device> devices = deviceRepository.findByDeviceIds(deviceIds);

        deviceStatus.setTotalDevices(devices.size());

        // 按状态分组
        Map<String, Long> statusCount = devices.stream()
            .collect(Collectors.groupingBy(Device::getStatus, Collectors.counting()));

        deviceStatus.setIdleDevices(statusCount.getOrDefault("IDLE", 0L).intValue());
        deviceStatus.setRunningDevices(statusCount.getOrDefault("RUNNING", 0L).intValue());
        deviceStatus.setFaultDevices(statusCount.getOrDefault("FAULT", 0L).intValue());
        deviceStatus.setOfflineDevices(statusCount.getOrDefault("OFFLINE", 0L).intValue());

        // 在线率
        LocalDateTime onlineThreshold = LocalDateTime.now().minusMinutes(5);
        long onlineCount = devices.stream()
            .filter(d -> d.getLastHeartbeat() != null && d.getLastHeartbeat().isAfter(onlineThreshold))
            .count();

        if (devices.size() > 0) {
            deviceStatus.setOnlineRate((double) onlineCount / devices.size() * 100);
        }

        return deviceStatus;
    }

    /**
     * 获取待处理事项
     */
    private List<PendingItem> getPendingItems(Long adminId) {
        List<PendingItem> pendingItems = new ArrayList<>();

        List<Long> deviceIds = getAdminDeviceIds(adminId);

        // 1. 故障设备
        List<Device> faultDevices = deviceRepository.findByDeviceIdsAndStatus(deviceIds, "FAULT");
        for (Device device : faultDevices) {
            PendingItem item = new PendingItem();
            item.setType("DEVICE_FAULT");
            item.setTitle("设备故障");
            item.setContent(device.getLocation() + " - " + device.getDeviceSn() + " 出现故障");
            item.setPriority("HIGH");
            item.setCreateTime(device.getUpdateTime());
            item.setActionUrl("/admin/devices/" + device.getId());
            pendingItems.add(item);
        }

        // 2. 异常订单
        LocalDateTime recentTime = LocalDateTime.now().minusHours(2);
        List<Order> abnormalOrders = orderRepository.findByDeviceIdsAndStatusAndUpdateTimeAfter(deviceIds, "ABNORMAL", recentTime);
        for (Order order : abnormalOrders) {
            PendingItem item = new PendingItem();
            item.setType("ORDER_ABNORMAL");
            item.setTitle("订单异常");
            item.setContent("订单 " + order.getOrderNo() + " 状态异常");
            item.setPriority("MEDIUM");
            item.setCreateTime(order.getUpdateTime());
            item.setActionUrl("/admin/orders/detail/" + order.getOrderNo());
            pendingItems.add(item);
        }

        // 3. 长时间排队
        LocalDateTime queueThreshold = LocalDateTime.now().minusMinutes(30);
        List<Queue> longQueues = queueRepository.findByDeviceIdsAndStatusAndCreateTimeBefore(deviceIds, "WAITING", queueThreshold);
        for (Queue queue : longQueues) {
            PendingItem item = new PendingItem();
            item.setType("LONG_QUEUE");
            item.setTitle("长时间排队");
            item.setContent("用户已排队超过30分钟");
            item.setPriority("LOW");
            item.setCreateTime(queue.getCreateTime());
            item.setActionUrl("/admin/queue/" + queue.getId());
            pendingItems.add(item);
        }

        // 按优先级和时间排序
        pendingItems.sort((a, b) -> {
            int priorityCompare = getPriorityLevel(a.getPriority()) - getPriorityLevel(b.getPriority());
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            return b.getCreateTime().compareTo(a.getCreateTime());
        });

        return pendingItems.subList(0, Math.min(pendingItems.size(), 10));
    }

    /**
     * 获取收益趋势
     */
    private List<RevenueTrendItem> getRevenueTrend(Long adminId, int days) {
        List<RevenueTrendItem> trend = new ArrayList<>();

        List<Long> deviceIds = getAdminDeviceIds(adminId);

        for (int i = days - 1; i >= 0; i--) {
            LocalDateTime dayStart = LocalDateTime.now().minusDays(i).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime dayEnd = dayStart.plusDays(1);

            List<Order> dayOrders = orderRepository.findByDeviceIdsAndCreateTimeBetween(deviceIds, dayStart, dayEnd);

            BigDecimal revenue = dayOrders.stream()
                .filter(o -> "FINISHED".equals(o.getStatus()))
                .map(Order::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            RevenueTrendItem item = new RevenueTrendItem();
            item.setDate(dayStart.toLocalDate().toString());
            item.setRevenue(revenue);
            item.setOrderCount((int) dayOrders.stream().filter(o -> "FINISHED".equals(o.getStatus())).count());

            trend.add(item);
        }

        return trend;
    }

    /**
     * 获取热门设备
     */
    private List<PopularDevice> getPopularDevices(Long adminId, int limit) {
        List<Long> deviceIds = getAdminDeviceIds(adminId);
        LocalDateTime weekStart = LocalDateTime.now().minusDays(7);

        List<Device> devices = deviceRepository.findByDeviceIds(deviceIds);
        List<PopularDevice> popularDevices = new ArrayList<>();

        for (Device device : devices) {
            List<Order> deviceOrders = orderRepository.findByDeviceIdAndCreateTimeAfter(device.getId(), weekStart);

            long orderCount = deviceOrders.stream().filter(o -> "FINISHED".equals(o.getStatus())).count();
            BigDecimal revenue = deviceOrders.stream()
                .filter(o -> "FINISHED".equals(o.getStatus()))
                .map(Order::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            PopularDevice popularDevice = new PopularDevice();
            popularDevice.setDeviceId(device.getId());
            popularDevice.setDeviceName(getDeviceDisplayName(device));
            popularDevice.setLocation(device.getLocation());
            popularDevice.setOrderCount((int) orderCount);
            popularDevice.setRevenue(revenue);
            popularDevice.setStatus(device.getStatus());

            popularDevices.add(popularDevice);
        }

        // 按订单数量排序
        popularDevices.sort((a, b) -> Long.compare(b.getOrderCount(), a.getOrderCount()));

        return popularDevices.subList(0, Math.min(popularDevices.size(), limit));
    }

    /**
     * 获取活跃用户
     */
    private List<ActiveUser> getActiveUsers(Long adminId, int limit) {
        List<Long> deviceIds = getAdminDeviceIds(adminId);
        LocalDateTime monthStart = LocalDateTime.now().minusDays(30);

        List<Order> monthOrders = orderRepository.findByDeviceIdsAndCreateTimeAfter(deviceIds, monthStart);

        Map<Long, List<Order>> userOrders = monthOrders.stream()
            .filter(o -> o.getUserId() != null)
            .collect(Collectors.groupingBy(Order::getUserId));

        List<ActiveUser> activeUsers = new ArrayList<>();

        for (Map.Entry<Long, List<Order>> entry : userOrders.entrySet()) {
            Long userId = entry.getKey();
            List<Order> orders = entry.getValue();

            userRepository.findById(userId).ifPresent(user -> {
                ActiveUser activeUser = new ActiveUser();
                activeUser.setUserId(user.getId());
                activeUser.setUserName(user.getNickName() != null ? user.getNickName() : "未设置昵称");
                activeUser.setPhone(user.getPhone() != null ? maskPhone(user.getPhone()) : "未绑定");
                activeUser.setOrderCount(orders.size());
                activeUser.setTotalConsumption(orders.stream()
                    .filter(o -> "FINISHED".equals(o.getStatus()))
                    .map(Order::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

                // 最近订单时间
                LocalDateTime lastOrderTime = orders.stream()
                    .map(Order::getCreateTime)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
                activeUser.setLastOrderTime(lastOrderTime != null ?
                    lastOrderTime.format(DateTimeFormatter.ofPattern("MM-dd HH:mm")) : "");

                activeUsers.add(activeUser);
            });
        }

        // 按订单数量排序
        activeUsers.sort((a, b) -> Integer.compare(b.getOrderCount(), a.getOrderCount()));

        return activeUsers.subList(0, Math.min(activeUsers.size(), limit));
    }

    // 辅助方法
    private List<Long> getAdminDeviceIds(Long adminId) {
        return adminDeviceBindingRepository.findByAdminUserId(adminId)
            .stream()
            .map(AdminDeviceBinding::getDeviceId)
            .collect(Collectors.toList());
    }

    private String getDeviceDisplayName(Device device) {
        if (device.getLocation() != null) {
            return device.getLocation() + " - " + device.getDeviceSn();
        }
        return device.getDeviceSn();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private int getPriorityLevel(String priority) {
        switch (priority) {
            case "HIGH": return 3;
            case "MEDIUM": return 2;
            case "LOW": return 1;
            default: return 0;
        }
    }

    // 数据传输对象
    public static class DashboardOverview {
        private RealtimeData realtimeData;
        private TodayStats todayStats;
        private WeeklyStats weeklyStats;
        private DeviceStatusSummary deviceStatus;
        private List<PendingItem> pendingItems;
        private List<RevenueTrendItem> revenueTrend;
        private List<PopularDevice> popularDevices;
        private List<ActiveUser> activeUsers;

        // getters and setters
        public RealtimeData getRealtimeData() { return realtimeData; }
        public void setRealtimeData(RealtimeData realtimeData) { this.realtimeData = realtimeData; }
        public TodayStats getTodayStats() { return todayStats; }
        public void setTodayStats(TodayStats todayStats) { this.todayStats = todayStats; }
        public WeeklyStats getWeeklyStats() { return weeklyStats; }
        public void setWeeklyStats(WeeklyStats weeklyStats) { this.weeklyStats = weeklyStats; }
        public DeviceStatusSummary getDeviceStatus() { return deviceStatus; }
        public void setDeviceStatus(DeviceStatusSummary deviceStatus) { this.deviceStatus = deviceStatus; }
        public List<PendingItem> getPendingItems() { return pendingItems; }
        public void setPendingItems(List<PendingItem> pendingItems) { this.pendingItems = pendingItems; }
        public List<RevenueTrendItem> getRevenueTrend() { return revenueTrend; }
        public void setRevenueTrend(List<RevenueTrendItem> revenueTrend) { this.revenueTrend = revenueTrend; }
        public List<PopularDevice> getPopularDevices() { return popularDevices; }
        public void setPopularDevices(List<PopularDevice> popularDevices) { this.popularDevices = popularDevices; }
        public List<ActiveUser> getActiveUsers() { return activeUsers; }
        public void setActiveUsers(List<ActiveUser> activeUsers) { this.activeUsers = activeUsers; }
    }

    public static class RealtimeData {
        private long onlineDevices;
        private long runningDevices;
        private long queueCount;
        private long todayReservations;
        private BigDecimal todayRevenue;
        private LocalDateTime updateTime;

        // getters and setters
        public long getOnlineDevices() { return onlineDevices; }
        public void setOnlineDevices(long onlineDevices) { this.onlineDevices = onlineDevices; }
        public long getRunningDevices() { return runningDevices; }
        public void setRunningDevices(long runningDevices) { this.runningDevices = runningDevices; }
        public long getQueueCount() { return queueCount; }
        public void setQueueCount(long queueCount) { this.queueCount = queueCount; }
        public long getTodayReservations() { return todayReservations; }
        public void setTodayReservations(long todayReservations) { this.todayReservations = todayReservations; }
        public BigDecimal getTodayRevenue() { return todayRevenue; }
        public void setTodayRevenue(BigDecimal todayRevenue) { this.todayRevenue = todayRevenue; }
        public LocalDateTime getUpdateTime() { return updateTime; }
        public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    }

    public static class TodayStats {
        private int totalOrders;
        private int completedOrders;
        private int cancelledOrders;
        private BigDecimal totalRevenue;
        private BigDecimal totalIncome;
        private BigDecimal averageOrderAmount;
        private double completionRate;

        // getters and setters
        public int getTotalOrders() { return totalOrders; }
        public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }
        public int getCompletedOrders() { return completedOrders; }
        public void setCompletedOrders(int completedOrders) { this.completedOrders = completedOrders; }
        public int getCancelledOrders() { return cancelledOrders; }
        public void setCancelledOrders(int cancelledOrders) { this.cancelledOrders = cancelledOrders; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
        public BigDecimal getTotalIncome() { return totalIncome; }
        public void setTotalIncome(BigDecimal totalIncome) { this.totalIncome = totalIncome; }
        public BigDecimal getAverageOrderAmount() { return averageOrderAmount; }
        public void setAverageOrderAmount(BigDecimal averageOrderAmount) { this.averageOrderAmount = averageOrderAmount; }
        public double getCompletionRate() { return completionRate; }
        public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }
    }

    public static class WeeklyStats {
        private int weeklyOrders;
        private int weeklyCompletedOrders;
        private BigDecimal weeklyRevenue;
        private double revenueGrowthRate;

        // getters and setters
        public int getWeeklyOrders() { return weeklyOrders; }
        public void setWeeklyOrders(int weeklyOrders) { this.weeklyOrders = weeklyOrders; }
        public int getWeeklyCompletedOrders() { return weeklyCompletedOrders; }
        public void setWeeklyCompletedOrders(int weeklyCompletedOrders) { this.weeklyCompletedOrders = weeklyCompletedOrders; }
        public BigDecimal getWeeklyRevenue() { return weeklyRevenue; }
        public void setWeeklyRevenue(BigDecimal weeklyRevenue) { this.weeklyRevenue = weeklyRevenue; }
        public double getRevenueGrowthRate() { return revenueGrowthRate; }
        public void setRevenueGrowthRate(double revenueGrowthRate) { this.revenueGrowthRate = revenueGrowthRate; }
    }

    public static class DeviceStatusSummary {
        private int totalDevices;
        private int idleDevices;
        private int runningDevices;
        private int faultDevices;
        private int offlineDevices;
        private double onlineRate;

        // getters and setters
        public int getTotalDevices() { return totalDevices; }
        public void setTotalDevices(int totalDevices) { this.totalDevices = totalDevices; }
        public int getIdleDevices() { return idleDevices; }
        public void setIdleDevices(int idleDevices) { this.idleDevices = idleDevices; }
        public int getRunningDevices() { return runningDevices; }
        public void setRunningDevices(int runningDevices) { this.runningDevices = runningDevices; }
        public int getFaultDevices() { return faultDevices; }
        public void setFaultDevices(int faultDevices) { this.faultDevices = faultDevices; }
        public int getOfflineDevices() { return offlineDevices; }
        public void setOfflineDevices(int offlineDevices) { this.offlineDevices = offlineDevices; }
        public double getOnlineRate() { return onlineRate; }
        public void setOnlineRate(double onlineRate) { this.onlineRate = onlineRate; }
    }

    public static class PendingItem {
        private String type;
        private String title;
        private String content;
        private String priority;
        private LocalDateTime createTime;
        private String actionUrl;

        // getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
        public String getActionUrl() { return actionUrl; }
        public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }
    }

    public static class RevenueTrendItem {
        private String date;
        private BigDecimal revenue;
        private int orderCount;

        // getters and setters
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
        public int getOrderCount() { return orderCount; }
        public void setOrderCount(int orderCount) { this.orderCount = orderCount; }
    }

    public static class PopularDevice {
        private Long deviceId;
        private String deviceName;
        private String location;
        private int orderCount;
        private BigDecimal revenue;
        private String status;

        // getters and setters
        public Long getDeviceId() { return deviceId; }
        public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
        public String getDeviceName() { return deviceName; }
        public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public int getOrderCount() { return orderCount; }
        public void setOrderCount(int orderCount) { this.orderCount = orderCount; }
        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class ActiveUser {
        private Long userId;
        private String userName;
        private String phone;
        private int orderCount;
        private BigDecimal totalConsumption;
        private String lastOrderTime;

        // getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public int getOrderCount() { return orderCount; }
        public void setOrderCount(int orderCount) { this.orderCount = orderCount; }
        public BigDecimal getTotalConsumption() { return totalConsumption; }
        public void setTotalConsumption(BigDecimal totalConsumption) { this.totalConsumption = totalConsumption; }
        public String getLastOrderTime() { return lastOrderTime; }
        public void setLastOrderTime(String lastOrderTime) { this.lastOrderTime = lastOrderTime; }
    }
}