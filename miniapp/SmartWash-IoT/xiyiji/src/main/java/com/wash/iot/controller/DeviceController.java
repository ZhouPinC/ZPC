package com.wash.iot.controller;

import com.wash.iot.entity.Device;
import com.wash.iot.entity.Order;
import com.wash.iot.mqtt.MqttService;
import com.wash.iot.repository.DeviceRepository;
import com.wash.iot.repository.OrderRepository;
import com.wash.iot.service.IncomeRecordService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    @Autowired
    private DeviceRepository deviceRepository;
    
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MqttService mqttService;

    @Autowired
    private IncomeRecordService incomeRecordService;

    // 1. 注册新设备 (初始化用)
    @PostMapping("/register")
    public Device register(@RequestParam(required = false) String deviceSn) {
        Device device = new Device();
        
        // 如果没有提供deviceSn，则自动生成
        if (deviceSn == null || deviceSn.isEmpty()) {
            // 获取当前设备数量，用于生成唯一SN码
            long count = deviceRepository.count();
            deviceSn = com.wash.iot.util.DeviceUtils.generateDeviceSn((int) count + 1);
        }
        
        device.setDeviceSn(deviceSn);
        device.setName(deviceSn);
        device.setStatus("IDLE"); // 初始设为空闲
        
        // 生成二维码
        String qrCodeContent = com.wash.iot.util.DeviceUtils.generateQrCodeContent(deviceSn);
        device.setQrCodeContent(qrCodeContent);
        
        String qrCodeUrl = com.wash.iot.util.DeviceUtils.generateQrCodeUrl(deviceSn);
        device.setQrCodeUrl(qrCodeUrl);
        
        return deviceRepository.save(device);
    }

    @GetMapping(value = "/{deviceSn}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public @ResponseBody byte[] getDeviceQrCode(@PathVariable String deviceSn) {
        Device device = deviceRepository.findByDeviceSn(deviceSn)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "设备不存在"));

        String content = device.getQrCodeContent();
        if (content == null || content.isBlank()) {
            content = com.wash.iot.util.DeviceUtils.generateQrCodeContent(deviceSn);
        }

        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 320, 320, hints);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "二维码生成失败");
        }
    }

    // 2. 查询所有设备列表（同时检查并自动完成超时的设备）
    @GetMapping("/list")
    public List<Map<String, Object>> list() {
        return deviceRepository.findAll().stream()
                .map(this::buildLegacyDeviceListItem)
                .collect(Collectors.toList());
    }

    private Map<String, Object> buildLegacyDeviceListItem(Device device) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", device.getId());
        item.put("deviceSn", device.getDeviceSn());
        item.put("name", device.getName());
        item.put("location", device.getLocation());
        item.put("status", device.getStatus());
        item.put("lastHeartbeat", device.getLastHeartbeat());

        String status = device.getStatus();
        boolean isIdle = "IDLE".equals(status) && device.getCurrentOrderId() == null && device.getCurrentOrderNo() == null;

        if (isIdle) {
            item.put("washMode", null);
            item.put("washModeName", null);
            item.put("totalDuration", null);
            item.put("remainSeconds", 0);
            item.put("workStartTime", null);
            item.put("estimatedEndTime", null);
            item.put("currentOrderId", null);
            item.put("currentOrderNo", null);
            item.put("currentUserId", null);
            return item;
        }

        item.put("washMode", device.getWashMode());
        item.put("washModeName", device.getWashModeName());
        item.put("totalDuration", device.getTotalDuration());
        item.put("remainSeconds", device.getRemainSeconds());
        item.put("workStartTime", device.getWorkStartTime());
        item.put("estimatedEndTime", device.getEstimatedEndTime());
        item.put("currentOrderId", device.getCurrentOrderId());
        item.put("currentOrderNo", device.getCurrentOrderNo());
        item.put("currentUserId", device.getCurrentUserId());
        return item;
    }

    // 3. 测试接口：远程强行启动设备 (模拟下单后的操作)
    @PostMapping("/debug/start")
    public String debugStart(@RequestParam String deviceSn) {
        try {
            // 构造启动指令 JSON
            String cmd = "{\"cmd\":\"START\", \"duration\":30}";
            mqttService.sendCommand(deviceSn, cmd);
            return "指令已发送: " + cmd;
        } catch (Exception e) {
            return "发送失败: " + e.getMessage();
        }
    }
    
    // 4. 系统健康检查接口：检查服务器和MQTT连接状态
    @GetMapping("/health")
    public java.util.Map<String, Object> healthCheck() {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("serverStatus", "OK");
        
        try {
            result.put("mqttConnected", mqttService.isConnected());
        } catch (Exception e) {
            result.put("mqttConnected", false);
            result.put("mqttError", e.getMessage());
        }
        
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
    
    // 5. 设备控制接口：处理各种设备指令
    @PostMapping("/control")
    public String controlDevice(@RequestParam String deviceSn, @RequestParam String command) {
        try {
            // 构造指令 JSON
            String cmd = String.format("{\"cmd\":\"%s\"}", command);
            mqttService.sendCommand(deviceSn, cmd);
            
            // 立即更新设备状态到数据库
            Device device = deviceRepository.findByDeviceSn(deviceSn)
                .orElseThrow(() -> new RuntimeException("设备不存在"));
            
            // 根据指令更新设备状态
            switch (command) {
                case "START":
                    device.setStatus("STARTING");
                    break;
                case "PAUSE":
                    device.setStatus("PAUSED");
                    break;
                case "CONTINUE":
                    device.setStatus("RUNNING");
                    break;
                case "END":
                    device.setStatus("FINISHED");
                    break;
                default:
                    break;
            }
            deviceRepository.save(device);
            
            return "指令已发送: " + cmd;
        } catch (Exception e) {
            return "发送失败: " + e.getMessage();
        }
    }
    
    // 6. 重置设备状态接口：强制将设备状态重置为IDLE（用于异常恢复）
    @PostMapping("/reset")
    public java.util.Map<String, Object> resetDevice(@RequestParam String deviceSn) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        try {
            Device device = deviceRepository.findByDeviceSn(deviceSn)
                .orElse(null);
            
            if (device == null) {
                // 设备不存在，创建新设备
                device = new Device();
                device.setDeviceSn(deviceSn);
            }
            
            // 强制重置为IDLE状态
            device.setStatus("IDLE");
            device.setLastHeartbeat(java.time.LocalDateTime.now());
            deviceRepository.save(device);
            
            result.put("success", true);
            result.put("message", "设备状态已重置为IDLE");
            result.put("deviceSn", deviceSn);
            result.put("status", "IDLE");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "重置失败: " + e.getMessage());
        }
        return result;
    }
    
    // 7. 获取单个设备详细状态（包含工作信息）
    @GetMapping("/{deviceSn}/status")
    public java.util.Map<String, Object> getDeviceStatus(@PathVariable String deviceSn) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        try {
            Device device = deviceRepository.findByDeviceSn(deviceSn)
                .orElse(null);
            
            if (device != null) {
                java.time.LocalDateTime now = java.time.LocalDateTime.now();

                result.put("deviceSn", device.getDeviceSn());
                result.put("status", device.getStatus());
                result.put("lastHeartbeat", device.getLastHeartbeat());
                result.put("found", true);
                
                // 工作状态相关信息
                result.put("washMode", device.getWashMode());
                result.put("washModeName", device.getWashModeName());
                result.put("totalDuration", device.getTotalDuration());
                result.put("remainSeconds", device.getRemainSeconds());
                result.put("workStartTime", device.getWorkStartTime());
                result.put("estimatedEndTime", device.getEstimatedEndTime());
                result.put("currentUserId", device.getCurrentUserId());
                result.put("currentOrderNo", device.getCurrentOrderNo());
            } else {
                result.put("found", false);
                result.put("status", "IDLE");
                result.put("deviceSn", deviceSn);
            }
        } catch (Exception e) {
            result.put("found", false);
            result.put("status", "FAULT");
            result.put("error", e.getMessage());
        }
        return result;
    }
    
    // 8. 启动设备（带洗衣模式和时长）
    @PostMapping("/start")
    public java.util.Map<String, Object> startDevice(
            @RequestParam String deviceSn,
            @RequestParam String washMode,
            @RequestParam String washModeName,
            @RequestParam Integer duration,
            @RequestParam Long userId,
            @RequestParam String orderNo) {
        
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        try {
            Device device = deviceRepository.findByDeviceSn(deviceSn)
                .orElseThrow(() -> new RuntimeException("设备不存在"));
            
            if (!"IDLE".equals(device.getStatus())) {
                result.put("success", false);
                result.put("message", "设备当前不可用");
                return result;
            }
            
            // 发送MQTT启动指令
            String cmd = String.format("{\"cmd\":\"START\", \"duration\":%d, \"mode\":\"%s\"}", duration, washMode);
            mqttService.sendCommand(deviceSn, cmd);
            
            // 更新设备状态
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            device.setStatus("STARTING");
            device.setWashMode(washMode);
            device.setWashModeName(washModeName);
            device.setTotalDuration(duration);
            device.setRemainSeconds(duration * 60);
            device.setWorkStartTime(now);
            device.setEstimatedEndTime(now.plusMinutes(duration));
            device.setCurrentUserId(userId);
            device.setCurrentOrderNo(orderNo);
            Order order = orderRepository.findByOrderNo(orderNo).orElse(null);
            if (order == null) {
                result.put("success", false);
                result.put("message", "订单不存在");
                return result;
            }
            if (!"PAID".equals(order.getStatus())) {
                result.put("success", false);
                result.put("message", "订单未支付或状态异常");
                return result;
            }
            device.setCurrentOrderId(order.getId());
            device.setLastHeartbeat(now);
            deviceRepository.save(device);
            
            // 同时更新订单的洗衣模式和时长
            order.setWashMode(washMode);
            order.setWashModeName(washModeName);
            order.setDurationMinutes(duration);
            orderRepository.save(order);
            
            result.put("success", true);
            result.put("message", "启动指令已下发");
            result.put("estimatedEndTime", device.getEstimatedEndTime());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "启动失败: " + e.getMessage());
        }
        return result;
    }
    
    // 9. 暂停设备
    @PostMapping("/pause")
    public java.util.Map<String, Object> pauseDevice(@RequestParam String deviceSn) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        try {
            Device device = deviceRepository.findByDeviceSn(deviceSn)
                .orElseThrow(() -> new RuntimeException("设备不存在"));
            
            if (!"RUNNING".equals(device.getStatus())) {
                result.put("success", false);
                result.put("message", "设备当前不在运行状态");
                return result;
            }
            
            // 发送MQTT暂停指令
            mqttService.sendCommand(deviceSn, "{\"cmd\":\"PAUSE\"}");
            
            // 计算剩余时间
            if (device.getWorkStartTime() != null && device.getTotalDuration() != null) {
                long elapsedSeconds = java.time.Duration.between(device.getWorkStartTime(), java.time.LocalDateTime.now()).getSeconds();
                int remainSeconds = Math.max(0, device.getTotalDuration() * 60 - (int) elapsedSeconds);
                device.setRemainSeconds(remainSeconds);
            }
            
            device.setStatus("PAUSED");
            device.setLastHeartbeat(java.time.LocalDateTime.now());
            deviceRepository.save(device);
            
            result.put("success", true);
            result.put("message", "设备已暂停");
            result.put("remainSeconds", device.getRemainSeconds());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "暂停失败: " + e.getMessage());
        }
        return result;
    }
    
    // 10. 继续设备
    @PostMapping("/continue")
    public java.util.Map<String, Object> continueDevice(@RequestParam String deviceSn) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        try {
            Device device = deviceRepository.findByDeviceSn(deviceSn)
                .orElseThrow(() -> new RuntimeException("设备不存在"));
            
            if (!"PAUSED".equals(device.getStatus())) {
                result.put("success", false);
                result.put("message", "设备当前不在暂停状态");
                return result;
            }
            
            // 发送MQTT继续指令
            mqttService.sendCommand(deviceSn, "{\"cmd\":\"CONTINUE\"}");
            
            // 重新计算预计结束时间
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            if (device.getRemainSeconds() != null) {
                device.setEstimatedEndTime(now.plusSeconds(device.getRemainSeconds()));
            }
            
            device.setStatus("RUNNING");
            device.setWorkStartTime(now); // 重置开始时间为继续时间
            device.setLastHeartbeat(now);
            deviceRepository.save(device);
            
            result.put("success", true);
            result.put("message", "设备已继续");
            result.put("estimatedEndTime", device.getEstimatedEndTime());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "继续失败: " + e.getMessage());
        }
        return result;
    }
    
    // 11. 结束设备（转为完成状态，等待取衣）
    @PostMapping("/end")
    public java.util.Map<String, Object> endDevice(@RequestParam String deviceSn) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        try {
            Device device = deviceRepository.findByDeviceSn(deviceSn)
                .orElseThrow(() -> new RuntimeException("设备不存在"));
            
            // 发送MQTT结束指令
            mqttService.sendCommand(deviceSn, "{\"cmd\":\"END\"}");
            
            // 更新订单状态为FINISHED
            String orderNo = device.getCurrentOrderNo();
            if (orderNo != null) {
                Order order = orderRepository.findByOrderNo(orderNo).orElse(null);
                if (order != null) {
                    order.setStatus("FINISHED");
                    order.setEndTime(java.time.LocalDateTime.now());
                    orderRepository.save(order);
                }
                // 创建收益记录（订单完成时）
                incomeRecordService.createIncomeRecordForDevice(deviceSn, orderNo);
            }
            
            // 转为完成状态（保留洗衣信息，等待取衣）
            device.setStatus("FINISHED");
            device.setRemainSeconds(0);
            device.setLastHeartbeat(java.time.LocalDateTime.now());
            deviceRepository.save(device);
            
            result.put("success", true);
            result.put("message", "洗衣完成，请取衣");
            result.put("status", "FINISHED");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "结束失败: " + e.getMessage());
        }
        return result;
    }
    
    // 12. 开门取衣（完成状态转为空闲）
    @PostMapping("/pickup")
    public java.util.Map<String, Object> pickupClothes(@RequestParam String deviceSn) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        try {
            Device device = deviceRepository.findByDeviceSn(deviceSn)
                .orElseThrow(() -> new RuntimeException("设备不存在"));
            
            if (!"FINISHED".equals(device.getStatus())) {
                result.put("success", false);
                result.put("message", "设备当前不在完成状态");
                return result;
            }
            
            // 发送MQTT开门指令
            mqttService.sendCommand(deviceSn, "{\"cmd\":\"OPEN_DOOR\"}");
            
            // 清空工作状态，转为空闲
            device.setStatus("IDLE");
            device.setWashMode(null);
            device.setWashModeName(null);
            device.setTotalDuration(null);
            device.setRemainSeconds(null);
            device.setWorkStartTime(null);
            device.setEstimatedEndTime(null);
            device.setCurrentUserId(null);
            device.setCurrentOrderNo(null);
            device.setLastHeartbeat(java.time.LocalDateTime.now());
            deviceRepository.save(device);
            
            result.put("success", true);
            result.put("message", "门已打开，请取走衣物");
            result.put("status", "IDLE");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "开门失败: " + e.getMessage());
        }
        return result;
    }
}
