package com.wash.iot.service;

import com.wash.iot.entity.Device;
import com.wash.iot.entity.DeviceStatusHistory;
import com.wash.iot.entity.Order;
import com.wash.iot.enums.DeviceStatus;
import com.wash.iot.interfaces.mqtt.dto.DeviceReportEvent;
import com.wash.iot.repository.DeviceRepository;
import com.wash.iot.repository.DeviceStatusHistoryRepository;
import com.wash.iot.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 设备状态管理器
 * 统一管理设备状态变更，确保状态转换的合理性和一致性
 */
@Slf4j
@Service
@Transactional
public class DeviceStateManager {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private DeviceStatusHistoryRepository statusHistoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderLifecycleService orderLifecycleService;

    @Autowired
    private NotificationService notificationService;

    /**
     * 处理设备上报事件
     */
    public void handleDeviceReport(DeviceReportEvent event) {
        try {
            Device device = deviceRepository.findByDeviceSn(event.getDeviceSn())
                    .orElseThrow(() -> new RuntimeException("设备不存在: " + event.getDeviceSn()));

            String oldStatus = device.getStatus();
            String newStatus = determineDeviceStatus(device, event);

            // 验证状态转换的合法性
            if (!isValidStatusTransition(oldStatus, newStatus, event)) {
                log.warn("非法的状态转换: deviceSn={}, {} -> {}", event.getDeviceSn(), oldStatus, newStatus);
                return;
            }

            // 更新设备状态
            updateDeviceStatus(device, event, newStatus);

            // 记录状态历史
            recordStatusHistory(device, oldStatus, newStatus, event);

            // 处理状态变更相关的业务逻辑
            handleStatusChangeBusinessLogic(device, oldStatus, newStatus, event);

            log.info("设备状态更新成功: deviceSn={}, {} -> {}", event.getDeviceSn(), oldStatus, newStatus);

        } catch (Exception e) {
            log.error("处理设备上报事件失败: deviceSn={}", event.getDeviceSn(), e);
            throw new RuntimeException("设备状态处理失败: " + e.getMessage());
        }
    }

    /**
     * 确定设备当前状态
     */
    private String determineDeviceStatus(Device device, DeviceReportEvent event) {
        String reportedStatus = event.getStatus();

        // 优先根据设备上报的状态确定
        if (reportedStatus != null && !reportedStatus.isEmpty()) {
            // 标准化状态名称
            switch (reportedStatus.toUpperCase()) {
                case "NORMAL":
                case "IDLE":
                    if (device.getCurrentOrderId() != null) {
                        Order currentOrder = orderRepository.findById(device.getCurrentOrderId()).orElse(null);
                        if (currentOrder != null && ("PAID".equals(currentOrder.getStatus()) || "RUNNING".equals(currentOrder.getStatus()))) {
                            return DeviceStatus.FINISHED.name();
                        }
                    }
                    return DeviceStatus.IDLE.name();
                case "RUNNING":
                case "WORKING":
                    return DeviceStatus.RUNNING.name();
                case "FINISHED":
                case "COMPLETE":
                    return DeviceStatus.FINISHED.name();
                case "FAULT":
                case "ERROR":
                    return DeviceStatus.FAULT.name();
                case "OFFLINE":
                    return DeviceStatus.OFFLINE.name();
                default:
                    return reportedStatus.toUpperCase();
            }
        }

        // 根据设备当前订单状态判断
        if (device.getCurrentOrderId() != null) {
            Order currentOrder = orderRepository.findById(device.getCurrentOrderId()).orElse(null);
            if (currentOrder != null) {
                switch (currentOrder.getStatus()) {
                    case "PAID":
                    case "RUNNING":
                        return DeviceStatus.RUNNING.name();
                    case "FINISHED":
                        return DeviceStatus.FINISHED.name();
                    default:
                        return DeviceStatus.IDLE.name();
                }
            }
        }

        // 默认状态
        return DeviceStatus.IDLE.name();
    }

