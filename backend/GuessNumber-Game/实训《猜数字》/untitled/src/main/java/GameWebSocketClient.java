import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GameWebSocketClient extends WebSocketClient {
    private final ObjectMapper objectMapper = new ObjectMapper(); // 用于JSON序列化

    public GameWebSocketClient(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("WebSocket连接已建立");
        // 连接成功后可发送初始化数据（如需要）
    }

    @Override
    public void onMessage(String message) {
        try {
            Map<String, Object> data = objectMapper.readValue(message, Map.class);
            String clientType = (String) data.get("clientType");
            // 只处理桌面端自己的消息（或忽略Web端消息）
            if ("desktop".equals(clientType)) {
                System.out.println("收到桌面端消息: " + message);
                // 处理逻辑...
            } else {
                // 忽略Web端消息
                // System.out.println("忽略Web端消息: " + message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("连接关闭: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
        System.err.println("WebSocket错误");
    }

    // 发送游戏数据到后台（核心方法）
    public void sendGameData(GameResult result) {
        try {
            // 构造与Web端格式一致的JSON数据
            Map<String, Object> data = new HashMap<>();
            data.put("clientId", result.getClientId()); // 客户端唯一标识（建议本地存储）
            data.put("clientType", "desktop"); // 区分桌面端和Web端
            data.put("gameType", "guessNumber"); // 与Web端一致
            data.put("score", result.isSuccess() ? 100 - result.getAttempts() * 5 : 0); // 计分规则与Web端一致
            data.put("level", result.getMinValue() + "-" + result.getMaxValue()); // 难度区间格式
            data.put("result", result.isSuccess() ? "win" : (result.isGaveUp() ? "giveup" : "lose")); // 结果状态
            data.put("attempts", result.getAttempts()); // 尝试次数


            // 补充服务器所需的关键字段（根据GameData实体类）
            data.put("gameMode", result.getGameMode()); // 游戏模式："guess"或"challenge"
            data.put("minValue", result.getMinValue()); // 最小值
            data.put("maxValue", result.getMaxValue()); // 最大值
            data.put("maxAttempts", result.getMaxAttempts()); // 最大尝试次数（挑战模式）
            data.put("target", result.getTarget()); // 目标数字
            data.put("updateTime", LocalDateTime.now().toString()); // 更新时间



            // 序列化为JSON并发送
            String jsonData = objectMapper.writeValueAsString(data);
            this.send(jsonData);
            System.out.println("发送数据: " + jsonData);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}