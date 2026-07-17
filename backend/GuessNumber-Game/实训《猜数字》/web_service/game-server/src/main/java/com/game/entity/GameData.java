// com.game.entity.GameData.java
package com.game.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "game_data")
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameData {

    @Column(name = "game_mode") // 关键：确保列名和数据库一致
    private String gameMode; // 字段值必须是 'challenge' 或 'guess'（和前端判断一致）
    private Integer minValue; // 最小值
    private Integer maxValue; // 最大值
    private Integer maxAttempts; // 限定次数
    private Integer target = 0; // 目标数字
    private String result = "unknown";
    private int score = 0;            // 分数

    // 关键：指定解析格式，适配前端的 yyyy-MM-dd HH:mm:ss
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime; // 更新时间

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id")
    private String clientId;      // 客户端标识（区分不同端）

    @Column(name = "client_type")
    private String clientType = "web"; // 默认值

    @Column(name = "game_type")
    private String gameType = "未显示模式"; // 默认值


    private String level;         // 当前关卡


    // 历史记录字段（可能需要使用JSON格式存储）
    @Column(name = "current_history", columnDefinition = "TEXT")
    private String currentHistory = "[]";  // 存储为JSON字符串

    @Column(name = "user_count")
    private Integer userCount = 1; // 默认值

    // 添加一个便捷方法将currentHistory转换为对象
    public List<Map<String, Object>> getCurrentHistoryAsList() {
        if (currentHistory == null || currentHistory.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(currentHistory,
                    new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }



    // ★ 新增字段：attempts（默认值为0）
    @Column(nullable = false)
    private Integer attempts = 0;





    public GameData() {}

    // 添加 date 字段（临时字段，不持久化到数据库）
    @Transient
    private String date;

    // 必须的 getter 和 setter 方法

    // 游戏模式 getter（必须存在，前端判断模式用）
    public String getGameMode() { return gameMode; }
    public void setGameMode(String gameMode) { this.gameMode = gameMode; }

    // 最小值 getter（前端计算区间用）
    public Integer getMinValue() { return minValue; }
    public void setMinValue(Integer minValue) { this.minValue = minValue; }

    // 最大值 getter（前端计算区间用，之前缺失导致报错）
    public Integer getMaxValue() { return maxValue; }
    public void setMaxValue(Integer maxValue) { this.maxValue = maxValue; }

    // 限定次数 getter（前端比较 log2 用）
    public Integer getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(Integer maxAttempts) { this.maxAttempts = maxAttempts; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientType() { return clientType; }
    public void setClientType(String clientType) { this.clientType = clientType; }

    public String getGameType() { return gameType; }  // 添加这个方法
    public void setGameType(String gameType) { this.gameType = gameType; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public Integer getAttempts() { return attempts; }
    public void setAttempts(Integer attempts) { this.attempts = attempts; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getTarget() { return target; }  // 添加这个方法
    public void setTarget(Integer target) { this.target = target; }

    public Integer getUserCount() { return userCount; }
    public void setUserCount(Integer userCount) { this.userCount = userCount; }

    public String getCurrentHistory() { return currentHistory; }  // 添加这个方法
    public void setCurrentHistory(String currentHistory) { this.currentHistory = currentHistory; }
}
