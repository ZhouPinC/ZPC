package com.wash.iot.repository;

import com.wash.iot.entity.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {
    
    List<UserPermission> findByUserIdAndStatus(Long userId, String status);
    
    Optional<UserPermission> findByUserIdAndDeviceIdAndStatus(Long userId, Long deviceId, String status);
    
    List<UserPermission> findByGrantedByAndStatus(Long grantedBy, String status);
    
    List<UserPermission> findByDeviceIdAndStatus(Long deviceId, String status);
}
