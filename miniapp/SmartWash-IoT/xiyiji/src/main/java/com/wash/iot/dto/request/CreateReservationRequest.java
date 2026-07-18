package com.wash.iot.dto.request;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 创建预约请求
 */
@Data
public class CreateReservationRequest {
    private Long deviceId;
    private LocalDate reservedDate;
    private LocalTime startTime;
    private LocalTime endTime;
}
