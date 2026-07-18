package com.wash.iot.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知响应
 */
@Data
@Builder
public class NotificationResponse {
    private Long id;
    private String type;
    private String title;
    private String content;
    private Long relatedId;
    private Boolean isRead;
    private LocalDateTime createTime;
}
