package com.rrajath.bloggo.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.rrajath.bloggo.data.network.ContentRequest
import com.rrajath.bloggo.data.network.GitHubService
import com.rrajath.bloggo.data.network.TreeResponse
import kotlinx.coroutines.flow.first
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

enum class ImageSize(val label: String, val maxWidth: Int) {
    SMALL("Small", 480),
    MEDIUM("Medium", 800),
    LARGE("Large", 1600),
}

@Singleton
class ImageRepository @Inject constructor(
    private val gitHubService: GitHubService,
    private val settingsRepository: SettingsRepository,
    private val networkMonitor: NetworkMonitor,
) {
    data class UploadResult(
        val markdownUrl: String? = null,
        val error: String? = null,
    )

    suspend fun uploadImage(
        imageBytes: ByteArray,
        fileName: String,
        size: ImageSize,
    ): UploadResult {
        if (!networkMonitor.checkConnectivity()) {
            return UploadResult(error = "Offline — cannot upload image.")
        }

        val settings = settingsRepository.settings.first()
        val (owner, repo) = settings.ownerRepo ?: return UploadResult(error = "Repository not configured.")
        val branch = settings.branch
        val imageRepoPath = settings.imageRepoPath
        val imageUrlBase = settings.imageUrlBase

        val resized = resizeImage(imageBytes, size.maxWidth)
        val encoded = Base64.getEncoder().encodeToString(resized)

        val uniqueFileName = ensureUniqueFileName(fileName, size)
        val targetPath = "$imageRepoPath/$uniqueFileName"

        val request = ContentRequest(
            message = "Upload image: $uniqueFileName",
            content = encoded,
            branch = branch,
        )

        val response = try {
            gitHubService.putContent(owner, repo, targetPath, request)
        } catch (e: Exception) {
            return UploadResult(error = "Network error: ${e.message}")
        }

        if (!response.isSuccessful) {
            return UploadResult(error = "GitHub API error (${response.code()})")
        }

        val publicUrl = "$imageUrlBase/$uniqueFileName"
        return UploadResult(markdownUrl = publicUrl)
    }

    fun buildMarkdownSnippet(altText: String, imageUrl: String): String {
        return "![$altText]($imageUrl)"
    }

    private fun resizeImage(imageBytes: ByteArray, maxWidth: Int): ByteArray {
        return try {
            val original = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return imageBytes
            if (original.width <= maxWidth) {
                val out = ByteArrayOutputStream()
                original.compress(Bitmap.CompressFormat.JPEG, 85, out)
                return out.toByteArray()
            }

            val ratio = maxWidth.toFloat() / original.width.toFloat()
            val newHeight = (original.height * ratio).toInt()
            val resized = Bitmap.createScaledBitmap(original, maxWidth, newHeight, true)

            val out = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.toByteArray()
        } catch (e: Exception) {
            imageBytes
        }
    }

    private fun ensureUniqueFileName(fileName: String, size: ImageSize): String {
        val timestamp = System.currentTimeMillis()
        val nameWithoutExt = fileName.substringBeforeLast(".")
        return "${nameWithoutExt}-${size.label.lowercase()}-$timestamp.jpg"
    }
}
