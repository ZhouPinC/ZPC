package com.wash.iot.domain.order.model;

/**
 * 订单状态枚举
 */
public enum OrderStatus {
    CREATED("已创建"),
    PAID("已支付"),
    RUNNING("运行中"),
    FINISHED("已完成"),
    CANCELLED("已取消"),
    REFUNDED("已退款"),
    REFUNDING("退款中"),
    FAILED("失败"),
    ABNORMAL("异常");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}