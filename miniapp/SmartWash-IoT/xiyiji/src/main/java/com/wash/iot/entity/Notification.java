package com.wash.iot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 消息通知
 */
@Data
@Entity
@Table(name = "iot_notification")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String type; // RESERVATION_REMIND, WASH_COMPLETE, QUEUE_TURN, DEVICE_FAULT, SYSTEM

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Long relatedId;

    private Boolean isRead;

    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        if (this.isRead == null) this.isRead = false;
    }
}
