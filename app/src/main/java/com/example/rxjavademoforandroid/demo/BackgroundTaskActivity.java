package com.example.rxjavademoforandroid.demo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.example.rxjavademoforandroid.R;
import com.example.rxjavademoforandroid.TurnActivity;
import com.example.rxjavademoforandroid.util.LogUtils;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;


/**
 * Created by Administrator on 2018/4/4.
 */

public class BackgroundTaskActivity extends AppCompatActivity {
    private static final String TAG = "BackgroundTaskActivity";
    private TextView mTv;
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();//连接关系的管理对象，使用OpenHashSet来存储Disposable对象
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_back_task);
        mTv = findViewById(R.id.tv);
    }

    public void btn1(View view){
        //上游
        Observable<Integer> observable = Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {//emitter是发射器,这里执行后台耗时操作
                //模拟下载操作，耗时操作
                for (int i = 0; i < 10; i++) {
                    LogUtils.e("是否断开连接："+emitter.isDisposed());//isDisposed:是否断开连接，是否被弃置的
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    emitter.onNext(i);
                    LogUtils.e("上游发射："+i);
                }
                emitter.onComplete();
                LogUtils.e("上游发射结束");
            }
        });
        //下游
        Observer<Integer> disposableObserver = new Observer<Integer>() {//所有回调都是在主线程中

            @Override
            public void onSubscribe(Disposable disposable) {//执行在发射之前
                mCompositeDisposable.add(disposable);//管理连接关系，用于避免内存泄漏
                LogUtils.e("下游onSubscribe");
            }


            @Override
            public void onNext(Integer integer) {//必须实现的方法
                LogUtils.e("下游接收到："+integer);
                mTv.setText("接收到数字："+integer);

            }

            @Override
            public void onError(Throwable e) {//必须实现的方法
                LogUtils.e("下游接收到错误："+e.getMessage());
            }

            @Override
            public void onComplete() {//必须实现的方法
                LogUtils.e("下游接收结束");
            }
        };


        observable.subscribeOn(Schedulers.io())//指定上游的subscribe（）方法工作在哪个线程；多次调用，以第一次指定的线程为准
                .observeOn(AndroidSchedulers.mainThread())//指定下游的回调工作在哪个线程；多次调用，以最后一次指定的线程为准
                .subscribe(disposableObserver);//上游连接下游


    }

    @Override
    protected void onPause() {
        super.onPause();
        mCompositeDisposable.clear();//断开与下游的连接，只是下游不再接收，上游还会一直发射，直到发射结束
    }

    /**
     * 跳转界面
     */
    public void btn2(View view){
        TurnActivity.newInstance(this);
    }
}
