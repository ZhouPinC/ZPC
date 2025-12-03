package com.example.waveform;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.DialogInterface;
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
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.Locale;

public class RecorderBottomSheetFragment extends BottomSheetDialogFragment {

    private AudioRecorder audioRecorder;
    private AudioWaveformView waveformView;
    private View btnRecord, bottomSheetRoot;
    private TextView tvTimer, tvStatus;
    private ImageView btnHistory;
    
    private boolean isRecording = false;
    private long startTime = 0;
    private Handler timerHandler = new Handler(Looper.getMainLooper());

    // 颜色定义
    private final int COLOR_BG_IDLE = 0xFFFFFFFF; // 纯白色 (不透明)
    private final int COLOR_BG_RECORDING = 0x80FFFFFF; // 50% 透明度白色
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 使用一个新的布局文件
        View view = inflater.inflate(R.layout.fragment_recorder_bottom_sheet, container, false);
        initViews(view);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // 设置 BottomSheet 默认全展开且背景透明(以便我们要自定义的背景色生效)
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if (dialog != null) {
            // 移除默认的白色背景，以便我们自己控制背景色和圆角
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                bottomSheet.setBackground(new ColorDrawable(Color.TRANSPARENT));
                
                // 强制展开
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }

    private void initViews(View view) {
        bottomSheetRoot = view.findViewById(R.id.bottomSheetRoot);
        waveformView = view.findViewById(R.id.waveformView);
        btnRecord = view.findViewById(R.id.btnRecord);
        tvTimer = view.findViewById(R.id.tvTimer);
        tvStatus = view.findViewById(R.id.tvStatus);
        btnHistory = view.findViewById(R.id.btnHistory);

        // 初始背景：白色
        bottomSheetRoot.setBackgroundColor(COLOR_BG_IDLE);

        // 历史记录跳转
        btnHistory.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(requireActivity(), RecordHistoryActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "无法打开历史记录", Toast.LENGTH_SHORT).show();
            }
        });

        audioRecorder = new AudioRecorder(requireContext());
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
        
        // 1. 背景颜色过渡动画 (白色 -> 半透明白)
        animateBackground(COLOR_BG_IDLE, COLOR_BG_RECORDING);
        
        tvStatus.setText("正在录音...");
        tvStatus.setTextColor(0xFFFF3B30); // 红色提示
        waveformView.setVisibility(View.VISIBLE);
        waveformView.clear();
        
        // 按钮视觉反馈
        btnRecord.animate().scaleX(0.6f).scaleY(0.6f).setDuration(300).start();
        // 注意：这里假设我们有bg_record_btn_square资源，如果没有，暂时注释掉
        // btnRecord.setBackgroundResource(R.drawable.bg_record_btn_square); // 切换为方形停止图标
    }

    private void stopRecording() {
        if (!isRecording) return;
        isRecording = false;
        
        String filePath = audioRecorder.stopRecording();
        timerHandler.removeCallbacks(timerRunnable);
        
        // 1. 背景颜色恢复动画 (半透明白 -> 白色)
        animateBackground(COLOR_BG_RECORDING, COLOR_BG_IDLE);
        
        tvStatus.setText("录音已保存");
        tvStatus.setTextColor(0xFF8E8E93); // 灰色
        tvTimer.setText("00:00");
        waveformView.setVisibility(View.INVISIBLE);
        
        // 按钮恢复
        btnRecord.animate().scaleX(1f).scaleY(1f).setDuration(300).start();
        btnRecord.setBackgroundResource(R.drawable.bg_record_button_inner_red); // 恢复圆形
        
        if (filePath != null) {
            Toast.makeText(getContext(), "保存成功", Toast.LENGTH_SHORT).show();
        }
    }

    private void animateBackground(int fromColor, int toColor) {
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), fromColor, toColor);
        colorAnimation.setDuration(500); // 500ms 舒适过渡
        colorAnimation.addUpdateListener(animator -> 
            bottomSheetRoot.setBackgroundColor((int) animator.getAnimatedValue())
        );
        colorAnimation.start();
    }

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRecording) return;
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds %= 60;
            tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            timerHandler.postDelayed(this, 500);
        }
    };

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (isRecording) {
            audioRecorder.stopRecording();
            timerHandler.removeCallbacks(timerRunnable);
        }
    }
}