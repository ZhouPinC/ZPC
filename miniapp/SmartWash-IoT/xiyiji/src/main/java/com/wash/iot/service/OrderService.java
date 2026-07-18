package com.wash.iot.service;

import com.wash.iot.entity.Device;
import com.wash.iot.entity.Order;
import com.wash.iot.mqtt.MqttService;
import com.wash.iot.repository.DeviceRepository;
import com.wash.iot.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private MqttService mqttService;
    
    @Autowired
    private UserStatusService userStatusService;

    /**
     * 1. 创建订单 (下单)
     */
    @Transactional
    public Order createOrder(Long userId, String deviceSn) {
        // A. 检查设备是否存在
        Device device = deviceRepository.findByDeviceSn(deviceSn)
                .orElseThrow(() -> new RuntimeException("设备不存在"));

        // B. 检查设备状态 (核心防撞单逻辑)
        if (!"IDLE".equals(device.getStatus())) {
            throw new RuntimeException("设备忙碌或离线，无法下单");
        }

        // C. 生成订单
        Order order = new Order();
        order.setOrderNo(UUID.randomUUID().toString().replace("-", ""));
        order.setUserId(userId);
        order.setDeviceId(device.getId());
        order.setAmount(new BigDecimal("9.90")); // MVP固定价格
        order.setDurationMinutes(30);
        order.setStatus("CREATED");
        order.setCreateTime(LocalDateTime.now());
        
        return orderRepository.save(order);
    }

    /**
     * 2. 支付成功回调 (模拟)
     * 真实场景下，这个方法由微信支付回调接口调用
     * 注意：支付成功后只更新订单状态，不启动设备
     * 设备启动由前端调用 /devices/start 接口完成（带洗衣模式参数）
     */
    @Transactional
    public void payOrderSuccess(String orderNo) {
        // A. 查询订单
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        // B. 校验状态 (防止重复回调)
        if (!"CREATED".equals(order.getStatus())) {
            return; // 已经处理过，忽略
        }

        // C. 更新订单为已支付（不启动设备，等待前端调用startDevice）
        order.setStatus("PAID");
        order.setPayTime(LocalDateTime.now());
        orderRepository.save(order);
        
        log.info("订单支付成功: orderNo={}, 等待前端调用startDevice启动设备", orderNo);
    }
}
