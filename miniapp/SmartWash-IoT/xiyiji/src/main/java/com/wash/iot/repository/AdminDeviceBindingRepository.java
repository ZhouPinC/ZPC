package com.wash.iot.repository;

import com.wash.iot.entity.AdminDeviceBinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminDeviceBindingRepository extends JpaRepository<AdminDeviceBinding, Long> {
    
    List<AdminDeviceBinding> findByAdminUserId(Long adminUserId);
    
    Optional<AdminDeviceBinding> findByAdminUserIdAndDeviceId(Long adminUserId, Long deviceId);
    
    boolean existsByAdminUserIdAndDeviceId(Long adminUserId, Long deviceId);
    
    List<AdminDeviceBinding> findByDeviceId(Long deviceId);
}
