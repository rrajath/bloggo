package com.rrajath.hugowriter.data

data class GitHubConfig(
    val personalAccessToken: String = "",
    val repositoryOwner: String = "",
    val repositoryName: String = "",
    val branch: String = "main",
    val targetDirectories: List<String> = listOf("content/posts/")
) {
    fun isValid(): Boolean {
        return personalAccessToken.isNotBlank() &&
                repositoryOwner.isNotBlank() &&
                repositoryName.isNotBlank() &&
                branch.isNotBlank() &&
                targetDirectories.isNotEmpty()
    }

    fun getFullRepositoryName(): String {
        return "$repositoryOwner/$repositoryName"
    }

    /**
     * Helper to get the first directory as a default.
     */
    fun getDefaultDirectory(): String = targetDirectories.firstOrNull() ?: "content/posts/"
}
