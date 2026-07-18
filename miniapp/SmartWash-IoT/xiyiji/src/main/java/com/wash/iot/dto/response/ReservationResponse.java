package com.wash.iot.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 预约响应
 */
@Data
@Builder
public class ReservationResponse {
    private Long id;
    private String reservationNo;
    private Long deviceId;
    private String deviceSn;
    private String deviceLocation;
    
    private LocalDate reservedDate;
    private LocalTime startTime;
    private LocalTime endTime;
    
    private String status;
    private String statusText;
    
    private LocalDateTime createTime;
}
