package com.wash.iot.service;

import com.wash.iot.common.exception.BusinessException;
import com.wash.iot.dto.request.CreateOrderRequest;
import com.wash.iot.dto.response.OrderDetailResponse;
import com.wash.iot.entity.Device;
import com.wash.iot.entity.Order;
import com.wash.iot.entity.User;
import com.wash.iot.repository.DeviceRepository;
import com.wash.iot.repository.OrderRepository;
import com.wash.iot.repository.UserRepository;
import com.wash.iot.util.OrderNoGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 终端用户订单服务
 */
@Slf4j
@Service
public class ConsumerOrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private UserRepository userRepository;

    // 洗衣模式配置
    private static final java.util.Map<String, WashModeConfig> WASH_MODES = java.util.Map.of(
            "standard", new WashModeConfig("标准洗", 30, new BigDecimal("3.00")),
            "quick", new WashModeConfig("快洗", 15, new BigDecimal("2.00")),
            "spin", new WashModeConfig("脱水", 10, new BigDecimal("1.00"))
    );

    /**
     * 创建订单
     */
    @Transactional
    public OrderDetailResponse createOrder(Long userId, CreateOrderRequest request) {
        // 1. 验证设备
        Device device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new BusinessException("设备不存在"));

        if (!"IDLE".equals(device.getStatus())) {
            throw new BusinessException("设备当前不可用");
        }

        // 2. 获取洗衣模式配置
        WashModeConfig modeConfig = WASH_MODES.get(request.getWashMode());
        if (modeConfig == null) {
            throw new BusinessException("无效的洗衣模式");
        }

        // 3. 创建订单
        Order order = new Order();
        order.setOrderNo(OrderNoGenerator.generate());
        order.setUserId(userId);
        order.setDeviceId(device.getId());
        order.setWashMode(request.getWashMode());
        order.setDurationMinutes(modeConfig.duration);
        order.setAmount(modeConfig.price);
        order.setStatus("CREATED");
        
        // 设置设备所有者ID（用于收益分配）
        if (device.getOwnerId() != null) {
            order.setDeviceOwnerId(device.getOwnerId());
        }

        // 4. 如果使用余额支付
        boolean needPay = true;
        if (request.isUseBalance()) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException("用户不存在"));

            if (user.getBalance().compareTo(modeConfig.price) >= 0) {
                // 余额足够，直接扣款
                user.setBalance(user.getBalance().subtract(modeConfig.price));
                userRepository.save(user);

                order.setStatus("PAID");
                order.setPayTime(LocalDateTime.now());
                needPay = false;
                log.info("订单余额支付成功: orderNo={}, amount={}", order.getOrderNo(), modeConfig.price);
            }
        }

        order = orderRepository.save(order);

        return buildOrderResponse(order, device, modeConfig.name, needPay);
    }

    /**
     * 获取订单列表
     */
    public List<OrderDetailResponse> getOrders(Long userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Order> orders = orderRepository.findByUserId(userId, pageRequest);

        // 批量获取设备信息，解决N+1问题
        List<Long> deviceIds = orders.getContent().stream()
                .map(Order::getDeviceId)
                .distinct()
                .collect(Collectors.toList());
        
        java.util.Map<Long, Device> deviceMap = deviceRepository.findAllById(deviceIds).stream()
                .collect(Collectors.toMap(Device::getId, d -> d));

        return orders.getContent().stream()
                .map(order -> {
                    Device device = deviceMap.get(order.getDeviceId());
                    WashModeConfig modeConfig = WASH_MODES.getOrDefault(order.getWashMode(),
                            new WashModeConfig("未知", 0, BigDecimal.ZERO));
                    return buildOrderResponse(order, device, modeConfig.name, false);
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取订单详情
     */
    public OrderDetailResponse getOrderDetail(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("订单不存在"));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权查看此订单");
        }

        Device device = deviceRepository.findById(order.getDeviceId()).orElse(null);
        WashModeConfig modeConfig = WASH_MODES.getOrDefault(order.getWashMode(),
                new WashModeConfig("未知", 0, BigDecimal.ZERO));

        return buildOrderResponse(order, device, modeConfig.name, false);
    }

    /**
     * 模拟支付
     */
    @Transactional
    public OrderDetailResponse mockPay(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("订单不存在"));

        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }

        if (!"CREATED".equals(order.getStatus())) {
            throw new BusinessException("订单状态不正确");
        }

        order.setStatus("PAID");
        order.setPayTime(LocalDateTime.now());
        order = orderRepository.save(order);

        Device device = deviceRepository.findById(order.getDeviceId()).orElse(null);
        WashModeConfig modeConfig = WASH_MODES.getOrDefault(order.getWashMode(),
                new WashModeConfig("未知", 0, BigDecimal.ZERO));

        log.info("订单模拟支付成功: orderNo={}", order.getOrderNo());
        return buildOrderResponse(order, device, modeConfig.name, false);
    }

    /**
     * 构建订单响应
     */
    private OrderDetailResponse buildOrderResponse(Order order, Device device, String washModeName, boolean needPay) {
        OrderDetailResponse.OrderDetailResponseBuilder builder = OrderDetailResponse.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .deviceId(order.getDeviceId())
                .deviceSn(device != null ? device.getDeviceSn() : null)
                .deviceLocation(device != null ? device.getLocation() : null)
                .washMode(order.getWashMode())
                .washModeName(washModeName)
                .durationMinutes(order.getDurationMinutes())
                .amount(order.getAmount())
                .status(order.getStatus())
                .statusText(getStatusText(order.getStatus()))
                .createTime(order.getCreateTime())
                .payTime(order.getPayTime())
                .startTime(order.getStartTime())
                .endTime(order.getEndTime());

        // 如果需要支付，生成支付参数（模拟）
        if (needPay && "CREATED".equals(order.getStatus())) {
            builder.payParams(OrderDetailResponse.PayParams.builder()
                    .timeStamp(String.valueOf(System.currentTimeMillis() / 1000))
                    .nonceStr(java.util.UUID.randomUUID().toString().replace("-", ""))
                    .packageValue("prepay_id=mock_" + order.getOrderNo())
                    .signType("MD5")
                    .paySign("mock_sign")
                    .build());
        }

        return builder.build();
    }

    private String getStatusText(String status) {
        if (status == null) return "未知";
        switch (status) {
            case "CREATED": return "待支付";
            case "PAID": return "已支付";
            case "RUNNING": return "进行中";
            case "FINISHED": return "已完成";
            case "CANCELLED": return "已取消";
            case "FAILED": return "失败";
            case "REFUNDED": return "已退款";
            default: return "未知";
        }
    }

    /**
     * 洗衣模式配置
     */
    private static class WashModeConfig {
        String name;
        int duration;
        BigDecimal price;

        WashModeConfig(String name, int duration, BigDecimal price) {
            this.name = name;
            this.duration = duration;
            this.price = price;
        }
    }
}
