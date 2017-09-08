package com.zyr.apiclient.network

import com.zyr.apiclient.data.ResponseWrapper
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import java.net.ConnectException

abstract class RequestCallback<T> : Observer<ResponseWrapper<T>> {
    abstract fun success(data: T)
    abstract fun failure(error: String)

    override fun onSubscribe(d: Disposable) {
        //no-op
    }

    override fun onNext(t: ResponseWrapper<T>) {
        if (t.code == 200) {
            success(t.data)
        } else {
            failure(t.message)
        }
    }

    override fun onComplete() {
        //no-op
    }

    override fun onError(throwable: Throwable) {
        val error = when (throwable) {
            is ConnectException -> "No Internet"
            else -> "Unknown error"
        }
        failure(error)
    }
}