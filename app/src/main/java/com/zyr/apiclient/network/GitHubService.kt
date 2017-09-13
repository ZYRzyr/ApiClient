package com.zyr.apiclient.network

import com.zyr.apiclient.data.Repo
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Path

interface GitHubService {
    @GET("users/{user}/repos")
    fun listRepos(@Path("user") user: String): Observable<List<Repo>>
}
