package com.example.rxjavademoforandroid.demo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import com.example.rxjavademoforandroid.R;
import com.example.rxjavademoforandroid.util.LogUtils;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * Created by Administrator on 2018/4/4.
 * 优化搜索联想功能
 */

public class OptimizeSearchActivity extends AppCompatActivity {

    private EditText mEditText;
    private TextView mTv;
    private PublishSubject<String> mPublishSubject;
    private DisposableObserver<String> mDisposableObserver;
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();//上下游连接关系的管理对象，使用OpenHashSet存储Dispoable对象

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_optimize_search);
        mEditText = findViewById(R.id.edit_text);
        mTv = findViewById(R.id.tv);

        initSubjectAndObserver();

        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mPublishSubject.onNext(s.toString());
            }
        });
    }

    /**
     * 初始化上下游并建立连接
     */
    private void initSubjectAndObserver() {
        //上游
        mPublishSubject = PublishSubject.create();
        //下游
        mDisposableObserver = new DisposableObserver<String>() {

            @Override
            protected void onStart() {
                super.onStart();
                LogUtils.e("onStart");
            }

            @Override
            public void onNext(String s) {
                mTv.setText(s);
                LogUtils.e("onNext:" + s);
            }

            @Override
            public void onError(Throwable e) {
                LogUtils.e("onError");
            }

            @Override
            public void onComplete() {
                LogUtils.e("onComplete");
            }
        };
        mPublishSubject.debounce(200, TimeUnit.MILLISECONDS)//在200毫秒内，mPublishSubject.onNext(value)中value没变，就发射这个value,如果在200毫秒内onNext(newValue)发生变化，就在等200毫秒，如果新的200毫秒内,newValue没变化就发射这个newValue，就是这个套路
                .filter(new Predicate<String>() {//filter操作符对源Observable发射的数据项按照指定的条件进行过滤，满足的条件的才会调给订阅者
                    @Override
                    public boolean test(String s) throws Exception {
                        LogUtils.e("filter过滤,只允许字符串长度大于0的通过");
                        return s.length() > 0;
                    }
                }).switchMap(new Function<String, ObservableSource<String>>() {//当源Observable发射一个新的数据项时，如果旧数据项订阅还未完成，就取消旧订阅数据和停止监视那个数据项产生的Observable,开始监视新的数据项.
            @Override
            public ObservableSource<String> apply(String s) throws Exception {
                LogUtils.e("switchmap：如果前面有请求还在执行，取消之前的请求，执行现在新的请求");
                return requestDataFromServer(s);
            }
        }).observeOn(AndroidSchedulers.mainThread())//指定更新UI的线程在安卓主线程
                .subscribe(mDisposableObserver);//建立连接关系


        //将下游添加到连接关系管理类中
        mCompositeDisposable.add(mDisposableObserver);
    }


    /**
     * 根据关键字请求服务端数据
     *
     * @param keywords 关键字
     */
    private ObservableSource<String> requestDataFromServer(final String keywords) {
        return Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {

                emitter.onNext("正在请求服务器  loading...");
                LogUtils.e("正在请求服务器");

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                emitter.onNext("完成搜索，关键字：" + keywords);
                emitter.onComplete();
                LogUtils.e("完成搜索，关键字：" + keywords);
            }
        }).subscribeOn(Schedulers.io());
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCompositeDisposable.clear();
    }
}
