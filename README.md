# ApiClient
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

**NOTE**:本例中使用的RxJava2的类均是不支持背压的，即`Observable(被观察者)`与`Observer（观察者）`。需要背压策略，请自行替换为对应的`Flowable(被观察者)`与`Subscriber(观察者)`即可。

## 开始

### 1.按惯例先添加依赖:
```gradle
//Retrofit相关
compile(['com.squareup.okhttp3:logging-interceptor:3.9.0',//用于查看http请求时的log
         'com.squareup.retrofit2:retrofit:2.3.0',
         'com.squareup.retrofit2:adapter-rxjava2:2.3.0',
         'com.squareup.retrofit2:converter-gson:2.3.0'])

//RxJava相关
compile(['io.reactivex.rxjava2:rxandroid:2.0.1',
         'io.reactivex.rxjava2:rxjava:2.1.3'])

//RxLifecycle相关
compile(['com.trello.rxlifecycle2:rxlifecycle-kotlin:2.2.0',
         'com.trello.rxlifecycle2:rxlifecycle-components:2.2.0'])
```

**NOTE**:可以去[Retrofit](https://github.com/square/retrofit)、[Rxjava2(RxAndroid)](https://github.com/ReactiveX/RxAndroid)、[okhttp](https://github.com/square/okhttp)、[RxLifecycle](https://github.com/trello/RxLifecycle)，查询最新版本号。

### 2.封装请求类
为了秉承`RxJava`的链式调用风格，也为了方便每一个`API`的调用操作，创建了一个单例类`ApiClient`，具体如下：
```kotlin
class ApiClient private constructor() {
    lateinit var service: GitHubService

    private object Holder {
        val INSTANCE = ApiClient()
    }

    companion object {
        val instance by lazy { Holder.INSTANCE }
    }

    fun init() {  //在Application的onCreate中调用一次即可
        val okHttpClient = OkHttpClient().newBuilder()
                 //输入http连接时的log，也可添加更多的Interceptor
                .addInterceptor(HttpLoggingInterceptor().setLevel( 
                        if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                        else HttpLoggingInterceptor.Level.NONE
                ))
                .build()

        val retrofit = Retrofit.Builder()
                .baseUrl("https://api.github.com/")   //本文以GitHub API为例
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(okHttpClient)
                .build()

        service = retrofit.create(GitHubService::class.java)
    }
}
```
其中使用`GitHub`的的`API`作为测试，`GitHubService`如下：
```kotlin
interface GitHubService {
//请添加相应的`API`调用方法
    @GET("users/{user}/repos")
    fun listRepos(@Path("user") user: String): Observable<List<Repo>> //每个方法的返回值即一个Observable
}
```
上面的`Repo`即一个简单的`Kotlin`数据类，可以去[这里](https://github.com/ZYRzyr/ApiClient/tree/master/app/src/main/java/com/zyr/apiclient/data)查看。

### 3.`RESTful API`请求响应的处理
`API`的响应返回形式有很多种，此处介绍最常见的两种形式的处理：标准`RESTful API`与`任性的后端写的API`。`GitHub`提供的`API`即标准`RESTful API`。

`RESTful API`的请求响应主要处理状态码与数据体，具体封装如下：
```kotlin
abstract class ApiResponse<T>(private val context: Context) : Observer<T> {
    abstract fun success(data: T)
    abstract fun failure(statusCode: Int, apiErrorModel: ApiErrorModel)

    override fun onSubscribe(d: Disposable) {
        LoadingDialog.show(context)
    }

    override fun onNext(t: T) {
        success(t)
    }

    override fun onComplete() {
        LoadingDialog.cancel()
    }

    override fun onError(e: Throwable) {
        LoadingDialog.cancel()
        if (e is HttpException) { //连接服务器成功但服务器返回错误状态码
            val apiErrorModel: ApiErrorModel = when (e.code()) {
                ApiErrorType.INTERNAL_SERVER_ERROR.code ->
                    ApiErrorType.INTERNAL_SERVER_ERROR.getApiErrorModel(context)
                ApiErrorType.BAD_GATEWAY.code ->
                    ApiErrorType.BAD_GATEWAY.getApiErrorModel(context)
                ApiErrorType.NOT_FOUND.code ->
                    ApiErrorType.NOT_FOUND.getApiErrorModel(context)
                else -> otherError(e)

            }
            failure(e.code(), apiErrorModel)
            return
        }

        val apiErrorType: ApiErrorType = when (e) {  //发送网络问题或其它未知问题，请根据实际情况进行修改
            is UnknownHostException -> ApiErrorType.NETWORK_NOT_CONNECT
            is ConnectException -> ApiErrorType.NETWORK_NOT_CONNECT
            is SocketTimeoutException -> ApiErrorType.CONNECTION_TIMEOUT
            else -> ApiErrorType.UNEXPECTED_ERROR
        }
        failure(apiErrorType.code, apiErrorType.getApiErrorModel(context))
    }

    private fun otherError(e: HttpException) =
            Gson().fromJson(e.response().errorBody()?.charStream(), ApiErrorModel::class.java)
}
```
**说明** :

1.每个响应继承`Observer`，其中的`泛型`以适配返回的不同的数据体；

2.定义两个抽象方法`success`和`failure`，在使用的时候只需关注成功和失败这两种情况；

3.在`onSubscribe`即开始请求的时候显示`Loading`，在请求完成或出错时隐藏；

4.在`onNext`即`Observer`成功接收数据后直接调用`success`，在调用处可直接使用返回的数据；

5.在`onError`即请求出错时处理，此处包含两种情况：连接服务器成功但服务器返回错误状态码、网络或其它问题。

在错误处理中，定义了一个枚举类`ApiErrorType`，用于列举出服务器定义的错误状态码情况：
```kotlin
enum class ApiErrorType(val code: Int, @param: StringRes private val messageId: Int) {
//根据实际情况进行增删
    INTERNAL_SERVER_ERROR(500, R.string.service_error), 
    BAD_GATEWAY(502, R.string.service_error),
    NOT_FOUND(404, R.string.not_found),
    CONNECTION_TIMEOUT(408, R.string.timeout),
    NETWORK_NOT_CONNECT(499, R.string.network_wrong),
    UNEXPECTED_ERROR(700, R.string.unexpected_error);

    private val DEFAULT_CODE = 1

    fun getApiErrorModel(context: Context): ApiErrorModel {
        return ApiErrorModel(DEFAULT_CODE, context.getString(messageId))
    }
}
```
还定义了一个错误消息的的实体类`ApiErrorModel`(在`Kotlin`中即为一个数据类)，用于包含错误信息提示用户或服务器返回的错误信息以提示开发人员：
```kotlin
data class ApiErrorModel(var status: Int, var message: String)
```

### 4.线程与生命周期
`RxJava`的一大特色即方便的线程切换操作，在请求`API`中需要进行线程的切换，通常是以下形式(伪代码)：
```kotlin
observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
```
但每个请求都写一段这个，显得特别麻烦，所以进行以下简单封装:
```kotlin
object NetworkScheduler {
    fun <T> compose(): ObservableTransformer<T, T> {
        return ObservableTransformer { observable ->
            observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
        }
    }
}
```
使用的时候简单搞定，伪代码如下：
```kotlin
observable.compose(NetworkScheduler.compose())
```
在`Android`中，当一个`Activity`在调`API`时`onDestroy`了，需要取消请求，所以此处引入了`RxLifecycle`进行管理：
`Activity`继承`RxAppCompatActivity`后，在`observable`的调用链中加入`.bindUntilEvent(this, ActivityEvent.DESTROY)`即可，伪代码如下：
```kotlin
observable.compose(NetworkScheduler.compose())
          .bindUntilEvent(this, ActivityEvent.DESTROY)  //加入这句
          .subscribe(...)
```

### 5.使用
在以上准备工作完成后，即可开始使用：

首先在`Application`中初始化`ApiClient`：
```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.instance.init() //这里
    }
}
```

在需要的地方使用`ApiClient`，点击按钮时，请求数据，成功后用`TextView`显示出来:
```kotlin
class MainActivity : RxAppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        submit.setOnClickListener { fetchRepo() } //按钮点击事件
    }

    private fun fetchRepo() {
        //链式调用
        ApiClient.instance.service.listRepos(inputUser.text.toString())   //GitHubService中的方法
                .compose(NetworkScheduler.compose())                      //线程切换处理
                .bindUntilEvent(this, ActivityEvent.DESTROY)              //生命周期管理
                .subscribe(object : ApiResponse<List<Repo>>(this) {       //对象表达式约等于Java中的匿名内部类 
                    override fun success(data: List<Repo>) {              //请求成功，此处显示一些返回的数据
                        userName.text = data[0].owner.login
                        repoName.text = data[0].name
                        description.text = data[0].description
                        url.text = data[0].html_url
                    }

                    override fun failure(statusCode: Int, apiErrorModel: ApiErrorModel) { //请求失败，此处直接显示Toast
                        Toast.makeText(this@MainActivity, apiErrorModel.message, Toast.LENGTH_SHORT).show()
                    }
                })
    }
}
```

效果如下：

![效果.gif](http://upload-images.jianshu.io/upload_images/4839535-aff440c06e964294.gif?imageMogr2/auto-orient/strip)

### 6.`任性的后端写的API`请求响应的处理
这种情况只需要对数据类和响应处理进行修改即可。有些后端开发者们，可能将返回体写成如下形式:
```json
{
    "code": "200",
    "data": [
        {
            "name": "Tom",
            "age": 12,
            "money": 100.5
        },
        {
            "name": "Bob",
            "age": 13,
            "money": 200.5
        }
    ],
    "message": "客户端请求成功"
}
```
所有返回的数据中，最外层都包裹了一层信息，以表示请求成功或失败，中间`data`才是具体数据，所以定义数据类(实体类)时，需要定义成如下形式：
```kotlin
data class ResponseWrapper<T>(var code: Int, var data: T, var message: String)
```
其中`data`为泛型，以适配不同的数据体。
然后将上面第3点中的`ApiResponse`修改如下：
```kotlin
abstract class RequestCallback<T>(private val context: Context) : Observer<ResponseWrapper<T>> {
    abstract fun success(data: T)
    abstract fun failure(statusCode: Int, apiErrorModel: ApiErrorModel)

    private object Status {
        val SUCCESS = 200
    }

    override fun onSubscribe(d: Disposable) {
        LoadingDialog.show(context)
    }

    override fun onNext(t: ResponseWrapper<T>) {
        if (t.code == Status.SUCCESS) {
            success(t.data)
            return
        }

        val apiErrorModel: ApiErrorModel = when (t.code) {
            ApiErrorType.INTERNAL_SERVER_ERROR.code ->
                ApiErrorType.INTERNAL_SERVER_ERROR.getApiErrorModel(context)
            ApiErrorType.BAD_GATEWAY.code ->
                ApiErrorType.BAD_GATEWAY.getApiErrorModel(context)
            ApiErrorType.NOT_FOUND.code ->
                ApiErrorType.NOT_FOUND.getApiErrorModel(context)
            else -> ApiErrorModel(t.code, t.message)
        }
        failure(t.code, apiErrorModel)
    }

    override fun onComplete() {
        LoadingDialog.cancel()
    }

    override fun onError(e: Throwable) {
        LoadingDialog.cancel()
        val apiErrorType: ApiErrorType = when (e) {
            is UnknownHostException -> ApiErrorType.NETWORK_NOT_CONNECT
            is ConnectException -> ApiErrorType.NETWORK_NOT_CONNECT
            is SocketTimeoutException -> ApiErrorType.CONNECTION_TIMEOUT
            else -> ApiErrorType.UNEXPECTED_ERROR
        }
        failure(apiErrorType.code, apiErrorType.getApiErrorModel(context))
    }
}
```

修改完成之后的使用与上文第5点相同。

**2017年10月13日更新—增加上传图片的方法**
新增`OkHttpUtil.kt`，用于上传图片，代码如下：
```kotlin
object OkHttpUtil {
    fun createTextRequestBody(source: String): RequestBody
            = RequestBody.create(MediaType.parse("text/plain"), source)

    fun createPartWithAllImageFormats(requestKey: String, file: File): MultipartBody.Part
            = MultipartBody.Part
            .createFormData(requestKey, file.name, RequestBody.create(MediaType.parse("image/*"), file))
}
```
使用方式:
1.先在`GitHubService.kt`中新增如下方法：
```kotlin
@Multipart
@POST("xxxx/xxxx") //This is imaginary URL
fun updateImage(@Part("name") name: RequestBody,
                @Part image: MultipartBody.Part): Observable<UserInfo>
```
2.在需要的地方使用:
```kotlin
ApiClient.instance.service.updateImage(OkHttpUtil.createTextRequestBody("Bob"),
                 OkHttpUtil.createPartWithAllImageFormats("avatar",file))   //此处调用OkHttpUtil中的方法
                .compose(NetworkScheduler.compose())
                .bindUntilEvent(this,ActivityEvent.DESTROY)
                .subscribe(object : ApiResponse<UserInfo>(this) {
                    override fun success(data: UserInfo) {
                        //Do something
                    }

                    override fun failure(statusCode: Int, apiErrorModel: ApiErrorModel) {
                        //Do something
                    }
                })
```

原文链接:http://www.jianshu.com/p/c66d50cd14ee (欢迎点赞，收藏，关注作者)

### *License*

ApiClient is released under the [Apache 2.0 license](LICENSE).

```
Copyright 2017 Yuran Zhang

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at following link.

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
