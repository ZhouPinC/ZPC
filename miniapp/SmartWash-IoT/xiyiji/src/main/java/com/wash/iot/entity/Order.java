package com.wash.iot.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "iot_order")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String orderNo;

    private Long userId;

    private Long deviceId;

    private BigDecimal amount;

    private Integer durationMinutes;

    private String washMode; // 洗衣模式: standard, quick, spin
    private String washModeName; // 洗涤模式名称

    private Long reservationId; // 关联预约ID

    private Long queueId; // 关联排队ID

    private Long deviceOwnerId; // 设备所有者ID

    private BigDecimal platformFee; // 平台服务费
    private BigDecimal ownerIncome; // 设备所有者收入

    // 支付相关字段
    private String paymentMethod; // 支付方式
    private String paymentChannel; // 支付渠道

    // 退款相关字段
    private BigDecimal refundAmount; // 退款金额
    private String refundReason; // 退款原因
    private LocalDateTime refundTime; // 退款时间

    // 完成状态相关字段
    private String completionStatus; // 完成状态
    private String failureReason; // 失败原因

    // 状态枚举：CREATED, PAID, RUNNING, PAUSED, FINISHED, CANCELLED, FAILED, REFUNDING, REFUNDED
    private String status;

    private LocalDateTime payTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

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
