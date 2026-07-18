package com.wash.iot.controller.admin;

import com.wash.iot.common.exception.BusinessException;
import com.wash.iot.common.response.ApiResponse;
import com.wash.iot.dto.response.DeviceStatusResponse;
import com.wash.iot.entity.AdminDeviceBinding;
import com.wash.iot.entity.Device;
import com.wash.iot.enums.UserRole;
import com.wash.iot.repository.AdminDeviceBindingRepository;
import com.wash.iot.repository.DeviceRepository;
import com.wash.iot.security.JwtAuthenticationFilter;
import com.wash.iot.util.DeviceUtils;
import com.wash.iot.security.RoleRequired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员设备控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/devices")
public class AdminDeviceController {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private AdminDeviceBindingRepository adminDeviceBindingRepository;

    /**
     * 获取我管理的设备列表
     * GET /api/v1/admin/devices
     */
    @GetMapping
    @RoleRequired(UserRole.ADMIN)
    public ApiResponse<List<DeviceStatusResponse>> getDevices() {
        Long adminUserId = JwtAuthenticationFilter.getCurrentUserId();
        boolean superAdmin = JwtAuthenticationFilter.getCurrentUser() != null && JwtAuthenticationFilter.getCurrentUser().isSuperAdmin();

        List<Device> devices;

        if (superAdmin) {
            devices = deviceRepository.findAll();
        } else {
            List<AdminDeviceBinding> bindings = adminDeviceBindingRepository.findByAdminUserId(adminUserId);
            List<Long> deviceIds = bindings.stream()
                    .map(AdminDeviceBinding::getDeviceId)
                    .collect(Collectors.toList());

            if (deviceIds.isEmpty()) {
                devices = deviceRepository.findAll();
            } else {
                devices = deviceRepository.findAllById(deviceIds);
            }
        }

        List<DeviceStatusResponse> result = devices.stream()
                .map(this::buildDeviceResponse)
                .collect(Collectors.toList());

        return ApiResponse.success(result);
    }

    /**
     * 添加设备
     * POST /api/v1/admin/devices
     */
    @PostMapping
    @RoleRequired(UserRole.ADMIN)
    public ApiResponse<DeviceStatusResponse> addDevice(@RequestBody Map<String, Object> request) {
        Long adminUserId = JwtAuthenticationFilter.getCurrentUserId();
        boolean superAdmin = JwtAuthenticationFilter.getCurrentUser() != null && JwtAuthenticationFilter.getCurrentUser().isSuperAdmin();
        String deviceSn = (String) request.get("deviceSn");
        String name = (String) request.get("name");
        String location = (String) request.get("location");

        if (deviceSn == null || deviceSn.isBlank()) {
            deviceSn = generateUniqueDeviceSnWithCheck();
        }

        // 检查设备是否已存在
        Device device = deviceRepository.findByDeviceSn(deviceSn).orElse(null);
        
        if (device == null) {
            // 创建新设备
            device = new Device();
            device.setDeviceSn(deviceSn);
            device.setName(name != null && !name.isBlank() ? name : deviceSn);
            device.setLocation(location);
            device.setStatus("OFFLINE");
            device.setOwnerId(adminUserId);
            device.setPricingMode("PER_USE");
            device.setPricePerUse(new BigDecimal("3.00"));
            device.setQrCodeContent(DeviceUtils.generateQrCodeContent(deviceSn));
            device.setQrCodeUrl(DeviceUtils.generateQrCodeUrl(deviceSn));
            device = deviceRepository.save(device);
        } else {
            // 检查是否已被其他管理员绑定
            if (!superAdmin && device.getOwnerId() != null && !device.getOwnerId().equals(adminUserId)) {
                throw new BusinessException("该设备已被其他管理员绑定");
            }
            device.setOwnerId(adminUserId);
            if (location != null) {
                device.setLocation(location);
            }
            if (name != null && !name.isBlank()) {
                device.setName(name);
            }
            if (device.getQrCodeContent() == null || device.getQrCodeContent().isBlank()) {
                device.setQrCodeContent(DeviceUtils.generateQrCodeContent(device.getDeviceSn()));
            }
            if (device.getQrCodeUrl() == null || device.getQrCodeUrl().isBlank()) {
                device.setQrCodeUrl(DeviceUtils.generateQrCodeUrl(device.getDeviceSn()));
            }
            device = deviceRepository.save(device);
        }

        // 创建绑定关系
        if (!adminDeviceBindingRepository.existsByAdminUserIdAndDeviceId(adminUserId, device.getId())) {
            AdminDeviceBinding binding = new AdminDeviceBinding();
            binding.setAdminUserId(adminUserId);
            binding.setDeviceId(device.getId());
            adminDeviceBindingRepository.save(binding);
        }

        log.info("管理员添加设备: adminUserId={}, deviceSn={}", adminUserId, deviceSn);
        return ApiResponse.success(buildDeviceResponse(device));
    }

