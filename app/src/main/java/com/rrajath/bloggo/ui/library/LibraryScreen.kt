package com.rrajath.bloggo.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rrajath.bloggo.data.SampleData
import com.rrajath.bloggo.designsystem.BloggoTheme
import com.rrajath.bloggo.designsystem.component.ArtMode
import com.rrajath.bloggo.designsystem.component.Banner
import com.rrajath.bloggo.designsystem.component.BloggoAppBar
import com.rrajath.bloggo.designsystem.component.BloggoChip
import com.rrajath.bloggo.designsystem.component.BloggoIconButton
import com.rrajath.bloggo.designsystem.component.ChipTone
import com.rrajath.bloggo.designsystem.component.Eyebrow
import com.rrajath.bloggo.designsystem.component.HeroCard
import com.rrajath.bloggo.designsystem.component.PostRow
import com.rrajath.bloggo.designsystem.icon.BloggoIcons
import com.rrajath.bloggo.model.Post
import com.rrajath.bloggo.model.PostState
import com.rrajath.bloggo.model.matchingSnippet
import com.rrajath.bloggo.model.searchLibrary
import kotlinx.coroutines.delay

/** How long a keystroke waits before it re-filters the library. Search reads
 * over every cached post's full markdown (see [com.rrajath.bloggo.model.searchLibrary]),
 * not just a title string, so debouncing keeps that scan to once per pause in
 * typing rather than once per character. Shorter than the editor's slug-sync
 * debounce (500ms, EditorScreen.kt): a search box is expected to feel closer
 * to instant than a background sync does. */
private const val SEARCH_DEBOUNCE_MS = 250L

