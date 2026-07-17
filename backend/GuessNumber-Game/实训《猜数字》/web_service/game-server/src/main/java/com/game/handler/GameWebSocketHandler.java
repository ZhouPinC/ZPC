// com.game.handler.GameWebSocketHandler.java
package com.game.handler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.entity.GameData;
import com.game.repository.GameDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {
    // 存储所有连接的会话
    private static final Set<WebSocketSession> sessions =
            Collections.synchronizedSet(new HashSet<>());

    private final ObjectMapper objectMapper;
    private final GameDataRepository gameDataRepository;

    // 构造函数注入
    public GameWebSocketHandler(GameDataRepository gameDataRepository) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.gameDataRepository = gameDataRepository;
    }

    // 连接建立时
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("新客户端连接: " + session.getId());
    }


    // 接收消息时
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            // 添加详细日志
            System.out.println("=== 收到 WebSocket 消息 ===");
            System.out.println("原始消息: " + message.getPayload());

            // 先解析为 Map 查看数据结构
            Map<String, Object> rawData = objectMapper.readValue(message.getPayload(), Map.class);
            System.out.println("解析为 Map: " + rawData);

            // 然后转换为 GameData
            GameData gameData = objectMapper.readValue(message.getPayload(), GameData.class);

            // 检查转换后的数据
            System.out.println("转换后的 GameData:");
            System.out.println("- ID: " + gameData.getId());
            System.out.println("- ClientId: " + gameData.getClientId());
            System.out.println("- ClientType: " + gameData.getClientType());
            System.out.println("- GameType: " + gameData.getGameType());
            System.out.println("- Level: " + gameData.getLevel());
            System.out.println("- Result: " + gameData.getResult());
            System.out.println("- Attempts: " + gameData.getAttempts());
            System.out.println("- Score: " + gameData.getScore());
            System.out.println("- Target: " + gameData.getTarget());
            System.out.println("- UserCount: " + gameData.getUserCount());
            System.out.println("- CurrentHistory: " + gameData.getCurrentHistory());
            System.out.println("- UpdateTime: " + gameData.getUpdateTime());

            // 设置默认值和更新时间
            if (gameData.getGameType() == null) {
                gameData.setGameType("猜数字模式");
                System.out.println("设置默认 GameType: 猜数字模式");
            }
            if (gameData.getResult() == null) {
                gameData.setResult("unknown");
                System.out.println("设置默认 Result: unknown");
            }
            if (gameData.getAttempts() == null) {
                gameData.setAttempts(0);
                System.out.println("设置默认 Attempts: 0");
            }
            if (gameData.getScore() == null) {
                gameData.setScore(0);
                System.out.println("设置默认 Score: 0");
            }
            if (gameData.getTarget() == null) {
                gameData.setTarget(0);
                System.out.println("设置默认 Target: 0");
            }
            if (gameData.getUserCount() == null) {
                gameData.setUserCount(1);
                System.out.println("设置默认 UserCount: 1");
            }
            if (gameData.getCurrentHistory() == null) {
                gameData.setCurrentHistory("[]");
                System.out.println("设置默认 CurrentHistory: []");
            }
            if (gameData.getClientType() == null) {
                gameData.setClientType("web");
                System.out.println("设置默认 ClientType: web");
            }
            if (gameData.getLevel() == null) {
                gameData.setLevel("1-100");
                System.out.println("设置默认 Level: 1-100");
            }

            // 确保有更新时间
            if (gameData.getUpdateTime() == null) {
                gameData.setUpdateTime(LocalDateTime.now());
                System.out.println("设置 UpdateTime: " + gameData.getUpdateTime());
            }

            // 保存到数据库
            GameData saved = gameDataRepository.save(gameData);
            System.out.println("数据保存成功，ID: " + saved.getId());

            // 广播给所有连接的客户端（包括后台管理端）
            broadcast(saved);

        } catch (Exception e) {
            System.err.println("处理 WebSocket 消息失败: " + e.getMessage());
            e.printStackTrace();
            // 向客户端发送错误信息
            session.sendMessage(new TextMessage("处理数据失败: " + e.getMessage()));
        }
    }

    // 连接关闭时
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("客户端断开连接: " + session.getId());
    }

    // 广播消息
    public void broadcast(GameData data) throws IOException {
        try {
            String json = objectMapper.writeValueAsString(data);
            // ... 广播逻辑
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        String jsonData = objectMapper.writeValueAsString(data);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(jsonData));
            }
        }
    }

//    @RestController
//    public class GameDataController {
//
//        @Autowired
//        private GameDataRepository gameDataRepository;
//
//        @GetMapping("/api/allData")
//        public List<GameData> getAllData() {
//            List<GameData> allData = gameDataRepository.findAll();
//
//            // 调试：打印第一条数据
//            if (!allData.isEmpty()) {
//                GameData first = allData.get(0);
//                System.out.println("第一条数据详情:");
//                System.out.println("ID: " + first.getId());
//                System.out.println("GameType: " + first.getGameType());
//                System.out.println("Target: " + first.getTarget());
//                System.out.println("Result: " + first.getResult());
//                System.out.println("Attempts: " + first.getAttempts());
//                System.out.println("CurrentHistory: " + first.getCurrentHistory());
//            }
//
//            return allData;
//        }
//    }
}