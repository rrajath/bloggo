package com.rrajath.bloggo.data

import com.google.common.truth.Truth.assertThat
import com.rrajath.bloggo.data.network.ContentUpdateResponse
import com.rrajath.bloggo.data.network.GitHubService
import com.rrajath.bloggo.data.network.ContentResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class ImageRepositoryTest {

    private val gitHubService = mockk<GitHubService>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val networkMonitor = mockk<NetworkMonitor>(relaxed = true)

    private lateinit var repository: ImageRepository

    private val testSettings = Settings(
        githubPat = "ghp_test",
        repository = "me/blog",
        branch = "main",
        imageRepoPath = "static/images",
        imageUrlBase = "/images",
    )

    @Before
    fun setup() {
        repository = ImageRepository(gitHubService, settingsRepository, networkMonitor)
        coEvery { settingsRepository.settings } returns flowOf(testSettings)
        coEvery { networkMonitor.checkConnectivity() } returns true
    }

    @Test
    fun uploadImage_offline_returnsError() = runTest {
        coEvery { networkMonitor.checkConnectivity() } returns false

        val result = repository.uploadImage(ByteArray(0), "photo.jpg", ImageSize.SMALL)

        assertThat(result.error).isNotNull()
        assertThat(result.error).contains("Offline")
    }

    @Test
    fun uploadImage_unconfiguredRepo_returnsError() = runTest {
        coEvery { settingsRepository.settings } returns flowOf(testSettings.copy(repository = ""))

        val result = repository.uploadImage(ByteArray(0), "photo.jpg", ImageSize.SMALL)

        assertThat(result.error).contains("not configured")
    }

    @Test
    fun uploadImage_success_returnsMarkdownUrl() = runTest {
        coEvery { gitHubService.putContent(any(), any(), any(), any()) } returns Response.success(
            ContentUpdateResponse(
                content = ContentResponse(null, null, "sha", "static/images/photo.jpg", "photo.jpg"),
                commit = null,
            ),
        )

        val result = repository.uploadImage(ByteArray(0), "photo.jpg", ImageSize.SMALL)

        assertThat(result.error).isNull()
        assertThat(result.markdownUrl).isNotNull()
        assertThat(result.markdownUrl).startsWith("/images/")
    }

    @Test
    fun uploadImage_apiError_returnsError() = runTest {
        coEvery { gitHubService.putContent(any(), any(), any(), any()) } returns Response.error(403, mockk(relaxed = true))

        val result = repository.uploadImage(ByteArray(0), "photo.jpg", ImageSize.SMALL)

        assertThat(result.error).isNotNull()
        assertThat(result.error).contains("403")
    }

    @Test
    fun uploadImage_networkException_returnsError() = runTest {
        coEvery { gitHubService.putContent(any(), any(), any(), any()) } throws RuntimeException("Timeout")

        val result = repository.uploadImage(ByteArray(0), "photo.jpg", ImageSize.SMALL)

        assertThat(result.error).isNotNull()
        assertThat(result.error).contains("Timeout")
    }

    @Test
    fun uploadImage_callsPutContentWithCorrectPath() = runTest {
        coEvery { gitHubService.putContent(any(), any(), any(), any()) } returns Response.success(
            ContentUpdateResponse(null, null),
        )

        repository.uploadImage(ByteArray(0), "photo.jpg", ImageSize.SMALL)

        coVerify {
            gitHubService.putContent(
                "me",
                "blog",
                match { it.startsWith("static/images/") },
                any(),
            )
        }
    }

    @Test
    fun uploadImage_commitMessageIncludesFileName() = runTest {
        coEvery { gitHubService.putContent(any(), any(), any(), any()) } returns Response.success(
            ContentUpdateResponse(null, null),
        )

        repository.uploadImage(ByteArray(0), "my-photo.jpg", ImageSize.MEDIUM)

        coVerify {
            gitHubService.putContent(any(), any(), any(), match { it.message.contains("my-photo") })
        }
    }

    @Test
    fun buildMarkdownSnippet_correctFormat() {
        val snippet = repository.buildMarkdownSnippet("alt text", "/images/photo.jpg")
        assertThat(snippet).isEqualTo("![alt text](/images/photo.jpg)")
    }

    @Test
    fun buildMarkdownSnippet_emptyAlt() {
        val snippet = repository.buildMarkdownSnippet("", "/images/photo.jpg")
        assertThat(snippet).isEqualTo("![](/images/photo.jpg)")
    }

    @Test
    fun uploadImage_largeSize_uses1600MaxWidth() = runTest {
        coEvery { gitHubService.putContent(any(), any(), any(), any()) } returns Response.success(
            ContentUpdateResponse(null, null),
        )

        repository.uploadImage(ByteArray(0), "photo.jpg", ImageSize.LARGE)

        coVerify {
            gitHubService.putContent(any(), any(), any(), match { it.message.contains("large") })
        }
    }
}