/**
 * The home screen: what you are working on, what is waiting on review, what is
 * already out there. Ordered by how much attention each needs, not by date.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
  drafts: List<Post>,
  inReview: Post?,
  published: List<Post>,
  queuedCommits: Int,
  repoSubtitle: String,
  onOpenDraft: (Post) -> Unit,
  onOpenPost: (Post) -> Unit,
  onOpenLive: (Post) -> Unit,
  onPush: () -> Unit,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
  artMode: ArtMode = ArtMode.Generated,
  isRefreshing: Boolean = false,
) {
  var isSearching by remember { mutableStateOf(false) }
  // The raw, every-keystroke value; [debouncedQuery] is what actually filters
  // the list below, so a fast typist never pays for a re-filter per character.
  var searchQuery by remember { mutableStateOf("") }
  var debouncedQuery by remember { mutableStateOf("") }

  LaunchedEffect(searchQuery) {
    delay(SEARCH_DEBOUNCE_MS)
    debouncedQuery = searchQuery
  }

  fun exitSearch() {
    isSearching = false
    searchQuery = ""
    debouncedQuery = ""
  }

  // Search is local UI state, not a nav destination, so the backstack-driven
  // BackHandler in BloggoApp is disabled while on the root Library tab — this
  // is what makes system Back close the search bar instead of exiting the app.
  BackHandler(enabled = isSearching) { exitSearch() }

  // Full-text search cuts across "in progress" / "open PR" / "published" —
  // those sections describe where a post stands in the pipeline, which a
  // search for its content has no reason to care about — so it runs over
  // every post the screen knows about, flattened into one list.
  val allPosts = remember(drafts, inReview, published) {
    buildList {
      addAll(drafts)
      inReview?.let(::add)
      addAll(published)
    }
  }
  val searchResults = remember(allPosts, debouncedQuery) { allPosts.searchLibrary(debouncedQuery) }
  val isFiltering = isSearching && debouncedQuery.isNotBlank()

  fun openSearchResult(post: Post) {
    if (post.state == PostState.Draft) onOpenDraft(post) else onOpenPost(post)
  }

  Column(modifier.fillMaxSize()) {
    if (isSearching) {
      LibrarySearchBar(
        query = searchQuery,
        onQueryChange = { searchQuery = it },
        onClose = ::exitSearch,
      )
    } else {
      BloggoAppBar(
        title = "Library",
        subtitle = repoSubtitle,
        actions = {
          BloggoIconButton(BloggoIcons.Refresh, "Refresh from GitHub", onRefresh)
          BloggoIconButton(BloggoIcons.Search, "Search", { isSearching = true })
        },
      )
    }

    PullToRefreshBox(
      isRefreshing = isRefreshing,
      onRefresh = onRefresh,
      modifier = Modifier.weight(1f).fillMaxWidth(),
    ) {
      LazyColumn(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
        if (isFiltering) {
          if (searchResults.isEmpty()) {
            item { LibraryNoSearchResults(debouncedQuery) }
          } else {
            item { Eyebrow("${searchResults.size} result${if (searchResults.size == 1) "" else "s"}") }
            items(searchResults, key = { it.slug }) { post ->
              // A hit in the body reads as an actual quote from the post, not
              // just an opaque "this one matched" — falls back to the usual
              // date/word-count meta when the query only hit the title, slug,
              // or a frontmatter field.
              val snippet = post.matchingSnippet(debouncedQuery)
              PostRow(
                title = post.title,
                slug = post.slug,
                onClick = { openSearchResult(post) },
                artMode = artMode,
                chip = { LibrarySearchResultChip(post) },
                meta = snippet ?: buildString {
                  if (post.date != null) append(post.date) else post.editedAgo?.let { append("edited $it") }
                  append(" · %,d words".format(post.wordCount))
                },
                metaHighlightQuery = if (snippet != null) debouncedQuery else null,
                onOpenLive = if (post.state == PostState.Published) ({ onOpenLive(post) }) else null,
              )
            }
          }
        } else {
          if (queuedCommits > 0) {
            item {
              Banner(
                text = "$queuedCommits posts waiting in the offline queue",
                icon = BloggoIcons.Push,
                actionLabel = "Push",
                onAction = onPush,
              )
            }
          }

          if (drafts.isNotEmpty()) {
            val hero = drafts.first()
            item { Eyebrow("In progress") }
            item {
              HeroCard(
                title = hero.title,
                slug = hero.slug,
                meta = buildString {
                  append("%,d words".format(hero.wordCount))
                  hero.editedAgo?.let { append(" · edited $it") }
                },
                onClick = { onOpenDraft(hero) },
                artMode = artMode,
              )
            }

            val rest = drafts.drop(1)
            if (rest.isNotEmpty()) {
              item { Eyebrow("Drafts") }
              items(rest, key = { it.slug }) { post ->
                PostRow(
                  title = post.title,
                  slug = post.slug,
                  onClick = { onOpenDraft(post) },
                  artMode = artMode,
                  chip = { BloggoChip("Draft", ChipTone.Draft) },
                  meta = buildString {
                    append("%,d words".format(post.wordCount))
                    post.editedAgo?.let { append(" · edited $it") }
                  },
                )
              }
            }
          }

          if (inReview != null) {
            item { Eyebrow("Open pull request") }
            item {
              PostRow(
                title = inReview.title,
                slug = inReview.slug,
                onClick = { onOpenPost(inReview) },
                artMode = artMode,
                chip = {
                  BloggoChip("#${inReview.pullRequest}", ChipTone.PullRequest, icon = BloggoIcons.Branch)
                },
                meta = "checks passing",
              )
            }
          }

          item { Eyebrow("Published") }
          items(published, key = { it.slug }) { post ->
            PostRow(
              title = post.title,
              slug = post.slug,
              onClick = { onOpenPost(post) },
              artMode = artMode,
              chip = { BloggoChip("Live", ChipTone.Live) },
              meta = "${post.date} · %,d words".format(post.wordCount),
              onOpenLive = { onOpenLive(post) },
            )
          }
        }

        item { Column(Modifier.padding(bottom = 38.dp)) {} }
      }
    }
  }
}

/**
 * Replaces [BloggoAppBar] while search is active — the standard "search takes
 * over the title" pattern, with a back arrow to leave it and a live "×" to
 * clear the query without leaving. Deliberately its own row rather than a mode
 * grafted onto [BloggoAppBar] itself: that composable is shared by every other
 * screen's header, and none of them need a text field in place of a title.
 */
