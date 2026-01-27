package com.rrajath.hugowriter.api

import com.google.gson.annotations.SerializedName

data class CreateFileRequest(
    @SerializedName("message")
    val message: String,
    @SerializedName("content")
    val content: String,
    @SerializedName("branch")
    val branch: String? = null,
    @SerializedName("sha")
    val sha: String? = null
)

data class DeleteFileRequest(
    @SerializedName("message")
    val message: String,
    @SerializedName("sha")
    val sha: String,
    @SerializedName("branch")
    val branch: String? = null
)

data class GitHubFileResponse(
    @SerializedName("content")
    val content: GitHubContent?,
    @SerializedName("commit")
    val commit: GitHubCommit?
)

data class GitHubContent(
    @SerializedName("name")
    val name: String,
    @SerializedName("path")
    val path: String,
    @SerializedName("sha")
    val sha: String,
    @SerializedName("size")
    val size: Int,
    @SerializedName("url")
    val url: String,
    @SerializedName("html_url")
    val htmlUrl: String,
    @SerializedName("git_url")
    val gitUrl: String,
    @SerializedName("download_url")
    val downloadUrl: String?,
    @SerializedName("content")
    val content: String? = null,
    @SerializedName("type")
    val type: String? = null
)

data class GitHubCommit(
    @SerializedName("sha")
    val sha: String,
    @SerializedName("url")
    val url: String
)

data class GitHubErrorResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("documentation_url")
    val documentationUrl: String?
)
