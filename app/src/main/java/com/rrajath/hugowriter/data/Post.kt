package com.rrajath.hugowriter.data

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Post(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val publishedAt: Long? = null,
    val isPublished: Boolean = false
) {
    fun getWordCount(): Int {
        return content.trim().split("\\s+".toRegex()).size
    }

    fun getFileName(): String {
        return title.lowercase().replace(" ", "-").replace("[^a-z0-9-]".toRegex(), "") + ".md"
    }

    /**
     * Extract the date from the post's frontmatter.
     * Returns the timestamp in milliseconds, or null if not found/parseable.
     * Falls back to updatedAt if frontmatter date is not available.
     */
    fun getFrontmatterDate(): Long {
        Log.d("Post", "getFrontmatterDate() called for post: $title (id: $id)")

        try {
            // Extract frontmatter - support both --- (YAML) and +++ (TOML) delimiters
            val yamlFrontmatterRegex = "^---\\n([\\s\\S]*?)\\n---".toRegex()
            val tomlFrontmatterRegex = "^\\+\\+\\+\\n([\\s\\S]*?)\\n\\+\\+\\+".toRegex()

            val yamlMatch = yamlFrontmatterRegex.find(content)
            val tomlMatch = tomlFrontmatterRegex.find(content)
            val frontmatterMatch = yamlMatch ?: tomlMatch

            if (frontmatterMatch == null) {
                Log.w("Post", "[$title] No frontmatter found (tried --- and +++). Content starts with: ${content.take(100)}")
                Log.d("Post", "[$title] Falling back to updatedAt: $updatedAt")
                return updatedAt
            }

            val frontmatter = frontmatterMatch.groupValues[1]
            val delimiter = if (yamlMatch != null) "---" else "+++"
            Log.d("Post", "[$title] Frontmatter extracted using $delimiter: ${frontmatter.take(200)}")

            // Extract date from frontmatter (support both YAML "date:" and TOML "date =")
            val dateRegex = "date\\s*[=:]\\s*[\"']?([^\"'\\n]+)[\"']?".toRegex()
            var dateMatch = dateRegex.find(frontmatter)

            // If date field not found, try lastmod as fallback
            if (dateMatch == null) {
                Log.d("Post", "[$title] No date field found, trying lastmod")
                val lastmodRegex = "lastmod\\s*[=:]\\s*[\"']?([^\"'\\n]+)[\"']?".toRegex()
                dateMatch = lastmodRegex.find(frontmatter)
            }

            if (dateMatch == null) {
                Log.w("Post", "[$title] No date or lastmod field found in frontmatter")
                Log.d("Post", "[$title] Falling back to updatedAt: $updatedAt")
                return updatedAt
            }

            val dateString = dateMatch.groupValues[1].trim()
            Log.d("Post", "[$title] Date string extracted: '$dateString'")

            // Try parsing ISO 8601 format first
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
                val parsedDate = sdf.parse(dateString)?.time
                if (parsedDate != null) {
                    Log.d("Post", "[$title] Successfully parsed ISO date: $parsedDate")
                    return parsedDate
                }
            } catch (e: Exception) {
                Log.d("Post", "[$title] ISO format parsing failed: ${e.message}")
            }

            // Try simple date format
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val parsedDate = sdf.parse(dateString)?.time
                if (parsedDate != null) {
                    Log.d("Post", "[$title] Successfully parsed simple date: $parsedDate")
                    return parsedDate
                }
            } catch (e: Exception) {
                Log.d("Post", "[$title] Simple format parsing failed: ${e.message}")
            }

            Log.w("Post", "[$title] All date parsing failed. Date string was: '$dateString'")
            Log.d("Post", "[$title] Falling back to updatedAt: $updatedAt")
        } catch (e: Exception) {
            Log.e("Post", "[$title] Exception in getFrontmatterDate: ${e.message}")
        }

        return updatedAt
    }

    companion object {
        fun generateId(): String {
            return System.currentTimeMillis().toString()
        }
    }
}
