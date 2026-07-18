package com.wash.iot.util;

import org.springframework.stereotype.Component;

import java.util.UUID;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 设备工具类，用于生成设备SN码和二维码
 */
@Component
public class DeviceUtils {

    public static String generateUniqueDeviceSn() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "WASH_" + dateStr + "_" + suffix;
    }

    /**
     * 生成设备唯一序列号 (SN码)
     * 格式: WASH_YYMMDD_XXXX (例如: WASH_251226_0001)
     * @param sequence 序列号，从1开始递增
     * @return 设备SN码
     */
    public static String generateDeviceSn(int sequence) {
        // 获取当前日期，格式化为 YYMMDD
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        // 格式化序列号，确保4位，不足前面补0
        String seqStr = String.format("%04d", sequence);
        return "WASH_" + dateStr + "_" + seqStr;
    }

    /**
     * 生成简单的设备SN码（用于现有设备批量更新）
     * 格式: WASH_XXX (例如: WASH_001)
     * @param sequence 序列号，从1开始递增
     * @return 设备SN码
     */
    public static String generateSimpleDeviceSn(int sequence) {
        return "WASH_" + String.format("%03d", sequence);
    }

    /**
     * 生成设备二维码内容
     * 格式: {"deviceSn":"设备SN码", "type":"WASHING_MACHINE"}
     * @param deviceSn 设备SN码
     * @return 二维码内容
     */
    public static String generateQrCodeContent(String deviceSn) {
        return String.format("{\"deviceSn\":\"%s\", \"type\":\"WASHING_MACHINE\"}", deviceSn);
    }

    /**
     * 生成设备二维码URL（假设使用外部服务生成二维码图片）
     * @param deviceSn 设备SN码
     * @return 二维码图片URL
     */
    public static String generateQrCodeUrl(String deviceSn) {
        return "/api/devices/" + deviceSn + "/qrcode";
    }

    /**
     * 验证设备SN码是否有效
     * @param deviceSn 设备SN码
     * @return 是否有效
     */
    public static boolean isValidDeviceSn(String deviceSn) {
        if (deviceSn == null || deviceSn.isEmpty()) {
            return false;
        }
        // 简单验证格式：以WASH_开头
        return deviceSn.startsWith("WASH_");
    }

    /**
     * 从二维码内容解析设备SN码
     * @param qrContent 二维码内容
     * @return 设备SN码
     */
    public static String parseDeviceSnFromQr(String qrContent) {
        if (qrContent == null || qrContent.isEmpty()) {
            return null;
        }

        String content = qrContent.trim();

        if (isValidDeviceSn(content)) {
            return content;
        }

        try {
            if (content.startsWith("{")) {
                com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSONObject.parseObject(content);
                String sn = json.getString("deviceSn");
                if (sn == null || sn.isBlank()) {
                    sn = json.getString("sn");
                }
                if (sn == null || sn.isBlank()) {
                    sn = json.getString("device_sn");
                }
                if (sn != null && isValidDeviceSn(sn.trim())) {
                    return sn.trim();
                }
            }
        } catch (Exception ignored) {
        }

        try {
            int qIndex = content.indexOf('?');
            String query = qIndex >= 0 ? content.substring(qIndex + 1) : content;
            for (String part : query.split("&")) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2) {
                    String key = kv[0];
                    String val = java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8);
                    if (("deviceSn".equalsIgnoreCase(key) || "sn".equalsIgnoreCase(key)) && isValidDeviceSn(val)) {
                        return val;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }
}
