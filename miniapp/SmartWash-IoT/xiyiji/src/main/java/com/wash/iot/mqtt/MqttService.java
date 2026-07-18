package com.wash.iot.mqtt;

import com.alibaba.fastjson2.JSONObject;
import com.wash.iot.interfaces.mqtt.dto.DeviceReportEvent;
import com.wash.iot.service.DeviceStateManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class MqttService {

    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${mqtt.default-topic}")
    private String defaultTopic; // 配置为 "status/+" 以监听所有设备

    @Autowired
    private DeviceStateManager deviceStateManager;

    @Autowired(required = false)
    private MqttClient client;

    @PostConstruct
    public void init() {
        try {
            // 检查client是否为null
            if (client == null) {
                log.warn("MQTT客户端未初始化，跳过MQTT初始化");
                return;
            }
            
            // 设置回调（处理收到的消息）
            client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    if (defaultTopic == null || defaultTopic.isBlank()) {
                        return;
                    }
                    try {
                        if (client != null && client.isConnected()) {
                            client.subscribe(defaultTopic);
                            log.info("MQTT连接就绪，已订阅 Topic: {}", defaultTopic);
                        }
                    } catch (MqttException e) {
                        log.error("MQTT重连后订阅失败: topic={}", defaultTopic, e);
                    }
                }

                @Override
                public void connectionLost(Throwable cause) {
                    log.error("MQTT 连接断开", cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    log.info("收到消息 Topic: {}, Payload: {}", topic, payload);
                    handleDeviceStatusMessage(topic, payload);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // 消息发送成功的回调，通常不需要处理
                }
            });

            // 订阅设备状态 Topic (status/+)
            if (defaultTopic != null && !defaultTopic.isBlank()) {
                client.subscribe(defaultTopic);
                log.info("已订阅 Topic: {}", defaultTopic);
            }

        } catch (MqttException e) {
            log.error("MQTT 初始化失败", e);
        }
    }

    /**
     * 发送指令给设备
     * Topic: cmd/{deviceSn}
     */
    public void sendCommand(String deviceSn, String commandJson) throws MqttException {
        if (client != null && client.isConnected()) {
            MqttMessage message = new MqttMessage(commandJson.getBytes());
            message.setQos(1); // 至少到达一次
            client.publish("cmd/" + deviceSn, message);
            log.info("指令已发送 -> Device: {}, Cmd: {}", deviceSn, commandJson);
        } else {
            throw new MqttException(MqttException.REASON_CODE_CLIENT_NOT_CONNECTED);
        }
    }

    /**
     * 返回 MQTT 客户端连接状态，供控制器用于健康检查
     */
    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    /**
     * 处理设备上报的状态
     * 逻辑：解析 JSON -> 找设备 -> 更新状态和心跳 -> 同步订单状态
     */
    private void handleDeviceStatusMessage(String topic, String payload) {
        try {
            // Topic 格式: status/{deviceSn}
            String[] parts = topic.split("/");
            if (parts.length < 2) return;
            String deviceSn = parts[1];

            // 解析 JSON, 例如: {"status": "RUNNING", "remainSeconds": 1200}
            String status = null;
            Integer remainSeconds = null;
            try {
                if (payload.trim().startsWith("{")) {
                    JSONObject json = JSONObject.parseObject(payload);
                    status = json.getString("status");
                    remainSeconds = json.getInteger("remainSeconds");
                    if (remainSeconds == null) {
                        remainSeconds = json.getInteger("remainingTime");
                    }
                } else {
                    // 非 JSON 格式，直接作为状态处理
                    status = payload.trim();
                }
            } catch (Exception e) {
                log.warn("解析 Payload 失败，尝试直接作为状态处理: {}", payload);
                status = payload.trim();
            }

            if (status == null) return;

            String normalizedStatus = status.trim().toUpperCase();

            DeviceReportEvent event = new DeviceReportEvent();
            event.setDeviceSn(deviceSn);
            event.setStatus(normalizedStatus);
            if (remainSeconds != null && remainSeconds >= 0) {
                event.setRemainingTime(remainSeconds);
            }
            event.setRawData(payload);
            event.setReportTime(LocalDateTime.now());

            deviceStateManager.handleDeviceReport(event);
        } catch (Exception e) {
            log.error("处理设备消息异常", e);
        }
    }
}
