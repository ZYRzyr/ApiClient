package com.zyr.apiclient.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.zyr.apiclient.R
import com.zyr.apiclient.data.Repo
import com.zyr.apiclient.network.ApiClient
import com.zyr.apiclient.network.ApiErrorModel
import com.zyr.apiclient.network.ApiResponse
import com.zyr.apiclient.network.NetworkScheduler
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        submit.setOnClickListener { fetchRepo() }
    }

    private fun fetchRepo() {
        ApiClient.instance.service.listRepos(inputUser.text.toString())
                .compose(NetworkScheduler.compose())
                .subscribe(object : ApiResponse<List<Repo>>(this) {
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
