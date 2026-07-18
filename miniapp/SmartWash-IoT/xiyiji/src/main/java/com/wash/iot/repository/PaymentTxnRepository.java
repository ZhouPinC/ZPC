package com.wash.iot.repository;

import com.wash.iot.entity.PaymentTxn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 支付交易流水仓库接口
 * 提供支付交易数据的持久化操作
 */
@Repository
public interface PaymentTxnRepository extends JpaRepository<PaymentTxn, Long> {
    
    /**
     * 根据订单号查询支付记录
     */
    List<PaymentTxn> findByOrderNo(String orderNo);
    
    /**
     * 根据交易ID查询支付记录
     */
    Optional<PaymentTxn> findByTransactionId(String transactionId);
    
    /**
     * 根据订单号和状态查询支付记录
     */
    List<PaymentTxn> findByOrderNoAndStatus(String orderNo, String status);
    
    /**
     * 根据状态查找支付记录
     */
    List<PaymentTxn> findByStatus(String status);
    
    /**
     * 根据创建时间之后查找支付记录
     */
    @Query("SELECT p FROM PaymentTxn p WHERE p.createTime > :createTime")
    List<PaymentTxn> findByCreateTimeAfter(@Param("createTime") LocalDateTime createTime);
    
    /**
     * 统计指定订单的支付总额
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentTxn p WHERE p.orderNo = :orderNo AND p.status = 'SUCCESS'")
    java.math.BigDecimal sumAmountByOrderNo(@Param("orderNo") String orderNo);
    
    /**
     * 统计指定时间范围内的支付金额
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentTxn p WHERE p.status = 'SUCCESS' AND p.createTime BETWEEN :startTime AND :endTime")
    java.math.BigDecimal sumSuccessfulAmountBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定时间范围内的支付数量
     */
    @Query("SELECT COUNT(p) FROM PaymentTxn p WHERE p.status = 'SUCCESS' AND p.createTime BETWEEN :startTime AND :endTime")
    long countSuccessfulPaymentsBetween(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
