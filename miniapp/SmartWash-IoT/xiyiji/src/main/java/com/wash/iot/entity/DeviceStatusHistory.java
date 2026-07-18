package com.wash.iot.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备状态历史实体
 */
@Data
@Entity
@Table(name = "iot_device_status_history")
public class DeviceStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "old_status", length = 32)
    private String oldStatus;

    @Column(name = "new_status", length = 32, nullable = false)
    private String newStatus;

    @Column(name = "status_type", length = 32)
    private String statusType;

    @Column(name = "order_no", length = 32)
    private String orderNo;

    @Column(name = "temperature", precision = 5, scale = 2)
    private BigDecimal temperature;

    @Column(name = "remain_seconds")
    private Integer remainSeconds;

    @Column(name = "error_code", length = 32)
    private String errorCode;

    @Column(length = 255)
    private String message;

    @Column(name = "change_time", nullable = false)
    private LocalDateTime changeTime;

    @PrePersist
    public void prePersist() {
        if (this.changeTime == null) {
            this.changeTime = LocalDateTime.now();
        }
    }
}