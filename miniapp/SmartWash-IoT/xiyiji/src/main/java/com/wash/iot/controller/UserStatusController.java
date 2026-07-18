package com.wash.iot.controller;

import com.wash.iot.entity.UserStatus;
import com.wash.iot.service.UserStatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * 用户状态管理控制器
 * 提供用户状态的查询和更新接口
 */
@RestController
@RequestMapping("/api/user-status")
public class UserStatusController {

    @Autowired
    private UserStatusService userStatusService;

    /**
     * 查询用户当前状态
     * @param userId 用户ID
     * @return 用户状态信息
     */
    @GetMapping("/query")
    public Optional<UserStatus> getUserStatus(@RequestParam Long userId) {
        return userStatusService.getUserStatus(userId);
    }

    /**
     * 更新用户状态
     * @param userId 用户ID
     * @param status 新状态
     * @param deviceId 设备ID（可选）
     * @param reason 更新原因
     * @return 更新后的用户状态
     */
    @PostMapping("/update")
    public UserStatus updateUserStatus(
            @RequestParam Long userId,
            @RequestParam String status,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) String reason) {
        return userStatusService.updateUserStatus(userId, status, deviceId, reason, null);
    }

    /**
     * 用户上线
     * @param userId 用户ID
     * @return 更新后的用户状态
     */
    @PostMapping("/online")
    public UserStatus userOnline(@RequestParam Long userId) {
        userStatusService.userOnline(userId);
        return userStatusService.getUserStatus(userId).orElseThrow(() -> new RuntimeException("用户状态更新失败"));
    }

    /**
     * 用户离线
     * @param userId 用户ID
     * @return 更新后的用户状态
     */
    @PostMapping("/offline")
    public UserStatus userOffline(@RequestParam Long userId) {
        userStatusService.userOffline(userId);
        return userStatusService.getUserStatus(userId).orElseThrow(() -> new RuntimeException("用户状态更新失败"));
    }

    /**
     * 用户开始使用设备
     * @param userId 用户ID
     * @param deviceId 设备ID
     * @param orderNo 关联订单号
     * @return 更新后的用户状态
     */
    @PostMapping("/start-using-device")
    public UserStatus userStartUsingDevice(
            @RequestParam Long userId,
            @RequestParam Long deviceId,
            @RequestParam String orderNo) {
        userStatusService.userStartUsingDevice(userId, deviceId, orderNo);
        return userStatusService.getUserStatus(userId).orElseThrow(() -> new RuntimeException("用户状态更新失败"));
    }

    /**
     * 用户停止使用设备
     * @param userId 用户ID
     * @param orderNo 关联订单号
     * @return 更新后的用户状态
     */
    @PostMapping("/stop-using-device")
    public UserStatus userStopUsingDevice(
            @RequestParam Long userId,
            @RequestParam String orderNo) {
        userStatusService.userStopUsingDevice(userId, orderNo);
        return userStatusService.getUserStatus(userId).orElseThrow(() -> new RuntimeException("用户状态更新失败"));
    }

    /**
     * 更新用户活动时间
     * @param userId 用户ID
     * @return 更新结果
     */
    @PostMapping("/update-activity")
    public String updateUserActivity(@RequestParam Long userId) {
        userStatusService.updateUserActivity(userId);
        return "用户活动时间更新成功";
    }
}