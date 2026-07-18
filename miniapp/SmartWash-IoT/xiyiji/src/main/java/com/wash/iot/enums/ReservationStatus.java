package com.wash.iot.enums;

/**
 * 预约状态枚举
 */
public enum ReservationStatus {
    PENDING("待确认"),
    CONFIRMED("已确认"),
    CANCELLED("已取消"),
    EXPIRED("已过期"),
    USED("已使用");

    private final String description;

    ReservationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
