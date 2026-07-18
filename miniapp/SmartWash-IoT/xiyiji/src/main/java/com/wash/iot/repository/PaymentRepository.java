package com.wash.iot.repository;

import com.wash.iot.entity.PaymentTxn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 支付记录Repository接口
 */
public interface PaymentRepository extends JpaRepository<PaymentTxn, Long> {

    /**
     * 根据交易ID查询支付记录
     */
    Optional<PaymentTxn> findByTransactionId(String transactionId);

    /**
     * 根据订单ID查询支付记录
     */
    List<PaymentTxn> findByOrderNo(String orderNo);

    /**
     * 根据状态查询支付记录
     */
    List<PaymentTxn> findByStatus(String status);

    /**
     * 根据支付方式查询支付记录
     */
    List<PaymentTxn> findByPaymentMethod(String paymentMethod);

    /**
     * 根据时间范围查询支付记录
     */
    @Query("SELECT p FROM PaymentTxn p WHERE p.createTime BETWEEN :start AND :end")
    List<PaymentTxn> findByCreateTimeBetween(@Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    /**
     * 统计指定时间范围内的支付总额
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentTxn p WHERE p.status = 'SUCCESS' AND p.createTime BETWEEN :start AND :end")
    java.math.BigDecimal sumSuccessfulPaymentsBetween(@Param("start") LocalDateTime start,
                                                     @Param("end") LocalDateTime end);
}
