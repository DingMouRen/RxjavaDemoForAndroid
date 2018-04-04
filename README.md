### **1.后台执行好事操作，实时通知UI更新**

#### 实例：
```
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
        mCompositeDisposable.clear();//断开与上游的连接，只是下游不再接收，上游还会一直发射，直到发射结束
    }


```

#### 实例解析

 ##### 1.1线程切换
 
 * 后台执行的耗时操作对应于subscribe(ObservableEmitter<Integer> emitter)中的代码。
 * 主线程进行UI更新的操作对应于Observer的所有回调，onSubscribe调用时机是上下游建立连接的时候，在上游发射之前；onNext负责进度的更新，onComplete和onError负责最终结果处理。
 * subscribeOn(Schedulers.io())：指定上游耗时操作subscribe(ObservableEmitter<Integer> emitter)方法工作在哪个线程，这里是IO线程；多次调用，以第一次指定的线程为准。
 * observeOn(AndroidSchedulers.mainThread())：指定下游的回调操作工作在哪个线程，这里是安卓的主线程；多次调用，以最后一次指定的线程为准。

 ##### 1.2线程类型
 
* Schedulers.computation()：用于计算任务，默认线程数等于处理器的数量。
* Schedulers.from(Executor executor)：使用Executor作为调度器。
* Schedulers.io( )：用于IO密集型任务，例如访问网络、数据库操作等，也是我们最常使用的。
* Schedulers.newThread( )：为每一个任务创建一个新的线程。
* Schedulers.trampoline( )：当其它排队的任务完成后，在当前线程排队开始执行。
* Schedulers.single()：所有任务共用一个后台线程。
```
compile 'io.reactivex.rxjava2:rxandroid:2.0.1' //关于安卓主线程的是添加了这个依赖
```
* AndroidSchedulers.mainThread()：运行在应用程序的主线程。
* AndroidSchedulers.from(Looper looper)：运行在该looper对应的线程当中。

 ##### 1.3使用CompositeDisposable对下游进行管理
 
 CompositeDisposable用来管理下游与上游的连接关系。CompositeDisposable里面通过OpenHashSet来存储所有的Disposable对象，CompositeDisposable.clear()可以断开所有下游与上游的连接关系，下游不再接收上游的任何消息。我们可以在Activity或Fragment对应的生命周期调用clear方法，来避免内存泄漏的发生。
 



