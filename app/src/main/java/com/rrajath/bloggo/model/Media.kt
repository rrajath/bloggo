package com.rrajath.bloggo.model

/**
 * An image picked on-device but not yet committed to the repo.
 *
 * Held only for the lifetime of the process — see `BloggoApp`'s in-memory
 * `stagedMedia` registry — until a post claims it (setting [claimedByPostSlug])
 * and its bytes go into that post's own commit, at which point it's dropped
 * from the registry entirely; there is no durable "uploaded but unclaimed"
 * state, on purpose, matching the fact that a post's own in-progress edits
 * aren't autosaved yet either (`PROGRESS.md`'s "Editor autosave to Room").
 *
 * [localPath] is a plain absolute filesystem `String`, deliberately never an
 * `android.net.Uri` — this module stays pure/JVM-testable, per [Post]'s own
 * doc comment.
 */
data class StagedMedia(
  /** Where the downscaled copy actually sits on disk right now. */
  val localPath: String,
  /** Where it will live in the repo once committed, e.g. `static/images/2026/on-agents-1.jpg`. */
  val repoPath: String,
  /** The site-absolute form of [repoPath] a post's markdown actually references. */
  val sitePath: String,
  val originalFileName: String,
  val claimedByPostSlug: String? = null,
  val stagedAt: Long,
)

/**
 * Where a newly staged image should live once committed.
 *
 * With a known [slug] — an Insert -> Image upload against a specific post —
 * the file is renamed by slug (`{imagePath}/{year}/{slug}-{n}.{ext}`), matching
 * the Media screen's own "renamed by slug" copy; [count] is how many images
 * this post has already staged, so a second upload doesn't collide with the
 * first. Without a slug — a bare Media-screen upload with no post context yet
 * — the file is named from the upload time and a sanitized original filename
 * instead, since there is nothing to rename it by.
 */
fun stagedImagePath(imagePath: String, slug: String?, originalFileName: String, timestamp: Long, count: Int = 0): String {
  val extension = originalFileName.substringAfterLast('.', missingDelimiterValue = "jpg").lowercase()
  val year = java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneOffset.UTC).year
  val base = imagePath.trim().trim('/')
  val fileName = if (slug != null) {
    "$slug-${count + 1}.$extension"
  } else {
    val sanitized = originalFileName.substringBeforeLast('.')
      .replace(Regex("[^a-zA-Z0-9-]+"), "-")
      .trim('-')
      .lowercase()
      .ifBlank { "upload" }
    "$timestamp-$sanitized.$extension"
  }
  return "$base/$year/$fileName"
}
