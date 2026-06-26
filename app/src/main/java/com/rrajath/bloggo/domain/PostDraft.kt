package com.rrajath.bloggo.domain

data class PostDraft(
    val localId: String,
    val repoPath: String? = null,
    val blobSha: String? = null,
    val syncState: SyncState = SyncState.LOCAL_ONLY,
    val title: String = "",
    val slug: String = "",
    val draft: Boolean = true,
    val slugAutoDerive: Boolean = true,
    val rawFrontMatter: String = "",
    val postDate: String? = null,
    val body: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val isSynced: Boolean
        get() = syncState != SyncState.LOCAL_ONLY

    val targetFilename: String
        get() = "$slug.md"

    fun targetPath(contentPath: String): String =
        "$contentPath/$targetFilename"
}
