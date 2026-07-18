package com.wash.iot.controller;

import com.wash.iot.entity.User;
import com.wash.iot.entity.Device;
import com.wash.iot.entity.Order;
import com.wash.iot.entity.UserDeviceBinding;
import com.wash.iot.service.UserService;
import com.wash.iot.repository.UserDeviceBindingRepository;
import com.wash.iot.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserDeviceBindingRepository userDeviceBindingRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    /**
     * 微信登录
     * 
     * @param code      微信登录code
     * @param nickName  用户昵称
     * @param avatarUrl 用户头像
     */
    @PostMapping("/wxLogin")
    public Map<String, Object> wxLogin(
            @RequestParam String code,
            @RequestParam(required = false) String nickName,
            @RequestParam(required = false) String avatarUrl) {

        Map<String, Object> result = new HashMap<>();
        try {
            User user = userService.wxLogin(code, nickName, avatarUrl);

            result.put("success", true);
            result.put("user", buildUserInfo(user));
            result.put("message", "登录成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "登录失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/info")
    public Map<String, Object> getUserInfo(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = userService.getUserById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            result.put("success", true);
            result.put("user", buildUserInfo(user));
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 更新用户信息
     */
    @PostMapping("/update")
    public Map<String, Object> updateUserInfo(
            @RequestParam Long userId,
            @RequestParam(required = false) String nickName,
            @RequestParam(required = false) String avatarUrl) {

        Map<String, Object> result = new HashMap<>();
        try {
            User user = userService.updateUserInfo(userId, nickName, avatarUrl);

            result.put("success", true);
            result.put("user", buildUserInfo(user));
            result.put("message", "更新成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取用户余额
     */
    @GetMapping("/balance")
    public Map<String, Object> getBalance(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            BigDecimal balance = userService.getBalance(userId);
            result.put("success", true);
            result.put("balance", balance);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 充值余额（模拟）
     */
    @PostMapping("/recharge")
    public Map<String, Object> recharge(
            @RequestParam Long userId,
            @RequestParam BigDecimal amount) {

        Map<String, Object> result = new HashMap<>();
        try {
            User user = userService.recharge(userId, amount);
            result.put("success", true);
            result.put("balance", user.getBalance());
            result.put("message", "充值成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取洗衣历史记录
     */
    @GetMapping("/history")
    public Map<String, Object> getWashHistory(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Order> orders = userService.getWashHistory(userId);

            // 构建详细的订单记录
            List<Map<String, Object>> records = new java.util.ArrayList<>();
            for (Order order : orders) {
                Map<String, Object> record = new HashMap<>();
                record.put("id", order.getId());
                record.put("orderNo", order.getOrderNo());
                record.put("deviceId", order.getDeviceId());
                record.put("deviceSn", getDeviceSnById(order.getDeviceId()));
                record.put("createTime", order.getCreateTime());
                record.put("payTime", order.getPayTime());
                record.put("startTime", order.getStartTime());
                record.put("endTime", order.getEndTime());
                record.put("amount", order.getAmount());
                record.put("status", order.getStatus());
                record.put("durationMinutes", order.getDurationMinutes());
                record.put("washMode", order.getWashMode());
                // 优先使用订单中保存的washModeName，如果没有则根据washMode推断
                String washModeName = order.getWashModeName();
                if (washModeName == null || washModeName.isEmpty()) {
                    washModeName = getWashModeName(order.getWashMode());
                }
                record.put("washModeName", washModeName);
                record.put("paymentMethod", order.getPaymentMethod());
                record.put("paymentMethodName", getPaymentMethodName(order.getPaymentMethod()));
                records.add(record);
            }

            result.put("success", true);
            result.put("records", records);
            result.put("total", records.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取支付方式名称
     */
    private String getPaymentMethodName(String paymentMethod) {
        if (paymentMethod == null)
            return "余额支付";
        switch (paymentMethod) {
            case "balance":
            case "BALANCE":
                return "余额支付";
            case "wechat":
            case "WECHAT":
                return "微信支付";
            case "alipay":
            case "ALIPAY":
                return "支付宝";
            default:
                return "余额支付";
        }
    }

    /**
     * 根据设备ID获取设备SN
     */
    private String getDeviceSnById(Long deviceId) {
        if (deviceId == null)
            return "未知设备";
        return deviceRepository.findById(deviceId)
                .map(d -> d.getDeviceSn())
                .orElse("未知设备");
    }

    /**
     * 获取洗衣模式名称
     */
    private String getWashModeName(String washMode) {
        if (washMode == null)
            return "标准洗";
        switch (washMode) {
            case "standard":
                return "标准洗";
            case "quick":
                return "快洗";
            case "dehydration":
            case "spin":
                return "脱水";
            default:
                return "标准洗";
        }
    }

    /**
     * 检查登录状态
     */
    @GetMapping("/checkLogin")
    public Map<String, Object> checkLogin(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean valid = userService.isTokenValid(userId);
            if (valid) {
                User user = userService.getUserById(userId).orElse(null);
                result.put("success", true);
                result.put("loggedIn", true);
                result.put("user", user != null ? buildUserInfo(user) : null);
            } else {
                result.put("success", true);
                result.put("loggedIn", false);
                result.put("message", "登录已过期，请重新登录");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("loggedIn", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 构建返回给前端的用户信息
     */
    private Map<String, Object> buildUserInfo(User user) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("nickName", user.getNickName());
        info.put("avatarUrl", user.getAvatarUrl());
        info.put("balance", user.getBalance());
        info.put("points", user.getPoints());
        info.put("phone", user.getPhone());
        info.put("lastLoginTime", user.getLastLoginTime());
        return info;
    }

    // ==================== 用户-设备绑定接口 ====================

    /**
     * 获取用户绑定的设备列表
     */
    @GetMapping("/devices")
    public Map<String, Object> getBindDevices(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<UserDeviceBinding> bindings = userDeviceBindingRepository.findByUserIdAndStatus(userId, "ACTIVE");

            // 批量获取设备信息，解决N+1问题
            List<Long> deviceIds = bindings.stream()
                    .map(UserDeviceBinding::getDeviceId)
                    .collect(java.util.stream.Collectors.toList());
            
            Map<Long, Device> deviceMap = new HashMap<>();
            if (!deviceIds.isEmpty()) {
                List<Device> deviceList = deviceRepository.findAllById(deviceIds);
                for (Device d : deviceList) {
                    deviceMap.put(d.getId(), d);
                }
            }

            List<Map<String, Object>> devices = new ArrayList<>();
            for (UserDeviceBinding binding : bindings) {
                Device device = deviceMap.get(binding.getDeviceId());
                if (device != null) {
                    Map<String, Object> deviceInfo = new HashMap<>();
                    deviceInfo.put("id", device.getId());
                    deviceInfo.put("deviceSn", device.getDeviceSn());
                    deviceInfo.put("name", device.getName() != null && !device.getName().isBlank() ? device.getName() : device.getDeviceSn());
                    deviceInfo.put("location", device.getLocation());
                    deviceInfo.put("status", device.getStatus());
                    deviceInfo.put("bindTime", binding.getBindTime());
                    devices.add(deviceInfo);
                }
            }

            result.put("success", true);
            result.put("devices", devices);
            result.put("total", devices.size());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 绑定设备
     */
    @PostMapping("/devices/bind")
    public Map<String, Object> bindDevice(
            @RequestParam Long userId,
            @RequestParam String deviceSn) {

        Map<String, Object> result = new HashMap<>();
        try {
            // 查找设备
            Device device = deviceRepository.findByDeviceSn(deviceSn)
                    .orElseThrow(() -> new RuntimeException("设备不存在: " + deviceSn));

            // 检查是否已绑定
            boolean exists = userDeviceBindingRepository.existsByUserIdAndDeviceIdAndStatus(userId, device.getId(),
                    "ACTIVE");
            if (exists) {
                result.put("success", false);
                result.put("message", "您已绑定该设备");
                return result;
            }

            // 创建绑定关系
            UserDeviceBinding binding = new UserDeviceBinding();
            binding.setUserId(userId);
            binding.setDeviceId(device.getId());
            binding.setStatus("ACTIVE");
            userDeviceBindingRepository.save(binding);

            result.put("success", true);
            result.put("message", "绑定成功");
            result.put("device", buildDeviceInfo(device));
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 解绑设备
     */
    @PostMapping("/devices/unbind")
    public Map<String, Object> unbindDevice(
            @RequestParam Long userId,
            @RequestParam Long deviceId) {

        Map<String, Object> result = new HashMap<>();
        try {
            UserDeviceBinding binding = userDeviceBindingRepository
                    .findByUserIdAndDeviceIdAndStatus(userId, deviceId, "ACTIVE")
                    .orElseThrow(() -> new RuntimeException("未找到绑定关系"));

            binding.setStatus("REMOVED");
            userDeviceBindingRepository.save(binding);

            result.put("success", true);
            result.put("message", "解绑成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 更新用户资料（头像、昵称）
     */
    @PostMapping("/profile")
    public Map<String, Object> updateProfile(
            @RequestParam Long userId,
            @RequestParam(required = false) String nickName,
            @RequestParam(required = false) String avatarUrl) {

        Map<String, Object> result = new HashMap<>();
        try {
            User user = userService.updateUserInfo(userId, nickName, avatarUrl);
            result.put("success", true);
            result.put("user", buildUserInfo(user));
            result.put("message", "更新成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 绑定手机号
     */
    @PostMapping("/bindPhone")
    public Map<String, Object> bindPhone(
            @RequestParam Long userId,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String encryptedData,
            @RequestParam(required = false) String iv) {

        Map<String, Object> result = new HashMap<>();
        try {
            if (code == null || code.isEmpty()) {
                throw new RuntimeException("授权失败：请允许获取手机号以完成绑定");
            }

            String phone = getPhoneFromWx(code);

            User user = userService.getUserById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                result.put("success", true);
                result.put("phone", user.getPhone());
                result.put("message", "手机号已绑定");
                return result;
            }

            user.setPhone(phone);
            userService.saveUser(user);

            result.put("success", true);
            result.put("phone", phone);
            result.put("message", "绑定成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 从微信获取手机号（简化版）
     */
    private String getPhoneFromWx(String code) {
        // 生产环境需要调用微信API获取手机号
        // 这里返回模拟手机号
        return "1380000" + code.substring(Math.max(0, code.length() - 4));
    }

    /**
     * 构建设备信息
     */
    private Map<String, Object> buildDeviceInfo(Device device) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", device.getId());
        info.put("deviceSn", device.getDeviceSn());
        info.put("name", device.getName() != null && !device.getName().isBlank() ? device.getName() : device.getDeviceSn());
        info.put("location", device.getLocation());
        info.put("status", device.getStatus());
        return info;
    }
}
