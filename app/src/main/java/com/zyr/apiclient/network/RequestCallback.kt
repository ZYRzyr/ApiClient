package com.zyr.apiclient.network

import android.content.Context
import com.zyr.apiclient.data.ResponseWrapper
import com.zyr.apiclient.view.LoadingDialog
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

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
