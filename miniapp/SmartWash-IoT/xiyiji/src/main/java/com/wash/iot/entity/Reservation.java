package com.wash.iot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 预约记录
 */
@Data
@Entity
@Table(name = "iot_reservation")
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String reservationNo;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long deviceId;

    @Column(nullable = false)
    private LocalDate reservedDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    private LocalDateTime reservationTime; // 预约时间（日期+时间）
    private Integer durationMinutes; // 持续时间(分钟)
    private String washMode; // 洗涤模式

    private String status; // PENDING, CONFIRMED, CANCELLED, EXPIRED, USED

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime cancelTime; // 取消时间
    private String cancelReason; // 取消原因

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        if (this.status == null) this.status = "PENDING";
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
