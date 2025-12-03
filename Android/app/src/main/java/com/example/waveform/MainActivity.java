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

        View cardRecorder = findViewById(R.id.cardRecorder);
        cardRecorder.setOnClickListener(v -> {
            if (checkPermission()) {
                // 使用新的 BottomSheet Fragment
                RecorderBottomSheetFragment bottomSheet = new RecorderBottomSheetFragment();
                bottomSheet.show(getSupportFragmentManager(), "RecorderSheet");
            }
        });
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