package com.rrajath.hugowriter.data

import android.util.Log
import android.icu.text.Transliterator
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
    val isPublished: Boolean = false,
    val publishedFilename: String? = null,  // Track the filename when first published
    val targetPath: String? = null,          // Track which directory this post belongs to
    val images: List<String> = emptyList(),  // List of image filenames attached to this post
    val isBundle: Boolean = false            // Track if it's currently a Hugo Page Bundle
) {
    fun shouldBeBundle(): Boolean = images.isNotEmpty()
    fun getWordCount(): Int {
        return content.trim().split("\\s+".toRegex()).size
    }

    fun getFileName(): String {
        return getBaseName().plus(".md")
    }

    fun getBaseName(): String {
        // Transliterate non-English characters to Latin (e.g. Cyrillic to Latin)
        val transliterated = try {
            transliteratorThreadLocal.get()?.transliterate(title.trim()) ?: title.trim()
        } catch (e: Exception) {
            title.trim()
        }

        return transliterated.lowercase()
            .replace("\\s*-\\s*".toRegex(), "-")
            .replace(" ", "-")
            .replace("[^a-z0-9-]".toRegex(), "")
            .replace("-+".toRegex(), "-") // Collapse multiple hyphens
            .removePrefix("-")
            .removeSuffix("-")
            .ifEmpty { "post-${id.takeLast(6)}" } // Use last part of ID as fallback
    }

    fun getBundleFolderName(): String {
        return getBaseName()
    }

    fun getBundleContentPath(): String {
        return "${getBundleFolderName()}/index.md"
    }

    /**
     * Extract the draft status from the post's frontmatter.
     * Returns true if draft field is set to true, false otherwise.
     */
    fun isDraft(): Boolean {
        try {
            // Extract frontmatter - support both --- (YAML) and +++ (TOML) delimiters
            val yamlFrontmatterRegex = "^---\\n([\\s\\S]*?)\\n---".toRegex()
            val tomlFrontmatterRegex = "^\\+\\+\\+\\n([\\s\\S]*?)\\n\\+\\+\\+".toRegex()

            val yamlMatch = yamlFrontmatterRegex.find(content)
            val tomlMatch = tomlFrontmatterRegex.find(content)
            val frontmatterMatch = yamlMatch ?: tomlMatch

            if (frontmatterMatch == null) {
                return false
            }

            val frontmatter = frontmatterMatch.groupValues[1]

            // Extract draft from frontmatter (support both YAML "draft:" and TOML "draft =")
            val draftRegex = "draft\\s*[=:]\\s*(true|false)".toRegex()
            val draftMatch = draftRegex.find(frontmatter)

            if (draftMatch != null) {
                val draftValue = draftMatch.groupValues[1].trim()
                return draftValue == "true"
            }
        } catch (e: Exception) {
            Log.e("Post", "[$title] Exception in isDraft: ${e.message}")
        }

        return false
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
        private val transliteratorThreadLocal = ThreadLocal.withInitial {
            try {
                Transliterator.getInstance("Any-Latin; Latin-ASCII")
            } catch (e: Exception) {
                null
            }
        }

        fun generateId(): String {
            return System.currentTimeMillis().toString()
        }
    }
}
