package com.example.waveform;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class RecordDialogFragment extends DialogFragment {

    private AudioWaveformView waveformView;
    private AudioRecorder audioRecorder;
    private View btnRecord, btnHistory, rootContainer, recorderBox;
    private TextView tvTimer, tvHint;
    
    private boolean isRecording = false;
    private long startTime = 0;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    
    // 背景遮罩动画相关
    private static final int ANIM_DURATION = 300;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置全屏透明样式
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recorder, container, false);
        
        // 确保Dialog背景透明
        if(getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND); // 我们自己控制变暗
        }

        initViews(view);
        setupRecorder();
        return view;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initViews(View view) {
        waveformView = view.findViewById(R.id.waveformView);
        btnRecord = view.findViewById(R.id.btnRecord);
        btnHistory = view.findViewById(R.id.btnHistory);
        rootContainer = view.findViewById(R.id.rootContainer);
        recorderBox = view.findViewById(R.id.recorderBox);
        tvTimer = view.findViewById(R.id.tvTimer);
        tvHint = view.findViewById(R.id.tvHint);

        // 初始状态：轻微透明背景
        rootContainer.setBackgroundColor(Color.parseColor("#4D000000")); // 30% Black

        // 点击空白处退出
        view.findViewById(R.id.touchOutside).setOnClickListener(v -> {
            if (!isRecording) dismiss();
        });

        // 历史记录跳转
        btnHistory.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), RecordHistoryActivity.class));
        });

        // 录音按钮：支持点击和长按
        btnRecord.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // 如果是长按逻辑，可以在这里启动，但为了混合支持，通常长按需要延时判断
                    // 这里为了简化“既支持轻点又支持长按”，采用简单的逻辑：
                    // ACTION_DOWN 按下不马上开始，通过 Handler 区分长按
                    break;
                case MotionEvent.ACTION_UP:
                    toggleRecording();
                    break;
            }
            return true;
        });
        
        // 注意：严格区分长按和点击需要更复杂的GestureDetector，
        // 这里简化为点击切换模式以保证用户体验流畅。
    }

    private void setupRecorder() {
        audioRecorder = new AudioRecorder(requireContext());
        audioRecorder.setAmplitudeListener(amplitude -> {
            if(getActivity() != null) {
                getActivity().runOnUiThread(() -> waveformView.addAmplitude(amplitude));
            }
        });
    }

    private void toggleRecording() {
        if (isRecording) {
            stopState();
        } else {
            startState();
        }
    }

    private void startState() {
        isRecording = true;
        
        // 1. 视觉变化
        // 背景变暗 (Opacity 增加)
        ObjectAnimator colorAnim = ObjectAnimator.ofArgb(rootContainer, "backgroundColor",
                Color.parseColor("#4D000000"), Color.parseColor("#CC000000")); // 80% Black
        colorAnim.setDuration(ANIM_DURATION).start();

        // 按钮变方 (视觉反馈) - 实际项目中建议使用 AnimatedVectorDrawable
        btnRecord.animate().scaleX(0.6f).scaleY(0.6f).setDuration(200).start();
        btnRecord.setBackgroundResource(R.drawable.bg_record_btn_inner_rec); // 使用矩形背景

        // 显示波形和计时器
        waveformView.setVisibility(View.VISIBLE);
        waveformView.clear();
        tvTimer.setVisibility(View.VISIBLE);
        tvHint.setText("Recording...");
        
        // 2. 逻辑启动
        audioRecorder.startRecording();
        startTime = System.currentTimeMillis();
        timerHandler.post(timerRunnable);
    }

    private void stopState() {
        isRecording = false;

        // 1. 停止逻辑
        String filePath = audioRecorder.stopRecording();
        timerHandler.removeCallbacks(timerRunnable);
        Toast.makeText(getContext(), "Saved: " + filePath, Toast.LENGTH_SHORT).show();

        // 2. 视觉恢复
        ObjectAnimator colorAnim = ObjectAnimator.ofArgb(rootContainer, "backgroundColor",
                Color.parseColor("#CC000000"), Color.parseColor("#4D000000"));
        colorAnim.setDuration(ANIM_DURATION).start();

        btnRecord.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
        btnRecord.setBackgroundResource(R.drawable.bg_record_btn_inner_stop); // 恢复圆形背景
        
        waveformView.setVisibility(View.INVISIBLE);
        tvTimer.setVisibility(View.INVISIBLE);
        tvHint.setText("Tap to Record");
        tvTimer.setText("00:00");
    }

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if(!isRecording) return;
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            tvTimer.setText(String.format(Locale.US, "%02d:%02d", minutes, seconds));
            timerHandler.postDelayed(this, 500);
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isRecording) {
            audioRecorder.stopRecording();
        }
    }
}