package com.wash.iot.repository;

import com.wash.iot.entity.DeviceStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备状态历史Repository
 */
@Repository
public interface DeviceStatusHistoryRepository extends JpaRepository<DeviceStatusHistory, Long> {

    /**
     * 根据设备ID查找状态历史
     */
    List<DeviceStatusHistory> findByDeviceIdOrderByChangeTimeDesc(Long deviceId);

    /**
     * 根据订单号查找状态历史
     */
    List<DeviceStatusHistory> findByOrderNo(String orderNo);

    /**
     * 根据设备ID和时间范围查找状态历史
     */
    @Query("SELECT h FROM DeviceStatusHistory h WHERE h.deviceId = :deviceId AND h.changeTime BETWEEN :startTime AND :endTime ORDER BY h.changeTime DESC")
    List<DeviceStatusHistory> findByDeviceIdAndChangeTimeBetween(@Param("deviceId") Long deviceId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 根据状态类型查找状态历史
     */
    List<DeviceStatusHistory> findByStatusType(String statusType);

    /**
     * 查找最近的错误记录
     */
    @Query("SELECT h FROM DeviceStatusHistory h WHERE h.errorCode IS NOT NULL ORDER BY h.changeTime DESC")
    List<DeviceStatusHistory> findRecentErrorRecords();

    /**
     * 统计设备状态变更次数
     */
    @Query("SELECT COUNT(h) FROM DeviceStatusHistory h WHERE h.deviceId = :deviceId AND h.changeTime BETWEEN :startTime AND :endTime")
    long countStatusChangesByDevice(@Param("deviceId") Long deviceId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}