@Composable
private fun LibrarySearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = BloggoTheme.colors
  val focusRequester = remember { FocusRequester() }
  val keyboardController = LocalSoftwareKeyboardController.current

  // The search bar only composes once the user has already tapped the search
  // icon, so requesting focus (and the keyboard that follows it) the moment
  // this enters composition is what makes that tap open straight into a
  // ready-to-type field instead of just an empty box the writer has to tap
  // again themselves.
  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
    keyboardController?.show()
  }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(colors.paper)
      .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    BloggoIconButton(BloggoIcons.ChevronLeft, "Close search", onClose)
    Box(Modifier.weight(1f)) {
      if (query.isEmpty()) {
        Text("Search posts", style = BloggoTheme.type.body, color = colors.inkFaint)
      }
      BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        textStyle = BloggoTheme.type.body.copy(color = colors.ink),
        cursorBrush = SolidColor(colors.accent),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {}),
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
      )
    }
    if (query.isNotEmpty()) {
      BloggoIconButton(BloggoIcons.Close, "Clear search", { onQueryChange("") })
    }
  }
}

/** The chip a search result row wears, matching whatever [BloggoChip] its
 * own section would have used — search flattens the pipeline sections away,
 * but the state each result is in is still worth a glance. */
@Composable
private fun LibrarySearchResultChip(post: Post) {
  when (post.state) {
    PostState.Draft -> BloggoChip("Draft", ChipTone.Draft)
    PostState.Published -> BloggoChip("Live", ChipTone.Live)
    PostState.InReview -> BloggoChip(
      post.pullRequest?.let { "#$it" } ?: "PR",
      ChipTone.PullRequest,
      icon = BloggoIcons.Branch,
    )
  }
}

/** A query that matched nothing. Plain rather than illustrated — this app has
 * no empty-state artwork anywhere else to be consistent with, so borrowing the
 * eyebrow/meta-text vocabulary already used for every other "here's the
 * state of things" moment in the library beats inventing a new visual
 * language for this one case. */
@Composable
private fun LibraryNoSearchResults(query: String, modifier: Modifier = Modifier) {
  Column(modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Text("No posts found", style = BloggoTheme.type.cellTitle, color = BloggoTheme.colors.ink)
    Text(
      "Nothing matches “$query”",
      style = BloggoTheme.type.meta,
      color = BloggoTheme.colors.inkFaint,
      modifier = Modifier.padding(top = 5.dp),
    )
  }
}

@Preview(heightDp = 860)
@Composable
private fun LibraryPreview() {
  BloggoTheme {
    LibraryScreen(
      drafts = listOf(SampleData.draft),
      inReview = SampleData.inReview,
      published = SampleData.published,
      queuedCommits = 2,
      repoSubtitle = "rrajath/blog · main · hugo",
      onOpenDraft = {},
      onOpenPost = {},
      onOpenLive = {},
      onPush = {},
      onRefresh = {},
    )
  }
}

@Preview(name = "No cover art", heightDp = 860)
@Composable
private fun LibraryNoArtPreview() {
  BloggoTheme {
    LibraryScreen(
      drafts = listOf(SampleData.draft),
      inReview = SampleData.inReview,
      published = SampleData.published,
      queuedCommits = 0,
      repoSubtitle = "rrajath/blog · main · hugo",
      onOpenDraft = {},
      onOpenPost = {},
      onOpenLive = {},
      onPush = {},
      onRefresh = {},
      artMode = ArtMode.None,
    )
  }
}
