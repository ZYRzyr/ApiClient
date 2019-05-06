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
package com.zyr.apiclient.activity

import android.os.Bundle
import android.widget.Toast
import com.trello.rxlifecycle2.android.ActivityEvent
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import com.zyr.apiclient.R
import com.zyr.apiclient.data.Repo
import com.zyr.apiclient.network.ApiClient
import com.zyr.apiclient.network.ApiErrorModel
import com.zyr.apiclient.network.ApiResponse
import com.zyr.apiclient.network.NetworkScheduler
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : RxAppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        submit.setOnClickListener { fetchRepo() }
    }

    private fun fetchRepo() {
//        使用RequestCallback
//        ApiClient.instance.service.repos(inputUser.text.toString())
//                .compose(NetworkScheduler.compose())
//                .bindUntilEvent(this,ActivityEvent.DESTROY)
//                .subscribe(object : RequestCallback<List<Repo>>(this) {
//                    override fun success(data: List<Repo>) {
//
//             cd        }
//
//                    override fun failure(statusCode: Int, apiErrorModel: ApiErrorModel) {
//
//                    }
//                })

        ApiClient.instance.service.listRepos(inputUser.text.toString())
                .compose(NetworkScheduler.compose())
                .bindUntilEvent(this, ActivityEvent.DESTROY)
                .subscribe(object : ApiResponse<List<Repo>>(this) {
                    //              .subscribe(object : ApiResponse<List<Repo>>(this,false) {  不需要loading使用2参数构造函数
                    override fun success(data: List<Repo>) {
                        userName.text = data[0].owner.login
                        repoName.text = data[0].name
                        description.text = data[0].description
                        url.text = data[0].html_url
                    }

                    override fun failure(statusCode: Int, apiErrorModel: ApiErrorModel) {
                        Toast.makeText(this@MainActivity, apiErrorModel.message, Toast.LENGTH_SHORT).show()
                    }
                })
    }
}
