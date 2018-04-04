package com.example.rxjavademoforandroid;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by Administrator on 2018/4/4.
 */

public class TurnActivity extends AppCompatActivity {

    public static void newInstance(Context context){
        context.startActivity(new Intent(context,TurnActivity.class));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_turn);
    }
}
