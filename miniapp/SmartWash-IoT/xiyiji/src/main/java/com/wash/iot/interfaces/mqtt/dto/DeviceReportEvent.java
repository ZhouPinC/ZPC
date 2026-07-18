package com.wash.iot.interfaces.mqtt.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备上报事件
 */
@Data
public class DeviceReportEvent {
    private String deviceSn;
    private String status;
    private Integer remainingTime;
    private Integer errorCode;
    private String workMode;
    private LocalDateTime reportTime;
    private BigDecimal temperature;
    private Double humidity;
    private Integer lockStatus;
    private String rawData;

    // 兼容旧代码访问器
    public Integer getRemainSeconds() {
        return this.remainingTime;
    }

    public String getType() {
        // 优先返回 workMode（业务上作为类型），若为空退回 status
        return this.workMode != null ? this.workMode : this.status;
    }

    public String getOrderNo() {
        // 目前上报中没有明确定义 orderNo，尝试从 rawData 中解析或返回 null
        return null;
    }

    public String getMessage() {
        return this.rawData;
    }

    // 保留 humidity/getTemperature 等 Lombok 生成的方法（temperature 类型已调整为 BigDecimal）
}