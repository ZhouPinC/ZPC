package com.wash.iot.enums;

/**
 * 排队状态枚举
 */
public enum QueueStatus {
    WAITING("排队等待中"),
    NOTIFIED("已通知"),
    EXPIRED("已过期"),
    CANCELLED("已取消"),
    USED("已使用");

    private final String description;

    QueueStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
