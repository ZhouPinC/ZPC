// 保存为 GuessNumberGame.java
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * GuessNumberGame.java
 * 完整可运行的猜数字游戏（Swing）
 * 设计要点：
 * - StartPanel（未开始界面）与 GamePanel（游戏界面）完全分离
 * - SettingDialog 仅能从未开始界面打开
 * - 区间允许负数，必须为整数，且 max - min >= 10
 * - 支持 猜数字模式（无限） 和 挑战模式（限定次数）
 * - 简单动画（标题缩放、猜中闪烁）
 *
 * 运行：
 * javac GuessNumberGame.java
 * java GuessNumberGame
 */
public class GuessNumberGame extends JFrame {

    // CardLayout 名称
    private static final String CARD_START = "start";
    private static final String CARD_GAME = "game";

    private CardLayout cardLayout;
    private JPanel cards;

    private StartPanel startPanel;
    private GamePanel gamePanel;

    private Settings settings;
    private GameController controller;

    public GuessNumberGame() {
        settings = Settings.load();
        controller = new GameController(settings);

        initUI();
    }

    private void initUI() {
        setTitle("猜数字小游戏 — 国风版");
        setSize(760, 520);
        setMinimumSize(new Dimension(700, 460));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);

        startPanel = new StartPanel(this);
        gamePanel = new GamePanel(this);

        cards.add(startPanel, CARD_START);
        cards.add(gamePanel, CARD_GAME);

        add(cards);

