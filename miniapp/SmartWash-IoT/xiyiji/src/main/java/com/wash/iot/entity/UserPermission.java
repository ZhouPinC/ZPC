package com.wash.iot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 用户权限
 */
@Data
@Entity
@Table(name = "iot_user_permission")
public class UserPermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long deviceId;

    @Column(nullable = false)
    private Long grantedBy;

    private String permissionType; // UNLIMITED, TIME_RANGE, COUNT_LIMIT

    private LocalTime startTime;

    private LocalTime endTime;

    private Integer remainingCount;

    private LocalDate expireDate;

    private String status; // ACTIVE, EXPIRED, REVOKED

    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        if (this.status == null) this.status = "ACTIVE";
        if (this.permissionType == null) this.permissionType = "UNLIMITED";
    }
}
