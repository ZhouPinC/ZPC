package com.wash.iot.dto.request;

import lombok.Data;

/**
 * 设备启动请求
 */
@Data
public class DeviceStartRequest {
    private String washMode;       // 洗衣模式
    private String washModeName;   // 洗衣模式名称
    private Integer duration;      // 时长（分钟）
    private String orderNo;        // 订单号
}