    @PostMapping("/{deviceId}/binding-codes")
    @RoleRequired(UserRole.ADMIN)
    public ApiResponse<DeviceStatusResponse> ensureBindingCodes(
            @PathVariable Long deviceId,
            @RequestBody(required = false) Map<String, Object> request) {
        Long adminUserId = JwtAuthenticationFilter.getCurrentUserId();

        boolean superAdmin = JwtAuthenticationFilter.getCurrentUser() != null && JwtAuthenticationFilter.getCurrentUser().isSuperAdmin();

        if (!superAdmin && !adminDeviceBindingRepository.existsByAdminUserIdAndDeviceId(adminUserId, deviceId)) {
            throw new BusinessException("无权操作此设备");
        }

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException("设备不存在"));

        String desiredSn = request != null ? (String) request.get("deviceSn") : null;
        if (desiredSn != null && !desiredSn.isBlank()) {
            Device exists = deviceRepository.findByDeviceSn(desiredSn).orElse(null);
            if (exists != null && !exists.getId().equals(device.getId())) {
                throw new BusinessException("SN码已存在");
            }
            device.setDeviceSn(desiredSn);
            if (device.getName() == null || device.getName().isBlank()) {
                device.setName(desiredSn);
            }
        }

        if (device.getDeviceSn() == null || device.getDeviceSn().isBlank()) {
            device.setDeviceSn(generateUniqueDeviceSnWithCheck());
            if (device.getName() == null || device.getName().isBlank()) {
                device.setName(device.getDeviceSn());
            }
        }

        if (device.getQrCodeContent() == null || device.getQrCodeContent().isBlank()) {
            device.setQrCodeContent(DeviceUtils.generateQrCodeContent(device.getDeviceSn()));
        }
        if (device.getQrCodeUrl() == null || device.getQrCodeUrl().isBlank()) {
            device.setQrCodeUrl(DeviceUtils.generateQrCodeUrl(device.getDeviceSn()));
        }

