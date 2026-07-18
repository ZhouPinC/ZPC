package com.wash.iot.repository;

import com.wash.iot.entity.Queue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QueueRepository extends JpaRepository<Queue, Long> {

    List<Queue> findByDeviceIdAndStatusOrderByQueuePosition(Long deviceId, String status);

    Optional<Queue> findByUserIdAndDeviceIdAndStatus(Long userId, Long deviceId, String status);

    List<Queue> findByUserIdAndStatusIn(Long userId, List<String> statuses);

    List<Queue> findByUserIdAndStatus(Long userId, String status);

    int countByDeviceIdAndStatus(Long deviceId, String status);

    int countByDeviceIdInAndStatus(List<Long> deviceIds, String status);
    
    @Query("SELECT COUNT(q) FROM Queue q WHERE q.deviceId IN :deviceIds AND q.status = :status")
    long countByDeviceIdsAndStatus(@Param("deviceIds") List<Long> deviceIds, @Param("status") String status);

    List<Queue> findByDeviceIdInAndStatusAndCreateTimeBefore(List<Long> deviceIds, String status, LocalDateTime beforeTime);
    
    @Query("SELECT q FROM Queue q WHERE q.deviceId IN :deviceIds AND q.status = :status AND q.createTime < :beforeTime")
    List<Queue> findByDeviceIdsAndStatusAndCreateTimeBefore(@Param("deviceIds") List<Long> deviceIds, @Param("status") String status, @Param("beforeTime") LocalDateTime beforeTime);

    Optional<Queue> findFirstByDeviceIdOrderByQueuePosition(Long deviceId);

    @Query("SELECT q FROM Queue q WHERE q.expireTime < :now AND q.status = 'WAITING'")
    List<Queue> findTimeoutQueues(@Param("now") LocalDateTime now);
}
