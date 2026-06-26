package com.rrajath.bloggo.data

import com.google.common.truth.Truth.assertThat
import com.rrajath.bloggo.ui.theme.ThemeMode
import org.junit.Test

class SettingsTest {

    @Test
    fun defaults_areCorrect() {
        val settings = Settings()

        assertThat(settings.githubPat).isEmpty()
        assertThat(settings.repository).isEmpty()
        assertThat(settings.branch).isEqualTo("main")
        assertThat(settings.contentPath).isEqualTo("content/posts")
        assertThat(settings.imageRepoPath).isEqualTo("static/images")
        assertThat(settings.imageUrlBase).isEqualTo("/images")
        assertThat(settings.blogBaseUrl).isEmpty()
        assertThat(settings.frontMatterTemplate).contains("{date}")
        assertThat(settings.theme).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    fun isConfigured_trueWhenPatAndRepoSet() {
        val settings = Settings(githubPat = "ghp_123", repository = "me/blog")
        assertThat(settings.isConfigured).isTrue()
    }

    @Test
    fun isConfigured_falseWhenPatEmpty() {
        val settings = Settings(githubPat = "", repository = "me/blog")
        assertThat(settings.isConfigured).isFalse()
    }

    @Test
    fun isConfigured_falseWhenRepoEmpty() {
        val settings = Settings(githubPat = "ghp_123", repository = "")
        assertThat(settings.isConfigured).isFalse()
    }

    @Test
    fun isConfigured_falseWhenBothEmpty() {
        val settings = Settings()
        assertThat(settings.isConfigured).isFalse()
    }

    @Test
    fun ownerRepo_parsesValidRepo() {
        val settings = Settings(repository = "me/blog")
        val (owner, repo) = settings.ownerRepo!!
        assertThat(owner).isEqualTo("me")
        assertThat(repo).isEqualTo("blog")
    }

    @Test
    fun ownerRepo_nullForMissingSlash() {
        val settings = Settings(repository = "justrepo")
        assertThat(settings.ownerRepo).isNull()
    }

    @Test
    fun ownerRepo_nullForEmptyRepo() {
        val settings = Settings(repository = "")
        assertThat(settings.ownerRepo).isNull()
    }

    @Test
    fun ownerRepo_nullForTooManySlashes() {
        val settings = Settings(repository = "a/b/c")
        assertThat(settings.ownerRepo).isNull()
    }

    @Test
    fun viewLiveUrl_buildsCorrectUrl() {
        val settings = Settings(blogBaseUrl = "https://blog.example.com")
        assertThat(settings.viewLiveUrl("my-post")).isEqualTo("https://blog.example.com/posts/my-post")
    }

    @Test
    fun viewLiveUrl_trimsTrailingSlash() {
        val settings = Settings(blogBaseUrl = "https://blog.example.com/")
        assertThat(settings.viewLiveUrl("my-post")).isEqualTo("https://blog.example.com/posts/my-post")
    }

    @Test
    fun viewLiveUrl_nullWhenBaseUrlEmpty() {
        val settings = Settings(blogBaseUrl = "")
        assertThat(settings.viewLiveUrl("my-post")).isNull()
    }

    @Test
    fun frontMatterTemplate_containsDateToken() {
        val settings = Settings()
        assertThat(settings.frontMatterTemplate).contains("{date}")
    }
}
