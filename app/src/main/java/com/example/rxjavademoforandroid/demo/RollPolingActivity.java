package com.example.rxjavademoforandroid.demo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.example.rxjavademoforandroid.R;
import com.example.rxjavademoforandroid.util.LogUtils;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;


/**
 * Created by Administrator on 2018/4/4.
 * 轮询操作
 */

public class RollPolingActivity extends AppCompatActivity{

    private TextView mTv1;

    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_roll_poling);
        mTv1 = findViewById(R.id.tv1);
    }

    /**
     * 固定时间间隔的轮询
     * @param view
     */
    public void btn1(View view){
        Observable.interval(0,1, TimeUnit.SECONDS, Schedulers.computation())
                .flatMap(new Function<Long, ObservableSource<Long>>() {
                    @Override
                    public ObservableSource<Long> apply(Long aLong) throws Exception {
                        LogUtils.e("interval-flatmap当前线程:"+Thread.currentThread().getName());
                        return Observable.just(aLong);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        mCompositeDisposable.add(d);
                    }

                    @Override
                    public void onNext(Long aLong) {
                        mTv1.setText(String.valueOf(aLong));
                        LogUtils.e("interval-observer当前线程:"+Thread.currentThread().getName());
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });

        Observable.intervalRange(6,6,0,1, TimeUnit.SECONDS, Schedulers.computation())
                .flatMap(new Function<Long, ObservableSource<Long>>() {
                    @Override
                    public ObservableSource<Long> apply(Long aLong) throws Exception {
                        LogUtils.e("intervalRange-flatmap当前线程:"+Thread.currentThread().getName());
                        return Observable.just(aLong);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        mCompositeDisposable.add(d);
                    }

                    @Override
                    public void onNext(Long aLong) {
                        LogUtils.e("intervalRange-observer当前线程:"+Thread.currentThread().getName());
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });

    }

    @Override
    protected void onPause() {
        super.onPause();
        mCompositeDisposable.clear();
    }
}