        device = deviceRepository.save(device);
        log.info("管理员生成绑定码: adminUserId={}, deviceId={}, deviceSn={}", adminUserId, deviceId, device.getDeviceSn());
        return ApiResponse.success(buildDeviceResponse(device));
    }

    private String generateUniqueDeviceSnWithCheck() {
        for (int i = 0; i < 10; i++) {
            String sn = DeviceUtils.generateUniqueDeviceSn();
            if (deviceRepository.findByDeviceSn(sn).isEmpty()) {
                return sn;
            }
        }
        throw new BusinessException("生成设备SN失败，请重试");
    }

    /**
     * 编辑设备
     * PUT /api/v1/admin/devices/{deviceId}
     */
    @PutMapping("/{deviceId}")
    @RoleRequired(UserRole.ADMIN)
    public ApiResponse<DeviceStatusResponse> updateDevice(
            @PathVariable Long deviceId,
            @RequestBody Map<String, Object> request) {
        Long adminUserId = JwtAuthenticationFilter.getCurrentUserId();
        boolean superAdmin = JwtAuthenticationFilter.getCurrentUser() != null && JwtAuthenticationFilter.getCurrentUser().isSuperAdmin();

        // 验证权限
        if (!superAdmin && !adminDeviceBindingRepository.existsByAdminUserIdAndDeviceId(adminUserId, deviceId)) {
            throw new BusinessException("无权操作此设备");
        }

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException("设备不存在"));

        // 更新字段
        if (request.containsKey("name")) {
            device.setName((String) request.get("name"));
        }
        if (request.containsKey("location")) {
            device.setLocation((String) request.get("location"));
        }
        if (request.containsKey("pricingMode")) {
            device.setPricingMode((String) request.get("pricingMode"));
        }
        if (request.containsKey("pricePerUse")) {
            device.setPricePerUse(new BigDecimal(request.get("pricePerUse").toString()));
        }
        if (request.containsKey("pricePerMinute")) {
            device.setPricePerMinute(new BigDecimal(request.get("pricePerMinute").toString()));
        }

        device = deviceRepository.save(device);
        return ApiResponse.success(buildDeviceResponse(device));
    }

    /**
     * 删除设备（解除绑定）
     * DELETE /api/v1/admin/devices/{deviceId}
     */
    @DeleteMapping("/{deviceId}")
    @RoleRequired(UserRole.ADMIN)
    public ApiResponse<Void> deleteDevice(@PathVariable Long deviceId) {
        Long adminUserId = JwtAuthenticationFilter.getCurrentUserId();
        boolean superAdmin = JwtAuthenticationFilter.getCurrentUser() != null && JwtAuthenticationFilter.getCurrentUser().isSuperAdmin();

        if (!superAdmin) {
            AdminDeviceBinding binding = adminDeviceBindingRepository
                    .findByAdminUserIdAndDeviceId(adminUserId, deviceId)
                    .orElseThrow(() -> new BusinessException("未找到绑定关系"));

            adminDeviceBindingRepository.delete(binding);
        }

        // 清除设备所有者
        Device device = deviceRepository.findById(deviceId).orElse(null);
        if (device != null && adminUserId.equals(device.getOwnerId())) {
            device.setOwnerId(null);
            deviceRepository.save(device);
        }

        log.info("管理员删除设备: adminUserId={}, deviceId={}", adminUserId, deviceId);
        return ApiResponse.success();
    }

    /**
     * 获取设备状态监控
     * GET /api/v1/admin/devices/{deviceId}/status
     */
    @GetMapping("/{deviceId}/status")
    @RoleRequired(UserRole.ADMIN)
    public ApiResponse<DeviceStatusResponse> getDeviceStatus(@PathVariable Long deviceId) {
        Long adminUserId = JwtAuthenticationFilter.getCurrentUserId();
        boolean superAdmin = JwtAuthenticationFilter.getCurrentUser() != null && JwtAuthenticationFilter.getCurrentUser().isSuperAdmin();

        if (!superAdmin && !adminDeviceBindingRepository.existsByAdminUserIdAndDeviceId(adminUserId, deviceId)) {
            throw new BusinessException("无权查看此设备");
        }

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessException("设备不存在"));

        return ApiResponse.success(buildDeviceResponse(device));
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
                .pricingMode(device.getPricingMode())
                .pricePerUse(device.getPricePerUse())
                .pricePerMinute(device.getPricePerMinute())
                .washMode(device.getWashMode())
                .washModeName(device.getWashModeName())
                .remainSeconds(device.getRemainSeconds())
                .totalDuration(device.getTotalDuration())
                .estimatedEndTime(device.getEstimatedEndTime() != null ?
                        device.getEstimatedEndTime().toString() : null)
                .queueLength(device.getCurrentQueueLength())
                .build();
    }

    private String getStatusText(String status) {
        if (status == null) return "未知";
        switch (status) {
            case "OFFLINE": return "离线";
            case "IDLE": return "空闲";
            case "RUNNING": return "运行中";
            case "PAUSED": return "已暂停";
            case "FINISHED": return "已完成";
            case "FAULT": return "故障";
            default: return "未知";
        }
    }
}
