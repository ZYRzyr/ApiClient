package com.zyr.apiclient.network

import android.content.Context
import com.zyr.apiclient.data.ResponseWrapper
import com.zyr.apiclient.view.LoadingDialog
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import java.net.ConnectException

abstract class RequestCallback<T>(private val context: Context) : Observer<ResponseWrapper<T>> {
    abstract fun success(data: T)
    abstract fun failure(error: String)

    override fun onSubscribe(d: Disposable) {
        LoadingDialog.show(context)
    }

    override fun onNext(t: ResponseWrapper<T>) {
        if (t.code == 200) {
            success(t.data)
        } else {
            failure(t.message)
        }
    }

    override fun onComplete() {
        LoadingDialog.cancel()
    }

    override fun onError(throwable: Throwable) {
        LoadingDialog.cancel()
        val error = when (throwable) {
            is ConnectException -> "No Internet"
            else -> "Unknown error"
        }

        failure(error)
    }
}
