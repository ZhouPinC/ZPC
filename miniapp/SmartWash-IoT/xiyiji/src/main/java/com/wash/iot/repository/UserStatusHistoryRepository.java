package com.wash.iot.repository;

import com.wash.iot.entity.UserStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserStatusHistoryRepository extends JpaRepository<UserStatusHistory, Long> {
    
    // 根据用户ID查询历史记录
    List<UserStatusHistory> findByUserIdOrderByChangeTimeDesc(@Param("userId") Long userId);
    
    // 根据设备ID查询历史记录
    List<UserStatusHistory> findByDeviceIdOrderByChangeTimeDesc(@Param("deviceId") Long deviceId);
    
    // 根据用户ID和时间段查询历史记录
    List<UserStatusHistory> findByUserIdAndChangeTimeBetweenOrderByChangeTimeDesc(
            @Param("userId") Long userId, 
            @Param("startTime") java.time.LocalDateTime startTime,
            @Param("endTime") java.time.LocalDateTime endTime);
    
    // 根据关联订单号查询历史记录
    List<UserStatusHistory> findByRelatedOrderNo(@Param("relatedOrderNo") String relatedOrderNo);
}