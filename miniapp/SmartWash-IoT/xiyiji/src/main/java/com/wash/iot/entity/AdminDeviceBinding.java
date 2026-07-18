package com.wash.iot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 管理者-设备绑定关系
 */
@Data
@Entity
@Table(name = "iot_admin_device")
public class AdminDeviceBinding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long adminUserId;

    @Column(nullable = false)
    private Long deviceId;

    private LocalDateTime bindTime;

    @PrePersist
    public void prePersist() {
        this.bindTime = LocalDateTime.now();
    }
}
