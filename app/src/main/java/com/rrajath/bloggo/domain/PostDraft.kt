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

    // Posts synced before slug back-fill shipped, or authored outside the app without an
    // explicit `slug` front matter field, may have a blank slug. Fall back to the filename
    // (how most static site generators derive the URL when no slug is set) so the Live
    // link still works without requiring a re-fetch from GitHub.
    val effectiveSlug: String
        get() = slug.ifBlank { repoPath?.substringAfterLast('/')?.removeSuffix(".md").orEmpty() }

    val targetFilename: String
        get() = "$slug.md"

    fun targetPath(contentPath: String): String =
        "$contentPath/$targetFilename"
}