        showStart();
    }

    public void showStart() {
        startPanel.applySavedStyle(); // 预览样式应用
        cardLayout.show(cards, CARD_START);
    }

    public void showGame() {
        // 初始化一局
        controller.startNewGame();
        gamePanel.prepareForNewGame();
        cardLayout.show(cards, CARD_GAME);
    }

    public void restartToStart() {
        controller.endGame();
        showStart();
    }

    public GameController getController() {
        return controller;
    }

    public Settings getSettings() {
        return settings;
    }

    public static void main(String[] args) {
        // 尝试设置系统外观以符合用户习惯
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) { /* ignore */ }

        SwingUtilities.invokeLater(() -> {
            GuessNumberGame app = new GuessNumberGame();
            app.setVisible(true);
        });
    }

    // -------------------------------
    // Settings: 持久化设置
    // -------------------------------
    public static class Settings {
        public int minValue = 0;
        public int maxValue = 100;
        public boolean challengeMode = false;
        public int maxAttempts = 10; // 仅挑战模式使用
        public String fontName = "微软雅黑";
        public int fontSize = 16;
        public Color fontColor = new Color(30, 30, 30);
        public boolean soundOn = false;
        public boolean showIntro = true;

        private static final String PROP_FILE = System.getProperty("user.home") + File.separator + ".guessgame.properties";

        public static Settings load() {
            Settings s = new Settings();
            Properties p = new Properties();
            Path path = Paths.get(PROP_FILE);
            if (Files.exists(path)) {
                try (InputStream in = Files.newInputStream(path)) {
                    p.load(in);
                    s.minValue = Integer.parseInt(p.getProperty("minValue", "0"));
                    s.maxValue = Integer.parseInt(p.getProperty("maxValue", "100"));
                    s.challengeMode = Boolean.parseBoolean(p.getProperty("challengeMode", "false"));
                    s.maxAttempts = Integer.parseInt(p.getProperty("maxAttempts", "10"));
                    s.fontName = p.getProperty("fontName", s.fontName);
                    s.fontSize = Integer.parseInt(p.getProperty("fontSize", "16"));
                    s.fontColor = new Color(Integer.parseInt(p.getProperty("fontColor", Integer.toString(s.fontColor.getRGB()))));
                    s.soundOn = Boolean.parseBoolean(p.getProperty("soundOn", "false"));
                    s.showIntro = Boolean.parseBoolean(p.getProperty("showIntro", "true"));
                } catch (Exception ex) {
                    // ignore, use defaults
                }
            }
            return s;
        }

        public boolean save() {
            Properties p = new Properties();
            p.setProperty("minValue", Integer.toString(minValue));
            p.setProperty("maxValue", Integer.toString(maxValue));
            p.setProperty("challengeMode", Boolean.toString(challengeMode));
            p.setProperty("maxAttempts", Integer.toString(maxAttempts));
            p.setProperty("fontName", fontName);
            p.setProperty("fontSize", Integer.toString(fontSize));
            p.setProperty("fontColor", Integer.toString(fontColor.getRGB()));
            p.setProperty("soundOn", Boolean.toString(soundOn));
            p.setProperty("showIntro", Boolean.toString(showIntro));
            try (OutputStream out = Files.newOutputStream(Paths.get(PROP_FILE))) {
                p.store(out, "GuessNumberGame Settings");
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }

    // -------------------------------
    // GameController: 逻辑
    // -------------------------------
    public static class GameController {
        private Settings settings;
        private Integer target = null;
        private int attempts = 0;
        private boolean running = false;
        private boolean finished = false;

        public GameController(Settings s) {
            this.settings = s;
        }

        public void startNewGame() {
            this.target = ThreadLocalRandom.current().nextInt(settings.minValue, settings.maxValue + 1);
            this.attempts = 0;
            this.running = true;
            this.finished = false;
        }

        public void endGame() {
            this.running = false;
            this.finished = true;
            this.target = null;
        }

        public boolean isRunning() {
            return running;
        }

        public int getAttempts() { return attempts; }

        /**
         * 提交猜测
         * @param guess 用户输入的整数
         * @return result map: keys:
         *   - status: "correct", "low", "high", "failed"
         *   - attempts: attempts after this submit
         *   - target: reveal target when correct or failed
         *   - remaining: remaining attempts (for challenge mode)
         */
        public Map<String, Object> submitGuess(int guess) {
            Map<String, Object> r = new HashMap<>();
            if (!running || finished || target == null) {
                r.put("status", "not_running");
                return r;
            }
            attempts++;
            if (guess == target) {
                r.put("status", "correct");
                r.put("attempts", attempts);
                r.put("target", target);
                running = false;
                finished = true;
                return r;
            } else {
                String status = guess < target ? "low" : "high";
                r.put("status", status);
                r.put("attempts", attempts);
                if (settings.challengeMode) {
                    int remaining = settings.maxAttempts - attempts;
                    r.put("remaining", remaining);
                    if (remaining <= 0) {
                        r.put("status", "failed");
                        r.put("target", target);
                        running = false;
                        finished = true;
                    }
                }
                return r;
            }
        }

        public Map<String, Object> giveUp() {
            Map<String, Object> r = new HashMap<>();
            if (!running) {
                r.put("status", "not_running");
                return r;
            }
            r.put("status", "gave_up");
            r.put("target", target);
            running = false;
            finished = true;
            return r;
        }

        public Settings getSettings() { return settings; }
    }

    // -------------------------------
    // StartPanel（未开始界面）
    // -------------------------------
    class StartPanel extends JPanel {
        private GuessNumberGame parent;
        private JLabel titleLabel;
        private JButton startBtn, settingBtn, exitBtn;
        private javax.swing.Timer titleAnimTimer;
        private float titleScale = 1.0f;
        private boolean scaleUp = true;

        public StartPanel(GuessNumberGame p) {
            this.parent = p;
            init();
            if (parent.getSettings().showIntro) showIntroOnce();
            startTitleAnimation();
        }

        private void init() {
            setLayout(new GridBagLayout());
            setBackground(new Color(245, 245, 240)); // 国风浅米色底

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.insets = new Insets(12, 12, 12, 12);

            titleLabel = new JLabel("猜数字 · 小游园");
            titleLabel.setFont(new Font(parent.getSettings().fontName, Font.BOLD, 34));
            titleLabel.setForeground(new Color(40, 30, 20));
            titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

            // 中央竖向按钮区
            startBtn = makeBigButton("开始游戏");
            settingBtn = makeBigButton("游戏设置");
            exitBtn = makeBigButton("退出游戏");

            // 布局
            gbc.gridy = 0;
            add(titleLabel, gbc);

            gbc.gridy = 1;
            add(Box.createVerticalStrut(6), gbc);

            gbc.gridy = 2;
            add(startBtn, gbc);

            gbc.gridy = 3;
            add(settingBtn, gbc);

            gbc.gridy = 4;
            add(exitBtn, gbc);

            gbc.gridy = 5;
            JLabel foot = new JLabel("版本 1.0  — 设计：丞哥");
            foot.setFont(new Font(parent.getSettings().fontName, Font.PLAIN, 12));
            foot.setForeground(new Color(100, 100, 100));
            add(foot, gbc);

            // 事件
            startBtn.addActionListener(e -> {
                parent.showGame();
            });

            settingBtn.addActionListener(e -> {
                // 仅在未开始时允许设置
                SettingDialog dlg = new SettingDialog(parent, parent.getSettings());
                dlg.setVisible(true);
                // 可能保存设置后，预览样式生效
                applySavedStyle();
            });

            exitBtn.addActionListener(e -> {
                int ok = JOptionPane.showConfirmDialog(parent, "确认退出游戏吗？", "退出确认", JOptionPane.YES_NO_OPTION);
                if (ok == JOptionPane.YES_OPTION) System.exit(0);
            });

            // 悬停微动效（按钮缩放）
            addHoverEffect(startBtn);
            addHoverEffect(settingBtn);
            addHoverEffect(exitBtn);
        }

        private JButton makeBigButton(String text) {
            JButton b = new JButton(text);
            b.setPreferredSize(new Dimension(220, 44));
            b.setFont(new Font(parent.getSettings().fontName, Font.PLAIN, 18));
            return b;
        }

        private void addHoverEffect(final JButton b) {
            b.setFocusable(false);
            b.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    b.setFont(b.getFont().deriveFont(b.getFont().getStyle(), b.getFont().getSize() + 1f));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    b.setFont(b.getFont().deriveFont(b.getFont().getStyle(), b.getFont().getSize() - 1f));
                }
            });
        }

        private void startTitleAnimation() {
            titleAnimTimer = new javax.swing.Timer(60, e -> {
                if (scaleUp) titleScale += 0.02f;
                else titleScale -= 0.02f;
                if (titleScale > 1.06f) scaleUp = false;
                if (titleScale < 0.94f) scaleUp = true;
                titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 34f * titleScale));
            });
            titleAnimTimer.start();
        }

        public void applySavedStyle() {
            // 应用预览样式（字体/颜色）
            try {
                titleLabel.setFont(new Font(parent.getSettings().fontName, Font.BOLD, 34));
            } catch (Exception e) { /* ignore */ }
        }

        private void showIntroOnce() {
            // 展示一次引导弹窗
            SwingUtilities.invokeLater(() -> {
                String msg = "<html><div style='width:360px;padding:6px'>欢迎来到《猜数字》小游戏！<br/>"
                        + "规则简述：在设置的整数区间内（允许负数，且区间长度至少为10）随机生成一个整数。<br/>"
                        + "有两种模式：<b>猜数字模式</b>（无限次）和<b>挑战模式</b>（限定次数）。<br/>"
                        + "开始前可通过“游戏设置”配置参数。开始后无法再修改设置。祝你好运！</div></html>";
                JCheckBox notShow = new JCheckBox("以后不再显示");
                Object[] arr = {new JLabel(msg), notShow};
                JOptionPane.showMessageDialog(parent, arr, "游戏说明", JOptionPane.INFORMATION_MESSAGE);
                if (notShow.isSelected()) {
                    parent.getSettings().showIntro = false;
                    parent.getSettings().save();
                }
            });
        }
    }

    // -------------------------------
    // GamePanel（游戏界面）
    // -------------------------------
    class GamePanel extends JPanel {
        private GuessNumberGame parent;

        private JLabel topInfoLabel;
        private JTextField inputField;
        private JButton submitBtn, giveUpBtn, pauseBtn;
        private JTextArea historyArea;
        private JLabel bottomStatus;

        private int lastValidInput = 0;

        // 动画计时器（猜中时高亮）
        private javax.swing.Timer flashTimer;
        private int flashCount = 0;
        private Color normalHistoryBG;

        public GamePanel(GuessNumberGame p) {
            this.parent = p;
            init();
        }

        private void init() {
            setLayout(new BorderLayout());
            setBackground(new Color(254, 252, 245));

            // 顶部信息
            JPanel top = new JPanel(new BorderLayout());
            top.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
            topInfoLabel = new JLabel("");
            topInfoLabel.setFont(new Font(parent.getSettings().fontName, Font.BOLD, 16));
            top.add(topInfoLabel, BorderLayout.WEST);
            add(top, BorderLayout.NORTH);

            // 中间：左操作 右历史
            JPanel center = new JPanel(new BorderLayout());
            center.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

            // 左侧操作区
            JPanel left = new JPanel();
            left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
            left.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 20));

            JLabel lbl = new JLabel("请输入你的数字：");
            lbl.setFont(new Font(parent.getSettings().fontName, Font.PLAIN, 15));
            left.add(lbl);
            left.add(Box.createVerticalStrut(6));

            inputField = new JTextField();
            inputField.setMaximumSize(new Dimension(280, 34));
            inputField.setFont(new Font(parent.getSettings().fontName, Font.PLAIN, parent.getSettings().fontSize));
            // 限制输入：只有数字和首位可出现 '-'
            ((AbstractDocument) inputField.getDocument()).setDocumentFilter(new IntegerDocumentFilter());

            left.add(inputField);
            left.add(Box.createVerticalStrut(10));

            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            submitBtn = new JButton("提交");
            giveUpBtn = new JButton("放弃");
            pauseBtn = new JButton("暂停");
            btnRow.add(submitBtn);
            btnRow.add(giveUpBtn);
            btnRow.add(pauseBtn);
            left.add(btnRow);

            // 按钮事件
            submitBtn.addActionListener(e -> handleSubmit());
            giveUpBtn.addActionListener(e -> handleGiveUp());
            pauseBtn.addActionListener(e -> handlePause());

            // 回车提交
            inputField.addActionListener(e -> handleSubmit());

            // 右侧历史
            historyArea = new JTextArea();
            historyArea.setEditable(false);
            historyArea.setFont(new Font(parent.getSettings().fontName, Font.PLAIN, 14));
            historyArea.setLineWrap(true);
            JScrollPane sp = new JScrollPane(historyArea);
            sp.setBorder(BorderFactory.createTitledBorder("历史记录"));
            sp.setPreferredSize(new Dimension(380, 300));

            center.add(left, BorderLayout.WEST);
            center.add(sp, BorderLayout.CENTER);

            add(center, BorderLayout.CENTER);

            // 底部状态条
            bottomStatus = new JLabel("状态：");
            bottomStatus.setFont(new Font(parent.getSettings().fontName, Font.PLAIN, 14));
            bottomStatus.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            add(bottomStatus, BorderLayout.SOUTH);

            // 快捷键：Esc 放弃
            InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap am = getActionMap();
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "giveup");
            am.put("giveup", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) { handleGiveUp(); }
            });

            // 历史区初始背景
            normalHistoryBG = historyArea.getBackground();
        }

        public void prepareForNewGame() {
            // 初始化 UI，读取设置样式
            Settings s = parent.getSettings();
            // 应用字体和颜色
            try {
                inputField.setFont(new Font(s.fontName, Font.PLAIN, s.fontSize));
                historyArea.setFont(new Font(s.fontName, Font.PLAIN, Math.max(13, s.fontSize - 2)));
            } catch (Exception e) { /* ignore */ }
            inputField.setText("");
            historyArea.setText("");
            lastValidInput = 0;
            // 顶部信息
            updateTopInfo();
            updateBottomStatus();

            // 将焦点放到输入框
            SwingUtilities.invokeLater(() -> inputField.requestFocusInWindow());
        }

        private void updateTopInfo() {
            Settings s = parent.getSettings();
            String modeText = s.challengeMode ? "挑战模式" : "猜数字模式";
            String interval = String.format("%d ～ %d", s.minValue, s.maxValue);
            String leftInfo = String.format("当前模式：%s    区间：%s", modeText, interval);
            topInfoLabel.setText(leftInfo);
        }

        private void updateBottomStatus() {
            Settings s = parent.getSettings();
            String status = String.format("已猜 %d 次", parent.getController().getAttempts());
            if (s.challengeMode) {
                int remaining = s.maxAttempts - parent.getController().getAttempts();
                status += String.format("    剩余 %d 次", Math.max(0, remaining));
            }
            bottomStatus.setText("状态：" + status);
        }

        private void handleSubmit() {
            if (!parent.getController().isRunning()) {
                JOptionPane.showMessageDialog(parent, "当前没有进行中的游戏，请返回主界面开始新局。", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String t = inputField.getText().trim();
            if (t.isEmpty()) {
                JOptionPane.showMessageDialog(parent, "请输入一个整数后再提交。", "输入错误", JOptionPane.WARNING_MESSAGE);
                inputField.setText(Integer.toString(lastValidInput));
                return;
            }
            // 校验整数格式（已由 DocumentFilter 限制，但还需解析异常）
            int val;
            try {
                val = Integer.parseInt(t);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(parent, "输入无效：请输入整数（区间内）。", "输入错误", JOptionPane.WARNING_MESSAGE);
                inputField.setText(Integer.toString(lastValidInput));
                return;
            }
            // 校验区间
            Settings s = parent.getSettings();
            if (val < s.minValue || val > s.maxValue) {
                JOptionPane.showMessageDialog(parent, String.format("输入超出区间，请输入 %d 到 %d 之间的整数。", s.minValue, s.maxValue), "范围错误", JOptionPane.WARNING_MESSAGE);
                inputField.setText(Integer.toString(lastValidInput));
                return;
            }

            lastValidInput = val;
            Map<String, Object> res = parent.getController().submitGuess(val);
            processResult(val, res);
        }

        private void processResult(int guess, Map<String, Object> res) {
            String status = (String) res.get("status");
            int attempts = (res.get("attempts") == null) ? parent.getController().getAttempts() : (int) res.get("attempts");
            String stamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            if ("correct".equals(status)) {
                int target = (int) res.get("target");
                appendHistory(String.format("%s 你猜 %d → 正确！答案：%d    （共 %d 次）", stamp, guess, target, attempts));
                // 加点动效：历史区闪烁
                startFlashAnimation();
                JOptionPane.showMessageDialog(parent, String.format("恭喜你！第 %d 次猜中，答案是：%d", attempts, target), "胜利", JOptionPane.INFORMATION_MESSAGE);
                parent.restartToStart();
            } else if ("failed".equals(status)) {
                int target = (int) res.get("target");
                appendHistory(String.format("%s 你猜 %d → %s    （第 %d 次）", stamp, guess, "仍未猜中", attempts));
                JOptionPane.showMessageDialog(parent, String.format("挑战失败，已用完所有次数。正确答案：%d", target), "挑战失败", JOptionPane.INFORMATION_MESSAGE);
                parent.restartToStart();
            } else if ("low".equals(status) || "high".equals(status)) {
                String hint = "偏小";
                if ("high".equals(status)) hint = "偏大";
                appendHistory(String.format("%s 你猜 %d → %s    （第 %d 次）", stamp, guess, hint, attempts));
                updateBottomStatus();
                // 若挑战模式，显示剩余次数
                if (parent.getSettings().challengeMode) {
                    int remaining = (int) res.getOrDefault("remaining", parent.getSettings().maxAttempts - attempts);
                    // 小提示
                    JOptionPane.showMessageDialog(parent, String.format("提示：%s。剩余 %d 次。", hint, Math.max(0, remaining)), "提示", JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                // status: not_running etc.
                JOptionPane.showMessageDialog(parent, "游戏已结束或未开始。", "提示", JOptionPane.INFORMATION_MESSAGE);
            }
            inputField.requestFocusInWindow();
            inputField.selectAll();
        }

        private void appendHistory(String line) {
            historyArea.append(line + "\n");
            historyArea.setCaretPosition(historyArea.getDocument().getLength());
        }

        private void handleGiveUp() {
            if (!parent.getController().isRunning()) {
                JOptionPane.showMessageDialog(parent, "当前没有进行中的游戏。", "提示", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int ok = JOptionPane.showConfirmDialog(parent, "确认放弃本局并显示答案吗？", "放弃确认", JOptionPane.YES_NO_OPTION);
            if (ok != JOptionPane.YES_OPTION) return;
            Map<String, Object> res = parent.getController().giveUp();
            int target = (int) res.get("target");
            appendHistory(String.format("%s 你选择放弃。答案：%d", new SimpleDateFormat("HH:mm:ss").format(new Date()), target));
            JOptionPane.showMessageDialog(parent, "已放弃，本局答案是：" + target, "放弃", JOptionPane.INFORMATION_MESSAGE);
            parent.restartToStart();
        }

        private void handlePause() {
            // 弹出暂停对话框（只读设置和已猜次数）
            JDialog dlg = new JDialog(parent, "游戏已暂停", true);
            dlg.setLayout(new BorderLayout());
            JPanel info = new JPanel();
            info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
            Settings s = parent.getSettings();
            info.add(new JLabel("当前模式：" + (s.challengeMode ? "挑战模式" : "猜数字模式")));
            info.add(new JLabel(String.format("区间：%d ～ %d", s.minValue, s.maxValue)));
            info.add(new JLabel("已猜次数：" + parent.getController().getAttempts()));
            if (s.challengeMode) info.add(new JLabel("挑战限定次数：" + s.maxAttempts));
            info.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            dlg.add(info, BorderLayout.CENTER);
            JButton resume = new JButton("继续游戏");
            resume.addActionListener(e -> dlg.dispose());
            JPanel btnp = new JPanel();
            btnp.add(resume);
            dlg.add(btnp, BorderLayout.SOUTH);
            dlg.setSize(360, 200);
            dlg.setLocationRelativeTo(parent);
            dlg.setVisible(true);
        }

        private void startFlashAnimation() {
            if (flashTimer != null && flashTimer.isRunning()) flashTimer.stop();
            flashCount = 0;
            flashTimer = new javax.swing.Timer(200, e -> {
                if (flashCount % 2 == 0) {
                    historyArea.setBackground(new Color(255, 240, 200));
                } else {
                    historyArea.setBackground(normalHistoryBG);
                }
                flashCount++;
                if (flashCount > 6) {
                    flashTimer.stop();
                    historyArea.setBackground(normalHistoryBG);
                }
            });
            flashTimer.start();
        }

        // DocumentFilter: 允许首位'-'与后续数字，阻止其他字符
        class IntegerDocumentFilter extends DocumentFilter {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                StringBuilder sb = new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
                sb.insert(offset, string);
                if (isValidIntegerInput(sb.toString())) {
                    super.insertString(fb, offset, string, attr);
                } else {
                    // 吃掉
                    Toolkit.getDefaultToolkit().beep();
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                StringBuilder sb = new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
                sb.replace(offset, offset + length, text == null ? "" : text);
                if (isValidIntegerInput(sb.toString())) {
                    super.replace(fb, offset, length, text, attrs);
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
            }

            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                super.remove(fb, offset, length);
            }

            private boolean isValidIntegerInput(String s) {
                if (s == null || s.isEmpty()) return true;
                if (s.equals("-")) return true;
                // 限制长度为 12 位以内，防止极大数输入
                if (s.length() > 12) return false;
                // 允许负号开头
                int start = 0;
                if (s.charAt(0) == '-') {
                    if (s.length() == 1) return true;
                    start = 1;
                }
                for (int i = start; i < s.length(); i++) {
                    if (!Character.isDigit(s.charAt(i))) return false;
                }
                return true;
            }
        }
    }

    // -------------------------------
    // SettingDialog（设置对话框）
    // 只能在未开始时打开；非法设置不保存
    // -------------------------------
    static class SettingDialog extends JDialog {
        private Settings temp; // 临时拷贝用于编辑
        private Settings persisted;
        private JTextField minField, maxField, attemptsField, fontSizeField;
        private JCheckBox challengeCB, soundCB, showIntroCB;
        private JComboBox<String> fontCombo;
        private JButton colorBtn;
        private Color chosenColor;

        public SettingDialog(JFrame owner, Settings base) {
            super(owner, "游戏设置", true);
            this.persisted = base;
            this.temp = copySettings(base);
            initUI();
            setSize(520, 380);
            setLocationRelativeTo(owner);
        }

        private Settings copySettings(Settings s) {
            Settings n = new Settings();
            n.minValue = s.minValue;
            n.maxValue = s.maxValue;
            n.challengeMode = s.challengeMode;
            n.maxAttempts = s.maxAttempts;
            n.fontName = s.fontName;
            n.fontSize = s.fontSize;
            n.fontColor = s.fontColor;
            n.soundOn = s.soundOn;
            n.showIntro = s.showIntro;
            return n;
        }

        private void initUI() {
            setLayout(new BorderLayout());
            JPanel main = new JPanel();
            main.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 8, 8, 8);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            // 区间
            gbc.gridx = 0; gbc.gridy = 0; main.add(new JLabel("数字区间（整数，可负）:"), gbc);
            minField = new JTextField(Integer.toString(temp.minValue)); maxField = new JTextField(Integer.toString(temp.maxValue));
            JPanel rangeP = new JPanel(new GridLayout(1, 3, 6, 6));
            rangeP.add(minField); rangeP.add(new JLabel("—", SwingConstants.CENTER)); rangeP.add(maxField);
            gbc.gridx = 1; gbc.gridy = 0; main.add(rangeP, gbc);

            // 模式
            gbc.gridx = 0; gbc.gridy = 1; main.add(new JLabel("游戏模式:"), gbc);
            JPanel modeP = new JPanel(new FlowLayout(FlowLayout.LEFT));
            challengeCB = new JCheckBox("挑战模式（启用后需设置限定次数）", temp.challengeMode);
            modeP.add(challengeCB);
            gbc.gridx = 1; gbc.gridy = 1; main.add(modeP, gbc);

            // 限定次数
            gbc.gridx = 0; gbc.gridy = 2; main.add(new JLabel("限定次数（挑战模式）:"), gbc);
            attemptsField = new JTextField(Integer.toString(temp.maxAttempts));
            gbc.gridx = 1; gbc.gridy = 2; main.add(attemptsField, gbc);

            // 字体/大小/颜色
            gbc.gridx = 0; gbc.gridy = 3; main.add(new JLabel("字体（中文优先）:"), gbc);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            String[] fonts = ge.getAvailableFontFamilyNames();
            // 只保留常用中文友好字体为首选项
            java.util.List<String> perf = new ArrayList<>(Arrays.asList("微软雅黑", "宋体", "黑体", "仿宋", "楷体"));
            java.util.List<String> other = new ArrayList<>();
            for (String f : fonts) if (!perf.contains(f)) other.add(f);
            String[] combined = new String[perf.size() + 10];
            int idx = 0;
            for (String s : perf) combined[idx++] = s;
            for (int i = 0; i < 10 && i < other.size(); i++) combined[idx++] = other.get(i);
            fontCombo = new JComboBox<>(Arrays.copyOf(combined, idx));
            fontCombo.setSelectedItem(temp.fontName);
            gbc.gridx = 1; gbc.gridy = 3; main.add(fontCombo, gbc);

            gbc.gridx = 0; gbc.gridy = 4; main.add(new JLabel("字体大小:"), gbc);
            fontSizeField = new JTextField(Integer.toString(temp.fontSize));
            gbc.gridx = 1; gbc.gridy = 4; main.add(fontSizeField, gbc);

            gbc.gridx = 0; gbc.gridy = 5; main.add(new JLabel("字体颜色:"), gbc);
            colorBtn = new JButton("选择颜色");
            chosenColor = temp.fontColor;
            colorBtn.addActionListener(e -> {
                Color c = JColorChooser.showDialog(this, "选择字体颜色", chosenColor);
                if (c != null) chosenColor = c;
            });
            gbc.gridx = 1; gbc.gridy = 5; main.add(colorBtn, gbc);

            // 声音 & 介绍提示
            gbc.gridx = 0; gbc.gridy = 6; main.add(new JLabel("其它:"), gbc);
            JPanel misc = new JPanel(new FlowLayout(FlowLayout.LEFT));
            soundCB = new JCheckBox("开启提示音", temp.soundOn);
            showIntroCB = new JCheckBox("程序启动时显示规则", temp.showIntro);
            misc.add(soundCB); misc.add(showIntroCB);
            gbc.gridx = 1; gbc.gridy = 6; main.add(misc, gbc);

            add(main, BorderLayout.CENTER);

            // 按钮
            JPanel btns = new JPanel();
            JButton save = new JButton("保存");
            JButton cancel = new JButton("取消");
            btns.add(save); btns.add(cancel);
            add(btns, BorderLayout.SOUTH);

            cancel.addActionListener(e -> dispose());
            save.addActionListener(e -> {
                if (applyAndValidate()) {
                    // 保存到 persisted settings
                    persisted.minValue = temp.minValue;
                    persisted.maxValue = temp.maxValue;
                    persisted.challengeMode = temp.challengeMode;
                    persisted.maxAttempts = temp.maxAttempts;
                    persisted.fontName = temp.fontName;
                    persisted.fontSize = temp.fontSize;
                    persisted.fontColor = temp.fontColor;
                    persisted.soundOn = temp.soundOn;
                    persisted.showIntro = temp.showIntro;
                    if (!persisted.save()) {
                        JOptionPane.showMessageDialog(this, "保存失败（无法写入配置文件）。", "保存错误", JOptionPane.ERROR_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, "设置已保存。", "提示", JOptionPane.INFORMATION_MESSAGE);
                    }
                    dispose();
                }
            });
        }

        private boolean applyAndValidate() {
            // 解析并校验输入
            int min, max, attempts;
            try {
                min = Integer.parseInt(minField.getText().trim());
                max = Integer.parseInt(maxField.getText().trim());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "区间必须为整数。", "输入错误", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            if (min >= max) {
                JOptionPane.showMessageDialog(this, "区间设置错误：最小值必须小于最大值。", "设置错误", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            if ((long)max - (long)min < 10L) {
                JOptionPane.showMessageDialog(this, "区间长度必须至少为 10。", "设置错误", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            boolean challenge = challengeCB.isSelected();
            if (challenge) {
                try {
                    attempts = Integer.parseInt(attemptsField.getText().trim());
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "限定次数必须为整数。", "输入错误", JOptionPane.WARNING_MESSAGE);
                    return false;
                }
                if (attempts <= 0) {
                    JOptionPane.showMessageDialog(this, "限定次数必须大于 0。", "设置错误", JOptionPane.WARNING_MESSAGE);
                    return false;
                }
            } else {
                attempts = Integer.parseInt(attemptsField.getText().trim());
                if (attempts <= 0) attempts = 10; // 兜底
            }

            // 字体大小
            int fsize;
            try {
                fsize = Integer.parseInt(fontSizeField.getText().trim());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "字体大小必须为整数。", "输入错误", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            if (fsize < 10 || fsize > 36) {
                JOptionPane.showMessageDialog(this, "字体大小建议 10~36。", "设置错误", JOptionPane.WARNING_MESSAGE);
                return false;
            }

            // 确认通过，写入 temp
            temp.minValue = min;
            temp.maxValue = max;
            temp.challengeMode = challenge;
            temp.maxAttempts = attempts;
            temp.fontName = (String) fontCombo.getSelectedItem();
            temp.fontSize = fsize;
            temp.fontColor = chosenColor;
            temp.soundOn = soundCB.isSelected();
            temp.showIntro = showIntroCB.isSelected();

            return true;
        }
    }
}
