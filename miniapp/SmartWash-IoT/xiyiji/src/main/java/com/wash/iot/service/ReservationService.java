package com.wash.iot.service;

import com.wash.iot.common.exception.BusinessException;
import com.wash.iot.dto.request.CreateReservationRequest;
import com.wash.iot.dto.response.ReservationResponse;
import com.wash.iot.dto.response.TimeSlotResponse;
import com.wash.iot.entity.Device;
import com.wash.iot.entity.Reservation;
import com.wash.iot.repository.DeviceRepository;
import com.wash.iot.repository.ReservationRepository;
import com.wash.iot.util.OrderNoGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 预约服务
 */
@Slf4j
@Service
public class ReservationService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    // 预约规则配置
    private static final int MAX_ADVANCE_DAYS = 7;           // 最多提前7天预约
    private static final int MIN_ADVANCE_MINUTES = 30;       // 最少提前30分钟预约
    private static final int SLOT_DURATION_MINUTES = 30;     // 每个时段30分钟
    private static final int MAX_ACTIVE_RESERVATIONS = 3;    // 每用户最多3个有效预约
    private static final LocalTime DAY_START = LocalTime.of(6, 0);   // 每天6点开始
    private static final LocalTime DAY_END = LocalTime.of(23, 0);    // 每天23点结束

    /**
     * 获取可预约时段
     */
    public List<TimeSlotResponse> getAvailableSlots(Long deviceId, LocalDate date) {
        // 验证日期
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            throw new BusinessException("不能预约过去的日期");
        }
        if (date.isAfter(today.plusDays(MAX_ADVANCE_DAYS))) {
            throw new BusinessException("最多只能提前" + MAX_ADVANCE_DAYS + "天预约");
        }

        // 获取该日期已有的预约
        List<Reservation> existingReservations = reservationRepository
                .findByDeviceIdAndReservedDateAndStatusIn(deviceId, date, 
                        Arrays.asList("PENDING", "CONFIRMED"));

        // 生成时段列表
        List<TimeSlotResponse> slots = new ArrayList<>();
        LocalTime currentTime = DAY_START;
        LocalTime now = LocalTime.now();

        while (currentTime.isBefore(DAY_END)) {
            LocalTime endTime = currentTime.plusMinutes(SLOT_DURATION_MINUTES);
            
            boolean available = true;
            String reason = null;

            // 检查是否是今天且已过时
            if (date.equals(today)) {
                if (currentTime.isBefore(now.plusMinutes(MIN_ADVANCE_MINUTES))) {
                    available = false;
                    reason = "时段已过或太近";
                }
            }

            // 检查是否与已有预约冲突
            if (available) {
                final LocalTime slotStart = currentTime;
                final LocalTime slotEnd = endTime;
                boolean hasConflict = existingReservations.stream()
                        .anyMatch(r -> isTimeOverlap(slotStart, slotEnd, r.getStartTime(), r.getEndTime()));
                if (hasConflict) {
                    available = false;
                    reason = "已被预约";
                }
            }

            slots.add(TimeSlotResponse.builder()
                    .startTime(currentTime)
                    .endTime(endTime)
                    .available(available)
                    .reason(reason)
                    .build());

            currentTime = endTime;
        }

        return slots;
    }

    /**
     * 创建预约
     */
    @Transactional
    public ReservationResponse createReservation(Long userId, CreateReservationRequest request) {
        // 1. 验证设备
        Device device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new BusinessException("设备不存在"));

        // 2. 验证用户预约数量
        List<Reservation> activeReservations = reservationRepository
                .findByUserIdAndStatusIn(userId, Arrays.asList("PENDING", "CONFIRMED"));
        if (activeReservations.size() >= MAX_ACTIVE_RESERVATIONS) {
            throw new BusinessException("您已有" + MAX_ACTIVE_RESERVATIONS + "个有效预约，请先使用或取消");
        }

        // 3. 验证时段是否可用
        LocalDate date = request.getReservedDate();
        LocalTime startTime = request.getStartTime();
        LocalTime endTime = request.getEndTime();

        List<Reservation> existingReservations = reservationRepository
                .findByDeviceIdAndReservedDateAndStatusIn(request.getDeviceId(), date,
                        Arrays.asList("PENDING", "CONFIRMED"));

        boolean hasConflict = existingReservations.stream()
                .anyMatch(r -> isTimeOverlap(startTime, endTime, r.getStartTime(), r.getEndTime()));
        if (hasConflict) {
            throw new BusinessException("该时段已被预约");
        }

        // 4. 创建预约
        Reservation reservation = new Reservation();
        reservation.setReservationNo(OrderNoGenerator.generateReservationNo());
        reservation.setUserId(userId);
        reservation.setDeviceId(request.getDeviceId());
        reservation.setReservedDate(date);
        reservation.setStartTime(startTime);
        reservation.setEndTime(endTime);
        reservation.setStatus("CONFIRMED");

        reservation = reservationRepository.save(reservation);
        log.info("预约创建成功: reservationNo={}, userId={}, deviceId={}", 
                reservation.getReservationNo(), userId, request.getDeviceId());

        return buildReservationResponse(reservation, device);
    }

    /**
     * 获取用户预约列表
     */
    public List<ReservationResponse> getUserReservations(Long userId) {
        List<Reservation> reservations = reservationRepository.findByUserIdOrderByCreateTimeDesc(userId);
        
        return reservations.stream()
                .map(r -> {
                    Device device = deviceRepository.findById(r.getDeviceId()).orElse(null);
                    return buildReservationResponse(r, device);
                })
                .collect(Collectors.toList());
    }

    /**
     * 取消预约
     */
    @Transactional
    public void cancelReservation(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException("预约不存在"));

        if (!reservation.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此预约");
        }

        if (!"PENDING".equals(reservation.getStatus()) && !"CONFIRMED".equals(reservation.getStatus())) {
            throw new BusinessException("该预约无法取消");
        }

        reservation.setStatus("CANCELLED");
        reservationRepository.save(reservation);
        log.info("预约已取消: reservationNo={}", reservation.getReservationNo());
    }

    /**
     * 检查时间是否重叠
     */
    private boolean isTimeOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    /**
     * 构建预约响应
     */
    private ReservationResponse buildReservationResponse(Reservation reservation, Device device) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .reservationNo(reservation.getReservationNo())
                .deviceId(reservation.getDeviceId())
                .deviceSn(device != null ? device.getDeviceSn() : null)
                .deviceLocation(device != null ? device.getLocation() : null)
                .reservedDate(reservation.getReservedDate())
                .startTime(reservation.getStartTime())
                .endTime(reservation.getEndTime())
                .status(reservation.getStatus())
                .statusText(getStatusText(reservation.getStatus()))
                .createTime(reservation.getCreateTime())
                .build();
    }

    private String getStatusText(String status) {
        if (status == null) return "未知";
        switch (status) {
            case "PENDING": return "待确认";
            case "CONFIRMED": return "已确认";
            case "CANCELLED": return "已取消";
            case "EXPIRED": return "已过期";
            case "USED": return "已使用";
            default: return "未知";
        }
    }
}
