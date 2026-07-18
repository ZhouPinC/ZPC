package com.wash.iot.controller.consumer;

import com.wash.iot.common.response.ApiResponse;
import com.wash.iot.dto.request.CreateOrderRequest;
import com.wash.iot.dto.response.OrderDetailResponse;
import com.wash.iot.enums.UserRole;
import com.wash.iot.security.JwtAuthenticationFilter;
import com.wash.iot.security.RoleRequired;
import com.wash.iot.service.ConsumerOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 终端用户订单控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/consumer/orders")
public class ConsumerOrderController {

    @Autowired
    private ConsumerOrderService orderService;

    /**
     * 创建订单
     * POST /api/v1/consumer/orders
     */
    @PostMapping
    @RoleRequired({UserRole.CONSUMER, UserRole.ADMIN, UserRole.SUPER_ADMIN})
    public ApiResponse<OrderDetailResponse> createOrder(@RequestBody CreateOrderRequest request) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId();
        OrderDetailResponse response = orderService.createOrder(userId, request);
        return ApiResponse.success(response);
    }

    /**
     * 获取订单列表
     * GET /api/v1/consumer/orders
     */
    @GetMapping
    @RoleRequired({UserRole.CONSUMER, UserRole.ADMIN, UserRole.SUPER_ADMIN})
    public ApiResponse<List<OrderDetailResponse>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId();
        List<OrderDetailResponse> orders = orderService.getOrders(userId, page, size);
        return ApiResponse.success(orders);
    }

    /**
     * 获取订单详情
     * GET /api/v1/consumer/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    @RoleRequired({UserRole.CONSUMER, UserRole.ADMIN, UserRole.SUPER_ADMIN})
    public ApiResponse<OrderDetailResponse> getOrderDetail(@PathVariable Long orderId) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId();
        OrderDetailResponse response = orderService.getOrderDetail(userId, orderId);
        return ApiResponse.success(response);
    }

    /**
     * 发起支付（模拟）
     * POST /api/v1/consumer/orders/{orderId}/pay
     */
    @PostMapping("/{orderId}/pay")
    @RoleRequired({UserRole.CONSUMER, UserRole.ADMIN, UserRole.SUPER_ADMIN})
    public ApiResponse<OrderDetailResponse> payOrder(@PathVariable Long orderId) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId();
        OrderDetailResponse response = orderService.mockPay(userId, orderId);
        return ApiResponse.success(response);
    }
}
