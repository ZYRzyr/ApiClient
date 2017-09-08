package com.zyr.apiclient.data

data class ResponseWrapper<T>(var code: Int, var data: T, var message: String)