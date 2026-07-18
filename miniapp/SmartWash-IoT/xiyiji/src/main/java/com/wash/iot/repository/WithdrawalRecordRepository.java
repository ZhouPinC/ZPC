package com.wash.iot.repository;

import com.wash.iot.entity.WithdrawalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 提现记录Repository
 */
@Repository
public interface WithdrawalRecordRepository extends JpaRepository<WithdrawalRecord, Long> {

    /**
     * 根据用户ID查找提现记录
     */
    List<WithdrawalRecord> findByUserIdOrderByCreateTimeDesc(Long userId);

    /**
     * 根据状态查找提现记录
     */
    List<WithdrawalRecord> findByStatus(String status);

    /**
     * 统计用户待处理提现金额
     */
    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM WithdrawalRecord w WHERE w.userId = :userId AND w.status = 'PENDING'")
    BigDecimal sumPendingAmountByUserId(@Param("userId") Long userId);

    /**
     * 根据时间范围查找提现记录
     */
    @Query("SELECT w FROM WithdrawalRecord w WHERE w.createTime BETWEEN :startTime AND :endTime ORDER BY w.createTime DESC")
    List<WithdrawalRecord> findByCreateTimeBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定时间范围内的提现总额
     */
    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM WithdrawalRecord w WHERE w.status = 'APPROVED' AND w.processTime BETWEEN :startTime AND :endTime")
    BigDecimal sumApprovedAmountBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}