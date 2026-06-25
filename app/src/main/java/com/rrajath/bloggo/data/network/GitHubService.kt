package com.rrajath.bloggo.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface GitHubService {

    @GET("repos/{owner}/{repo}/git/trees/{branch}?recursive=1")
    suspend fun getTree(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String,
    ): Response<TreeResponse>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Query("ref") ref: String,
    ): Response<ContentResponse>

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun putContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Body body: ContentRequest,
    ): Response<ContentUpdateResponse>
}
