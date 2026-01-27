package com.rrajath.hugowriter.api

import retrofit2.Response
import retrofit2.http.*

interface GitHubApi {
    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun listFiles(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String,
        @Query("ref") ref: String? = null
    ): Response<List<GitHubContent>>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFileContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String,
        @Query("ref") ref: String? = null
    ): Response<GitHubContent>

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun createOrUpdateFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String,
        @Body request: CreateFileRequest
    ): Response<GitHubFileResponse>

    @HTTP(method = "DELETE", path = "repos/{owner}/{repo}/contents/{path}", hasBody = true)
    suspend fun deleteFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String,
        @Body request: DeleteFileRequest
    ): Response<GitHubFileResponse>
}
