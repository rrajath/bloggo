package com.rrajath.bloggo.ui.preview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.SubcomposeAsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.rrajath.bloggo.data.RepoConnection
import com.rrajath.bloggo.data.sitePathToRepoPath
import com.rrajath.bloggo.designsystem.BloggoTheme
import com.rrajath.bloggo.designsystem.component.BloggoIconButton
import com.rrajath.bloggo.designsystem.icon.BloggoIcons
import com.rrajath.bloggo.model.StagedMedia
import java.io.File

/**
 * The same "resolve a site-absolute image path to real bytes" logic
 * [RepoAsyncImage] and [RepoFigureImage] both need: staged media loads
 * straight from disk, anything already in the repo goes through the
 * authenticated raw-content endpoint. Shared so the two composables can't
 * drift on how a path gets resolved.
 */
@Composable
private fun rememberRepoImageModel(
  sitePath: String,
  connection: RepoConnection,
  token: String?,
  stagedMedia: List<StagedMedia>,
): Any {
  val context = LocalContext.current
  val staged = remember(stagedMedia, sitePath) { stagedMedia.firstOrNull { it.sitePath == sitePath } }

  return remember(sitePath, staged, connection.repository, connection.branch, token) {
    val local = staged
    if (local != null) {
      File(local.localPath)
    } else {
      val repoPath = sitePathToRepoPath(sitePath)
      ImageRequest.Builder(context)
        .data("https://api.github.com/repos/${connection.repository}/contents/$repoPath?ref=${connection.branch}")
        .httpHeaders(
          NetworkHeaders.Builder()
            .apply {
              val trimmed = token?.trim()
              if (!trimmed.isNullOrBlank()) add("Authorization", "Bearer $trimmed")
              add("Accept", "application/vnd.github.raw")
            }
            .build()
        )
        .build()
    }
  }
}

/**
 * A figure's `src` (a site-absolute path like `/images/2026/slug.png`)
 * resolved to real bytes, in whichever of two places it might actually live:
 *
 * - Still local, not yet committed — [stagedMedia] carries it, so it loads
 *   straight from disk with no network call at all.
 * - Already in the repo — resolved back to its repo-relative path via
 *   [sitePathToRepoPath] and fetched through the same authenticated raw-content
 *   endpoint [com.rrajath.bloggo.data.github.GitHubClient.getFileContent] already
 *   uses (`Accept: application/vnd.github.raw`), just routed through Coil's own
 *   OkHttp fetcher instead of Retrofit so Coil owns caching and loading state.
 *
 * Falls back to the same gray placeholder box the figure used to always show,
 * on either a load in progress or a genuine failure — never a blank space,
 * and never a visual regression from what was there before this existed.
 */
@Composable
fun RepoAsyncImage(
  sitePath: String,
  connection: RepoConnection,
  token: String?,
  stagedMedia: List<StagedMedia>,
  modifier: Modifier = Modifier,
  contentDescription: String? = null,
  contentScale: ContentScale = ContentScale.Crop,
) {
  val model = rememberRepoImageModel(sitePath, connection, token, stagedMedia)

  SubcomposeAsyncImage(
    model = model,
    contentDescription = contentDescription,
    modifier = modifier,
    contentScale = contentScale,
    loading = { RepoImagePlaceholder(sitePath) },
    error = { RepoImagePlaceholder(sitePath) },
  )
}

@Composable
private fun RepoImagePlaceholder(sitePath: String) {
  val colors = BloggoTheme.colors
  Box(Modifier.fillMaxSize().background(colors.paperSunk), contentAlignment = Alignment.Center) {
    Text(sitePath.substringAfterLast('/'), style = BloggoTheme.type.meta, color = colors.inkFaint)
  }
}

/**
 * A figure at its own aspect ratio, scaled down to fit the article column
 * instead of being cropped into a fixed box — a portrait phone screenshot
 * keeps its shape instead of being center-cropped into a landscape frame.
 *
 * The real ratio isn't known until the image loads, so the box starts at a
 * 16:9 guess and settles into the true ratio (and, for a tall portrait shot,
 * a width narrower than the column) the moment [SubcomposeAsyncImage]
 * reports its intrinsic size. [maxHeight] keeps a very tall portrait shot
 * from taking over the screen.
 */
@Composable
fun RepoFigureImage(
  sitePath: String,
  connection: RepoConnection,
  token: String?,
  stagedMedia: List<StagedMedia>,
  modifier: Modifier = Modifier,
  contentDescription: String? = null,
  maxHeight: Dp = 340.dp,
  onClick: (() -> Unit)? = null,
) {
  val model = rememberRepoImageModel(sitePath, connection, token, stagedMedia)
  var ratio by remember(sitePath) { mutableStateOf(16f / 9f) }

  BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    val naturalHeight = maxWidth / ratio
    val displayHeight = if (naturalHeight <= maxHeight) naturalHeight else maxHeight
    val displayWidth = if (naturalHeight <= maxHeight) maxWidth else maxHeight * ratio

    SubcomposeAsyncImage(
      model = model,
      contentDescription = contentDescription,
      contentScale = ContentScale.Fit,
      modifier = Modifier
        .width(displayWidth)
        .height(displayHeight)
        .clip(BloggoTheme.shapes.medium)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
      loading = { RepoImagePlaceholder(sitePath) },
      error = { RepoImagePlaceholder(sitePath) },
      success = { state ->
        val size = state.painter.intrinsicSize
        if (size.isSpecified && size.height > 0f) {
          LaunchedEffect(sitePath, size.width, size.height) { ratio = size.width / size.height }
        }
        Image(
          painter = state.painter,
          contentDescription = contentDescription,
          contentScale = ContentScale.Fit,
          modifier = Modifier.fillMaxSize(),
        )
      },
    )
  }
}

/**
 * The same image, full screen, uncropped — reached by tapping a figure or a
 * media grid tile. [ContentScale.Fit] against a screen-filling box is what
 * keeps the whole picture visible instead of the zoomed-in crop a fixed-size
 * thumbnail shows.
 */
@Composable
fun RepoImageViewerDialog(
  sitePath: String,
  connection: RepoConnection,
  token: String?,
  stagedMedia: List<StagedMedia>,
  onDismiss: () -> Unit,
  contentDescription: String? = null,
) {
  Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
    Box(
      Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.94f))
        .clickable(
          indication = null,
          interactionSource = remember { MutableInteractionSource() },
          onClick = onDismiss,
        ),
      contentAlignment = Alignment.Center,
    ) {
      RepoAsyncImage(
        sitePath = sitePath,
        connection = connection,
        token = token,
        stagedMedia = stagedMedia,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize().padding(24.dp),
      )
      BloggoIconButton(
        icon = BloggoIcons.Close,
        contentDescription = "Close",
        onClick = onDismiss,
        tint = Color.White,
        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
      )
    }
  }
}
