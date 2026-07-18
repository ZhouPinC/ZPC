package com.wash.iot.enums;

/**
 * 设备状态枚举
 */
public enum DeviceStatus {
    OFFLINE("离线"),
    IDLE("空闲"),
    STARTING("启动中"),
    RUNNING("运行中"),
    PAUSED("已暂停"),
    FAULT("故障"),
    FINISHED("已完成"),
    RESERVED("已预约");

    private final String description;

    DeviceStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
