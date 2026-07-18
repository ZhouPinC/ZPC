package com.wash.iot.repository;

import com.wash.iot.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserStatusRepository extends JpaRepository<UserStatus, Long> {
    
    // 根据用户ID查询用户状态
    Optional<UserStatus> findByUserId(@Param("userId") Long userId);
    
    // 根据设备ID查询正在使用该设备的用户状态
    Optional<UserStatus> findByDeviceId(@Param("deviceId") Long deviceId);
    
    // 根据用户ID和状态查询
    Optional<UserStatus> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);
    
    // 查询指定状态的用户数量
    long countByStatus(@Param("status") String status);
}