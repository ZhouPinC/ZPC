package com.wash.iot.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付交易实体
 */
@Data
@Entity
@Table(name = "iot_payment_txn")
public class PaymentTxn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orderNo;

    @Column(unique = true, nullable = false)
    private String transactionId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 8)
    private String currency = "CNY";

    @Column(length = 32)
    private String paymentMethod;

    @Column(length = 32)
    private String paymentChannel;

    @Column(length = 20)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String thirdPartyResponse;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    // 构造函数
    public PaymentTxn() {}

    public PaymentTxn(String orderNo, String transactionId, BigDecimal amount) {
        this.orderNo = orderNo;
        this.transactionId = transactionId;
        this.amount = amount;
    }

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}