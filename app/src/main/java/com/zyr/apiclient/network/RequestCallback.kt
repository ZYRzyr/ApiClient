/*******************************************************************************
 * Copyright 2017 Yuran Zhang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
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
