package com.wash.iot.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 订单号生成器
 */
public class OrderNoGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    /**
     * 生成订单号
     * 格式: yyyyMMddHHmmss + 4位序列号
     */
    public static String generate() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        int seq = SEQUENCE.incrementAndGet() % 10000;
        return timestamp + String.format("%04d", seq);
    }

    /**
     * 生成预约号
     * 格式: R + yyyyMMddHHmmss + 4位序列号
     */
    public static String generateReservationNo() {
        return "R" + generate();
    }
}
