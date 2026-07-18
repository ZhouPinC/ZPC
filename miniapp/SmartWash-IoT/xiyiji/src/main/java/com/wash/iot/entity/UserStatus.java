package com.wash.iot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "iot_user_status")
public class UserStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long userId; // 关联用户表

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device; // 关联设备表，用户当前使用的设备

    // 状态枚举：OFFLINE, ONLINE, USING_DEVICE
    private String status;

    private LocalDateTime lastActivity; // 最后活动时间
    private LocalDateTime lastUpdate; // 状态最后更新时间
    private String updateReason; // 状态更新原因

    @PrePersist
    public void prePersist() {
        this.lastActivity = LocalDateTime.now();
        this.lastUpdate = LocalDateTime.now();
        if (this.status == null) this.status = "OFFLINE";
    }

    @PreUpdate
    public void preUpdate() {
        this.lastUpdate = LocalDateTime.now();
    }
}