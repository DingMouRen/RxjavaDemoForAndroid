### **1.后台执行耗时操作，实时通知UI更新**

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
 
 ### **2.优化搜索联想功能**
 
 #### 应用场景
 
搜索联想功能：客户端通过EditText的addTextChangedListener方法监听输入框的变化，当输入框发生变化之后就会回调afterTextChanged方法，客户端利用当前输入框内的文字向服务器发起请求，服务器返回与该搜索文字关联的结果给客户端进行展示。

需要优化的方面：
* 如果用户依次输入a和ab,此时会有发起两个请求，一个关键字是a的请求，之后的是关键字为ab的请求；如果关键字是ab的请求先于关键字是a的请求返回，那么用户期望搜索的结果就出错了。
* 搜索关键字不能为空
* 用户连续输入的情况下，要避免不必要的请求。

#### 实例
```
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
```

#### 实例解析


此功能的优化主要是通过三个操作符来实现的。

* PublishSubject:与普通的Subject不同，在订阅时并不立即触发订阅事件，而是允许我们在任意时刻手动调用onNext(),onError(),onCompleted来触发事件。
* debounce操作符：假设 mPublishSubject.debounce(200, TimeUnit.MILLISECONDS)，意思就是在200毫秒内，mPublishSubject.onNext(value)中value没变，就发射这个value,如果在200毫秒内onNext(newValue)发生变化，就在等200毫秒，如果新的200毫秒内,newValue没变化就发射这个newValue，就是这个套路
* filter操作符：filter操作符对源Observable发射的数据项按照指定的条件进行过滤，满足的条件的才会调给订阅者
* switchMap操作符：当源Observable发射一个新的数据项时，如果旧数据项订阅还未完成，就取消旧订阅数据和停止监视那个数据项产生的Observable,开始监视新的数据项.就是说mPublishSubject.onNext(a),这个请求发出后，服务端还没有返回数据时，mPublishSubject.onNext(ab)执行了，那么就取消还没有返回数据的订阅，去完成新的订阅关系。
 
### **3.轮询操作**

#### 应用场景

间隔一段时间就向服务器发起一次请求，一般使用Timer来实现。这里使用Rxjava来实现，还有一种需要轮询操作就是长连接中，每隔固定时间向服务端发送心跳包

#### 实例
```
Observable.interval(0,1, TimeUnit.SECONDS, Schedulers.computation())
                .flatMap(new Function<Long, ObservableSource<Long>>() {
                    @Override
                    public ObservableSource<Long> apply(Long aLong) throws Exception {
                        LogUtils.e("flatmap当前线程:"+Thread.currentThread().getName());
                        return Observable.just(aLong);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Long aLong) {
                        mTv1.setText(String.valueOf(aLong));
                        LogUtils.e("observer当前线程:"+Thread.currentThread().getName());
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
```

#### 实例解析

interval（）属于创建型操作符，创建一个按固定时间间隔发射整数序列的Observable，有多个重载方法。
* interval(long initialDelay, long period, TimeUnit unit)
* interval(long initialDelay, long period, TimeUnit unit, Scheduler scheduler)
* interval(long period, TimeUnit unit)
* interval(long period, TimeUnit unit, Scheduler scheduler)
* 	intervalRange(long start, long count, long initialDelay, long period, TimeUnit unit)
* 	intervalRange(long start, long count, long initialDelay, long period, TimeUnit unit, Scheduler scheduler)

参数解析：
* initialDelay:第一次延时多久发射数据
* period:发射数据的时间间隔（从第二次开始）
* unit:时间单位
* scheduler:指定发射数据所在的线程
* start:指定第一个发射的值，不指定的话默认是0
* count:发射的个数

