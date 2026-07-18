package com.wash.iot.service;

import com.wash.iot.entity.AdminDeviceBinding;
import com.wash.iot.entity.Device;
import com.wash.iot.entity.IncomeRecord;
import com.wash.iot.entity.Order;
import com.wash.iot.entity.PaymentTxn;
import com.wash.iot.repository.AdminDeviceBindingRepository;
import com.wash.iot.repository.DeviceRepository;
import com.wash.iot.repository.IncomeRecordRepository;
import com.wash.iot.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 收益记录服务
 */
@Slf4j
@Service
public class IncomeRecordService {

    @Autowired
    private IncomeRecordRepository incomeRecordRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private AdminDeviceBindingRepository adminDeviceBindingRepository;

    // 平台服务费率（默认10%）
    @Value("${platform.fee.rate:0.10}")
    private BigDecimal platformFeeRate;

    /**
     * 订单完成时创建收益记录
     * @param orderNo 订单号
     */
    @Transactional
    public void createIncomeRecordForOrder(String orderNo) {
        if (orderNo == null || orderNo.isEmpty()) {
            log.warn("订单号为空，无法创建收益记录");
            return;
        }

        try {
            Order order = orderRepository.findByOrderNo(orderNo).orElse(null);
            if (order == null) {
                log.warn("订单不存在: {}", orderNo);
                return;
            }

            // 检查是否已经创建过收益记录
            if (incomeRecordRepository.existsByOrderId(order.getId())) {
                log.info("订单 {} 的收益记录已存在，跳过创建", orderNo);
                return;
            }

            // 获取设备信息
            Device device = null;
            if (order.getDeviceId() != null) {
                device = deviceRepository.findById(order.getDeviceId()).orElse(null);
            }

            // 确定管理员用户ID
            Long adminUserId = null;
            
            // 优先使用订单中的设备所有者ID
            if (order.getDeviceOwnerId() != null) {
                adminUserId = order.getDeviceOwnerId();
            }
            // 其次使用设备的所有者ID
            else if (device != null && device.getOwnerId() != null) {
                adminUserId = device.getOwnerId();
            }
            // 最后查找设备绑定关系
            else if (order.getDeviceId() != null) {
                List<AdminDeviceBinding> bindings = adminDeviceBindingRepository.findByDeviceId(order.getDeviceId());
                if (!bindings.isEmpty()) {
                    adminUserId = bindings.get(0).getAdminUserId();
                }
            }

            // 如果找不到管理员，使用订单用户ID作为默认（自用场景）
            if (adminUserId == null) {
                adminUserId = order.getUserId();
                log.info("订单 {} 未找到设备管理员，使用订单用户ID: {}", orderNo, adminUserId);
            }

            // 计算收益
            BigDecimal orderAmount = order.getAmount();
            if (orderAmount == null) {
                orderAmount = BigDecimal.ZERO;
            }

            BigDecimal platformFee = orderAmount.multiply(platformFeeRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal netIncome = orderAmount.subtract(platformFee);

            // 创建收益记录
            IncomeRecord record = new IncomeRecord();
            record.setAdminUserId(adminUserId);
            record.setDeviceId(order.getDeviceId());
            record.setOrderId(order.getId());
            record.setOrderAmount(orderAmount);
            record.setPlatformFee(platformFee);
            record.setNetIncome(netIncome);
            record.setSettleStatus("PENDING");

            incomeRecordRepository.save(record);
            log.info("创建收益记录成功: 订单={}, 管理员={}, 金额={}, 净收入={}", 
                    orderNo, adminUserId, orderAmount, netIncome);

            // 更新订单中的收益信息
            order.setPlatformFee(platformFee);
            order.setOwnerIncome(netIncome);
            order.setDeviceOwnerId(adminUserId);
            orderRepository.save(order);

        } catch (Exception e) {
            log.error("创建收益记录失败: 订单={}", orderNo, e);
        }
    }

    /**
     * 根据设备SN和当前订单号创建收益记录
     */
    @Transactional
    public void createIncomeRecordForDevice(String deviceSn, String orderNo) {
        if (orderNo != null && !orderNo.isEmpty()) {
            createIncomeRecordForOrder(orderNo);
        } else {
            log.warn("设备 {} 没有关联订单号，无法创建收益记录", deviceSn);
        }
    }

    /**
     * 创建收益记录
     */
    public void createIncomeRecord(Order order, PaymentTxn paymentTxn) {
        try {
            IncomeRecord incomeRecord = new IncomeRecord();
            incomeRecord.setOrderId(order.getId());
            incomeRecord.setDeviceId(order.getDeviceId());
            incomeRecord.setAdminUserId(getDeviceOwnerId(order.getDeviceId()));
            incomeRecord.setOrderAmount(order.getAmount());
            incomeRecord.setNetIncome(order.getAmount().multiply(new BigDecimal("0.9"))); // 90%给设备所有者
            incomeRecord.setPlatformFee(order.getAmount().multiply(new BigDecimal("0.1"))); // 10%平台费
            incomeRecord.setSettleStatus("PENDING");

            incomeRecordRepository.save(incomeRecord);
            log.info("收益记录创建成功: orderNo={}, amount={}", order.getOrderNo(), order.getAmount());
        } catch (Exception e) {
            log.error("创建收益记录失败", e);
            throw new RuntimeException("创建收益记录失败: " + e.getMessage());
        }
    }

    /**
     * 处理退款
     */
    public void processRefund(Order order, BigDecimal refundAmount) {
        try {
            // 查找相关的收益记录
            List<IncomeRecord> incomeRecords = incomeRecordRepository.findByOrderId(order.getId());
            for (IncomeRecord record : incomeRecords) {
                if ("PENDING".equals(record.getSettleStatus())) {
                    // 更新收益记录状态
                    record.setSettleStatus("REFUNDED");
                    record.setSettleTime(LocalDateTime.now());
                    incomeRecordRepository.save(record);
                }
            }
            log.info("退款处理完成: orderNo={}, refundAmount={}", order.getOrderNo(), refundAmount);
        } catch (Exception e) {
            log.error("退款处理失败", e);
            throw new RuntimeException("退款处理失败: " + e.getMessage());
        }
    }

    private Long getDeviceOwnerId(Long deviceId) {
        return deviceRepository.findById(deviceId)
                .map(Device::getOwnerId)
                .orElse(1L); // 默认管理员ID
    }
}
