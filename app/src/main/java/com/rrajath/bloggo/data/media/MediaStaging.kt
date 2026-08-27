package com.rrajath.bloggo.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import com.rrajath.bloggo.data.repoPathToSitePath
import com.rrajath.bloggo.model.StagedMedia
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val STAGED_MEDIA_DIR = "staged-media"

/** The longest edge a staged image is allowed to keep. Caps what a real
 * multi-MB phone photo turns into before it's held as base64 for a commit —
 * without this, [MediaScreen]'s own "Resized, renamed by slug, committed with
 * the post" copy would be false the moment a real upload existed. */
private const val MAX_LONGEST_EDGE = 2000

/**
 * Copies [uri]'s bytes into a local cache file, downscaled, so they survive
 * being read exactly once: a `content://` Uri from the Photo Picker isn't
 * guaranteed to stay readable past this call, and the eventual commit needs
 * real bytes on disk, not a Uri to re-resolve later.
 *
 * [targetRepoPath] is decided by the caller (`stagedImagePath`, `model/Media.kt`)
 * before this runs — this function only ever writes the bytes, never decides
 * where they end up.
 */
suspend fun stageFromUri(
  context: Context,
  uri: Uri,
  targetRepoPath: String,
  originalFileName: String,
): StagedMedia = withContext(Dispatchers.IO) {
  val source = ImageDecoder.createSource(context.contentResolver, uri)
  val bitmap = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
    // compress() below needs real pixels, not a GPU-backed HARDWARE bitmap.
    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
  }
  val scaled = downscale(bitmap, MAX_LONGEST_EDGE)

  val stagingDir = File(context.cacheDir, STAGED_MEDIA_DIR).apply { mkdirs() }
  val localFile = File(stagingDir, targetRepoPath.substringAfterLast('/'))
  FileOutputStream(localFile).use { out ->
    val format = if (targetRepoPath.substringAfterLast('.', "").lowercase() == "png") {
      Bitmap.CompressFormat.PNG
    } else {
      Bitmap.CompressFormat.JPEG
    }
    scaled.compress(format, 90, out)
  }

  StagedMedia(
    localPath = localFile.absolutePath,
    repoPath = targetRepoPath,
    sitePath = repoPathToSitePath(targetRepoPath),
    originalFileName = originalFileName,
    stagedAt = System.currentTimeMillis(),
  )
}

private fun downscale(bitmap: Bitmap, maxLongestEdge: Int): Bitmap {
  val longestEdge = maxOf(bitmap.width, bitmap.height)
  if (longestEdge <= maxLongestEdge) return bitmap
  val scale = maxLongestEdge.toFloat() / longestEdge
  val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
  val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
  return Bitmap.createScaledBitmap(bitmap, width, height, true)
}

/** An upload that's staged but never claimed by a post doesn't survive an app
 * restart — there is no durable "uploaded but unclaimed" state (see
 * [StagedMedia]'s own doc comment), so its cached bytes would otherwise leak
 * in [Context.getCacheDir] forever. Called once, at cold start, before the
 * in-memory registry it backs has anything in it — so this is always a full
 * clear, never a selective sweep. */
fun clearStagedMediaCache(context: Context) {
  File(context.cacheDir, STAGED_MEDIA_DIR).deleteRecursively()
}
