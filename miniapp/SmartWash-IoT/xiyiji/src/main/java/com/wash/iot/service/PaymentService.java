package com.wash.iot.service;

import com.wash.iot.entity.Order;
import com.wash.iot.entity.PaymentTxn;
import com.wash.iot.entity.User;
import com.wash.iot.enums.PaymentStatus;
import com.wash.iot.repository.OrderRepository;
import com.wash.iot.repository.PaymentTxnRepository;
import com.wash.iot.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 支付服务类
 * 处理微信支付、余额支付、退款等业务逻辑
 */
@Slf4j
@Service
@Transactional
public class PaymentService {

    @Autowired
    private PaymentTxnRepository paymentTxnRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private IncomeRecordService incomeRecordService;

    @Autowired
    private WxPayService wxPayService;

    private static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.10"); // 10%平台费率

    /**
     * 创建支付订单
     */
    public PaymentResponse createPayment(Long orderId, String paymentMethod, String paymentChannel) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!"CREATED".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不允许支付");
        }

        // 创建支付交易记录
        PaymentTxn paymentTxn = createPaymentTransaction(order, paymentMethod, paymentChannel);

        try {
            if ("WECHAT".equals(paymentMethod)) {
                return createWechatPayment(paymentTxn);
            } else if ("BALANCE".equals(paymentMethod)) {
                return processBalancePayment(paymentTxn);
            } else {
                throw new RuntimeException("不支持的支付方式");
            }
        } catch (Exception e) {
            paymentTxn.setStatus("FAILED");
            paymentTxnRepository.save(paymentTxn);
            throw e;
        }
    }

    /**
     * 处理支付回调
     */
    public PaymentCallbackResult handlePaymentCallback(String transactionId, String status, String thirdPartyResponse) {
        PaymentTxn paymentTxn = paymentTxnRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("支付交易不存在"));

        try {
            if ("SUCCESS".equals(status)) {
                return processSuccessfulPayment(paymentTxn, thirdPartyResponse);
            } else {
                return processFailedPayment(paymentTxn, thirdPartyResponse);
            }
        } catch (Exception e) {
            log.error("处理支付回调失败: transactionId={}", transactionId, e);
            throw new RuntimeException("支付回调处理失败");
        }
    }

    /**
     * 申请退款
     */
    public RefundResponse requestRefund(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!"PAID".equals(order.getStatus()) && !"RUNNING".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不允许退款");
        }

        // 检查是否有支付记录
        List<PaymentTxn> paymentTxns = paymentTxnRepository.findByOrderNo(order.getOrderNo());
        if (paymentTxns.isEmpty()) {
            throw new RuntimeException("未找到支付记录");
        }
        PaymentTxn paymentTxn = paymentTxns.get(0);

        if (!"SUCCESS".equals(paymentTxn.getStatus())) {
            throw new RuntimeException("支付未成功，无法退款");
        }

        try {
            // 处理退款逻辑
            return processRefund(order, paymentTxn, reason);
        } catch (Exception e) {
            log.error("申请退款失败: orderId={}", orderId, e);
            throw new RuntimeException("退款申请失败: " + e.getMessage());
        }
    }

    /**
     * 创建支付交易记录
     */
    private PaymentTxn createPaymentTransaction(Order order, String paymentMethod, String paymentChannel) {
        PaymentTxn paymentTxn = new PaymentTxn();
        paymentTxn.setOrderNo(order.getOrderNo());
        paymentTxn.setTransactionId(generateTransactionId());
        paymentTxn.setAmount(order.getAmount());
        paymentTxn.setCurrency("CNY");
        paymentTxn.setPaymentMethod(paymentMethod);
        paymentTxn.setPaymentChannel(paymentChannel);
        paymentTxn.setStatus("PENDING");
        paymentTxn.setCreateTime(LocalDateTime.now());

        return paymentTxnRepository.save(paymentTxn);
    }

    /**
     * 创建微信支付
     */
    private PaymentResponse createWechatPayment(PaymentTxn paymentTxn) {
        try {
            // 调用微信支付API
            WxPayService.WxPayResponse wxPayResponse = wxPayService.createOrder(
                paymentTxn.getTransactionId(),
                paymentTxn.getAmount(),
                "共享洗衣机服务",
                paymentTxn.getOrderNo()
            );

            PaymentResponse response = new PaymentResponse();
            response.setTransactionId(paymentTxn.getTransactionId());
            response.setPaymentMethod("WECHAT");
            response.setStatus("PENDING");
            response.setPayUrl(wxPayResponse.getCodeUrl());
            response.setQrCode(wxPayResponse.getCodeUrl());

            log.info("微信支付订单创建成功: transactionId={}", paymentTxn.getTransactionId());
            return response;

        } catch (Exception e) {
            log.error("创建微信支付失败", e);
            throw new RuntimeException("创建微信支付失败: " + e.getMessage());
        }
    }

    /**
     * 处理余额支付
     */
    private PaymentResponse processBalancePayment(PaymentTxn paymentTxn) {
        Order order = orderRepository.findByOrderNo(paymentTxn.getOrderNo())
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        User user = userRepository.findById(order.getUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查余额是否足够
        if (user.getBalance().compareTo(paymentTxn.getAmount()) < 0) {
            throw new RuntimeException("余额不足");
        }

        // 扣除余额
        user.setBalance(user.getBalance().subtract(paymentTxn.getAmount()));
        userRepository.save(user);

        // 更新支付状态
        paymentTxn.setStatus("SUCCESS");
        paymentTxn.setUpdateTime(LocalDateTime.now());
        paymentTxnRepository.save(paymentTxn);

        // 更新订单状态
        order.setStatus("PAID");
        order.setPayTime(LocalDateTime.now());
        orderRepository.save(order);

        // 记录收益
        incomeRecordService.createIncomeRecord(order, paymentTxn);

        PaymentResponse response = new PaymentResponse();
        response.setTransactionId(paymentTxn.getTransactionId());
        response.setPaymentMethod("BALANCE");
        response.setStatus("SUCCESS");
        response.setMessage("余额支付成功");

        log.info("余额支付成功: transactionId={}, userId={}, amount={}",
                paymentTxn.getTransactionId(), user.getId(), paymentTxn.getAmount());
        return response;
    }

    /**
     * 处理支付成功
     */
    private PaymentCallbackResult processSuccessfulPayment(PaymentTxn paymentTxn, String thirdPartyResponse) {
        // 更新支付状态
        paymentTxn.setStatus("SUCCESS");
        paymentTxn.setThirdPartyResponse(thirdPartyResponse);
        paymentTxn.setUpdateTime(LocalDateTime.now());
        paymentTxnRepository.save(paymentTxn);

        // 更新订单状态
        Order order = orderRepository.findByOrderNo(paymentTxn.getOrderNo())
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        order.setStatus("PAID");
        order.setPayTime(LocalDateTime.now());
        orderRepository.save(order);

        // 记录收益
        incomeRecordService.createIncomeRecord(order, paymentTxn);

        PaymentCallbackResult result = new PaymentCallbackResult();
        result.setSuccess(true);
        result.setOrderNo(order.getOrderNo());
        result.setMessage("支付成功");

        log.info("支付成功处理完成: transactionId={}, orderNo={}",
                paymentTxn.getTransactionId(), order.getOrderNo());
        return result;
    }

    /**
     * 处理支付失败
     */
    private PaymentCallbackResult processFailedPayment(PaymentTxn paymentTxn, String thirdPartyResponse) {
        paymentTxn.setStatus("FAILED");
        paymentTxn.setThirdPartyResponse(thirdPartyResponse);
        paymentTxn.setUpdateTime(LocalDateTime.now());
        paymentTxnRepository.save(paymentTxn);

        PaymentCallbackResult result = new PaymentCallbackResult();
        result.setSuccess(false);
        result.setOrderNo(paymentTxn.getOrderNo());
        result.setMessage("支付失败");

        log.info("支付失败处理完成: transactionId={}", paymentTxn.getTransactionId());
        return result;
    }

    /**
     * 处理退款
     */
    private RefundResponse processRefund(Order order, PaymentTxn paymentTxn, String reason) {
        try {
            // 1. 如果是微信支付，调用微信退款API
            if ("WECHAT".equals(paymentTxn.getPaymentMethod())) {
                wxPayService.refund(paymentTxn.getTransactionId(), "refund" + System.currentTimeMillis(), paymentTxn.getAmount(), reason);
            }

            // 2. 如果是余额支付，退还到用户余额
            if ("BALANCE".equals(paymentTxn.getPaymentMethod())) {
                User user = userRepository.findById(order.getUserId())
                        .orElseThrow(() -> new RuntimeException("用户不存在"));
                user.setBalance(user.getBalance().add(paymentTxn.getAmount()));
                userRepository.save(user);
            }

            // 3. 更新订单状态
            order.setStatus("REFUNDED");
            order.setRefundAmount(paymentTxn.getAmount());
            order.setRefundReason(reason);
            order.setRefundTime(LocalDateTime.now());
            orderRepository.save(order);

            // 4. 更新支付交易状态
            paymentTxn.setStatus("REFUNDED");
            paymentTxn.setUpdateTime(LocalDateTime.now());
            paymentTxnRepository.save(paymentTxn);

            // 5. 处理收益记录退款
            incomeRecordService.processRefund(order, paymentTxn.getAmount());

            RefundResponse response = new RefundResponse();
            response.setSuccess(true);
            response.setRefundAmount(paymentTxn.getAmount());
            response.setMessage("退款成功");

            log.info("退款成功: orderId={}, amount={}", order.getId(), paymentTxn.getAmount());
            return response;

        } catch (Exception e) {
            log.error("退款处理失败: orderId={}", order.getId(), e);
            throw new RuntimeException("退款处理失败: " + e.getMessage());
        }
    }

    /**
     * 生成交易ID
     */
    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);
    }

    // 响应对象定义
    public static class PaymentResponse {
        private String transactionId;
        private String paymentMethod;
        private String status;
        private String payUrl;
        private String qrCode;
        private String message;

        // getters and setters
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getPayUrl() { return payUrl; }
        public void setPayUrl(String payUrl) { this.payUrl = payUrl; }
        public String getQrCode() { return qrCode; }
        public void setQrCode(String qrCode) { this.qrCode = qrCode; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class PaymentCallbackResult {
        private boolean success;
        private String orderNo;
        private String message;

        // getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getOrderNo() { return orderNo; }
        public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class RefundResponse {
        private boolean success;
        private BigDecimal refundAmount;
        private String message;

        // getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public BigDecimal getRefundAmount() { return refundAmount; }
        public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    // 微信支付响应对象
    private static class WxPayResponse {
        private String codeUrl;

        public String getCodeUrl() { return codeUrl; }
        public void setCodeUrl(String codeUrl) { this.codeUrl = codeUrl; }
    }
}