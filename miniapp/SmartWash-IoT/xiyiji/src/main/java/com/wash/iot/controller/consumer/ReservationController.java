package com.wash.iot.controller.consumer;

import com.wash.iot.common.response.ApiResponse;
import com.wash.iot.dto.request.CreateReservationRequest;
import com.wash.iot.dto.response.ReservationResponse;
import com.wash.iot.dto.response.TimeSlotResponse;
import com.wash.iot.enums.UserRole;
import com.wash.iot.security.JwtAuthenticationFilter;
import com.wash.iot.security.RoleRequired;
import com.wash.iot.service.ReservationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 预约控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/consumer")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    /**
     * 获取可预约时段
     * GET /api/v1/consumer/devices/{deviceId}/slots
     */
    @GetMapping("/devices/{deviceId}/slots")
    @RoleRequired({UserRole.CONSUMER, UserRole.ADMIN, UserRole.SUPER_ADMIN})
    public ApiResponse<List<TimeSlotResponse>> getAvailableSlots(
            @PathVariable Long deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<TimeSlotResponse> slots = reservationService.getAvailableSlots(deviceId, date);
        return ApiResponse.success(slots);
    }

    /**
     * 创建预约
     * POST /api/v1/consumer/reservations
     */
    @PostMapping("/reservations")
    @RoleRequired({UserRole.CONSUMER, UserRole.ADMIN, UserRole.SUPER_ADMIN})
    public ApiResponse<ReservationResponse> createReservation(@RequestBody CreateReservationRequest request) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId();
        ReservationResponse response = reservationService.createReservation(userId, request);
        return ApiResponse.success(response);
    }

    /**
     * 获取我的预约列表
     * GET /api/v1/consumer/reservations
     */
    @GetMapping("/reservations")
    @RoleRequired({UserRole.CONSUMER, UserRole.ADMIN, UserRole.SUPER_ADMIN})
    public ApiResponse<List<ReservationResponse>> getReservations() {
        Long userId = JwtAuthenticationFilter.getCurrentUserId();
        List<ReservationResponse> reservations = reservationService.getUserReservations(userId);
        return ApiResponse.success(reservations);
    }

    /**
     * 取消预约
     * DELETE /api/v1/consumer/reservations/{reservationId}
     */
    @DeleteMapping("/reservations/{reservationId}")
    @RoleRequired({UserRole.CONSUMER, UserRole.ADMIN, UserRole.SUPER_ADMIN})
    public ApiResponse<Void> cancelReservation(@PathVariable Long reservationId) {
        Long userId = JwtAuthenticationFilter.getCurrentUserId();
        reservationService.cancelReservation(userId, reservationId);
        return ApiResponse.success();
    }
}
