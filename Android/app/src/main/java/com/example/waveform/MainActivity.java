package com.example.waveform;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private boolean pendingOpenRecorder = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View cardRecorder = findViewById(R.id.cardRecorder);
        cardRecorder.setOnClickListener(v -> {
            pendingOpenRecorder = true;
            if (checkAllPermissions()) {
                openRecorder();
            }
        });

        View cardCalculator = findViewById(R.id.cardCalculator);
        cardCalculator.setOnClickListener(v -> {
            Intent intent = new Intent(this, CalculatorActivity.class);
            startActivity(intent);
        });
    }

    private void openRecorder() {
        RecorderBottomSheetFragment bottomSheet = new RecorderBottomSheetFragment();
        bottomSheet.show(getSupportFragmentManager(), "RecorderSheet");
        pendingOpenRecorder = false;
    }

    private boolean checkAllPermissions() {
        // 检查录音权限（必需）
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
            return false;
        }

        // 检查存储权限（Android 10以下需要，Android 10+使用分区存储）
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                return false;
            }
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean audioGranted = false;
            boolean storageGranted = true; // 默认为true，因为Android 10+不需要

            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                if (permission.equals(Manifest.permission.RECORD_AUDIO)) {
                    audioGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                } else if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    storageGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                }
            }

            if (audioGranted && storageGranted) {
                Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show();
                if (pendingOpenRecorder) {
                    openRecorder();
                }
            } else {
                String missingPermission = !audioGranted ? "录音权限" : "存储权限";
                Toast.makeText(this, "需要" + missingPermission + "才能使用功能", Toast.LENGTH_LONG).show();
                pendingOpenRecorder = false;
            }
        }
    }
}