package com.wash.iot.enums;

/**
 * 计费模式枚举
 */
public enum PricingMode {
    FREE("免费"),
    PER_USE("按次计费"),
    PER_MINUTE("按分钟计费");

    private final String description;

    PricingMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
