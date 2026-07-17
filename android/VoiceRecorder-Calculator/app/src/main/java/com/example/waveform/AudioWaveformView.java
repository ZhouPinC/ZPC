package com.example.waveform;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.LinkedList;

public class AudioWaveformView extends View {
    private LinkedList<Float> amplitudes = new LinkedList<>();
    private Paint paint;
    
    private float barWidth = 10f; // 3-4dp
    private float gap = 12f;      // 间隔
    private float minHeight = 10f;
    private float maxHeight = 150f;

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
        paint = new Paint();
        paint.setColor(0xFFFF3B30); // iOS Red
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAntiAlias(true);
        
        float density = getResources().getDisplayMetrics().density;
        barWidth = 4 * density; // 约3-4dp
        gap = 6 * density; // 6dp间隔 (题目要求)
        minHeight = 4 * density;
        maxHeight = 58 * density; // 约58dp (题目要求)
    }

    public void addAmplitude(float amp) {
        // 归一化 (0.0 - 1.0)
        float ratio = Math.min(amp / 20000f, 1.0f);
        amplitudes.add(ratio);
        
        // 移除屏幕外的旧数据
        int maxBars = (int)(getWidth() / (barWidth + gap)) + 2;
        if (amplitudes.size() > maxBars) {
            amplitudes.removeFirst();
        }
        invalidate();
    }
    
    public void clear() {
        amplitudes.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float centerY = getHeight() / 2f;
        // 从最右侧开始绘制
        float x = getWidth() - barWidth;
        
        // 倒序遍历（最新的在最右边）
        for (int i = amplitudes.size() - 1; i >= 0; i--) {
            float ratio = amplitudes.get(i);
            // 简单的非线性平滑
            float height = minHeight + (maxHeight - minHeight) * ratio;
            
            canvas.drawRoundRect(
                x, centerY - height/2,
                x + barWidth, centerY + height/2,
                barWidth/2, barWidth/2,
                paint
            );
            x -= (barWidth + gap);
            if (x < -barWidth) break;
        }
    }
}