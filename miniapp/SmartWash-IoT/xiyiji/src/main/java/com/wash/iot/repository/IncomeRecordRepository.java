package com.wash.iot.repository;

import com.wash.iot.entity.IncomeRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 收益记录仓库接口
 * 提供收益记录的持久化操作
 */
@Repository
public interface IncomeRecordRepository extends JpaRepository<IncomeRecord, Long> {
    
    /**
     * 根据管理员用户ID查询收益记录，按创建时间倒序
     */
    List<IncomeRecord> findByAdminUserIdOrderByCreateTimeDesc(Long adminUserId);
    
    /**
     * 分页查询管理员收益记录
     */
    Page<IncomeRecord> findByAdminUserId(Long adminUserId, Pageable pageable);
    
    /**
     * 查询指定时间段内的管理员收益记录
     */
    List<IncomeRecord> findByAdminUserIdAndCreateTimeBetween(Long adminUserId, LocalDateTime start, LocalDateTime end);
    
    /**
     * 统计管理员的净收益总额
     */
    @Query("SELECT SUM(i.netIncome) FROM IncomeRecord i WHERE i.adminUserId = ?1")
    BigDecimal sumNetIncomeByAdminUserId(Long adminUserId);
    
    /**
     * 统计指定时间段内管理员的净收益总额
     */
    @Query("SELECT SUM(i.netIncome) FROM IncomeRecord i WHERE i.adminUserId = ?1 AND i.createTime BETWEEN ?2 AND ?3")
    BigDecimal sumNetIncomeByAdminUserIdAndPeriod(Long adminUserId, LocalDateTime start, LocalDateTime end);
    
    /**
     * 检查订单是否已有收益记录
     */
    boolean existsByOrderId(Long orderId);
    
    /**
     * 根据订单ID查找收益记录
     */
    List<IncomeRecord> findByOrderId(Long orderId);

    /**
     * 根据设备ID查找收益记录
     */
    List<IncomeRecord> findByDeviceId(Long deviceId);

    /**
     * 根据管理员ID查找待结算记录
     */
    @Query("SELECT i FROM IncomeRecord i WHERE i.adminUserId = :adminUserId AND i.settleStatus = 'PENDING' AND i.createTime < :time")
    List<IncomeRecord> findPendingRecordsBeforeTime(@Param("adminUserId") Long adminUserId, @Param("time") LocalDateTime time);

    /**
     * 查找所有管理员在指定时间之前的待结算记录（不按管理员分组）
     */
    @Query("SELECT i FROM IncomeRecord i WHERE i.settleStatus = 'PENDING' AND i.createTime < :time")
    List<IncomeRecord> findPendingRecordsBeforeTime(@Param("time") LocalDateTime time);
    /**
     * 根据时间段查找收益记录
     */
    @Query("SELECT i FROM IncomeRecord i WHERE i.createTime BETWEEN :start AND :end")
    List<IncomeRecord> findByCreateTimeBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
