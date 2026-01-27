package com.rrajath.hugowriter.data

data class GitHubConfig(
    val personalAccessToken: String = "",
    val repositoryOwner: String = "",
    val repositoryName: String = "",
    val branch: String = "main",
    val targetDirectory: String = "content/posts/"
) {
    fun isValid(): Boolean {
        return personalAccessToken.isNotBlank() &&
                repositoryOwner.isNotBlank() &&
                repositoryName.isNotBlank() &&
                branch.isNotBlank()
    }

    fun getFullRepositoryName(): String {
        return "$repositoryOwner/$repositoryName"
    }
}
