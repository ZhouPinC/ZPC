public class GameResult {
    private String clientId; // 客户端ID（建议本地存储，如文件存储）
    private boolean success; // 是否成功
    private boolean gaveUp; // 是否放弃
    private int attempts; // 尝试次数
    private int minValue; // 最小值（难度区间）
    private int maxValue; // 最大值（难度区间）
    private String gameMode; // "guess"或"challenge"
    private int maxAttempts;
    private int target;

    // getter和setter
    // 补充getter方法
    public String getGameMode() { return gameMode; }
    public void setGameMode(String gameMode) { this.gameMode = gameMode; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public int getTarget() { return target; }
    public void setTarget(int target) { this.target = target; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public boolean isGaveUp() { return gaveUp; }
    public void setGaveUp(boolean gaveUp) { this.gaveUp = gaveUp; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public int getMinValue() { return minValue; }
    public void setMinValue(int minValue) { this.minValue = minValue; }
    public int getMaxValue() { return maxValue; }
    public void setMaxValue(int maxValue) { this.maxValue = maxValue; }
}