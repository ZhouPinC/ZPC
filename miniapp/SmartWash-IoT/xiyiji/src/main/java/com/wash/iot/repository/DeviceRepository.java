package com.wash.iot.repository;

import com.wash.iot.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 设备Repository接口
 */
public interface DeviceRepository extends JpaRepository<Device, Long> {

    /**
     * 根据序列号查找设备
     */
    Optional<Device> findByDeviceSn(String deviceSn);

    /**
     * 根据状态查找设备
     */
    List<Device> findByStatus(String status);

    /**
     * 根据所有者ID查找设备
     */
    List<Device> findByOwnerId(Long ownerId);

    /**
     * 根据设备ID列表查找设备
     */
    List<Device> findByIdIn(List<Long> deviceIds);

    /**
     * 根据设备ID列表查找设备
     */
    @Query("SELECT d FROM Device d WHERE d.id IN :deviceIds")
    List<Device> findByDeviceIds(@Param("deviceIds") List<Long> deviceIds);

    /**
     * 统计指定状态的设备数量
     */
    @Query("SELECT COUNT(d) FROM Device d WHERE d.id IN :deviceIds AND d.status = :status")
    long countByDeviceIdsAndStatus(@Param("deviceIds") List<Long> deviceIds, @Param("status") String status);

    /**
     * 统计心跳时间在指定时间之后的设备数量
     */
    @Query("SELECT COUNT(d) FROM Device d WHERE d.id IN :deviceIds AND d.lastHeartbeat > :heartbeatTime")
    long countByDeviceIdsAndLastHeartbeatAfter(@Param("deviceIds") List<Long> deviceIds, @Param("heartbeatTime") LocalDateTime heartbeatTime);

    /**
     * 查找心跳超时的设备
     */
    @Query("SELECT d FROM Device d WHERE d.lastHeartbeat < :offlineTime AND d.status != 'OFFLINE'")
    List<Device> findOfflineDevices(@Param("offlineTime") LocalDateTime offlineTime);

    /**
     * 根据所有者ID和状态查找设备
     */
    List<Device> findByOwnerIdAndStatus(Long ownerId, String status);

    /**
     * 根据位置模糊查找设备
     */
    @Query("SELECT d FROM Device d WHERE d.location LIKE %:location%")
    List<Device> findByLocationContaining(@Param("location") String location);

    /**
     * 统计设备总数
     */
    @Query("SELECT COUNT(d) FROM Device d WHERE d.id IN :deviceIds")
    long countByDeviceIds(@Param("deviceIds") List<Long> deviceIds);

    /**
     * 查找所有设备
     */
    @Query("SELECT d FROM Device d ORDER BY d.deviceSn")
    List<Device> findAllOrderByDeviceSn();

    /**
     * 根据设备ID列表和状态查找设备（兼容 AdminDashboard 等调用）
     */
    @Query("SELECT d FROM Device d WHERE d.id IN :deviceIds AND d.status = :status")
    List<Device> findByDeviceIdsAndStatus(@Param("deviceIds") List<Long> deviceIds, @Param("status") String status);

    /**
     * 查找活跃设备（有订单记录的设备）
     */
    @Query("SELECT DISTINCT d FROM Device d JOIN Order o ON d.id = o.deviceId WHERE d.id IN :deviceIds")
    List<Device> findActiveDevices(@Param("deviceIds") List<Long> deviceIds);
}
