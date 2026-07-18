package com.wash.iot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "iot_user_status_history")
public class UserStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId; // 关联用户表

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device; // 关联设备表

    private String oldStatus; // 变更前状态
    private String newStatus; // 变更后状态
    private LocalDateTime changeTime; // 状态变更时间
    private String changeReason; // 状态变更原因
    private String relatedOrderNo; // 关联订单号

    @PrePersist
    public void prePersist() {
        this.changeTime = LocalDateTime.now();
    }
}