    /**
     * 验证状态转换的合法性
     */
    private boolean isValidStatusTransition(String oldStatus, String newStatus, DeviceReportEvent event) {
        // 如果状态没有变化，允许
        if (oldStatus.equals(newStatus)) {
            return true;
        }

        // 定义允许的状态转换
        List<String> allowedFromAny = Arrays.asList("OFFLINE", "FAULT");
        List<String> allowedFromIdle = Arrays.asList("STARTING", "RUNNING", "OFFLINE", "FAULT");
        List<String> allowedFromStarting = Arrays.asList("RUNNING", "FINISHED", "FAULT", "OFFLINE", "IDLE");
        List<String> allowedFromRunning = Arrays.asList("FINISHED", "FAULT", "OFFLINE");
        List<String> allowedFromFinished = Arrays.asList("IDLE", "RUNNING", "OFFLINE", "FAULT");

        switch (oldStatus) {
            case "IDLE":
                return allowedFromIdle.contains(newStatus);
            case "STARTING":
                return allowedFromStarting.contains(newStatus);
            case "RUNNING":
                return allowedFromRunning.contains(newStatus);
            case "FINISHED":
                return allowedFromFinished.contains(newStatus);
            case "OFFLINE":
            case "FAULT":
                return allowedFromAny.contains(newStatus) || "IDLE".equals(newStatus);
            default:
                log.warn("未知的状态: {}", oldStatus);
                return true; // 未知状态允许转换
        }
    }

    /**
     * 更新设备状态
     */
    private void updateDeviceStatus(Device device, DeviceReportEvent event, String newStatus) {
        device.setStatus(newStatus);
        device.setLastHeartbeat(LocalDateTime.now());

        // 更新设备运行相关字段
        if (event.getRemainSeconds() != null) {
            device.setRemainSeconds(event.getRemainSeconds());
            device.setEstimatedEndTime(LocalDateTime.now().plusSeconds(event.getRemainSeconds()));
        }

        // 更新温度信息
        if (event.getTemperature() != null) {
            // 可以将温度信息存储到设备的扩展字段中
            log.debug("设备温度更新: deviceSn={}, temperature={}", device.getDeviceSn(), event.getTemperature());
        }

        deviceRepository.save(device);
    }

    /**
     * 记录状态历史
     */
    private void recordStatusHistory(Device device, String oldStatus, String newStatus, DeviceReportEvent event) {
        DeviceStatusHistory history = new DeviceStatusHistory();
        history.setDeviceId(device.getId());
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setStatusType(event.getType());
        history.setOrderNo(event.getOrderNo());
        history.setTemperature(event.getTemperature());
        history.setRemainSeconds(event.getRemainSeconds());
        history.setErrorCode(event.getErrorCode() != null ? String.valueOf(event.getErrorCode()) : null);
        history.setMessage(event.getMessage());
        history.setChangeTime(LocalDateTime.now());

        statusHistoryRepository.save(history);
    }

    /**
     * 处理状态变更相关的业务逻辑
     */
    private void handleStatusChangeBusinessLogic(Device device, String oldStatus, String newStatus, DeviceReportEvent event) {
        switch (newStatus) {
            case "RUNNING":
                handleDeviceStarted(device, oldStatus, event);
                break;
            case "FINISHED":
                handleDeviceFinished(device, oldStatus, event);
                break;
            case "FAULT":
                handleDeviceFault(device, oldStatus, event);
                break;
            case "OFFLINE":
                handleDeviceOffline(device, oldStatus, event);
                break;
            case "IDLE":
                handleDeviceIdle(device, oldStatus, event);
                break;
        }
    }

    /**
     * 处理设备开始工作
     */
    private void handleDeviceStarted(Device device, String oldStatus, DeviceReportEvent event) {
        if (device.getCurrentOrderId() != null) {
            Order order = orderRepository.findById(device.getCurrentOrderId()).orElse(null);
            if (order != null && "PAID".equals(order.getStatus())) {
                orderLifecycleService.startOrder(order, event);
                device.setWorkStartTime(LocalDateTime.now());
                deviceRepository.save(device);

                log.info("设备开始工作: deviceSn={}, orderNo={}", device.getDeviceSn(), order.getOrderNo());
            }
        }
    }

