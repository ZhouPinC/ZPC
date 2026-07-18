package com.wash.iot.controller;

import com.wash.iot.entity.Order;
import com.wash.iot.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // 1. 下单接口
    @PostMapping("/create")
    public Order create(@RequestParam Long userId, @RequestParam String deviceSn) {
        return orderService.createOrder(userId, deviceSn);
    }

    // 2. 模拟支付成功回调 (为了测试方便，直接GET或POST都行)
    // 真实项目中这里是微信的回调 notify_url
    @PostMapping("/pay/mock")
    public String mockPay(@RequestParam String orderNo) {
        orderService.payOrderSuccess(orderNo);
        return "支付成功，已下发启动指令";
    }
}
