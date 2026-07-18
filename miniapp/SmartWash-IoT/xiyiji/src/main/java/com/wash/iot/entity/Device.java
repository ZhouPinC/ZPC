package com.wash.iot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "iot_device")
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String deviceSn; // 设备唯一序列号，如 "WASH_001"

    private String qrCodeContent; // 二维码内容
    private String qrCodeUrl; // 二维码图片URL
    
    private String name; // 设备名称
    private String location; // 设备位置
    private String model; // 设备型号
    private String manufacturer; // 制造商

    // 状态枚举：OFFLINE, IDLE, RUNNING, FAULT
    private String status;

    private Long ownerId; // 设备所有者ID
    private LocalDateTime lastHeartbeat; // 最后通信时间
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 设备运行状态字段
    private String washMode; // 当前洗涤模式
    private String washModeName; // 洗涤模式名称
    private Integer remainSeconds; // 剩余时间(秒)
    private Integer totalDuration; // 总时长(分钟)
    private LocalDateTime workStartTime; // 工作开始时间
    private LocalDateTime estimatedEndTime; // 预计结束时间
    private Long currentOrderId; // 当前订单ID
    private String currentOrderNo; // 当前订单号
    private Long currentUserId; // 当前用户ID
    private Integer currentQueueLength; // 当前排队长度

    // 设备定价字段
    private String pricingMode; // 定价模式
    private BigDecimal pricePerUse; // 每次使用价格
    private BigDecimal pricePerMinute; // 每分钟价格

    // 统计字段
    private Integer totalOrders; // 总订单数

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        if (this.status == null) this.status = "OFFLINE";
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}