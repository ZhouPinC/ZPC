package com.wash.iot.service;

import com.wash.iot.entity.Device;
import com.wash.iot.entity.UserStatus;
import com.wash.iot.entity.UserStatusHistory;
import com.wash.iot.repository.DeviceRepository;
import com.wash.iot.repository.UserStatusHistoryRepository;
import com.wash.iot.repository.UserStatusRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
public class UserStatusService {
    
    @Autowired
    private UserStatusRepository userStatusRepository;
    
    @Autowired
    private UserStatusHistoryRepository userStatusHistoryRepository;
    
    @Autowired
    private DeviceRepository deviceRepository;
    
    /**
     * 更新用户状态
     * @param userId 用户ID
     * @param newStatus 新状态
     * @param deviceId 设备ID（可选）
     * @param reason 更新原因
     * @param relatedOrderNo 关联订单号（可选）
     * @return 更新后的用户状态
     */
    @Transactional
    public UserStatus updateUserStatus(Long userId, String newStatus, Long deviceId, String reason, String relatedOrderNo) {
        // 1. 查询当前用户状态
        Optional<UserStatus> statusOpt = userStatusRepository.findByUserId(userId);
        UserStatus userStatus;
        String oldStatus = "OFFLINE"; // 默认旧状态
        
        // 2. 处理设备关联
        Device device = null;
        if (deviceId != null) {
            device = deviceRepository.findById(deviceId)
                    .orElseThrow(() -> new RuntimeException("设备不存在，设备ID: " + deviceId));
        }
        
        // 3. 创建或更新用户状态
        if (statusOpt.isPresent()) {
            // 更新现有状态
            userStatus = statusOpt.get();
            oldStatus = userStatus.getStatus();
            
            // 更新状态字段
            userStatus.setStatus(newStatus);
            userStatus.setDevice(device);
            userStatus.setLastActivity(LocalDateTime.now());
            userStatus.setUpdateReason(reason);
        } else {
            // 创建新状态
            userStatus = new UserStatus();
            userStatus.setUserId(userId);
            userStatus.setStatus(newStatus);
            userStatus.setDevice(device);
            userStatus.setLastActivity(LocalDateTime.now());
            userStatus.setUpdateReason(reason);
        }
        
        // 4. 保存用户状态
        UserStatus savedStatus = userStatusRepository.save(userStatus);
        
        // 5. 记录状态变更历史
        UserStatusHistory history = new UserStatusHistory();
        history.setUserId(userId);
        history.setDevice(device);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setChangeReason(reason);
        history.setRelatedOrderNo(relatedOrderNo);
        userStatusHistoryRepository.save(history);
        
        log.info("用户状态更新成功，用户ID: {}, 旧状态: {}, 新状态: {}, 设备ID: {}, 原因: {}", 
                userId, oldStatus, newStatus, deviceId, reason);
        
        return savedStatus;
    }
    
    /**
     * 查询用户当前状态
     * @param userId 用户ID
     * @return 用户状态
     */
    public Optional<UserStatus> getUserStatus(Long userId) {
        return userStatusRepository.findByUserId(userId);
    }
    
    /**
     * 查询正在使用设备的用户
     * @param deviceId 设备ID
     * @return 用户状态
     */
    public Optional<UserStatus> getUserUsingDevice(Long deviceId) {
        return userStatusRepository.findByDeviceId(deviceId);
    }
    
    /**
     * 更新用户活动时间
     * @param userId 用户ID
     */
    public void updateUserActivity(Long userId) {
        Optional<UserStatus> statusOpt = userStatusRepository.findByUserId(userId);
        if (statusOpt.isPresent()) {
            UserStatus userStatus = statusOpt.get();
            userStatus.setLastActivity(LocalDateTime.now());
            userStatusRepository.save(userStatus);
            log.debug("用户活动时间更新成功，用户ID: {}", userId);
        }
    }
    
    /**
     * 用户上线
     * @param userId 用户ID
     */
    public void userOnline(Long userId) {
        updateUserStatus(userId, "ONLINE", null, "用户上线", null);
    }
    
    /**
     * 用户离线
     * @param userId 用户ID
     */
    public void userOffline(Long userId) {
        updateUserStatus(userId, "OFFLINE", null, "用户离线", null);
    }
    
    /**
     * 用户开始使用设备
     * @param userId 用户ID
     * @param deviceId 设备ID
     * @param orderNo 关联订单号
     */
    public void userStartUsingDevice(Long userId, Long deviceId, String orderNo) {
        updateUserStatus(userId, "USING_DEVICE", deviceId, "开始使用设备", orderNo);
    }
    
    /**
     * 用户停止使用设备
     * @param userId 用户ID
     * @param orderNo 关联订单号
     */
    public void userStopUsingDevice(Long userId, String orderNo) {
        updateUserStatus(userId, "ONLINE", null, "停止使用设备", orderNo);
    }
    
    /**
     * 设备状态变更时更新关联用户状态
     * @param deviceId 设备ID
     * @param deviceStatus 设备状态
     */
    @Transactional
    public void updateUserStatusByDeviceStatus(Long deviceId, String deviceStatus) {
        // 查询正在使用该设备的用户
        Optional<UserStatus> userStatusOpt = userStatusRepository.findByDeviceId(deviceId);
        if (userStatusOpt.isPresent()) {
            UserStatus userStatus = userStatusOpt.get();
            Long userId = userStatus.getUserId();
            String reason = "设备状态变更: " + deviceStatus;
            
            // 根据设备状态更新用户状态
            if ("FAULT".equals(deviceStatus)) {
                // 设备故障，用户停止使用设备
                updateUserStatus(userId, "ONLINE", null, reason, null);
            } else if ("OFFLINE".equals(deviceStatus)) {
                // 设备离线，用户停止使用设备
                updateUserStatus(userId, "ONLINE", null, reason, null);
            } else if ("IDLE".equals(deviceStatus)) {
                // 设备空闲，用户停止使用设备
                updateUserStatus(userId, "ONLINE", null, reason, null);
            }
        }
    }
}