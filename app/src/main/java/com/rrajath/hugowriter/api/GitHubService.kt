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

    suspend fun publishPost(
        post: Post,
        config: GitHubConfig,
        targetPath: String? = null,
        images: List<Pair<String, ByteArray>> = emptyList()
    ): Result<String> {
        return try {
            if (!config.isValid()) {
                return Result.failure(IllegalArgumentException("GitHub configuration is invalid"))
            }

            initializeRetrofit(config.personalAccessToken)

            val directory = (targetPath ?: post.targetPath ?: config.getDefaultDirectory()).trimEnd('/')
            val isBundle = images.isNotEmpty()
            
            val newFilename = if (isBundle) post.getBundleContentPath() else post.getFileName()
            val newFilePath = "$directory/$newFilename".trimStart('/')
            
            // If the post was previously published with a different filename/path, delete the old file
            val oldFilename = post.publishedFilename
            if (oldFilename != null && oldFilename != newFilename) {
                val oldDirectory = post.targetPath ?: config.getDefaultDirectory()
                val oldFilePath = "${oldDirectory.trimEnd('/')}/$oldFilename".trimStart('/')
                deleteFileOnGitHub(config, oldFilePath, "Rename/Move post: $oldFilename -> $newFilename")
                
                // If it was a bundle, we might need to delete other files, 
                // but for now let's focus on the main content file.
            }

            // 1. Upload Images if it's a bundle
            if (isBundle) {
                val bundleFolder = post.getBundleFolderName()
                for ((imageName, imageBytes) in images) {
                    val imagePath = "$directory/$bundleFolder/$imageName".trimStart('/')
                    uploadFile(config, imagePath, imageBytes, "Upload image: $imageName")
                }
            }

            // 2. Upload the markdown content
            val content = post.content
            val encodedContent = Base64.encodeToString(
                content.toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP
            )

            val htmlUrl = uploadFile(config, newFilePath, encodedContent, "Publish post: ${post.title}", isBase64 = true)
            Result.success(htmlUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun uploadFile(
        config: GitHubConfig,
        path: String,
        content: Any,
        message: String,
        isBase64: Boolean = false
    ): String {
        val encodedContent = if (content is ByteArray) {
            Base64.encodeToString(content, Base64.NO_WRAP)
        } else if (content is String && !isBase64) {
            Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        } else {
            content as String
        }

        var existingSha: String? = null
        try {
            val response = api?.getFileContent(config.repositoryOwner, config.repositoryName, path, config.branch)
            if (response?.isSuccessful == true) {
                existingSha = response.body()?.sha
            }
        } catch (e: Exception) {}

        val request = CreateFileRequest(
            message = message,
            content = encodedContent,
            branch = config.branch,
            sha = existingSha
        )

        val response = api?.createOrUpdateFile(config.repositoryOwner, config.repositoryName, path, request)
        if (response?.isSuccessful == true) {
            return response.body()?.content?.htmlUrl ?: ""
        } else {
            throw Exception("Failed to upload $path: ${response?.code()} - ${response?.errorBody()?.string()}")
        }
    }

    private suspend fun deleteFileOnGitHub(config: GitHubConfig, path: String, message: String) {
        try {
            val response = api?.getFileContent(config.repositoryOwner, config.repositoryName, path, config.branch)
            if (response?.isSuccessful == true && response.body() != null) {
                val sha = response.body()!!.sha
                val request = DeleteFileRequest(message = message, sha = sha, branch = config.branch)
                api?.deleteFile(config.repositoryOwner, config.repositoryName, path, request)
            }
        } catch (e: Exception) {}
    }

    suspend fun deletePost(post: Post, config: GitHubConfig): Result<Unit> {
        return try {
            if (!config.isValid()) {
                return Result.failure(IllegalArgumentException("GitHub configuration is invalid"))
            }

            initializeRetrofit(config.personalAccessToken)

            // Use publishedFilename if available, otherwise use current filename
            val filename = post.publishedFilename ?: post.getFileName()
            val directory = post.targetPath ?: config.getDefaultDirectory()
            val filePath = "${directory.trimEnd('/')}/$filename".trimStart('/')

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

            val allPosts = mutableListOf<Post>()
            
            for (targetDirectory in config.targetDirectories) {
                // List all files in the target directory
                val listResponse = api?.listFiles(
                    owner = config.repositoryOwner,
                    repo = config.repositoryName,
                    path = targetDirectory.trimEnd('/'),
                    ref = config.branch
                )

                if (listResponse?.isSuccessful != true || listResponse.body() == null) {
                    continue // Skip directories that don't exist or fail
                }

                val entries = listResponse.body()!!
                val mdFiles = entries.filter { it.type == "file" && it.name.endsWith(".md") && it.name != "index.md" }
                val directories = entries.filter { it.type == "dir" }

                // 1. Process flat .md files
                for (file in mdFiles) {
                    processFile(config, file.path, targetDirectory, allPosts, isBundle = false)
                }

                // 2. Process directories (potential bundles)
                for (dir in directories) {
                    val bundlePath = "${dir.path}/index.md"
                    // Check if index.md exists in this directory
                    val bundleContentResponse = api?.getFileContent(
                        owner = config.repositoryOwner,
                        repo = config.repositoryName,
                        path = bundlePath,
                        ref = config.branch
                    )
                    
                    if (bundleContentResponse?.isSuccessful == true) {
                        // It's a bundle!
                        // List images in this bundle folder
                        val bundleEntriesResponse = api?.listFiles(
                            owner = config.repositoryOwner,
                            repo = config.repositoryName,
                            path = dir.path,
                            ref = config.branch
                        )
                        
                        val images = bundleEntriesResponse?.body()
                            ?.filter { it.type == "file" && (it.name.endsWith(".jpg") || it.name.endsWith(".png") || it.name.endsWith(".jpeg")) }
                            ?.map { it.name } ?: emptyList()
                            
                        processFile(config, bundlePath, targetDirectory, allPosts, isBundle = true, images = images)
                    }
                }
            }
            Result.success(allPosts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun processFile(
        config: GitHubConfig,
        filePath: String,
        targetDirectory: String,
        allPosts: MutableList<Post>,
        isBundle: Boolean,
        images: List<String> = emptyList()
    ) {
        try {
            val contentResponse = api?.getFileContent(
                owner = config.repositoryOwner,
                repo = config.repositoryName,
                path = filePath,
                ref = config.branch
            )

            if (contentResponse?.isSuccessful == true && contentResponse.body() != null) {
                val content = contentResponse.body()!!.content ?: return
                val fileName = contentResponse.body()!!.name
                
                // Decode base64 content
                val decodedContent = String(
                    Base64.decode(content.replace("\n", ""), Base64.DEFAULT),
                    Charsets.UTF_8
                )

                // Parse frontmatter
                val yamlFrontmatterRegex = "^---\\n([\\s\\S]*?)\\n---".toRegex()
                val tomlFrontmatterRegex = "^\\+\\+\\+\\n([\\s\\S]*?)\\n\\+\\+\\+".toRegex()

                val yamlMatch = yamlFrontmatterRegex.find(decodedContent)
                val tomlMatch = tomlFrontmatterRegex.find(decodedContent)
                val frontmatterMatch = yamlMatch ?: tomlMatch

                var title = fileName
                    .removeSuffix(".md")
                    .removeSuffix("index") // if it was index.md
                    .replace("-", " ")
                    .trim()
                    .split(" ")
                    .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

                if (title.isEmpty() && isBundle) {
                    // Use folder name as title if index.md is in a folder
                    title = filePath.split("/").dropLast(1).last()
                        .replace("-", " ")
                        .split(" ")
                        .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                }

                var postDate = System.currentTimeMillis()

                if (frontmatterMatch != null) {
                    val frontmatter = frontmatterMatch.groupValues[1]
                    val titleRegex = "title\\s*[=:]\\s*[\"']?([^\"'\\n]+)[\"']?".toRegex()
                    val titleMatch = titleRegex.find(frontmatter)
                    if (titleMatch != null) {
                        title = titleMatch.groupValues[1].trim()
                    }

                    val dateRegex = "date\\s*[=:]\\s*[\"']?([^\"'\\n]+)[\"']?".toRegex()
                    val dateMatch = dateRegex.find(frontmatter) ?: "lastmod\\s*[=:]\\s*[\"']?([^\"'\\n]+)[\"']?".toRegex().find(frontmatter)

                    if (dateMatch != null) {
                        val dateString = dateMatch.groupValues[1].trim()
                        try {
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US)
                            postDate = sdf.parse(dateString)?.time ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            try {
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                postDate = sdf.parse(dateString)?.time ?: System.currentTimeMillis()
                            } catch (e: Exception) {}
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
                    isPublished = true,
                    targetPath = targetDirectory,
                    publishedFilename = if (isBundle) filePath.removePrefix(targetDirectory).trimStart('/') else fileName,
                    isBundle = isBundle,
                    images = images
                )
                allPosts.add(post)
            }
        } catch (e: Exception) {}
    }
}
