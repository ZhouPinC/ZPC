package com.wash.iot.enums;

/**
 * 用户角色枚举
 */
public enum UserRole {
    CONSUMER("终端用户"),
    ADMIN("设备管理者"),
    SUPER_ADMIN("超级管理员");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
