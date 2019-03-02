package com.sixin.cherry;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.sixin.library.LockView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private LockView lockView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lockView = findViewById(R.id.lockView);
        //todo 屏幕仿佛旋转出现了bug
        lockView.setVerifyMode(LockView.VerifyMode.WRONG);
        lockView.setColorWrong(getResources().getColor(R.color.colorPrimaryDark));
//        lockView.setDotSelectedAnimDuration(1000);
        lockView.setCorrectColor(getResources().getColor(R.color.colorAccent));
        lockView.setLockViewListener(new LockView.LockViewListener() {
            @Override
            public void onStart(int dotValue) {
//                Log.d(TAG, "onStart:" + dotValue);
//                lockView.setVerifyMode(LockView.VerifyMode.WRONG);
//                  lockView.setDotSelectedAnimDuration(1000);
            }

            @Override
            public void onProgress(int progressValue) {
//                Log.d(TAG, "onProgress:" + progressValue);
//                lockView.setVerifyMode(LockView.VerifyMode.WRONG);
//                lockView.setDotSelectedAnimDuration(1000);
            }

            @Override
            public void onComplete(int[] result) {
                for (int i = 0; i < result.length; i++) {
                    Log.d(TAG, "onComplete:"+result[i]);
                }
//                lockView.setVerifyMode(LockView.VerifyMode.CORRECT);
//                lockView.setDotNormalSize(dip2px(getResources().getDimension(R.dimen.dp_15)));
//                lockView.setDotSelectedAnimDuration(1000);
            }

            @Override
            public void onCancel() {
//                lockView.setDotCount(4);
//                lockView.setVerifyMode(LockView.VerifyMode.CORRECT);

            }
        });

        findViewById(R.id.img).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                lockView.setNormalColor(getResources().getColor(R.color.ColorWrong));
//                lockView.setDotNormalSize(dip2px(getResources().getDimension(R.dimen.dp_15)));
//                lockView.setCorrectColor(getResources().getColor(R.color.colorPrimaryDark));
                lockView.setDotSelectedAnimDuration(1000);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
//        lockView.setDotCount(5);
    }

    public  int dip2px(float dpValue) {
        float scale =getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        App.getRefWatcher(getApplicationContext()).watch(this);
    }
}


