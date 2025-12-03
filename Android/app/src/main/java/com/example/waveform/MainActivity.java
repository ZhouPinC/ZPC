package com.example.waveform;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 1;

    private AudioWaveformView waveformView;
    private AudioRecorder audioRecorder;
    private Button recordButton;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 创建布局
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(32, 32, 32, 32);

        // 创建波形视图
        waveformView = new AudioWaveformView(this);
        // 转换dp到px，设置更灵活的高度
        float density = getResources().getDisplayMetrics().density;
        int waveformHeight = (int) (200 * density); // 200dp
        LinearLayout.LayoutParams waveformParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            waveformHeight
        );
        waveformParams.setMargins(0, 0, 0, (int) (32 * density)); // 32dp margin
        waveformView.setLayoutParams(waveformParams);

        // 创建录音按钮
        recordButton = new Button(this);
        recordButton.setText(R.string.start_recording);
        recordButton.setOnClickListener(v -> toggleRecording());

        // 添加到布局
        mainLayout.addView(waveformView);
        mainLayout.addView(recordButton);
        setContentView(mainLayout);

        // 初始化录音器
        audioRecorder = new AudioRecorder();
        audioRecorder.setAmplitudeListener(amplitude -> {
            // 在UI线程更新波形
            runOnUiThread(() -> waveformView.addAmplitude(amplitude));
        });

        // 检查权限
        checkAudioPermission();
    }

    private void checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                PERMISSION_REQUEST_RECORD_AUDIO
            );
        } else {
            // 权限已授予，启用按钮
            recordButton.setEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予
                recordButton.setEnabled(true);
                recordButton.setText(R.string.start_recording);
                Toast.makeText(this, "录音权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                // 权限被拒绝
                recordButton.setEnabled(false);
                recordButton.setText(R.string.record_permission_required);
                Toast.makeText(this, "录音权限被拒绝，无法使用录音功能", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void toggleRecording() {
        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            checkAudioPermission();
            return;
        }

        waveformView.clear();
        waveformView.startAnimation();
        audioRecorder.startRecording();

        isRecording = true;
        recordButton.setText(R.string.stop_recording);
    }

    private void stopRecording() {
        audioRecorder.stopRecording();
        waveformView.stopAnimation();

        isRecording = false;
        recordButton.setText(R.string.start_recording);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isRecording) {
            stopRecording();
        }
    }
}