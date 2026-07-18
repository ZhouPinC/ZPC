package com.wash.iot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收益记录
 */
@Data
@Entity
@Table(name = "iot_income_record")
public class IncomeRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long adminUserId;

    @Column(nullable = false)
    private Long deviceId;

    @Column(nullable = false)
    private Long orderId;

    private BigDecimal orderAmount;

    private BigDecimal platformFee;

    private BigDecimal netIncome;

    private String settleStatus; // PENDING, SETTLED

    private LocalDateTime createTime;

    private LocalDateTime settleTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        if (this.settleStatus == null) this.settleStatus = "PENDING";
    }
}
