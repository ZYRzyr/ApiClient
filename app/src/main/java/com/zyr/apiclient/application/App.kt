package com.zyr.apiclient.application

import android.app.Application
import com.zyr.apiclient.network.ApiClient

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.instance.init()
    }
}