package com.wash.iot.dto.request;

import lombok.Data;

/**
 * 创建订单请求
 */
@Data
public class CreateOrderRequest {
    private Long deviceId;
    private String washMode;      // 洗衣模式: standard, quick, spin
    private Long reservationId;   // 预约ID（可选）
    private boolean useBalance;   // 是否使用余额支付
}
