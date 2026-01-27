package com.rrajath.hugowriter.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.rrajath.hugowriter.data.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostRepository(private val context: Context) {
    private val gson = Gson()
    private val postsDirectory: File
        get() = File(context.filesDir, "posts").apply {
            if (!exists()) mkdirs()
        }

    suspend fun getAllPosts(): List<Post> = withContext(Dispatchers.IO) {
        try {
            // Clean up duplicate files first
            cleanupDuplicateFiles()

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            val posts = postsDirectory.listFiles()
                ?.filter { it.extension == "json" }
                ?.mapNotNull { file ->
                    try {
                        val post = gson.fromJson(file.readText(), Post::class.java)
                        if (post != null) {
                            val frontmatterDate = post.getFrontmatterDate()
                            val formattedDate = dateFormat.format(Date(frontmatterDate))
                            Log.d("PostRepository", "File: ${file.name}, Title: ${post.title}, Frontmatter Date: $formattedDate (timestamp: $frontmatterDate)")
                        }
                        post
                    } catch (e: Exception) {
                        Log.e("PostRepository", "Error loading post from ${file.name}: ${e.message}")
                        null
                    }
                }
                ?.distinctBy { it.id }
                ?.sortedByDescending { it.getFrontmatterDate() }
                ?: emptyList()

            Log.d("PostRepository", "Total posts loaded: ${posts.size}")
            Log.d("PostRepository", "Posts after sorting:")
            posts.forEachIndexed { index, post ->
                val frontmatterDate = post.getFrontmatterDate()
                val formattedDate = dateFormat.format(Date(frontmatterDate))
                Log.d("PostRepository", "  $index. ${post.title} - $formattedDate")
            }

            return@withContext posts
        } catch (e: Exception) {
            Log.e("PostRepository", "Error in getAllPosts: ${e.message}")
            emptyList()
        }
    }

    private fun cleanupDuplicateFiles() {
        try {
            val files = postsDirectory.listFiles()?.filter { it.extension == "json" } ?: return
            val postsByIdAndFile = mutableMapOf<String, MutableList<Pair<File, Post>>>()

            // Group files by post ID
            files.forEach { file ->
                try {
                    val post = gson.fromJson(file.readText(), Post::class.java)
                    if (post != null) {
                        postsByIdAndFile.getOrPut(post.id) { mutableListOf() }.add(file to post)
                    }
                } catch (e: Exception) {
                    // Ignore corrupt files
                }
            }

            // For each post ID with multiple files, keep only the correct one
            postsByIdAndFile.forEach { (postId, filesAndPosts) ->
                if (filesAndPosts.size > 1) {
                    // Keep the file named correctly (postId.json), delete others
                    val correctFileName = "$postId.json"
                    filesAndPosts.forEach { (file, post) ->
                        if (file.name != correctFileName) {
                            file.delete()
                        }
                    }

                    // If the correct file doesn't exist, keep the most recently updated one
                    val correctFile = File(postsDirectory, correctFileName)
                    if (!correctFile.exists() && filesAndPosts.isNotEmpty()) {
                        val mostRecent = filesAndPosts.maxByOrNull { it.second.getFrontmatterDate() }
                        mostRecent?.let { (file, post) ->
                            // Rename to correct filename
                            file.renameTo(correctFile)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    suspend fun getPostById(id: String): Post? = withContext(Dispatchers.IO) {
        try {
            val file = File(postsDirectory, "$id.json")
            if (file.exists()) {
                gson.fromJson(file.readText(), Post::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getPostByTitle(title: String): Post? = withContext(Dispatchers.IO) {
        try {
            getAllPosts().find { it.title.equals(title, ignoreCase = true) }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun savePost(post: Post): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(postsDirectory, "${post.id}.json")
            file.writeText(gson.toJson(post))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePost(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(postsDirectory, "$id.json")
            if (file.exists()) {
                file.delete()
                Result.success(Unit)
            } else {
                Result.failure(FileNotFoundException("Post not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchPosts(query: String): List<Post> = withContext(Dispatchers.IO) {
        getAllPosts().filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.content.contains(query, ignoreCase = true)
        }
    }
}
