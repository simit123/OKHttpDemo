package com.example.okhttpdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    public static final String URL = "https://www.baidu.com/";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initGet();
    }

    private void initSyncGet(){
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(URL)
                .build();
        final Call call = client.newCall(request);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //同步
                    Response execute = call.execute();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        /**
         *  同步和异步的区别主要在于execute
         *   @Override public Response execute() throws IOException {
         *     synchronized (this) {
         *       if (executed) throw new IllegalStateException("Already Executed");//检查这个call是否已经运行过
         *       executed = true;
         *     }
         *     captureCallStackTrace();
         *     eventListener.callStart(this);
         *     try {
         *       client.dispatcher().executed(this);// 加入同步队列runningSyncCalls中
         *       Response result = getResponseWithInterceptorChain();
         *       if (result == null) throw new IOException("Canceled");
         *       return result;
         *     } catch (IOException e) {
         *       eventListener.callFailed(this, e);
         *       throw e;
         *     } finally {
         *       client.dispatcher().finished(this);
         *     }
         *   }
         *
         *
         *
         */

    }





    private void  initGet(){
        //构建http客户端
        OkHttpClient client = new OkHttpClient();
        //build方式构建request
        /**
         * Builder 是 Request 的静态内部类
         * Request.Builder builder = new Request.Builder();
         * new Request.Builder() 构建的是Builder内部类对象
         * url() 方法返回一个builder对象
         * new Request(this)
         * build() 方法返回一个Request对象并传入当前builder对象
         * 通过这样的方式构建了一个Request对象
         */

        Request request = new Request.Builder()
                .url(URL)
                .build();
        //构建call 对象  call 是一个接口 RealCall 是其子类
        /**
         * 调用HttpClient中的newCall方法并传入构建好的request对象
         * newCall 方法会返回 RealCall.newRealCall(this, request, false)
         * RealCall 调用他的静态方法newRealCall
         * newRealCall 方法中
         * RealCall call = new RealCall(client, originalRequest, forWebSocket);
         * call.eventListener = client.eventListenerFactory().create(call);
         * 构建了一个RealCall对象 并且 创建了一个    事件监听对象用来监听事件流程
         *
         *
         * RealCall 的构造方法中
         *
         * private RealCall(OkHttpClient client, Request originalRequest, boolean forWebSocket) {
         *     this.client = client;
         *     this.originalRequest = originalRequest;
         *     this.forWebSocket = forWebSocket;
         *     //默认创建一个retryAndFollowUpInterceptor过滤器
         *     this.retryAndFollowUpInterceptor = new RetryAndFollowUpInterceptor(client, forWebSocket);
         *   }
         *   其中构造了一个retryAndFollowUpInterceptor过滤器 这个需要重点关注
         *
         */

        Call call = client.newCall(request);
        //使用call执行异步请求

        /**
         * call 是一个接口 所以调用的是RealCall 的enqueue方法
         * @Override public void enqueue(Callback responseCallback) {
         *     synchronized (this) {  //加入锁是为了防止多线程同时调用
         *       if (executed) throw new IllegalStateException("Already Executed");
         *       executed = true;//true表示已经执行过了
         *     }
         *     captureCallStackTrace();  //为retryAndFollowUpInterceptor加入了一个用于追踪堆栈信息的callStackTrace
         *     eventListener.callStart(this); //回调callStart方法，监听事件流程
         *     client.dispatcher().enqueue(new AsyncCall(responseCallback));
         *   }
         *
         * client.dispatcher().enqueue(new AsyncCall(responseCallback));
         * 最后一行代码 要回到okhttpclient中查看dispatcher的作用
         * client.dispatcher() 返回一个dispatcher对象
         *
         * dispatcher对象的enqueue方法
         *   synchronized void enqueue(AsyncCall call) {
         *     if (runningAsyncCalls.size() < maxRequests && runningCallsForHost(call) < maxRequestsPerHost) { //当正在执行的异步队列数小于最大请求数64个并且请求同一个主机的个数小于maxRequestsPerHost (5)时
         *       runningAsyncCalls.add(call);   //此时将这个请求加入异步请求队列
         *       executorService().execute(call);//并且利用线程池执行这个请求
         *     } else {
         *       readyAsyncCalls.add(call);//否则加入异步等待队列
         *     }
         *   }
         *
         *  查看 runningCallsForHost
         *   private int runningCallsForHost(AsyncCall call) {
         *     int result = 0;
         *     for (AsyncCall c : runningAsyncCalls) {
         *       if (c.get().forWebSocket) continue;
         *       if (c.host().equals(call.host())) result++;
         *     }
         *     return result;
         *   }
         *  //runningAsyncCalls队列中同一个host的个数并记录
         *
         * 查看 AsyncCall 类
         *  AsyncCall类继承NamedRunnable类 NamedRunnable是一个抽象类并且实现了Runnable接口
         *  NamedRunnable 中的run方法如下
         *    @Override public final void run() {
         *     String oldName = Thread.currentThread().getName();
         *     Thread.currentThread().setName(name);
         *     try {
         *       execute();
         *     } finally {
         *       Thread.currentThread().setName(oldName);
         *     }
         *   }
         *  重点是抽象方法 execute 所以 我们又可以回到AsyncCall中查看execute()方法
         *
         *   @Override protected void execute() {
         *       boolean signalledCallback = false;
         *       try {
         *         Response response = getResponseWithInterceptorChain();
         *         if (retryAndFollowUpInterceptor.isCanceled()) {
         *           signalledCallback = true;
         *           responseCallback.onFailure(RealCall.this, new IOException("Canceled"));
         *         } else {
         *           signalledCallback = true;
         *           responseCallback.onResponse(RealCall.this, response);
         *         }
         *       } catch (IOException e) {
         *         if (signalledCallback) {
         *           // Do not signal the callback twice!
         *           Platform.get().log(INFO, "Callback failure for " + toLoggableString(), e);
         *         } else {
         *           eventListener.callFailed(RealCall.this, e);
         *           responseCallback.onFailure(RealCall.this, e);
         *         }
         *       } finally {
         *         client.dispatcher().finished(this);
         *       }
         *     }
         *    execute() 方法中可以看到 请求成功 的到Response 此时重点在于getResponseWithInterceptorChain()方法获取response对象，之后的东西就是成功以及失败的回调等等
         *
         *      getResponseWithInterceptorChain()方法的内容如下
         *
         *      Response getResponseWithInterceptorChain() throws IOException {
         *     // Build a full stack of interceptors.
         *     List<Interceptor> interceptors = new ArrayList<>();
         *     interceptors.addAll(client.interceptors());//用户自定义的拦截器列表
         *     //失败和重定向过滤器
         *     interceptors.add(retryAndFollowUpInterceptor);
         *     //封装request和response的过滤器
         *     interceptors.add(new BridgeInterceptor(client.cookieJar()));
         *     //缓存相关的过滤器，负责读取缓存直接返回、更新缓存
         *     interceptors.add(new CacheInterceptor(client.internalCache()));
         *     ////负责和服务器建立连接
         *     interceptors.add(new ConnectInterceptor(client));
         *     if (!forWebSocket) {
         *     //配置 OkHttpClient 时设置的 networkInterceptors
         *       interceptors.addAll(client.networkInterceptors());
         *     }
         *     //负责向服务器发送请求数据、从服务器读取响应数据(实际网络请求)
         *     interceptors.add(new CallServerInterceptor(forWebSocket));
         *
         *     Interceptor.Chain chain = new RealInterceptorChain(interceptors, null, null, null, 0,
         *         originalRequest, this, eventListener, client.connectTimeoutMillis(),
         *         client.readTimeoutMillis(), client.writeTimeoutMillis());
         *
         *     return chain.proceed(originalRequest);
         *   }
         *  这里首先new 了一个interceptors集合 并且向这个集合中添加各种过滤器，每个过滤器负责执行不同的功能
         *
         *  添加完过滤器之后 构建了一个chain的子类对象 RealInterceptorChain 并且调用chain.proceed(originalRequest)方法
         *  originalRequest 为我们刚才要求请的Request对象
         *  chain.proceed(originalRequest)方法如下：
         *
         *
         *    public Response proceed(Request request, StreamAllocation streamAllocation, HttpCodec httpCodec,
         *       RealConnection connection) throws IOException {
         *     if (index >= interceptors.size()) throw new AssertionError();
         *
         *     calls++;
         *
         *     // If we already have a stream, confirm that the incoming request will use it.
         *     if (this.httpCodec != null && !this.connection.supportsUrl(request.url())) {
         *       throw new IllegalStateException("network interceptor " + interceptors.get(index - 1)
         *           + " must retain the same host and port");
         *     }
         *
         *     // If we already have a stream, confirm that this is the only call to chain.proceed().
         *     if (this.httpCodec != null && calls > 1) {
         *       throw new IllegalStateException("network interceptor " + interceptors.get(index - 1)
         *           + " must call proceed() exactly once");
         *     }
         *
         *     // Call the next interceptor in the chain.
         *     RealInterceptorChain next = new RealInterceptorChain(interceptors, streamAllocation, httpCodec,
         *         connection, index + 1, request, call, eventListener, connectTimeout, readTimeout,
         *         writeTimeout);
         *     Interceptor interceptor = interceptors.get(index);
         *     Response response = interceptor.intercept(next);
         *
         *     // Confirm that the next interceptor made its required call to chain.proceed().
         *     if (httpCodec != null && index + 1 < interceptors.size() && next.calls != 1) {
         *       throw new IllegalStateException("network interceptor " + interceptor
         *           + " must call proceed() exactly once");
         *     }
         *
         *     // Confirm that the intercepted response isn't null.
         *     if (response == null) {
         *       throw new NullPointerException("interceptor " + interceptor + " returned null");
         *     }
         *
         *     if (response.body() == null) {
         *       throw new IllegalStateException(
         *           "interceptor " + interceptor + " returned a response with no body");
         *     }
         *
         *     return response;
         *   }
             *  注意这个index变量 开始我们传入的是0 proceed方法中会做index + 1 的操作，这个index是为了获取各个拦截器，递归执行
             *   Response response = interceptor.intercept(next);
             *   每一个拦截器的intercept方法中都会重新调用realChain.proceed(request, streamAllocation, httpCodec, connection);方法
         *
         *
         *
         *
         *
         *
         *
         *
         *
         *
         *
         *
         *
         *   dispatcher中有三个队列分别用于保存call对象
         *    Ready async calls in the order they'll be run.
         *  private final Deque<AsyncCall> readyAsyncCalls = new ArrayDeque<>();//异步等待
         *
         *   Running asynchronous calls. Includes canceled calls that haven't finished yet.
         *   private final Deque<AsyncCall> runningAsyncCalls = new ArrayDeque<>();//异步running
         *
         *   Running synchronous calls. Includes canceled calls that haven't finished yet.//同步running
         *   private final Deque<RealCall> runningSyncCalls = new ArrayDeque<>();
         *
         *
         *
         */
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

            }
        });

        /**
         * OKHttp中各个类和作用：
         *
         *  OkHttpClient    okHttp客户端 通过newCall方法和request对象构建call请求对象
         *  Request  okHttp请求，可以配置url，method，header，body等请求信息
         *  Call   接口  其实现为RealCall
         *  RealCall 请求的具体实现，调用enqueue方法执行异步请求
         *  Dispatcher 异步请求的执行策略，异步请求才会用到，定义了线程池以及最大线程并发数等
         *  AsyncCall  是RealCall的final类型的内部类，实现了Runnable接口
         *  NamedRunnable AsyncCall 继承 NamedRunnable NamedRunnable实现了Runnable接口
         *  executorService().execute(call); 使用线程池执行AsyncCall这个请求线程
         *
         */

    }
}
