package com.rrajath.bloggo.data

import com.rrajath.bloggo.ui.theme.ThemeMode

data class Settings(
    val githubPat: String = "",
    val repository: String = "",
    val branch: String = "main",
    val contentPath: String = "content/posts",
    val imageRepoPath: String = "static/images",
    val imageUrlBase: String = "/images",
    val blogBaseUrl: String = "",
    val frontMatterTemplate: String = "date: {date}\ntags: []\nsummary: \"\"",
    val theme: ThemeMode = ThemeMode.SYSTEM,
) {
    val isConfigured: Boolean
        get() = githubPat.isNotBlank() && repository.isNotBlank()

    val ownerRepo: Pair<String, String>?
        get() = repository.split("/").takeIf { it.size == 2 }
            ?.let { it[0] to it[1] }

    fun viewLiveUrl(slug: String): String? {
        if (blogBaseUrl.isBlank()) return null
        val base = blogBaseUrl.trimEnd('/')
        return "$base/posts/$slug"
    }
}
