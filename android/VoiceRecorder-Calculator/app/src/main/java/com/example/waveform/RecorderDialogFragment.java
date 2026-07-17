package com.example.waveform;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import java.util.Locale;

public class RecorderDialogFragment extends DialogFragment {

    private AudioRecorder audioRecorder;
    private AudioWaveformView waveformView;
    private View btnRecord, rootLayout;
    private TextView tvTimer, tvStatus;
    private boolean isRecording = false;
    private long startTime = 0;
    private Handler timerHandler = new Handler(Looper.getMainLooper());

    // 颜色定义
    private final int COLOR_BG_IDLE = 0x1A000000; // 浅透 (未录音)
    private final int COLOR_BG_RECORDING = 0xB3000000; // 深透 (录音中)
    private final int COLOR_TEXT_IDLE = 0xFF000000;
    private final int COLOR_TEXT_RECORDING = 0xFFFFFFFF;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            // 应用放大动画
            getDialog().getWindow().setWindowAnimations(R.style.DialogAnimation);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_recorder_fullscreen, container, false);
        initViews(view);
        return view;
    }

    private void initViews(View view) {
        rootLayout = view.findViewById(R.id.rootLayout);
        waveformView = view.findViewById(R.id.waveformView);
        btnRecord = view.findViewById(R.id.btnRecord);
        tvTimer = view.findViewById(R.id.tvTimer);
        tvStatus = view.findViewById(R.id.tvStatus);
        
        // 点击空白处关闭
        view.findViewById(R.id.touchOutside).setOnClickListener(v -> {
            if (!isRecording) dismiss();
        });

        // 跳转历史记录
        view.findViewById(R.id.btnHistory).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), RecordHistoryActivity.class));
        });

        audioRecorder = new AudioRecorder(getContext());
        audioRecorder.setAmplitudeListener(amplitude -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> waveformView.addAmplitude(amplitude));
            }
        });

        btnRecord.setOnClickListener(v -> {
            if (isRecording) stopRecording(); else startRecording();
        });
    }

    private void startRecording() {
        if (isRecording) return;
        isRecording = true;
        
        audioRecorder.startRecording();
        startTime = System.currentTimeMillis();
        timerHandler.post(timerRunnable);
        
        // 视觉变化
        animateBackground(COLOR_BG_IDLE, COLOR_BG_RECORDING);
        updateTextColor(COLOR_TEXT_RECORDING);
        
        tvStatus.setText("正在录音...");
        waveformView.setVisibility(View.VISIBLE);
        waveformView.clear();
        
        // 按钮动画：变方、变小
        btnRecord.animate().scaleX(0.5f).scaleY(0.5f).setDuration(300).start();
    }

    private void stopRecording() {
        if (!isRecording) return;
        isRecording = false;
        
        String filePath = audioRecorder.stopRecording();
        timerHandler.removeCallbacks(timerRunnable);
        
        // 视觉恢复
        animateBackground(COLOR_BG_RECORDING, COLOR_BG_IDLE);
        updateTextColor(COLOR_TEXT_IDLE);
        
        tvStatus.setText("已保存");
        tvTimer.setText("00:00");
        waveformView.setVisibility(View.INVISIBLE);
        
        // 按钮恢复
        btnRecord.animate().scaleX(1f).scaleY(1f).setDuration(300).start();
        
        Toast.makeText(getContext(), "录音已保存至历史记录", Toast.LENGTH_SHORT).show();
    }

    private void animateBackground(int fromColor, int toColor) {
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), fromColor, toColor);
        colorAnimation.setDuration(500); // 500ms 平滑过渡
        colorAnimation.addUpdateListener(animator -> rootLayout.setBackgroundColor((int) animator.getAnimatedValue()));
        colorAnimation.start();
    }

    private void updateTextColor(int color) {
        tvTimer.setTextColor(color);
        // 状态文字稍微浅一点
        tvStatus.setTextColor(color == COLOR_TEXT_RECORDING ? 0xCCFFFFFF : 0xCC000000);
        // 更新历史记录按钮文字颜色
        LinearLayout btnHistory = rootLayout.findViewById(R.id.btnHistory);
        TextView tvHistText = btnHistory.findViewById(android.R.id.text1);
        ImageView ivHistIcon = btnHistory.findViewById(android.R.id.icon);
        
        // 直接通过索引获取子View，因为我们知道布局结构
        for (int i = 0; i < btnHistory.getChildCount(); i++) {
            View child = btnHistory.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(color == COLOR_TEXT_RECORDING ? 0xFFFFFFFF : 0xFF333333);
            } else if (child instanceof ImageView) {
                ((ImageView) child).setAlpha(color == COLOR_TEXT_RECORDING ? 1.0f : 0.7f);
            }
        }
    }

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds %= 60;
            
            tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            if (isRecording) timerHandler.postDelayed(this, 500);
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isRecording) audioRecorder.stopRecording();
    }
}