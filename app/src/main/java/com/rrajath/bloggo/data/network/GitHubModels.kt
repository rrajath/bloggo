package com.rrajath.bloggo.data.network

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TreeResponse(
    val tree: List<TreeItem>,
    val truncated: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class TreeItem(
    val path: String,
    val mode: String,
    val type: String,
    val sha: String,
    val size: Long? = null,
)

@JsonClass(generateAdapter = true)
data class ContentResponse(
    val content: String?,
    val encoding: String?,
    val sha: String,
    val path: String,
    val name: String,
)

@JsonClass(generateAdapter = true)
data class ContentRequest(
    val message: String,
    val content: String,
    val branch: String,
    val sha: String? = null,
)

@JsonClass(generateAdapter = true)
data class ContentUpdateResponse(
    val content: ContentResponse?,
    val commit: CommitInfo?,
)

@JsonClass(generateAdapter = true)
data class CommitInfo(
    val sha: String,
    val htmlUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class GitHubError(
    val message: String,
    val documentationUrl: String? = null,
)
