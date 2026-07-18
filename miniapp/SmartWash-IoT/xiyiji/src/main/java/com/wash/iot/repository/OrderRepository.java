package com.wash.iot.repository;

import com.wash.iot.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 订单仓库接口
 * 提供订单数据的持久化操作
 */
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    /**
     * 根据订单号查订单
     */
    Optional<Order> findByOrderNo(String orderNo);

    /**
     * 查找某台设备当前是否还有"未完成"的订单 (用于状态同步)
     */
    @Query("SELECT o FROM Order o WHERE o.deviceId = :deviceId AND o.status IN ('PAID', 'RUNNING')")
    Optional<Order> findActiveOrderByDeviceId(@Param("deviceId") Long deviceId);
    
    /**
     * 查找用户当前进行中的订单
     */
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.status IN ('PAID', 'RUNNING')")
    Optional<Order> findActiveOrderByUserId(@Param("userId") Long userId);
    
    /**
     * 根据用户ID查询订单历史，按创建时间倒序
     */
    List<Order> findByUserIdOrderByCreateTimeDesc(Long userId);
    
    /**
     * 分页查询用户订单
     */
    Page<Order> findByUserId(Long userId, Pageable pageable);
    
    /**
     * 根据用户ID和创建时间范围查询订单
     */
    List<Order> findByUserIdAndCreateTimeBetween(Long userId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 根据设备ID分页查询订单
     */
    Page<Order> findByDeviceId(Long deviceId, Pageable pageable);
    
    /**
     * 统计指定设备在指定时间段内的订单数
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.deviceId IN :deviceIds AND o.createTime BETWEEN :start AND :end")
    int countByDeviceIdsAndPeriod(@Param("deviceIds") List<Long> deviceIds, 
                                   @Param("start") LocalDateTime start, 
                                   @Param("end") LocalDateTime end);
    
    /**
     * 统计指定设备的总订单数
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.deviceId IN :deviceIds")
    int countByDeviceIds(@Param("deviceIds") List<Long> deviceIds);
    
    /**
     * 查询指定设备在指定时间段内的订单列表
     */
    @Query("SELECT o FROM Order o WHERE o.deviceId IN :deviceIds AND o.createTime BETWEEN :start AND :end ORDER BY o.createTime DESC")
    List<Order> findByDeviceIdsAndPeriod(@Param("deviceIds") List<Long> deviceIds,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);
    
    /**
     * 查询指定设备的最近订单
     */
    @Query("SELECT o FROM Order o WHERE o.deviceId IN :deviceIds ORDER BY o.createTime DESC")
    List<Order> findRecentByDeviceIds(@Param("deviceIds") List<Long> deviceIds, Pageable pageable);
    
    /**
     * 根据设备所有者ID查询订单
     */
    List<Order> findByDeviceOwnerIdOrderByCreateTimeDesc(Long deviceOwnerId);
    
    /**
     * 统计设备所有者的订单数
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.deviceOwnerId = :ownerId AND o.createTime BETWEEN :start AND :end")
    int countByOwnerIdAndPeriod(@Param("ownerId") Long ownerId,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    /**
     * 根据状态查找订单
     */
    List<Order> findByStatus(String status);

    /**
     * 根据状态和时间范围查找订单
     */
    List<Order> findByStatusAndCreateTimeBetween(String status, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据设备ID列表和状态查找订单
     */
    List<Order> findByDeviceIdInAndStatus(List<Long> deviceIds, String status);

    /**
     * 根据用户ID查找订单
     */
    List<Order> findByUserId(Long userId);

    /**
     * 查找支付超时的订单
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'CREATED' AND o.createTime < :timeout")
    List<Order> findPaymentTimeoutOrders(@Param("timeout") LocalDateTime timeout);

    /**
     * 查找启动超时的订单
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'PAID' AND o.payTime < :timeout")
    List<Order> findStartTimeoutOrders(@Param("timeout") LocalDateTime timeout);

    /**
     * 查找卡单的订单
     */
    @Query("SELECT o FROM Order o WHERE o.createTime < :timeout AND o.status NOT IN ('FINISHED', 'CANCELLED', 'REFUNDED')")
    List<Order> findStuckOrders(@Param("timeout") LocalDateTime timeout);

       /**
        * 根据创建时间之后查找订单（兼容旧调用）
        */
       List<Order> findByCreateTimeAfter(LocalDateTime after);

    /**
     * 根据设备ID列表、状态、时间范围、用户ID分页查询订单
     */
    @Query("SELECT o FROM Order o WHERE o.deviceId IN :deviceIds " +
           "AND (:status IS NULL OR o.status = :status) " +
           "AND (:startTime IS NULL OR o.createTime >= :startTime) " +
           "AND (:endTime IS NULL OR o.createTime <= :endTime) " +
           "AND (:userId IS NULL OR o.userId = :userId) " +
           "ORDER BY o.createTime DESC")
    Page<Order> findByDeviceIdsWithFilters(@Param("deviceIds") List<Long> deviceIds,
                                           @Param("status") String status,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime,
                                           @Param("userId") Long userId,
                                           Pageable pageable);

    /**
     * 根据设备ID列表和时间范围查找订单
     */
    @Query("SELECT o FROM Order o WHERE o.deviceId IN :deviceIds AND o.createTime BETWEEN :startTime AND :endTime")
    List<Order> findByDeviceIdsAndTimeRange(@Param("deviceIds") List<Long> deviceIds,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);

       /**
        * 兼容旧代码使用的命名：根据设备ID列表和创建时间范围查询订单
        */
       @Query("SELECT o FROM Order o WHERE o.deviceId IN :deviceIds AND o.createTime BETWEEN :start AND :end ORDER BY o.createTime DESC")
       List<Order> findByDeviceIdsAndCreateTimeBetween(@Param("deviceIds") List<Long> deviceIds,
                                                                                           @Param("start") LocalDateTime start,
                                                                                           @Param("end") LocalDateTime end);

       /**
        * 根据单个设备ID和创建时间之后查询订单
        */
       @Query("SELECT o FROM Order o WHERE o.deviceId = :deviceId AND o.createTime >= :after ORDER BY o.createTime DESC")
       List<Order> findByDeviceIdAndCreateTimeAfter(@Param("deviceId") Long deviceId, @Param("after") LocalDateTime after);

       /**
        * 根据设备ID列表和创建时间之后查询订单（兼容活跃用户统计）
        */
       @Query("SELECT o FROM Order o WHERE o.deviceId IN :deviceIds AND o.createTime >= :after ORDER BY o.createTime DESC")
       List<Order> findByDeviceIdsAndCreateTimeAfter(@Param("deviceIds") List<Long> deviceIds, @Param("after") LocalDateTime after);

    /**
     * 根据设备ID列表和更新时间之后查找异常订单
     */
    @Query("SELECT o FROM Order o WHERE o.deviceId IN :deviceIds AND o.status = :status AND o.updateTime > :updateTime")
    List<Order> findByDeviceIdsAndStatusAndUpdateTimeAfter(@Param("deviceIds") List<Long> deviceIds,
                                                         @Param("status") String status,
                                                         @Param("updateTime") LocalDateTime updateTime);

    /**
     * 根据状态和结束时间之后查找订单
     */
    List<Order> findByStatusAndEndTimeAfter(String status, LocalDateTime endTime);

    /**
     * 根据状态和更新时间之前查找订单
     */
    List<Order> findByStatusAndUpdateTimeBefore(String status, LocalDateTime updateTime);
}
