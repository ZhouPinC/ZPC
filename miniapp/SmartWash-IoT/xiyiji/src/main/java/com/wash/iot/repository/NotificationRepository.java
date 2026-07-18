package com.wash.iot.repository;

import com.wash.iot.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    List<Notification> findByUserIdOrderByCreateTimeDesc(Long userId);
    
    List<Notification> findByUserIdAndIsReadOrderByCreateTimeDesc(Long userId, Boolean isRead);
    
    int countByUserIdAndIsRead(Long userId, Boolean isRead);
}
