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
        //todo 屏幕仿佛旋转出现了bug
        lockView.setVerifyMode(LockView.VerifyMode.CORRECT);
        lockView.setLockViewListener(new LockView.LockViewListener() {
            @Override
            public void onStart(int dotValue) {
//                Log.d(TAG, "onStart:" + dotValue);
//                lockView.setVerifyMode(LockView.VerifyMode.WRONG);
            }

            @Override
            public void onProgress(int progressValue) {
//                Log.d(TAG, "onProgress:" + progressValue);
//                lockView.setVerifyMode(LockView.VerifyMode.WRONG);
            }

            @Override
            public void onComplete(int[] result) {
                for (int i = 0; i < result.length; i++) {
                    Log.d(TAG, "onComplete:"+result[i]);
                }
//                lockView.setVerifyMode(LockView.VerifyMode.WRONG);
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "onCancel");
//                lockView.setVerifyMode(LockView.VerifyMode.CORRECT);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        App.getRefWatcher(getApplicationContext()).watch(this);
    }
}
