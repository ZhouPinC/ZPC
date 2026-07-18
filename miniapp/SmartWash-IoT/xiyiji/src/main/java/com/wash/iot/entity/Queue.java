package com.wash.iot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 排队记录
 */
@Data
@Entity
@Table(name = "iot_queue")
public class Queue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long deviceId;

    private Integer queuePosition;

    private Integer estimatedWaitMinutes;

    private String washMode; // 洗涤模式
    private Integer durationMinutes; // 持续时间(分钟)
    private LocalDateTime serveTime; // 服务时间

    private String status; // WAITING, NOTIFIED, EXPIRED, CANCELLED

    private LocalDateTime joinTime;
    private LocalDateTime createTime;
    private LocalDateTime notifyTime;
    private LocalDateTime expireTime;
    private LocalDateTime cancelTime;
    private String cancelReason; // 取消原因

    @PrePersist
    public void prePersist() {
        this.joinTime = LocalDateTime.now();
        this.createTime = LocalDateTime.now();
        if (this.status == null) this.status = "WAITING";
    }

    @PreUpdate
    public void preUpdate() {
        // 更新时间字段
    }
}
