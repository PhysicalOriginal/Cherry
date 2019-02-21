package com.sixin.cherry;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.sixin.library.LockView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final LockView lockView = findViewById(R.id.lockView);
        final Button button = findViewById(R.id.button);
        lockView.setLockViewListener(new LockView.LockViewListener() {
            @Override
            public void onStart(int dotValue) {
                Log.d(TAG, "onStart:" + dotValue);
            }

            @Override
            public void onProgress(int progressValue) {
                Log.d(TAG, "onProgress:" + progressValue);
            }

            @Override
            public void onComplete(int[] result) {
                for (int i = 0; i < result.length; i++) {
                    Log.d(TAG, "onComplete:"+result[i]);
                }
                lockView.setVerifyMode(LockView.VerifyMode.CORRECT);
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "onCancel");
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                lockView.setDotCount(5);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        App.getRefWatcher(getApplicationContext()).watch(this);
    }
}
