package com.wash.iot.infrastructure.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * MQTT网关
 */
@Slf4j
@Component
public class MqttGateway {

    @Autowired(required = false)
    private MqttClient mqttClient;

    /**
     * 发送MQTT消息
     */
    public boolean sendMessage(String topic, String payload) {
        if (mqttClient == null) {
            log.warn("MQTT客户端未初始化，无法发送消息: topic={}, payload={}", topic, payload);
            return false;
        }
        try {
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(1);
            message.setRetained(false);

            mqttClient.publish(topic, message);
            log.info("MQTT消息发送成功: topic={}, payload={}", topic, payload);
            return true;
        } catch (MqttException e) {
            log.error("MQTT消息发送失败: topic={}, payload={}", topic, payload, e);
            return false;
        }
    }

    /**
     * 发送设备控制命令
     */
    public boolean sendDeviceCommand(String deviceSn, String command) {
        String topic = "cmd/" + deviceSn;
        return sendMessage(topic, command);
    }

    /**
     * 发送启动命令
     */
    public boolean sendStartCommand(String deviceSn, String orderNo, Integer duration) {
        String command = String.format("{\"cmd\":\"START\", \"orderNo\":\"%s\", \"duration\":%d}", orderNo, duration);
        return sendDeviceCommand(deviceSn, command);
    }

    /**
     * 订阅主题
     */
    public boolean subscribe(String topic) {
        if (mqttClient == null) {
            log.warn("MQTT客户端未初始化，无法订阅主题: topic={}", topic);
            return false;
        }
        try {
            mqttClient.subscribe(topic, 1);
            log.info("MQTT主题订阅成功: topic={}", topic);
            return true;
        } catch (MqttException e) {
            log.error("MQTT主题订阅失败: topic={}", topic, e);
            return false;
        }
    }
}
