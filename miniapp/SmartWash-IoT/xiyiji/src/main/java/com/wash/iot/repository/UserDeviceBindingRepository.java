package com.wash.iot.repository;

import com.wash.iot.entity.UserDeviceBinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceBindingRepository extends JpaRepository<UserDeviceBinding, Long> {
    
    List<UserDeviceBinding> findByUserIdAndStatus(Long userId, String status);
    
    List<UserDeviceBinding> findByDeviceIdAndStatus(Long deviceId, String status);
    
    Optional<UserDeviceBinding> findByUserIdAndDeviceIdAndStatus(Long userId, Long deviceId, String status);
    
    boolean existsByUserIdAndDeviceIdAndStatus(Long userId, Long deviceId, String status);
}
