package com.example.rxjavademoforandroid;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.example.rxjavademoforandroid.demo.BackgroundTaskActivity;
import com.example.rxjavademoforandroid.demo.OptimizeSearchActivity;
import com.example.rxjavademoforandroid.demo.RollPolingActivity;

/**
 * https://www.jianshu.com/p/d135f19e045c
 */

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * 后台执行好事操作，实时通知UI更新
     */
    public void btn1(View view){startActivity(new Intent(this, BackgroundTaskActivity.class));}

    /**
     * 优化搜索联想
     */
    public void btn2(View view){startActivity(new Intent(this, OptimizeSearchActivity.class));}

    /**
     * 轮询操作
     */
    public void btn3(View view){startActivity(new Intent(this, RollPolingActivity.class));}
}
