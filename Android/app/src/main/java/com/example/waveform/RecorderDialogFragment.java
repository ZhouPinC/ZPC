package com.example.waveform;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class RecorderDialogFragment extends DialogFragment {

    private AudioRecorder audioRecorder;
    private AudioWaveformView waveformView;
    private View btnRecord;
    private TextView tvTimer, tvStatus;
    private boolean isRecording = false;
    private long startTime = 0;
    private Handler timerHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 全屏样式
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Override
    public void onStart() {
        super.onStart();
        // 设置入场动画和背景
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setWindowAnimations(R.style.DialogAnimation); // 应用缩放动画
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
        waveformView = view.findViewById(R.id.waveformView);
        btnRecord = view.findViewById(R.id.btnRecord);
        tvTimer = view.findViewById(R.id.tvTimer);
        tvStatus = view.findViewById(R.id.tvStatus);

        // 点击空白处退出
        view.findViewById(R.id.touchOutside).setOnClickListener(v -> {
            if (!isRecording) dismiss();
        });
        
        // 历史记录
        view.findViewById(R.id.btnHistory).setOnClickListener(v -> {
             // startActivity(new Intent(getActivity(), HistoryActivity.class));
             Toast.makeText(getContext(), "History Clicked", Toast.LENGTH_SHORT).show();
        });

        audioRecorder = new AudioRecorder(getContext());
        audioRecorder.setAmplitudeListener(amplitude -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> waveformView.addAmplitude(amplitude));
            }
        });

        // 录音按钮点击
        btnRecord.setOnClickListener(v -> {
            if (isRecording) stopRecording(); else startRecording();
        });
        
        // 支持长按录音 (可选)
        btnRecord.setOnLongClickListener(v -> {
            startRecording();
            return true;
        });
    }

    private void startRecording() {
        if (isRecording) return;
        isRecording = true;
        
        audioRecorder.startRecording();
        startTime = System.currentTimeMillis();
        timerHandler.post(timerRunnable);
        
        // UI 更新
        tvStatus.setText("Recording...");
        // 按钮变形成方形 (简单的缩放模拟)
        btnRecord.animate().scaleX(0.5f).scaleY(0.5f).setDuration(200).start();
    }

    private void stopRecording() {
        if (!isRecording) return;
        isRecording = false;
        
        audioRecorder.stopRecording();
        timerHandler.removeCallbacks(timerRunnable);
        
        tvStatus.setText("Saved");
        btnRecord.animate().scaleX(1f).scaleY(1f).setDuration(200).start();
        
        Toast.makeText(getContext(), "Recording Saved", Toast.LENGTH_SHORT).show();
    }

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            int hours = minutes / 60;
            seconds %= 60;
            minutes %= 60;
            
            tvTimer.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
            if (isRecording) timerHandler.postDelayed(this, 500);
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isRecording) audioRecorder.stopRecording();
    }
}