    /**
     * 处理设备工作完成
     */
    private void handleDeviceFinished(Device device, String oldStatus, DeviceReportEvent event) {
        if (device.getCurrentOrderId() != null) {
            Order order = orderRepository.findById(device.getCurrentOrderId()).orElse(null);
            if (order != null && "RUNNING".equals(order.getStatus())) {
                orderLifecycleService.completeOrder(order, event);
                device.setWorkStartTime(null);
                device.setCurrentOrderId(null);
                device.setCurrentOrderNo(null);
                device.setCurrentUserId(null);
                device.setRemainSeconds(0);
                device.setEstimatedEndTime(null);
                deviceRepository.save(device);

                // 发送完成通知
                notificationService.sendOrderCompletionNotification(order);

                log.info("设备工作完成: deviceSn={}, orderNo={}", device.getDeviceSn(), order.getOrderNo());
            }
        }
    }

    /**
     * 处理设备故障
     */
    private void handleDeviceFault(Device device, String oldStatus, DeviceReportEvent event) {
        // 如果设备正在工作中，中断当前订单
        if (device.getCurrentOrderId() != null) {
            Order order = orderRepository.findById(device.getCurrentOrderId()).orElse(null);
            if (order != null && ("RUNNING".equals(order.getStatus()) || "PAID".equals(order.getStatus()))) {
                orderLifecycleService.interruptOrder(order, "设备故障: " + event.getMessage());

                // 发送故障通知
                notificationService.sendDeviceFaultNotification(device, order);

                log.warn("设备故障中断订单: deviceSn={}, orderNo={}, error={}",
                        device.getDeviceSn(), order.getOrderNo(), event.getMessage());
            }
        }

        // 发送设备故障通知给管理员
        notificationService.sendDeviceFaultToAdmin(device, event);
    }

    /**
     * 处理设备离线
     */
    private void handleDeviceOffline(Device device, String oldStatus, DeviceReportEvent event) {
        // 如果设备正在工作中，标记为异常
        if (device.getCurrentOrderId() != null) {
            Order order = orderRepository.findById(device.getCurrentOrderId()).orElse(null);
            if (order != null && ("RUNNING".equals(order.getStatus()) || "PAID".equals(order.getStatus()))) {
                orderLifecycleService.markOrderAbnormal(order, "设备离线");

                log.warn("设备离线，订单标记异常: deviceSn={}, orderNo={}",
                        device.getDeviceSn(), order.getOrderNo());
            }
        }

        // 发送离线通知给管理员
        notificationService.sendDeviceOfflineNotification(device);
    }

    /**
     * 处理设备空闲
     */
    private void handleDeviceIdle(Device device, String oldStatus, DeviceReportEvent event) {
        // 清理设备的工作状态
        if (device.getCurrentOrderId() != null) {
            Order order = orderRepository.findById(device.getCurrentOrderId()).orElse(null);
            if (order != null && !"FINISHED".equals(order.getStatus()) && !"CANCELLED".equals(order.getStatus())) {
                // 如果有未完成的订单，需要清理
                log.warn("设备变为空闲但存在未完成订单: deviceSn={}, orderNo={}",
                        device.getDeviceSn(), order.getOrderNo());
            }
        }

        // 清理设备工作相关字段
        device.setWashMode(null);
        device.setWashModeName(null);
        device.setTotalDuration(null);
        device.setWorkStartTime(null);
        device.setCurrentOrderId(null);
        device.setCurrentOrderNo(null);
        device.setCurrentUserId(null);
        device.setRemainSeconds(0);
        device.setEstimatedEndTime(null);
        deviceRepository.save(device);
    }

