package com.rrajath.bloggo.ui.media

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rrajath.bloggo.data.RepoConnection
import com.rrajath.bloggo.data.SampleData
import com.rrajath.bloggo.designsystem.BloggoTheme
import com.rrajath.bloggo.designsystem.component.BloggoAppBar
import com.rrajath.bloggo.designsystem.component.BloggoChip
import com.rrajath.bloggo.designsystem.component.BloggoIconButton
import com.rrajath.bloggo.designsystem.component.ChipTone
import com.rrajath.bloggo.designsystem.icon.BloggoIcon
import com.rrajath.bloggo.designsystem.icon.BloggoIcons
import com.rrajath.bloggo.model.MediaFile
import com.rrajath.bloggo.model.StagedMedia
import com.rrajath.bloggo.ui.preview.RepoAsyncImage
import com.rrajath.bloggo.ui.preview.RepoImageViewerDialog

/**
 * What lives in the repo's configured image path — a browser for what's
 * already committed, merged with whatever's been staged this session but not
 * committed yet (tagged "Staged" so the two are never confused). Reached
 * either as the Media tab, or pushed from the editor's Insert -> Image row
 * (`onBack`/`onPick` both non-null then), in which case tapping a file
 * returns it to the post that asked for it instead of just browsing.
 */
@Composable
fun MediaScreen(
  files: List<MediaFile>,
  connection: RepoConnection,
  token: String?,
  stagedMedia: List<StagedMedia>,
  onUpload: (Uri) -> Unit,
  modifier: Modifier = Modifier,
  onBack: (() -> Unit)? = null,
  onPick: ((MediaFile) -> Unit)? = null,
) {
  val colors = BloggoTheme.colors
  var viewingImage by remember { mutableStateOf<MediaFile?>(null) }

  val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    if (uri != null) onUpload(uri)
  }

  Column(modifier.fillMaxSize()) {
    BloggoAppBar(
      title = "Media",
      subtitle = "static/images · ${files.size} files",
      onBack = onBack,
      actions = { BloggoIconButton(BloggoIcons.Search, "Search media", {}) },
    )

    LazyVerticalGrid(
      columns = GridCells.Fixed(3),
      contentPadding = PaddingValues(18.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.weight(1f),
    ) {
      items(files, key = { it.sitePath }) { file ->
        Box(
          Modifier
            .aspectRatio(1f)
            .clip(BloggoTheme.shapes.thumbnail)
            .border(1.dp, colors.ruleSoft, BloggoTheme.shapes.thumbnail)
            .clickable { if (onPick != null) onPick(file) else viewingImage = file },
        ) {
          RepoAsyncImage(
            sitePath = file.sitePath,
            connection = connection,
            token = token,
            stagedMedia = stagedMedia,
            contentDescription = file.name,
            modifier = Modifier.fillMaxSize(),
          )
          if (file.isStaged) {
            BloggoChip(
              "Staged",
              ChipTone.Queued,
              modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
            )
          }
          Text(
            file.name,
            style = BloggoTheme.type.meta.copy(fontSize = BloggoTheme.type.tabLabel.fontSize),
            color = colors.paper,
            maxLines = 1,
            modifier = Modifier
              .align(Alignment.BottomStart)
              .background(colors.ink.copy(alpha = 0.55f))
              .fillMaxWidth()
              .padding(horizontal = 6.dp, vertical = 4.dp),
          )
        }
      }

      item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
        Column(
          Modifier
            .fillMaxWidth()
            .padding(top = 22.dp)
            .clip(BloggoTheme.shapes.medium)
            .border(1.5.dp, colors.rule, BloggoTheme.shapes.medium)
            .clickable(onClick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
            .padding(vertical = 22.dp, horizontal = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
          BloggoIcon(BloggoIcons.Upload, contentDescription = null, size = 24.dp, tint = colors.inkFaint)
          Text("Add from camera or files", style = BloggoTheme.type.cellTitle, color = colors.inkFaint)
          Text(
            "Resized, staged until a post uses it, then committed with it",
            style = BloggoTheme.type.cellSubtitle,
            color = colors.inkFaint,
          )
        }
      }
    }
  }

  viewingImage?.let { file ->
    RepoImageViewerDialog(
      sitePath = file.sitePath,
      connection = connection,
      token = token,
      stagedMedia = stagedMedia,
      contentDescription = file.name,
      onDismiss = { viewingImage = null },
    )
  }
}

@Preview(heightDp = 800)
@Composable
private fun MediaPreview() {
  BloggoTheme {
    MediaScreen(
      files = SampleData.media,
      connection = RepoConnection(),
      token = null,
      stagedMedia = emptyList(),
      onUpload = {},
    )
  }
}
