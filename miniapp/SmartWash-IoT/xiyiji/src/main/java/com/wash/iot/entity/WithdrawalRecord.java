package com.wash.iot.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提现记录实体
 */
@Data
@Entity
@Table(name = "iot_withdrawal_record")
public class WithdrawalRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "withdraw_method", length = 32)
    private String withdrawMethod;

    @Column(length = 20)
    private String status;

    @Column(length = 500)
    private String remark;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "process_time")
    private LocalDateTime processTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        if (this.status == null) {
            this.status = "PENDING";
        }
    }
}