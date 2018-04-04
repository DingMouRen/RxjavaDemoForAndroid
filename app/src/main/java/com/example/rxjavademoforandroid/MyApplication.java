package com.example.rxjavademoforandroid;

import android.app.Application;
import android.content.Context;

/**
 * Created by Administrator on 2018/4/4.
 */

public class MyApplication extends Application {

    public static Context sContext;

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = this;
    }
}
