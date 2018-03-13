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

import com.zyr.apiclient.data.Repo
import com.zyr.apiclient.data.ResponseWrapper
import io.reactivex.Observable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface GitHubService {
    @GET("users/{user}/repos")
    fun listRepos(@Path("user") user: String): Observable<List<Repo>>

    @Multipart
    @POST("xxxx/xxxx") //This is imaginary URL
    fun updateImage(@Part("name") name: RequestBody,
                    @Part image: MultipartBody.Part): Observable<String>

    @GET("xxx/xxx") //用于使用ResponseWrapper的举例，实际使用此API将无效
    fun repos(@Path("user") user: String): Observable<ResponseWrapper<List<Repo>>> //使用ResponseWrapper时注意Observable尖括号里的内容
}
