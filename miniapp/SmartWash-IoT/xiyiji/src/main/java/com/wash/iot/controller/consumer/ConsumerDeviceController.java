package com.wash.iot.controller.consumer;

import com.wash.iot.common.response.ApiResponse;
import com.wash.iot.dto.response.DeviceStatusResponse;
import com.wash.iot.entity.Device;
import com.wash.iot.entity.DeviceStatusHistory;
import com.wash.iot.entity.UserDeviceBinding;
import com.wash.iot.enums.UserRole;
import com.wash.iot.common.exception.BusinessException;
import com.wash.iot.repository.DeviceRepository;
import com.wash.iot.repository.DeviceStatusHistoryRepository;
import com.wash.iot.repository.UserDeviceBindingRepository;
import com.wash.iot.security.JwtAuthenticationFilter;
import com.wash.iot.security.RoleRequired;
import com.wash.iot.util.DeviceUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 终端用户设备控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/consumer/devices")
public class ConsumerDeviceController {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private UserDeviceBindingRepository userDeviceBindingRepository;

    @Autowired
    private DeviceStatusHistoryRepository deviceStatusHistoryRepository;

    /**
     * 获取已绑定设备列表
     * GET /api/v1/consumer/devices
     */
    @GetMapping
    @RoleRequired({UserRole.CONSUMER, UserRole.ADMIN, UserRole.SUPER_ADMIN})
    public ApiResponse<List<DeviceStatusResponse>> getDevices() {
        Long userId = JwtAuthenticationFilter.getCurrentUserId();
        
        // 获取用户绑定的设备
        List<UserDeviceBinding> bindings = userDeviceBindingRepository
                .findByUserIdAndStatus(userId, "ACTIVE");
        
        List<Long> deviceIds = bindings.stream()
                .map(UserDeviceBinding::getDeviceId)
                .collect(Collectors.toList());
        
        List<Device> devices = deviceRepository.findAllById(deviceIds);
        
        List<DeviceStatusResponse> result = devices.stream()
                .map(this::buildDeviceResponse)
                .collect(Collectors.toList());
        
        return ApiResponse.success(result);
    }

    /**
     * 绑定设备
     * POST /api/v1/consumer/devices/binding
     */
    @PostMapping("/binding")
    @RoleRequired({UserRole.CONSUMER, UserRole.ADMIN, UserRole.SUPER_ADMIN})
    public ApiResponse<DeviceStatusResponse> bindDevice(@RequestBody Map<String, String> request) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId();

        String deviceSn = request.get("deviceSn");
        if (deviceSn == null || deviceSn.isBlank()) {
            String qrCodeContent = request.get("qrCodeContent");
            deviceSn = DeviceUtils.parseDeviceSnFromQr(qrCodeContent);
        }

        if (deviceSn == null || deviceSn.isBlank()) {
            throw new BusinessException("设备码无效");
        }
        
        // 查找设备
        Device device = deviceRepository.findByDeviceSn(deviceSn)
                .orElseThrow(() -> new BusinessException("设备不存在"));
        
        // 检查是否已绑定
        if (userDeviceBindingRepository.existsByUserIdAndDeviceIdAndStatus(userId, device.getId(), "ACTIVE")) {
            return ApiResponse.success("设备已绑定", buildDeviceResponse(device));
        }
        
        // 创建绑定关系
        UserDeviceBinding binding = new UserDeviceBinding();
        binding.setUserId(userId);
        binding.setDeviceId(device.getId());
        userDeviceBindingRepository.save(binding);

        recordBindingLog(device.getId(), null, "BIND", "USER_BIND userId=" + userId + " deviceSn=" + device.getDeviceSn());
        
        return ApiResponse.success(buildDeviceResponse(device));
    }

    /**
     * 解绑设备
     * DELETE /api/v1/consumer/devices/{deviceId}/binding
     */
    @DeleteMapping("/{deviceId}/binding")
    @RoleRequired({UserRole.CONSUMER, UserRole.ADMIN, UserRole.SUPER_ADMIN})
    public ApiResponse<Void> unbindDevice(@PathVariable Long deviceId) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId();
        
        UserDeviceBinding binding = userDeviceBindingRepository
                .findByUserIdAndDeviceIdAndStatus(userId, deviceId, "ACTIVE")
                .orElseThrow(() -> new BusinessException("未找到绑定关系"));
        
        binding.setStatus("REMOVED");
        userDeviceBindingRepository.save(binding);

        recordBindingLog(deviceId, null, "UNBIND", "USER_UNBIND userId=" + userId + " deviceId=" + deviceId);
        
        return ApiResponse.success();
    }

    /**
     * 获取设备实时状态
     * GET /api/v1/consumer/devices/{deviceSn}/status
     */
    @GetMapping("/{deviceSn}/status")
    @RoleRequired({UserRole.CONSUMER, UserRole.ADMIN, UserRole.SUPER_ADMIN})
    public ApiResponse<DeviceStatusResponse> getDeviceStatus(@PathVariable String deviceSn) {
        Device device = deviceRepository.findByDeviceSn(deviceSn)
                .orElseThrow(() -> new BusinessException("设备不存在"));

        Long userId = JwtAuthenticationFilter.getCurrentUserId();
        if (!userDeviceBindingRepository.existsByUserIdAndDeviceIdAndStatus(userId, device.getId(), "ACTIVE")) {
            throw new BusinessException("设备未绑定");
        }
        
        return ApiResponse.success(buildDeviceResponse(device));
    }

    private void recordBindingLog(Long deviceId, String oldStatus, String newStatus, String message) {
        try {
            DeviceStatusHistory history = new DeviceStatusHistory();
            history.setDeviceId(deviceId);
            history.setOldStatus(oldStatus);
            history.setNewStatus(newStatus);
            history.setStatusType("BINDING");
            history.setMessage(message);
            deviceStatusHistoryRepository.save(history);
        } catch (Exception e) {
            log.warn("记录绑定日志失败: deviceId={}, err={}", deviceId, e.getMessage());
        }
    }

    /**
     * 构建设备响应
     */
    private DeviceStatusResponse buildDeviceResponse(Device device) {
        return DeviceStatusResponse.builder()
                .id(device.getId())
                .deviceSn(device.getDeviceSn())
                .name(device.getName())
                .location(device.getLocation())
                .status(device.getStatus())
                .statusText(getStatusText(device.getStatus()))
                .qrCodeContent(device.getQrCodeContent())
                .qrCodeUrl(device.getQrCodeUrl())
                .washMode(device.getWashMode())
                .washModeName(device.getWashModeName())
                .remainSeconds(device.getRemainSeconds())
                .totalDuration(device.getTotalDuration())
                .estimatedEndTime(device.getEstimatedEndTime() != null ? 
                        device.getEstimatedEndTime().toString() : null)
                .build();
    }

    private String getStatusText(String status) {
        if (status == null) return "未知";
        switch (status) {
            case "OFFLINE": return "离线";
            case "IDLE": return "空闲";
            case "RUNNING": return "运行中";
            case "PAUSED": return "已暂停";
            case "FAULT": return "故障";
            default: return "未知";
        }
    }
}