    /**
     * 手动重置设备状态
     */
    public void resetDeviceStatus(String deviceSn, String reason) {
        try {
            Device device = deviceRepository.findByDeviceSn(deviceSn)
                    .orElseThrow(() -> new RuntimeException("设备不存在: " + deviceSn));

            String oldStatus = device.getStatus();

            // 重置设备状态
            device.setStatus(DeviceStatus.IDLE.name());
            device.setWashMode(null);
            device.setWashModeName(null);
            device.setTotalDuration(null);
            device.setWorkStartTime(null);
            device.setCurrentOrderId(null);
            device.setCurrentOrderNo(null);
            device.setCurrentUserId(null);
            device.setRemainSeconds(0);
            device.setEstimatedEndTime(null);
            device.setLastHeartbeat(LocalDateTime.now());
            deviceRepository.save(device);

            // 记录状态历史
            DeviceStatusHistory history = new DeviceStatusHistory();
            history.setDeviceId(device.getId());
            history.setOldStatus(oldStatus);
            history.setNewStatus(DeviceStatus.IDLE.name());
            history.setStatusType("MANUAL_RESET");
            history.setMessage("手动重置: " + reason);
            history.setChangeTime(LocalDateTime.now());
            statusHistoryRepository.save(history);

            log.info("设备状态手动重置: deviceSn={}, {} -> {}, reason={}",
                    deviceSn, oldStatus, DeviceStatus.IDLE.name(), reason);

        } catch (Exception e) {
            log.error("重置设备状态失败: deviceSn={}", deviceSn, e);
            throw new RuntimeException("重置设备状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取设备状态统计
     */
    public DeviceStatusStatistics getDeviceStatusStatistics() {
        List<Device> allDevices = deviceRepository.findAll();

        long totalDevices = allDevices.size();
        long onlineDevices = allDevices.stream()
                .filter(d -> d.getLastHeartbeat() != null &&
                           d.getLastHeartbeat().isAfter(LocalDateTime.now().minusMinutes(5)))
                .count();

        long idleDevices = allDevices.stream()
                .filter(d -> DeviceStatus.IDLE.name().equals(d.getStatus()))
                .count();

        long runningDevices = allDevices.stream()
                .filter(d -> DeviceStatus.RUNNING.name().equals(d.getStatus()))
                .count();

        long faultDevices = allDevices.stream()
                .filter(d -> DeviceStatus.FAULT.name().equals(d.getStatus()))
                .count();

        DeviceStatusStatistics statistics = new DeviceStatusStatistics();
        statistics.setTotalDevices(totalDevices);
        statistics.setOnlineDevices(onlineDevices);
        statistics.setOfflineDevices(totalDevices - onlineDevices);
        statistics.setIdleDevices(idleDevices);
        statistics.setRunningDevices(runningDevices);
        statistics.setFaultDevices(faultDevices);
        statistics.setOnlineRate(totalDevices > 0 ? (double) onlineDevices / totalDevices * 100 : 0);

        return statistics;
    }

    /**
     * 设备状态统计
     */
    public static class DeviceStatusStatistics {
        private long totalDevices;
        private long onlineDevices;
        private long offlineDevices;
        private long idleDevices;
        private long runningDevices;
        private long faultDevices;
        private double onlineRate;

        // getters and setters
        public long getTotalDevices() { return totalDevices; }
        public void setTotalDevices(long totalDevices) { this.totalDevices = totalDevices; }
        public long getOnlineDevices() { return onlineDevices; }
        public void setOnlineDevices(long onlineDevices) { this.onlineDevices = onlineDevices; }
        public long getOfflineDevices() { return offlineDevices; }
        public void setOfflineDevices(long offlineDevices) { this.offlineDevices = offlineDevices; }
        public long getIdleDevices() { return idleDevices; }
        public void setIdleDevices(long idleDevices) { this.idleDevices = idleDevices; }
        public long getRunningDevices() { return runningDevices; }
        public void setRunningDevices(long runningDevices) { this.runningDevices = runningDevices; }
        public long getFaultDevices() { return faultDevices; }
        public void setFaultDevices(long faultDevices) { this.faultDevices = faultDevices; }
        public double getOnlineRate() { return onlineRate; }
        public void setOnlineRate(double onlineRate) { this.onlineRate = onlineRate; }
    }
}
