package com.wash.iot.application;

import com.wash.iot.common.exception.BusinessException;
import com.wash.iot.domain.order.model.OrderStatus;
import com.wash.iot.domain.order.service.OrderDomainService;
import com.wash.iot.entity.Device;
import com.wash.iot.entity.Order;
import com.wash.iot.entity.PaymentTxn;
import com.wash.iot.infrastructure.mqtt.MqttGateway;
import com.wash.iot.repository.DeviceRepository;
import com.wash.iot.repository.OrderRepository;
import com.wash.iot.repository.PaymentRepository;
import com.wash.iot.service.UserStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单应用服务
 * 负责编排订单相关的业务流程，包括支付回调处理、设备启动指令发送等
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderAppService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final DeviceRepository deviceRepository;
    private final OrderDomainService orderDomainService;
    private final MqttGateway mqttGateway;
    private final UserStatusService userStatusService;

    /**
     * 处理支付回调 (核心一致性链路)
     * @param orderNo 订单号
     * @param transactionId 支付平台交易ID
     * @param amount 支付金额
     * @throws BusinessException 业务异常
     */
    @Transactional(rollbackFor = Exception.class) // 确保 DB 操作原子性
    public void handlePaySuccess(String orderNo, String transactionId, BigDecimal amount) {
        // 1. 幂等性检查 (防止重复回调)
        Order order = orderRepository.findByOrderNo(orderNo)
            .orElseThrow(() -> new BusinessException("订单不存在"));
            
        // 已经处理过的订单直接返回，不重复处理
        if (!"CREATED".equals(order.getStatus())) {
            log.info("订单 {} 已经处理过，当前状态: {}", orderNo, order.getStatus());
            return; 
        }

        // 2. 记录支付流水 (审计用)
        paymentRepository.save(new PaymentTxn(orderNo, transactionId, amount));

        // 3. 执行状态变更 (调用领域服务校验规则)
        orderDomainService.transitionTo(order, OrderStatus.PAID);
        order.setPayTime(LocalDateTime.now());
        orderRepository.save(order);

        // 4. 发送设备启动指令
        // 查出设备信息
        Device device = deviceRepository.findById(order.getDeviceId())
            .orElseThrow(() -> new BusinessException("设备数据异常"));
        
        try {
            // 使用结构化指令，不再拼接字符串
            mqttGateway.sendStartCommand(
                device.getDeviceSn(),
                order.getOrderNo(),
                order.getDurationMinutes()
            );
            
            device.setStatus("STARTING");
            device.setLastHeartbeat(LocalDateTime.now());
            deviceRepository.save(device);
            log.info("设备启动指令已下发，设备状态已更新为STARTING，设备SN: {}", device.getDeviceSn());
        } catch (Exception e) {
            // 如果发送失败，事务会回滚，避免支付成功但设备未启动的不一致状态
            log.error("支付成功但设备启动失败: {}", e.getMessage(), e);
            // 抛出异常，触发事务回滚
            throw new BusinessException("支付成功但设备启动失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 模拟支付成功回调 (简化版，用于测试)
     * @param orderNo 订单号
     * @throws BusinessException 业务异常
     */
    @Transactional(rollbackFor = Exception.class)
    public void mockPaySuccess(String orderNo) {
        // 简化版，使用默认的交易ID和金额
        handlePaySuccess(orderNo, "mock-txn-" + orderNo, BigDecimal.valueOf(9.90));
    }
}
