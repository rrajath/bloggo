package com.rrajath.hugowriter.api

import android.util.Base64
import com.rrajath.hugowriter.data.GitHubConfig
import com.rrajath.hugowriter.data.Post
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class GitHubService {
    private var retrofit: Retrofit? = null
    private var api: GitHubApi? = null
    private var currentToken: String = ""

    private fun initializeRetrofit(token: String) {
        if (retrofit == null || currentToken != token) {
            currentToken = token

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val authInterceptor = Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "token $token")
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .build()
                chain.proceed(request)
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            api = retrofit?.create(GitHubApi::class.java)
        }
    }

    suspend fun publishPost(post: Post, config: GitHubConfig): Result<String> {
        return try {
            if (!config.isValid()) {
                return Result.failure(IllegalArgumentException("GitHub configuration is invalid"))
            }

            initializeRetrofit(config.personalAccessToken)

            val newFilename = post.getFileName()
            val newFilePath = "${config.targetDirectory}$newFilename"
            val content = post.content
            val encodedContent = Base64.encodeToString(
                content.toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP
            )

            // If the post was previously published with a different filename, delete the old file
            if (post.publishedFilename != null && post.publishedFilename != newFilename) {
                val oldFilePath = "${config.targetDirectory}${post.publishedFilename}"
                try {
                    val oldFileResponse = api?.getFileContent(
                        owner = config.repositoryOwner,
                        repo = config.repositoryName,
                        path = oldFilePath,
                        ref = config.branch
                    )
                    if (oldFileResponse?.isSuccessful == true && oldFileResponse.body() != null) {
                        val oldSha = oldFileResponse.body()!!.sha
                        val deleteRequest = DeleteFileRequest(
                            message = "Rename post: ${post.publishedFilename} -> $newFilename",
                            sha = oldSha,
                            branch = config.branch
                        )
                        api?.deleteFile(
                            owner = config.repositoryOwner,
                            repo = config.repositoryName,
                            path = oldFilePath,
                            request = deleteRequest
                        )
                    }
                } catch (e: Exception) {
                    // Old file doesn't exist or couldn't be deleted, continue anyway
                }
            }

            // Check if file already exists to get its SHA (for the new filename)
            var existingSha: String? = null
            try {
                val existingFileResponse = api?.getFileContent(
                    owner = config.repositoryOwner,
                    repo = config.repositoryName,
                    path = newFilePath,
                    ref = config.branch
                )
                if (existingFileResponse?.isSuccessful == true) {
                    existingSha = existingFileResponse.body()?.sha
                }
            } catch (e: Exception) {
                // File doesn't exist, which is fine
            }

            val request = CreateFileRequest(
                message = if (existingSha != null) {
                    "Update post: ${post.title}"
                } else {
                    "Create post: ${post.title}"
                },
                content = encodedContent,
                branch = config.branch,
                sha = existingSha
            )

            val response = api?.createOrUpdateFile(
                owner = config.repositoryOwner,
                repo = config.repositoryName,
                path = newFilePath,
                request = request
            )

            if (response?.isSuccessful == true) {
                val htmlUrl = response.body()?.content?.htmlUrl
                    ?: "https://github.com/${config.getFullRepositoryName()}/blob/${config.branch}/$newFilePath"
                Result.success(htmlUrl)
            } else {
                val errorBody = response?.errorBody()?.string()
                Result.failure(Exception("Failed to publish: ${response?.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePost(post: Post, config: GitHubConfig): Result<Unit> {
        return try {
            if (!config.isValid()) {
                return Result.failure(IllegalArgumentException("GitHub configuration is invalid"))
            }

            initializeRetrofit(config.personalAccessToken)

            // Use publishedFilename if available, otherwise use current filename
            val filename = post.publishedFilename ?: post.getFileName()
            val filePath = "${config.targetDirectory}$filename"

            // Get the file's SHA first
            val fileResponse = api?.getFileContent(
                owner = config.repositoryOwner,
                repo = config.repositoryName,
                path = filePath,
                ref = config.branch
            )

            if (fileResponse?.isSuccessful != true || fileResponse.body() == null) {
                return Result.failure(Exception("File not found on GitHub"))
            }

            val sha = fileResponse.body()!!.sha

            val request = DeleteFileRequest(
                message = "Delete post: ${post.title}",
                sha = sha,
                branch = config.branch
            )

            val response = api?.deleteFile(
                owner = config.repositoryOwner,
                repo = config.repositoryName,
                path = filePath,
                request = request
            )

            if (response?.isSuccessful == true) {
                Result.success(Unit)
            } else {
                val errorBody = response?.errorBody()?.string()
                Result.failure(Exception("Failed to delete: ${response?.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAllPosts(config: GitHubConfig): Result<List<Post>> {
        return try {
            if (!config.isValid()) {
                return Result.failure(IllegalArgumentException("GitHub configuration is invalid"))
            }

            initializeRetrofit(config.personalAccessToken)

            // List all files in the target directory
            val listResponse = api?.listFiles(
                owner = config.repositoryOwner,
                repo = config.repositoryName,
                path = config.targetDirectory.trimEnd('/'),
                ref = config.branch
            )

            if (listResponse?.isSuccessful != true || listResponse.body() == null) {
                return Result.failure(Exception("Failed to list files: ${listResponse?.code()}"))
            }

            val files = listResponse.body()!!
                .filter { it.type == "file" && it.name.endsWith(".md") }

            val posts = mutableListOf<Post>()

            // Fetch content for each file
            for (file in files) {
                try {
                    val contentResponse = api?.getFileContent(
                        owner = config.repositoryOwner,
                        repo = config.repositoryName,
                        path = file.path,
                        ref = config.branch
                    )

                    if (contentResponse?.isSuccessful == true && contentResponse.body() != null) {
                        val content = contentResponse.body()!!.content
                        if (content != null) {
                            // Decode base64 content
                            val decodedContent = String(
                                Base64.decode(content.replace("\n", ""), Base64.DEFAULT),
                                Charsets.UTF_8
                            )

                            // Parse frontmatter to extract title and date - support both --- (YAML) and +++ (TOML)
                            val yamlFrontmatterRegex = "^---\\n([\\s\\S]*?)\\n---".toRegex()
                            val tomlFrontmatterRegex = "^\\+\\+\\+\\n([\\s\\S]*?)\\n\\+\\+\\+".toRegex()

                            val yamlMatch = yamlFrontmatterRegex.find(decodedContent)
                            val tomlMatch = tomlFrontmatterRegex.find(decodedContent)
                            val frontmatterMatch = yamlMatch ?: tomlMatch

                            var title = file.name
                                .removeSuffix(".md")
                                .replace("-", " ")
                                .split(" ")
                                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

                            var postDate = System.currentTimeMillis()

                            if (frontmatterMatch != null) {
                                val frontmatter = frontmatterMatch.groupValues[1]

                                // Extract title from frontmatter (support both YAML "title:" and TOML "title =")
                                val titleRegex = "title\\s*[=:]\\s*[\"']?([^\"'\\n]+)[\"']?".toRegex()
                                val titleMatch = titleRegex.find(frontmatter)
                                if (titleMatch != null) {
                                    title = titleMatch.groupValues[1].trim()
                                }

                                // Extract date from frontmatter (support both YAML "date:" and TOML "date =")
                                val dateRegex = "date\\s*[=:]\\s*[\"']?([^\"'\\n]+)[\"']?".toRegex()
                                var dateMatch = dateRegex.find(frontmatter)

                                // If date field not found, try lastmod as fallback
                                if (dateMatch == null) {
                                    val lastmodRegex = "lastmod\\s*[=:]\\s*[\"']?([^\"'\\n]+)[\"']?".toRegex()
                                    dateMatch = lastmodRegex.find(frontmatter)
                                }

                                if (dateMatch != null) {
                                    val dateString = dateMatch.groupValues[1].trim()
                                    try {
                                        // Parse ISO date format
                                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US)
                                        postDate = sdf.parse(dateString)?.time ?: System.currentTimeMillis()
                                    } catch (e: Exception) {
                                        try {
                                            // Try simple date format
                                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                            postDate = sdf.parse(dateString)?.time ?: System.currentTimeMillis()
                                        } catch (e: Exception) {
                                            // Use current time if parsing fails
                                            postDate = System.currentTimeMillis()
                                        }
                                    }
                                }
                            }

                            val post = Post(
                                id = Post.generateId(),
                                title = title,
                                content = decodedContent,
                                createdAt = postDate,
                                updatedAt = postDate,
                                publishedAt = postDate,
                                isPublished = true
                            )
                            posts.add(post)
                        }
                    }
                } catch (e: Exception) {
                    // Skip this file if there's an error
                    continue
                }
            }

            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
