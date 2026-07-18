package com.wash.iot.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "iot_user", indexes = {
        @Index(name = "idx_user_openid", columnList = "openId"),
        @Index(name = "idx_user_phone", columnList = "phone"),
        @Index(name = "idx_user_nickname", columnList = "nickName")
})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String openId; // 微信openId，唯一标识用户

    private String unionId; // 微信unionId（可选）
    
    @Column(unique = true)
    private String userIdentifier; // 用户唯一标识符，用于身份识别和关联数据

    private String nickName; // 用户昵称
    private String realName; // 真实姓名
    private String avatarUrl; // 用户头像URL

    private Integer gender; // 性别：0-未知，1-男，2-女

    private BigDecimal balance; // 账户余额
    private BigDecimal totalConsumption; // 总消费金额

    private Integer points; // 积分
    private Integer totalOrders; // 总订单数

    private String phone; // 手机号（可选）
    
    /**
     * 获取手机号（加密存储）
     */
    public String getPhone() {
        return phone;
    }
    
    /**
     * 设置手机号（加密存储）
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    /**
     * 获取脱敏后的手机号（用于日志和显示）
     */
    public String getMaskedPhone() {
        if (phone == null || phone.length() < 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private String role; // 用户角色: CONSUMER, ADMIN, SUPER_ADMIN

    private String status; // 账户状态: ACTIVE, DISABLED, INACTIVE

    private String sessionKey; // 微信session_key（用于解密数据）

    private LocalDateTime lastLoginTime; // 最后登录时间
    
    private LocalDateTime lastActiveTime; // 最后活跃时间

    private LocalDateTime tokenExpireTime; // token过期时间

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        if (this.gender == null) {
            this.gender = 0;
        }
        if (this.balance == null)
            this.balance = BigDecimal.ZERO;
        if (this.points == null)
            this.points = 0;
        if (this.role == null)
            this.role = "CONSUMER";
        if (this.status == null)
            this.status = "ACTIVE";
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
