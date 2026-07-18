package com.wash.iot.config;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MQTT配置类
 */
@Slf4j
@Configuration
public class MqttConfig {

    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    @Value("${mqtt.client-id}")
    private String clientId;

    /**
     * 创建MQTT客户端Bean
     * 如果MQTT连接失败，返回null，不影响应用启动
     */
    @Bean
    public MqttClient mqttClient() {
        try {
            MqttClient client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
            
            // 配置连接参数
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(20);
            options.setAutomaticReconnect(true);
            
            // 建立连接
            client.connect(options);
            log.info("MQTT客户端连接成功: {}", brokerUrl);
            
            return client;
        } catch (MqttException e) {
            log.error("MQTT客户端初始化失败，应用将继续运行但不支持MQTT功能: {}", e.getMessage());
            return null;
        }
    }
}
