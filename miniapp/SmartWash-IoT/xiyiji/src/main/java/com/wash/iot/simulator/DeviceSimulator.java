package com.wash.iot.simulator;

import com.alibaba.fastjson2.JSONObject;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 共享洗衣机设备模拟器
 * 功能：
 * 1. 模拟多个真实设备连接 MQTT (WASH_001, WASH_002, WASH_003)
 * 2. 监听 cmd/WASH_XXX 指令
 * 3. 收到 START 后，自动进入 RUNNING 状态
 * 4. 模拟运行 N 秒后，自动恢复 IDLE 状态
 * 5. 定时上报心跳，防止被后端判定为离线
 */
public class DeviceSimulator {

    // 配置参数 (需与后端一致)
    private static final String BROKER_URL = "tcp://broker.emqx.io:1883";
    private final String deviceSn;
    private final String topicCmd;
    private final String topicStatus;
    private final String clientId;

    private MqttClient client;
    private String currentStatus = "IDLE";
    private volatile boolean paused = false;
    private volatile int simulatedTotalSeconds = 10;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public DeviceSimulator(String deviceSn) {
        this.deviceSn = deviceSn;
        this.topicCmd = "cmd/" + deviceSn;
        this.topicStatus = "status/" + deviceSn;
        this.clientId = "mock-device-" + deviceSn + "-" + System.currentTimeMillis();
    }

    public static void main(String[] args) {
        // 同时启动三个模拟器实例
        String[] devices = {"WASH_001", "WASH_002", "WASH_003"};
        for (String sn : devices) {
            new Thread(() -> new DeviceSimulator(sn).start()).start();
        }
    }

    public void start() {
        try {
            // 1. 初始化客户端
            client = new MqttClient(BROKER_URL, clientId, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setKeepAliveInterval(20);
            options.setAutomaticReconnect(true);

            // 2. 设置回调监听指令
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("❌ [模拟器-" + deviceSn + "] 断线了: " + (cause != null ? cause.getMessage() : "未知原因"));
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    System.out.println("📩 [模拟器-" + deviceSn + "] 收到指令: " + payload);
                    handleCommand(payload);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            // 3. 连接并订阅
            System.out.println("⏳ [模拟器-" + deviceSn + "] 正在连接 MQTT...");
            client.connect(options);
            client.subscribe(topicCmd);
            
            System.out.println("✅ [模拟器-" + deviceSn + "] 启动成功！");
            
            // 4. 启动定时心跳 (每 60 秒上报一次当前状态)
            startHeartbeat();
            
            // 上线先报个平安
            reportStatus(currentStatus);

        } catch (MqttException e) {
            System.err.println("❌ [模拟器-" + deviceSn + "] 启动失败: " + e.getMessage());
        }
    }

    private void startHeartbeat() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (client != null && client.isConnected()) {
                    reportStatus(currentStatus);
                }
            } catch (Exception e) {
                System.err.println("❌ [模拟器-" + deviceSn + "] 心跳上报失败: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    // 处理接收到的指令
    private void handleCommand(String jsonCmd) {
        try {
            JSONObject cmdObj = JSONObject.parseObject(jsonCmd);
            String action = cmdObj.getString("cmd");
            
            if ("START".equals(action)) {
                Integer durationMinutes = cmdObj.getInteger("duration");
                if (durationMinutes != null && durationMinutes > 0) {
                    long seconds = (long) durationMinutes * 60L;
                    long maxSeconds = 30L * 60L;
                    simulatedTotalSeconds = (int) Math.min(seconds, maxSeconds);
                } else {
                    simulatedTotalSeconds = 10 * 60;
                }
                paused = false;
                new Thread(this::simulateWashingProcess).start();
            } else if ("PAUSE".equals(action)) {
                paused = true;
                currentStatus = "PAUSED";
                reportStatus(currentStatus, null);
            } else if ("CONTINUE".equals(action)) {
                paused = false;
                currentStatus = "RUNNING";
                reportStatus(currentStatus, null);
            } else if ("END".equals(action)) {
                paused = false;
                currentStatus = "FINISHED";
                reportStatus(currentStatus, 0);
            } else if ("OPEN_DOOR".equals(action)) {
                paused = false;
                currentStatus = "IDLE";
                reportStatus(currentStatus, 0);
            }
        } catch (Exception e) {
            System.err.println("解析指令失败: " + e.getMessage());
        }
    }

    // 模拟洗衣全流程
    private void simulateWashingProcess() {
        try {
            System.out.println("\n--- 🚀 [模拟器-" + deviceSn + "] 开始洗衣流程 ---");
            
            currentStatus = "RUNNING";
            reportStatus(currentStatus, simulatedTotalSeconds);
            System.out.println("🌊 [模拟器-" + deviceSn + "] 进水中... 滚筒开始旋转...");

            int remain = simulatedTotalSeconds;
            while (remain > 0) {
                TimeUnit.SECONDS.sleep(1);
                if (paused) {
                    continue;
                }
                remain -= 1;
                reportStatus(currentStatus, remain);
            }
            System.out.println("🎉 [模拟器-" + deviceSn + "] 清洗完成！");

            currentStatus = "FINISHED";
            reportStatus(currentStatus, 0);
            System.out.println("--- ✅ [模拟器-" + deviceSn + "] 流程结束 ---\n");

        } catch (InterruptedException | MqttException e) {
            e.printStackTrace();
        }
    }

    private void reportStatus(String status) throws MqttException {
        reportStatus(status, null);
    }

    // 上报状态
    private void reportStatus(String status, Integer remainSeconds) throws MqttException {
        if (client == null || !client.isConnected()) return;

        JSONObject payload = new JSONObject();
        payload.put("status", status);
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("deviceSn", deviceSn);
        if (remainSeconds != null) {
            payload.put("remainSeconds", remainSeconds);
        }

        MqttMessage message = new MqttMessage(payload.toJSONString().getBytes());
        message.setQos(1);
        client.publish(topicStatus, message);
        
        System.out.println("📡 [模拟器-" + deviceSn + "] 上报状态: " + status);
    }
}
