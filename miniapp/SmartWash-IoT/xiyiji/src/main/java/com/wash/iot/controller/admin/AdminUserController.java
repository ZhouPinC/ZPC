package com.wash.iot.controller.admin;

import com.wash.iot.common.exception.BusinessException;
import com.wash.iot.common.response.ApiResponse;
import com.wash.iot.dto.response.AdminUserResponse;
import com.wash.iot.entity.AdminDeviceBinding;
import com.wash.iot.entity.Device;
import com.wash.iot.entity.User;
import com.wash.iot.entity.UserDeviceBinding;
import com.wash.iot.entity.UserPermission;
import com.wash.iot.enums.UserRole;
import com.wash.iot.repository.AdminDeviceBindingRepository;
import com.wash.iot.repository.DeviceRepository;
import com.wash.iot.repository.UserDeviceBindingRepository;
import com.wash.iot.repository.UserPermissionRepository;
import com.wash.iot.repository.UserRepository;
import com.wash.iot.security.JwtAuthenticationFilter;
import com.wash.iot.security.RoleRequired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员用户管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Autowired
    private UserDeviceBindingRepository userDeviceBindingRepository;

    @Autowired
    private AdminDeviceBindingRepository adminDeviceBindingRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    /**
     * 获取所有用户列表（包含绑定设备信息）
     * GET /api/v1/admin/users
     */
    @GetMapping
    @RoleRequired(UserRole.ADMIN)
    public ApiResponse<List<AdminUserResponse>> getUsers() {
        // 获取所有用户
        List<User> users = userRepository.findAll();
        
        List<AdminUserResponse> result = new ArrayList<>();
        for (User user : users) {
            // 获取用户绑定的设备
            List<UserDeviceBinding> bindings = userDeviceBindingRepository.findByUserIdAndStatus(user.getId(), "ACTIVE");
            
            AdminUserResponse response = AdminUserResponse.builder()
                    .id(user.getId())
                    .nickname(user.getNickName())
                    .avatarUrl(user.getAvatarUrl())
                    .phone(user.getPhone())
                    .balance(user.getBalance())
                    .build();
            
            result.add(response);
        }
        
        return ApiResponse.success(result);
    }

    /**
     * 设置用户权限
     * POST /api/v1/admin/users/{userId}/permission
     */
    @PostMapping("/{userId}/permission")
    @RoleRequired(UserRole.ADMIN)
    public ApiResponse<Void> setUserPermission(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> request) {
        Long adminUserId = JwtAuthenticationFilter.getCurrentUserId();
        Long deviceId = Long.valueOf(request.get("deviceId").toString());

        // 验证管理员是否有权限管理该设备
        if (!adminDeviceBindingRepository.existsByAdminUserIdAndDeviceId(adminUserId, deviceId)) {
            throw new BusinessException("无权操作此设备");
        }

        // 查找或创建权限记录
        UserPermission permission = userPermissionRepository
                .findByUserIdAndDeviceIdAndStatus(userId, deviceId, "ACTIVE")
                .orElse(new UserPermission());

        permission.setUserId(userId);
        permission.setDeviceId(deviceId);
        permission.setGrantedBy(adminUserId);

        // 设置权限类型
        String permissionType = (String) request.getOrDefault("permissionType", "UNLIMITED");
        permission.setPermissionType(permissionType);

        if ("TIME_RANGE".equals(permissionType)) {
            String startTime = (String) request.get("startTime");
            String endTime = (String) request.get("endTime");
            if (startTime != null) permission.setStartTime(LocalTime.parse(startTime));
            if (endTime != null) permission.setEndTime(LocalTime.parse(endTime));
        } else if ("COUNT_LIMIT".equals(permissionType)) {
            Integer count = (Integer) request.get("remainingCount");
            permission.setRemainingCount(count);
        }

        // 设置过期日期
        String expireDate = (String) request.get("expireDate");
        if (expireDate != null) {
            permission.setExpireDate(LocalDate.parse(expireDate));
        }

        permission.setStatus("ACTIVE");
        userPermissionRepository.save(permission);

        log.info("设置用户权限: adminUserId={}, userId={}, deviceId={}, type={}",
                adminUserId, userId, deviceId, permissionType);
        return ApiResponse.success();
    }

    /**
     * 移除用户权限
     * DELETE /api/v1/admin/users/{userId}/permission
     */
    @DeleteMapping("/{userId}/permission")
    @RoleRequired(UserRole.ADMIN)
    public ApiResponse<Void> removeUserPermission(
            @PathVariable Long userId,
            @RequestParam Long deviceId) {
        Long adminUserId = JwtAuthenticationFilter.getCurrentUserId();

        // 验证管理员是否有权限管理该设备
        if (!adminDeviceBindingRepository.existsByAdminUserIdAndDeviceId(adminUserId, deviceId)) {
            throw new BusinessException("无权操作此设备");
        }

        UserPermission permission = userPermissionRepository
                .findByUserIdAndDeviceIdAndStatus(userId, deviceId, "ACTIVE")
                .orElseThrow(() -> new BusinessException("权限记录不存在"));

        permission.setStatus("REVOKED");
        userPermissionRepository.save(permission);

        log.info("移除用户权限: adminUserId={}, userId={}, deviceId={}", adminUserId, userId, deviceId);
        return ApiResponse.success();
    }
}
