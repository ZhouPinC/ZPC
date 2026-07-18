package com.wash.iot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户-设备绑定关系
 */
@Data
@Entity
@Table(name = "iot_user_device_binding")
public class UserDeviceBinding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long deviceId;

    private LocalDateTime bindTime;

    private String status; // ACTIVE, REMOVED

    @PrePersist
    public void prePersist() {
        this.bindTime = LocalDateTime.now();
        if (this.status == null) this.status = "ACTIVE";
    }
}
