package com.example.waveform;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 点击方形组件，弹出全屏录音界面
        findViewById(R.id.cardRecorder).setOnClickListener(v -> {
            if (checkPermission()) {
                showRecorderDialog();
            }
        });
    }

    private void showRecorderDialog() {
        RecorderDialogFragment dialog = new RecorderDialogFragment();
        dialog.show(getSupportFragmentManager(), "RecorderFullscreen");
    }

    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            return false;
        }
        return true;
    }
}