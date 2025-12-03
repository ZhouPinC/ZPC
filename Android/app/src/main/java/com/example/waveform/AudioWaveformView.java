package com.example.waveform;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class AudioWaveformView extends View {

    private static final int MIN_HEIGHT_DP = 12;
    private static final int MAX_HEIGHT_DP = 58;
    private static final int BAR_SPACING_DP = 6;
    private static final int BAR_WIDTH_DP = 4;
    private static final int MAX_BARS = 50;

    private Paint barPaint;
    private List<Float> amplitudes;
    private int barWidth;
    private int barSpacing;
    private int minHeight;
    private int maxHeight;
    private float animationOffset = 0f;
    private boolean isAnimating = false;

    public AudioWaveformView(Context context) {
        super(context);
        init();
    }

    public AudioWaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AudioWaveformView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        amplitudes = new ArrayList<>();

        // 初始化画笔
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(ContextCompat.getColor(getContext(), R.color.ios_blue));
        barPaint.setStyle(Paint.Style.FILL);

        // 转换dp到px
        float density = getResources().getDisplayMetrics().density;
        barWidth = (int) (BAR_WIDTH_DP * density);
        barSpacing = (int) (BAR_SPACING_DP * density);
        minHeight = (int) (MIN_HEIGHT_DP * density);
        maxHeight = (int) (MAX_HEIGHT_DP * density);

        // 初始化波形数据，确保有初始值
        initializeWaveformData();
    }

    /**
     * 初始化波形数据
     */
    private void initializeWaveformData() {
        // 清空现有数据
        amplitudes.clear();
        // 添加一些默认的波形数据，确保视图显示时有内容
        for (int i = 0; i < MAX_BARS; i++) {
            // 添加一些有规律的数据，而不是完全随机的数据
            float defaultHeight = minHeight + (float) (i % 10) / 10f * (maxHeight - minHeight);
            amplitudes.add(defaultHeight);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (amplitudes.isEmpty()) return;

        int centerY = getHeight() / 2;
        int totalWidth = getWidth();
        int barTotalWidth = barWidth + barSpacing;

        // 计算可见的条形数量
        int visibleBars = Math.min(amplitudes.size(), totalWidth / barTotalWidth + 1);
        
        // 计算起始索引（考虑动画偏移）
        int startIndex = Math.max(0, amplitudes.size() - visibleBars);

        // 绘制波形条，从左向右
        for (int i = 0; i < visibleBars; i++) {
            // 计算当前条形的索引
            int currentIndex = startIndex + i;
            // 应用动画偏移
            int animatedIndex = (int) ((currentIndex + animationOffset) % amplitudes.size());
            if (animatedIndex < 0) animatedIndex += amplitudes.size();

            float amplitude = amplitudes.get(animatedIndex);
            int x = i * barTotalWidth;

            if (x > totalWidth) break;

            // 绘制中心对称的波形条
            int barHeight = (int) amplitude;
            int topY = centerY - barHeight / 2;
            int bottomY = centerY + barHeight / 2;

            // 圆角矩形
            float radius = barWidth / 2f;
            canvas.drawRoundRect(
                x, topY,
                x + barWidth, bottomY,
                radius, radius,
                barPaint
            );
        }
    }

    /**
     * 添加新的音频振幅数据
     */
    public void addAmplitude(float amplitude) {
        // 将振幅映射到高度范围
        float normalizedHeight = minHeight + (amplitude / 32767f) * (maxHeight - minHeight);
        normalizedHeight = Math.max(minHeight, Math.min(maxHeight, normalizedHeight));

        amplitudes.add(normalizedHeight);

        // 保持最大条数限制
        if (amplitudes.size() > MAX_BARS) {
            amplitudes.remove(0);
        }

        invalidate();
    }

    /**
     * 开始动画
     */
    public void startAnimation() {
        if (isAnimating) return;
        isAnimating = true;

        post(new Runnable() {
            @Override
            public void run() {
                if (!isAnimating) return;

                animationOffset += 0.5f; // 控制动画速度
                if (animationOffset >= amplitudes.size()) {
                    animationOffset = 0;
                }

                invalidate();
                postDelayed(this, 16); // 约60fps
            }
        });
    }

    /**
     * 停止动画
     */
    public void stopAnimation() {
        isAnimating = false;
    }

    /**
     * 清空波形数据
     */
    public void clear() {
        // 清空现有数据
        amplitudes.clear();
        // 重新添加一些默认数据，避免视图显示空白
        for (int i = 0; i < 10; i++) {
            amplitudes.add((float) minHeight);
        }
        animationOffset = 0;
        invalidate();
    }

    /**
     * 更新条形颜色
     */
    public void setBarColor(int color) {
        barPaint.setColor(color);
        invalidate();
    }
}