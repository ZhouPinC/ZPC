package com.wash.iot.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

/**
 * 可预约时段响应
 */
@Data
@Builder
public class TimeSlotResponse {
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean available;
    private String reason; // 不可用原因
}
