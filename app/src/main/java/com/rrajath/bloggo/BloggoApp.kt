package com.rrajath.bloggo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.rrajath.bloggo.data.FrontmatterType
import com.rrajath.bloggo.data.PublishAction
import com.rrajath.bloggo.data.RepoConnection
import com.rrajath.bloggo.data.RepoConnectionRepository
import com.rrajath.bloggo.data.SampleData
import com.rrajath.bloggo.data.SettingsBackup
import com.rrajath.bloggo.data.github.CommitResult
import com.rrajath.bloggo.data.github.ConnectionCheck
import com.rrajath.bloggo.data.github.GitHubApiError
import com.rrajath.bloggo.data.github.GitHubClient
import com.rrajath.bloggo.data.github.describe
import com.rrajath.bloggo.data.SettingsRepository
import com.rrajath.bloggo.data.resolvePagePath
import com.rrajath.bloggo.data.resolvePostPath
import com.rrajath.bloggo.data.ThemeMode
import com.rrajath.bloggo.data.inbox.FragmentStore
import com.rrajath.bloggo.data.library.BloggoDatabase
import com.rrajath.bloggo.data.library.LibraryRefreshResult
import com.rrajath.bloggo.data.library.LocalPostStore
import com.rrajath.bloggo.data.library.PageLibraryRefreshResult
import com.rrajath.bloggo.data.library.PageLibraryRepository
import com.rrajath.bloggo.data.library.PostLibraryRepository
import com.rrajath.bloggo.data.media.MediaRefreshResult
import com.rrajath.bloggo.data.media.MediaRepository
import com.rrajath.bloggo.data.media.clearStagedMediaCache
import com.rrajath.bloggo.data.media.stageFromUri
import com.rrajath.bloggo.data.publish.PostPublishRepository
import com.rrajath.bloggo.data.publish.PublishResult
import com.rrajath.bloggo.data.review.ReadabilityIgnoreStore
import com.rrajath.bloggo.designsystem.BloggoTheme
import com.rrajath.bloggo.designsystem.component.BloggoTab
import com.rrajath.bloggo.designsystem.component.BloggoTabBar
import com.rrajath.bloggo.designsystem.component.BloggoToast
import com.rrajath.bloggo.designsystem.icon.BloggoIcons
import com.rrajath.bloggo.designsystem.paperGrain
import com.rrajath.bloggo.model.DocKind
import com.rrajath.bloggo.model.Fragment
import com.rrajath.bloggo.model.Post
import com.rrajath.bloggo.model.MediaFile
import com.rrajath.bloggo.model.PostState
import com.rrajath.bloggo.model.StagedMedia
import com.rrajath.bloggo.model.currentFrontmatterTimestamp
import com.rrajath.bloggo.model.effectiveDate
import com.rrajath.bloggo.model.frontmatterBlock
import com.rrajath.bloggo.model.frontmatterTimestampFromMillis
import com.rrajath.bloggo.model.isPushed
import com.rrajath.bloggo.model.lastEditedAtMillis
import com.rrajath.bloggo.model.markdownWordCount
import com.rrajath.bloggo.model.parseFrontmatter
import com.rrajath.bloggo.model.parseFrontmatterDateEpochMillis
import com.rrajath.bloggo.model.parseTagList
import com.rrajath.bloggo.model.slugify
import com.rrajath.bloggo.model.stagedImagePath
import com.rrajath.bloggo.model.withUpdatedDate
import com.rrajath.bloggo.model.withUpdatedDraft
import com.rrajath.bloggo.ui.editor.EditorScreen
import com.rrajath.bloggo.ui.focus.FocusScreen
import com.rrajath.bloggo.ui.inbox.InboxScreen
import com.rrajath.bloggo.ui.library.LibraryScreen
import com.rrajath.bloggo.ui.mastodon.MastodonScreen
import com.rrajath.bloggo.ui.media.MediaScreen
import com.rrajath.bloggo.ui.pages.PagesScreen
import com.rrajath.bloggo.ui.preview.PreviewScreen
import com.rrajath.bloggo.ui.review.ReadabilityCheck
import com.rrajath.bloggo.ui.settings.SettingsAppearanceScreen
import com.rrajath.bloggo.ui.settings.SettingsConnectionScreen
import com.rrajath.bloggo.ui.settings.SettingsImportExportScreen
import com.rrajath.bloggo.ui.settings.SettingsPage
import com.rrajath.bloggo.ui.settings.SettingsPublishingScreen
import com.rrajath.bloggo.ui.settings.SettingsReadabilityScreen
import com.rrajath.bloggo.ui.settings.SettingsRepoScreen
import com.rrajath.bloggo.ui.settings.SettingsScreen
import com.rrajath.bloggo.ui.review.ReviewScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** Every destination in the app. Tab destinations keep the bottom bar visible. */
sealed interface Route {
  data object Library : Route
  data object Inbox : Route
  data object Pages : Route

  /** Reached only from the editor's Insert -> Image row now that Pages has
   * taken Media's place in the tab bar: picking a file there returns it to
   * that post instead of just browsing, and this visit is a normal
   * back-stack push rather than a tab switch. [returnToEditorSlug] is never
   * null in practice any more, but stays optional rather than becoming a
   * required param nothing else in the type needs to change for. */
  data class Media(val returnToEditorSlug: String? = null) : Route

  /** The Settings tab: a menu of the sub-pages below. */
  data object Settings : Route

  /** One Settings sub-page. Not a tab: renders with a back chevron and no tab
   * bar, the same way [Editor] and [Review] do. Back returns to the menu. */
  data class SettingsDetail(val page: SettingsPage) : Route

  data class Editor(val slug: String) : Route
  data class Preview(val slug: String, val published: Boolean) : Route
  data class Focus(val slug: String) : Route

  /** Read-only Hemingway-style pass over a draft, reached from the editor
   * toolbar. Back returns to the editor. */
  data class Review(val slug: String) : Route

  /** No longer reachable from anywhere in the app — Preview's Share button
   * opens the native share sheet instead (see `onShare` below). Left in
   * place rather than deleted; PROGRESS.md tracks this as an open item. */
  data object Mastodon : Route

  val isTab: Boolean
    get() = this is Library || this is Inbox || this is Pages || this is Settings
}

private val TABS = listOf(
  BloggoTab(BloggoIcons.Library, "Library", "library"),
  BloggoTab(BloggoIcons.Inbox, "Inbox", "inbox"),
  BloggoTab(BloggoIcons.File, "Pages", "pages"),
  BloggoTab(BloggoIcons.Settings, "Settings", "settings"),
)

/** Builds a new post's frontmatter from the writer's own configured field list
 * (Settings screen), not a fixed shape — a field this app recognizes gets a real
 * default; anything else gets an empty line for the writer to fill in. The fence
 * style ([type]) follows the Repo Settings choice: YAML `---`/`key: value` or
 * TOML `+++`/`key = value`, with strings quoted and the timestamp left bare in
 * TOML, matching what `Model.kt`'s own frontmatter helpers already write. */
private fun frontmatterFor(
  title: String,
  slug: String,
  fields: List<String>,
  type: FrontmatterType,
  date: String = currentFrontmatterTimestamp(),
): String {
  val safeTitle = title.replace("\"", "'")
  val toml = type == FrontmatterType.Toml
  val fence = if (toml) "+++" else "---"
  val lines = fields.ifEmpty { listOf("title") }.joinToString("\n") { field ->
    when (field.trim().lowercase()) {
      "title" -> if (toml) "title = \"$safeTitle\"" else "title: $safeTitle"
      "date" -> if (toml) "date = $date" else "date: $date"
      "slug" -> if (toml) "slug = \"$slug\"" else "slug: $slug"
      "tags" -> if (toml) "tags = []" else "tags: []"
      "draft" -> if (toml) "draft = true" else "draft: true"
      else -> if (toml) "${field.trim()} = \"\"" else "${field.trim()}: "
    }
  }
  return "$fence\n$lines\n$fence\n\n"
}

/** A genuinely empty post: what "new post" must open, never a stale sample. */
private fun newBlankPost(fields: List<String>, type: FrontmatterType): Post {
  val slug = "untitled-${System.currentTimeMillis()}"
  val markdown = frontmatterFor(title = "Untitled", slug = "untitled", fields = fields, type = type)
  return Post(
    slug = slug,
    title = "Untitled",
    state = PostState.Draft,
    markdown = markdown,
    wordCount = markdown.markdownWordCount(),
    editedAgo = "just now",
    updatedAt = System.currentTimeMillis(),
  )
}

/** A page's frontmatter fields are fixed — title, slug, date — regardless of the
 * writer's configured post field list: a page like About isn't tagged or
 * drafted, so [RepoConnection.frontmatterFields] (built for posts) doesn't
 * apply to it. The fence style still follows the Repo Settings [FrontmatterType]
 * choice, so a page and a post created on the same site match. */
private fun frontmatterForPage(title: String, slug: String, type: FrontmatterType): String {
  val safeTitle = title.replace("\"", "'")
  val date = currentFrontmatterTimestamp()
  return if (type == FrontmatterType.Toml) {
    "+++\ntitle = \"$safeTitle\"\nslug = \"$slug\"\ndate = $date\n+++\n\n"
  } else {
    "---\ntitle: $safeTitle\nslug: $slug\ndate: $date\n---\n\n"
  }
}

/** A genuinely empty page, the Pages tab's counterpart to [newBlankPost]. */
private fun newBlankPage(type: FrontmatterType): Post {
  val slug = "untitled-page-${System.currentTimeMillis()}"
  val markdown = frontmatterForPage(title = "Untitled page", slug = "untitled-page", type = type)
  return Post(
    slug = slug,
    title = "Untitled page",
    state = PostState.Draft,
    kind = DocKind.Page,
    markdown = markdown,
    wordCount = markdown.markdownWordCount(),
    editedAgo = "just now",
    updatedAt = System.currentTimeMillis(),
  )
}

/** The Photo Picker hands back only a `content://` Uri, no filename — this is
 * the one query that gets one back, for `stagedImagePath`'s extension-sniffing
 * and the "original filename" the Media screen shows. Falls back to the Uri's
 * own last path segment on any failure; that's rarely a real filename, but
 * `stagedImagePath` only actually needs a plausible extension from it. */
private fun resolveDisplayName(context: Context, uri: Uri): String {
  val fallback = uri.lastPathSegment ?: "photo.jpg"
  return runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
      if (cursor.moveToFirst()) cursor.getString(0) else null
    }
  }.getOrNull() ?: fallback
}

/** "Promote to post": a fragment becomes the seed of a real, editable draft. */
private fun newPostFromFragment(fragment: Fragment, fields: List<String>, type: FrontmatterType): Post {
  // Only the capture's first line, never more — a multi-line fragment used to
  // hand its first *sentence* to `title:`, which for a fragment with no
  // sentence-ending punctuation for a while meant several raw lines landing
  // in a single-line frontmatter field. That broke `parseFrontmatter` (which
  // reads only up to the first newline of a `key: value` line) for every
  // reader downstream — the title shown in the Library, and the slug-sync
  // baseline in PostDetailsSheet/EditorScreen, which is why editing the title
  // afterward could get permanently stuck unable to update the slug.
  val title = fragment.text.lineSequence().firstOrNull { it.isNotBlank() }?.trim()?.take(80) ?: "Untitled"
  val titleSlug = slugify(title)
  val slug = "$titleSlug-${System.currentTimeMillis()}"
  // The moment the thought was actually captured, not whenever it happens to be
  // opened or promoted — see Fragment.capturedAtMillis's own doc comment.
  val date = frontmatterTimestampFromMillis(fragment.capturedAtMillis)
  val markdown =
    frontmatterFor(title = title, slug = titleSlug, fields = fields, type = type, date = date) + "${fragment.text}\n"
  return Post(
    slug = slug,
    title = title,
    state = PostState.Draft,
    markdown = markdown,
    wordCount = markdown.markdownWordCount(),
    editedAgo = "just now",
    updatedAt = System.currentTimeMillis(),
  )
}

/** "Move to Inbox": the reverse of [newPostFromFragment] — an unpushed draft's
 * body becomes a fresh fragment, the same shape [BloggoApp]'s own `onCapture`
 * already builds one in ("captured-" id, the default "Today" bucket). Falls
 * back to the post's title on a body-only-frontmatter draft, so a fragment
 * demoted straight back to Inbox is never blank. */
private fun newFragmentFromPost(post: Post): Fragment {
  val body = post.markdown.removePrefix(post.markdown.frontmatterBlock()).trim()
  // The post's own frontmatter date, never "now": if this fragment is later
  // promoted back to a post (newPostFromFragment), it must reproduce the date
  // the writer actually set, not whenever they happened to tap Move to Inbox.
  // Only a post with no parseable date at all falls back to the move instant.
  val originalDateMillis = post.markdown.parseFrontmatter().effectiveDate()
    ?.let(::parseFrontmatterDateEpochMillis)
  return Fragment(
    id = "captured-${System.currentTimeMillis()}",
    text = body.ifBlank { post.title },
    capturedAtMillis = originalDateMillis ?: System.currentTimeMillis(),
  )
}

/**
 * The app shell.
 *
 * Navigation is a plain back stack rather than a library. The graph is small and
 * entirely internal, and keeping it here means the screens stay parameterised by
 * data and callbacks, which is what makes them previewable.
 */
@Composable
fun BloggoApp(launchIntent: Intent? = null) {
  val context = LocalContext.current
  val settingsRepository = remember { SettingsRepository(context) }
  val repoConnectionRepository = remember { RepoConnectionRepository(context) }
  val gitHubClient = remember { GitHubClient() }
  val postLibraryRepository = remember {
    PostLibraryRepository(gitHubClient, BloggoDatabase.get(context).postCacheDao())
  }
  val pageLibraryRepository = remember {
    PageLibraryRepository(gitHubClient, BloggoDatabase.get(context).pageCacheDao())
  }
  // Durable storage for a post/page that only exists on this device — see its
  // own doc comment for why this is entirely out of band from the two
  // repositories above (remote sync must never be able to touch a local-only
  // row) and for the "never delete except on an explicit user action"
  // invariant this whole class exists to uphold.
  val localPostStore = remember { LocalPostStore(context, BloggoDatabase.get(context).localPostDao()) }
  // Inbox's own counterpart to localPostStore, durable storage for a
  // captured-but-unpromoted fragment — see its own doc comment
  // (data/inbox/FragmentStore.kt) for why a fragment needs no mirrored file
  // the way a local post does.
  val fragmentStore = remember { FragmentStore(BloggoDatabase.get(context).fragmentDao()) }
  // Per-post store of readability findings the writer has ignored on the review
  // screen; cleared for a post only by that screen's recompute action.
  val readabilityIgnoreStore =
    remember { ReadabilityIgnoreStore(BloggoDatabase.get(context).readabilityIgnoreDao()) }
  val postPublishRepository = remember { PostPublishRepository(gitHubClient) }
  val mediaRepository = remember { MediaRepository(gitHubClient) }
  val scope = rememberCoroutineScope()
  val themeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.System)
  val readabilityChecks by settingsRepository.readabilityChecks
    .collectAsState(initial = ReadabilityCheck.All)
  val repoConnection by repoConnectionRepository.connection.collectAsState(initial = RepoConnection())
  val systemDarkTheme = isSystemInDarkTheme()
  val darkTheme = when (themeMode) {
    ThemeMode.System -> systemDarkTheme
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
  }

  fun openUrl(url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
  }

  // enableEdgeToEdge() in MainActivity picks a one-time default for the status
  // bar icons that only follows the *system* theme; it knows nothing about the
  // in-app Auto/Light/Dark override, so dark mode was left with dark-on-dark
  // status bar icons. Kept in step with the theme actually in effect here.
  val view = LocalView.current
  SideEffect {
    val window = (view.context as Activity).window
    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
  }

  BloggoTheme(darkTheme = darkTheme) {
    // The "Capture a thought" shortcut seeds the back stack directly to
    // Inbox rather than starting at Library and hopping over via a
    // LaunchedEffect — that would still work, but would flash Library for
    // one frame first. A plain launcher tap delivers ACTION_MAIN here, so
    // the default (Library) is untouched.
    val backStack = remember {
      mutableStateListOf<Route>(
        if (launchIntent?.action == ACTION_CAPTURE_THOUGHT) Route.Inbox else Route.Library
      )
    }
    var toast by remember { mutableStateOf<String?>(null) }
    // One-shot: true only for the launch/resume that actually came from the
    // shortcut, consumed by InboxScreen the moment it requests focus. Normal
    // tab navigation to Inbox never sets this, so it never steals focus or
    // pops the keyboard outside the shortcut's own flow.
    var requestInboxFocus by remember {
      mutableStateOf(launchIntent?.action == ACTION_CAPTURE_THOUGHT)
    }

    // Every post and page the app knows about: the draft, the one in review,
    // the published posts, and the top-level pages — one list, so Editor and
    // Preview can always look either up by slug and land on its own content,
    // never a stale stand-in. Post.kind is what tells them apart; the Library
    // and Pages screens each filter to the one they show.
    //
    // Starts empty rather than seeded with SampleData: this device's cached
    // library (Room, read below) is checked first, and SampleData is only
    // added if that cache turns out to be genuinely empty. Seeding
    // synchronously here used to mean every cold start — including right
    // after an update, when the cache is already full of real posts — briefly
    // rendered the sample placeholders before the async cache read replaced
    // them a frame or two later.
    val posts = remember { mutableStateListOf<Post>() }
    // No SampleData.fragments seeding: a fresh install's Inbox is genuinely
    // empty until FragmentStore.loadAll() (below) restores whatever survived
    // the last process death, and real captures add to it from there.
    val fragments = remember { mutableStateListOf<Fragment>() }
    // The fragment currently open in the Editor as a preview, and the
    // in-memory Post built from it — set by Route.Inbox's onOpen, cleared on
    // promotion or on leaving the Editor. Deliberately NOT added to `posts`
    // or `LocalPostStore` until "Promote to Post" is tapped: opening a
    // fragment to look at it, then backing out, must leave it exactly where
    // it was in `fragments`/FragmentStore, never silently create a draft.
    var transientFragmentSource by remember { mutableStateOf<Fragment?>(null) }
    var transientFragmentPost by remember { mutableStateOf<Post?>(null) }
    var connectionCheck by remember { mutableStateOf<ConnectionCheck?>(null) }
    // Read once per connection change, off the main thread. Evaluating
    // repoConnectionRepository.getToken() inline as a RepoScreen argument put a
    // Keystore unwrap and an encrypted-preferences decrypt on every
    // recomposition of this composable while the Settings tab was open.
    var storedToken by remember { mutableStateOf<String?>(null) }
    var isCheckingConnection by remember { mutableStateOf(false) }
    // Slugs from the sample seed that a real connection is allowed to clear out
    // once the library/pages have real data to show instead.
    val sampleSlugs = remember { (SampleData.published.map { it.slug } + SampleData.draft.slug).toSet() }
    val samplePageSlugs = remember { SampleData.pages.map { it.slug }.toSet() }
    var remotePostSlugs by remember { mutableStateOf<Set<String>>(emptySet()) }
    var remotePageSlugs by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Images picked on-device but not yet committed, in-memory only — lost on
    // process death by design, the same durability boundary a post's own
    // in-progress edits sit behind today (PROGRESS.md's still-open "Editor
    // autosave to Room"). A post claims entries via StagedMedia.claimedByPostSlug
    // once Insert -> Image stages one against it; publish drops the claimed
    // entries once they're actually committed.
    val stagedMedia = remember { mutableStateListOf<StagedMedia>() }
    var isPublishing by remember { mutableStateOf(false) }
    var publishResult by remember { mutableStateOf<PublishResult?>(null) }
    // Set once when a pick/upload returns from Route.Media's picker mode;
    // EditorScreen consumes it via a LaunchedEffect and reports back through
    // onPendingInsertConsumed the same one-shot way requestInboxFocus already
    // works for the "Capture a thought" shortcut.
    var pendingInsertImage by remember { mutableStateOf<MediaFile?>(null) }
    var mediaFiles by remember { mutableStateOf(SampleData.media) }

    // Staged bytes are process-lifetime only (StagedMedia's own doc comment) —
    // any left over from a previous process are already orphaned, so the cache
    // dir is always fully cleared once, at cold start, before anything stages
    // into it.
    LaunchedEffect(Unit) { clearStagedMediaCache(context) }

    // Falls back to transientFragmentPost when slug isn't in `posts` — the
    // one case that's true for is a fragment currently being previewed in
    // the Editor (Route.Inbox's onOpen, below), which is deliberately never
    // added to `posts` until it's promoted. Every Editor/Preview/Focus route
    // already looks posts up this same way, so all three keep working for a
    // fragment preview without a separate transient-only code path in each.
    fun postBySlug(slug: String): Post =
      posts.firstOrNull { it.slug == slug }
        ?: transientFragmentPost?.takeIf { it.slug == slug }
        ?: error("No post or page for slug $slug")

    // Persists [post] to LocalPostStore iff it hasn't reached the repo yet
    // (Post.isPushed) — the moment a post/page has, the remote caches
    // (post_cache/page_cache) are the durable copy of record for it, and this
    // store simply stops being written to for that slug (see LocalPostStore's
    // own doc comment for why it's never proactively cleaned up here either:
    // that would be exactly the kind of implicit, sync-adjacent delete the
    // "always user-initiated" requirement rules out).
    fun persistIfLocal(post: Post) {
      val remoteSlugs = if (post.kind == DocKind.Page) remotePageSlugs else remotePostSlugs
      if (!post.isPushed(remoteSlugs)) {
        scope.launch { localPostStore.save(post) }
      }
    }

    fun updatePost(slug: String, transform: (Post) -> Post) {
      val index = posts.indexOfFirst { it.slug == slug }
      if (index >= 0) {
        val updated = transform(posts[index])
        posts[index] = updated
        persistIfLocal(updated)
      } else if (transientFragmentPost?.slug == slug) {
        // A fragment preview's edits stay purely in-memory until promotion —
        // never persistIfLocal'd here, or "open a fragment, then just back
        // out" would silently write a LocalPostStore row for a "post" that
        // was never actually created.
        transientFragmentPost = transform(transientFragmentPost!!)
      }
    }

    // Restores whatever local-only work survived the last process death —
    // the fix for the data-loss bug this whole store exists for. Merges by
    // slug the same way refreshLibrary/refreshPages do, and only ever adds or
    // replaces an entry, never removes one: a sample or remote post already
    // occupying this slug is left alone rather than clobbered by a stale
    // local copy of the same slug (shouldn't happen in practice, since local
    // slugs are generated with a timestamp suffix, but this is the same
    // "never delete/overwrite what isn't clearly ours to replace" posture as
    // everywhere else this store is touched).
    LaunchedEffect(Unit) {
      for (local in localPostStore.loadAll()) {
        val index = posts.indexOfFirst { it.slug == local.slug }
        if (index < 0) posts.add(local)
      }
    }

    // FragmentStore's counterpart to the restore above — every captured
    // thought that survived the last process death, merged in the same
    // add-or-replace-never-remove way.
    LaunchedEffect(Unit) {
      for (saved in fragmentStore.loadAll()) {
        val index = fragments.indexOfFirst { it.id == saved.id }
        if (index < 0) fragments.add(saved)
      }
    }

    // `posts` starts empty above; this is what fills it in, from this
    // device's own last-synced library (Room, local and near-instant) rather
    // than from the network — refreshLibrary/refreshPages' tree-plus-per-file
    // round trip is far too slow to gate the very first frame on. SampleData
    // is the fallback, used only when both caches come back empty, i.e. this
    // device has genuinely never synced before: a fresh install, not a
    // restart or an update.
    LaunchedEffect(Unit) {
      val cachedPosts = postLibraryRepository.loadCached()
      val cachedPages = pageLibraryRepository.loadCached()
      if (cachedPosts.isEmpty() && cachedPages.isEmpty()) {
        posts.add(SampleData.draft)
        posts.add(SampleData.inReview)
        posts.addAll(SampleData.published)
        posts.addAll(SampleData.pages)
      } else {
        for (post in cachedPosts) {
          val index = posts.indexOfFirst { it.slug == post.slug }
          if (index >= 0) posts[index] = post else posts.add(post)
        }
        for (page in cachedPages) {
          val index = posts.indexOfFirst { it.slug == page.slug }
          if (index >= 0) posts[index] = page else posts.add(page)
        }
      }
    }

    // The hero card shows whichever draft was actually touched last, not a
    // separately tracked "current" draft — so promoting a fragment, opening an
    // old draft from the list below, or just typing all move the same post to
    // the top next time Library is shown.
    //
    // Deliberately NOT computed here as a top-level val: `posts` is a SnapshotStateList,
    // and every editor keystroke mutates one entry in it (onMarkdownChange below), which
    // invalidates this whole composable's scope. A plain val here would re-filter and
    // re-sort the *entire* post list on every single keystroke even while Route.Editor is
    // the only thing on screen — the same "recomputed in full on every keystroke" shape
    // Milestone 10 (PROGRESS.md) already fixed once for `repoConfig`. Computed instead,
    // below, only inside the Library/Settings branches that actually read it.

    // GitHub Pages is the only host discoverable from the repo itself; most Hugo
    // sites deploy elsewhere, so the real value comes from what was typed into
    // the Settings screen, falling back to the sample host only when nothing has
    // been entered yet.
    val siteHost = repoConnection.siteHost.ifBlank { SampleData.sampleSiteHost }

    val defaultFrontmatterFields = repoConnection.frontmatterFields
      .split(",")
      .map { it.trim() }
      .filter { it.isNotEmpty() }
    val frontmatterType = repoConnection.frontmatterType

    // Never leave the library with nothing to compose into — a delete or a
    // refresh that drops every draft must not leave "In progress" empty.
    // Scoped to posts: a page being deleted or refreshed away has no such
    // invariant to preserve.
    fun ensureDraftExists() {
      if (posts.none { it.state == PostState.Draft && it.kind == DocKind.Post }) {
        val blank = newBlankPost(defaultFrontmatterFields, frontmatterType)
        posts.add(blank)
        persistIfLocal(blank)
      }
    }

    val current = backStack.last()

    // replace = true swaps the top of the stack instead of pushing: Editor and
    // Preview are two modes of looking at the same post, not two steps deeper
    // into the app, so toggling between them must not grow the back stack.
    // Without this, repeatedly hitting Read/Edit left a long alternating trail
    // that Back had to walk through one screen at a time before it reached
    // Library.
    fun go(route: Route, replace: Boolean = false) {
      if (route.isTab) {
        backStack.clear()
        backStack.add(route)
      } else {
        if (replace && backStack.size > 1) backStack.removeAt(backStack.lastIndex)
        backStack.add(route)
      }
    }

    // Both the hardware/gesture back action (BackHandler, below) and every
    // screen's in-app "‹" button/onExit funnel through this one function
    // (EditorScreen's onBack and FocusScreen's onExit are both literally
    // `::back`), which is what makes it the single choke point to clear
    // transientFragmentPost/transientFragmentSource from, rather than a
    // per-screen effect keyed on composition presence: Editor <-> Preview
    // and Editor <-> Focus both swap which route is on top via `go(...)`
    // (a plain push for Focus, a replace for Preview) without ever calling
    // this function, so a composable-disposal-based cleanup would fire on
    // every one of those toggles too — including the ones that must leave a
    // fragment preview's transient state intact. Only clear once popping
    // actually leaves every view of that same slug (Editor, Preview, Focus)
    // behind entirely.
    fun back() {
      if (backStack.size > 1) {
        backStack.removeAt(backStack.lastIndex)
        val previewingSlug = transientFragmentPost?.slug
        if (previewingSlug != null) {
          val stillViewingPreview = when (val top = backStack.last()) {
            is Route.Editor -> top.slug == previewingSlug
            is Route.Preview -> top.slug == previewingSlug
            is Route.Focus -> top.slug == previewingSlug
            is Route.Review -> top.slug == previewingSlug
            else -> false
          }
          if (!stillViewingPreview) {
            // Backing out of a fragment preview must not promote it (see
            // transientFragmentPost's own doc comment) — but it must also not
            // throw away edits the writer just made to it. Without this, a
            // capture opened from the Inbox, edited, then closed without
            // "Promote to Post" silently reverted to whatever it was before
            // the edit: transientFragmentPost was discarded outright, never
            // written back to `fragments`/FragmentStore, which stayed on the
            // untouched, pre-edit source the whole time.
            val source = transientFragmentSource
            val edited = transientFragmentPost
            if (source != null && edited != null) {
              val editedText = edited.markdown.removePrefix(edited.markdown.frontmatterBlock()).trim()
              if (editedText.isNotBlank() && editedText != source.text) {
                val updated = source.copy(text = editedText)
                val index = fragments.indexOfFirst { it.id == source.id }
                if (index >= 0) fragments[index] = updated
                scope.launch { fragmentStore.save(updated) }
              }
            }
            transientFragmentSource = null
            transientFragmentPost = null
          }
        }
      }
    }

    // Without this, the hardware/gesture back action was never wired to the
    // app's own navigation at all — every screen's in-app "<" button called
    // back() correctly, but the system back gesture fell straight through to
    // finishing the Activity regardless of how deep the stack was.
    BackHandler(enabled = backStack.size > 1) { back() }

    // Covers the warm-resume case the backStack's initial value above
    // doesn't: the app already has a task running, the shortcut is tapped
    // again, and onNewIntent delivers a *new* Intent instance (singleTask
    // launch mode in the manifest is what makes that delivery happen at
    // all rather than the tap just bringing the existing screen forward
    // untouched).
    LaunchedEffect(launchIntent) {
      if (launchIntent?.action == ACTION_CAPTURE_THOUGHT) {
        requestInboxFocus = true
        go(Route.Inbox)
      }
    }

    LaunchedEffect(toast) {
      if (toast != null) {
        delay(2200)
        toast = null
      }
    }

    var isRefreshingLibrary by remember { mutableStateOf(false) }

    // ANDROID_TDD.md §5.2: refresh the library from the git trees API. Runs
    // whenever a usable connection appears (on launch, or right after Save on
    // the Settings screen) and on demand from the Library screen's refresh button
    // or pull-to-refresh — never on a timer, per §5.4.
    suspend fun refreshLibrary(explicit: Boolean = false) {
      if (repoConnection.repository.isBlank() || !repoConnection.hasToken) {
        if (explicit) toast = "Connect a repository and token on the Settings screen first"
        return
      }
      isRefreshingLibrary = true
      when (
        val result = postLibraryRepository.refresh(
          repository = repoConnection.repository,
          branch = repoConnection.branch,
          token = repoConnectionRepository.getToken(),
        )
      ) {
        is LibraryRefreshResult.Success -> {
          val freshSlugs = result.posts.map { it.slug }.toSet()
          posts.removeAll {
            it.kind == DocKind.Post && (it.slug in sampleSlugs || it.slug in remotePostSlugs) && it.slug !in freshSlugs
          }
          for (post in result.posts) {
            val index = posts.indexOfFirst { it.slug == post.slug }
            if (index >= 0) posts[index] = post else posts.add(post)
          }
          remotePostSlugs = freshSlugs
          ensureDraftExists()
        }
        is LibraryRefreshResult.Failed -> toast = "Couldn't refresh the library: ${result.error.describe()}"
      }
      isRefreshingLibrary = false
    }

    LaunchedEffect(repoConnection.repository, repoConnection.branch, repoConnection.hasToken) {
      refreshLibrary()
    }

    var isRefreshingPages by remember { mutableStateOf(false) }

    // The Pages tab's counterpart to refreshLibrary — same trigger, same
    // git-trees-plus-cache shape, over PageLibraryRepository instead.
    suspend fun refreshPages(explicit: Boolean = false) {
      if (repoConnection.repository.isBlank() || !repoConnection.hasToken) {
        if (explicit) toast = "Connect a repository and token on the Settings screen first"
        return
      }
      isRefreshingPages = true
      when (
        val result = pageLibraryRepository.refresh(
          repository = repoConnection.repository,
          branch = repoConnection.branch,
          token = repoConnectionRepository.getToken(),
        )
      ) {
        is PageLibraryRefreshResult.Success -> {
          val freshSlugs = result.pages.map { it.slug }.toSet()
          posts.removeAll {
            it.kind == DocKind.Page && (it.slug in samplePageSlugs || it.slug in remotePageSlugs) && it.slug !in freshSlugs
          }
          for (page in result.pages) {
            val index = posts.indexOfFirst { it.slug == page.slug }
            if (index >= 0) posts[index] = page else posts.add(page)
          }
          remotePageSlugs = freshSlugs
        }
        is PageLibraryRefreshResult.Failed -> toast = "Couldn't refresh pages: ${result.error.describe()}"
      }
      isRefreshingPages = false
    }

    LaunchedEffect(repoConnection.repository, repoConnection.branch, repoConnection.hasToken) {
      refreshPages()
    }

    // SampleData.inReview has no real fetch to replace it — PR listing
    // (ANDROID_TDD.md §5.1) isn't wired up yet, so it can't be cleared the
    // way refreshLibrary/refreshPages clear the rest of the sample seed by
    // slug. It exists only to keep the "Open pull request" section from
    // looking broken before a repo is connected, so once a real connection
    // exists it must go — a fake open PR next to a real, connected repo is
    // actively misleading, not illustrative.
    LaunchedEffect(repoConnection.repository, repoConnection.hasToken) {
      if (repoConnection.repository.isNotBlank() && repoConnection.hasToken) {
        posts.removeAll { it.slug == SampleData.inReview.slug }
      }
    }

    // The Media screen's counterpart to refreshLibrary — same trigger, same
    // "usable connection appears" condition. A failure here isn't surfaced as
    // its own toast: it's the same connectivity problem refreshLibrary above
    // already reports at the same moment, and doubling that message up would
    // just be noise.
    suspend fun refreshMedia() {
      if (repoConnection.repository.isBlank() || !repoConnection.hasToken) return
      when (
        val result = mediaRepository.refresh(
          repository = repoConnection.repository,
          branch = repoConnection.branch,
          imagePath = repoConnection.imagePath,
          token = repoConnectionRepository.getToken(),
        )
      ) {
        is MediaRefreshResult.Success -> mediaFiles = result.files
        is MediaRefreshResult.Failed -> Unit
      }
    }

    LaunchedEffect(repoConnection.repository, repoConnection.branch, repoConnection.hasToken) {
      refreshMedia()
    }

    LaunchedEffect(repoConnection.hasToken) {
      storedToken = if (repoConnection.hasToken) repoConnectionRepository.getToken() else null
    }

    // BuildConfig is disabled project-wide, so the version comes from the
    // installed package. The `debug` build type's versionNameSuffix means a
    // debug build already reports "1.1.0 (debug)" here.
    val appVersion = remember {
      runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
      }.getOrNull().orEmpty()
    }

    // Settings import/export (Settings -> Import / Export). Pretty-printed so a
    // backup file is readable; encodeDefaults so `version` is always written;
    // ignoreUnknownKeys so a newer file still imports on an older build.
    val settingsJson = remember { Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true } }

    fun currentSettingsBackup(): SettingsBackup = SettingsBackup(
      repository = repoConnection.repository,
      branch = repoConnection.branch,
      siteUrl = repoConnection.siteUrl,
      authorName = repoConnection.authorName,
      postPath = repoConnection.postPath,
      imagePath = repoConnection.imagePath,
      frontmatterFields = repoConnection.frontmatterFields,
      frontmatterType = repoConnection.frontmatterType.name,
      publishAction = repoConnection.publishAction.name,
      themeMode = themeMode.name,
      readabilityChecks = readabilityChecks.map { it.name },
    )

    // Applies every key present in [backup]; an absent key (null) is left
    // exactly as it was, and the PAT is never touched. No connection re-check.
    suspend fun applySettingsBackup(backup: SettingsBackup) {
      if (backup.repository != null || backup.branch != null || backup.siteUrl != null || backup.authorName != null) {
        repoConnectionRepository.setRepo(
          repository = backup.repository ?: repoConnection.repository,
          branch = backup.branch ?: repoConnection.branch,
          siteUrl = backup.siteUrl ?: repoConnection.siteUrl,
          authorName = backup.authorName ?: repoConnection.authorName,
        )
      }
      backup.postPath?.let { repoConnectionRepository.setPostPath(it) }
      backup.imagePath?.let { repoConnectionRepository.setImagePath(it) }
      backup.frontmatterFields?.let { repoConnectionRepository.setFrontmatterFields(it) }
      backup.frontmatterType
        ?.let { raw -> runCatching { FrontmatterType.valueOf(raw) }.getOrNull() }
        ?.let { repoConnectionRepository.setFrontmatterType(it) }
      backup.publishAction
        ?.let { raw -> runCatching { PublishAction.valueOf(raw) }.getOrNull() }
        ?.let { repoConnectionRepository.setPublishAction(it) }
      backup.themeMode?.let { settingsRepository.setThemeMode(ThemeMode.fromStored(it)) }
      backup.readabilityChecks?.let { names ->
        settingsRepository.setReadabilityChecks(
          names.mapNotNullTo(mutableSetOf()) { n -> ReadabilityCheck.entries.firstOrNull { it.name == n } }
        )
      }
    }

    val exportSettingsLauncher = rememberLauncherForActivityResult(
      ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
      if (uri == null) return@rememberLauncherForActivityResult
      val payload = settingsJson.encodeToString(SettingsBackup.serializer(), currentSettingsBackup())
      scope.launch {
        val ok = runCatching {
          withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(payload.toByteArray()) }
              ?: error("no output stream")
          }
        }.isSuccess
        toast = if (ok) "Settings exported" else "Couldn't export settings"
      }
    }

    val importSettingsLauncher = rememberLauncherForActivityResult(
      ActivityResultContracts.OpenDocument()
    ) { uri ->
      if (uri == null) return@rememberLauncherForActivityResult
      scope.launch {
        val backup = runCatching {
          val text = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
          } ?: error("no input stream")
          settingsJson.decodeFromString(SettingsBackup.serializer(), text)
        }.getOrNull()
        if (backup == null) {
          toast = "Couldn't read that settings file"
        } else {
          applySettingsBackup(backup)
          toast = "Settings imported — open GitHub Connection and Save to reconnect"
        }
      }
    }

    Box(
      Modifier
        .fillMaxSize()
        .background(BloggoTheme.colors.paper)
        .paperGrain()
    ) {
      Column(
        Modifier
          .fillMaxSize()
          .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
          .imePadding()
      ) {
        Box(Modifier.weight(1f)) {
          when (val route = current) {
            Route.Library -> {
              // Both lists are remembered against the post list itself. Without
              // that they were re-filtered and re-sorted on every recomposition
              // of this composable, and `posts` is a SnapshotStateList that every
              // editor keystroke invalidates. The selector is a field read now
              // (Post.dateMillis), which matters because sortedByDescending
              // invokes it once per *comparison*, not once per element.
              val snapshot = posts.toList()
              val drafts = remember(snapshot) {
                snapshot.filter { it.state == PostState.Draft && it.kind == DocKind.Post }
                  .sortedByDescending { it.lastEditedAtMillis() ?: 0L }
              }
              val published = remember(snapshot) {
                snapshot.filter { it.state == PostState.Published && it.kind == DocKind.Post }
                  .sortedByDescending { it.lastEditedAtMillis() ?: 0L }
              }
              LibraryScreen(
                drafts = drafts,
                inReview = posts.firstOrNull { it.state == PostState.InReview },
                published = published,
                // No outbox exists yet (ANDROID_TDD.md §6), so there is never
                // really a queue to report — showing a fixed sample count here
                // was a standing lie about local state. Zero until that
                // milestone gives this a real count to show instead.
                queuedCommits = 0,
                // Only shown once the repo actually checks out — an
                // unverified repo/branch pair is not something to advertise.
                repoSubtitle = (connectionCheck as? ConnectionCheck.Connected)?.let {
                  "${repoConnection.repository} · ${repoConnection.branch}"
                },
                isRefreshing = isRefreshingLibrary,
                onRefresh = { scope.launch { refreshLibrary(explicit = true) } },
                onOpenDraft = { post -> go(Route.Editor(post.slug)) },
                onOpenPost = { go(Route.Preview(it.slug, it.state == PostState.Published)) },
                onOpenLive = { openUrl("https://${it.liveUrl(siteHost)}") },
                onPush = { toast = "Pushing 2 queued commits" },
              )
            }

            Route.Inbox -> InboxScreen(
              fragments = fragments,
              onOpen = { fragment ->
                // Deliberately not posts.add(...)/fragments.remove(...) here
                // — opening a fragment to look at it must not promote it.
                // Held as transient state instead; only "Promote to Post"
                // (PostDetailsSheet, wired below in Route.Editor) makes it
                // real. See transientFragmentPost's own doc comment.
                transientFragmentSource = fragment
                transientFragmentPost = newPostFromFragment(fragment, defaultFrontmatterFields, frontmatterType)
                go(Route.Editor(transientFragmentPost!!.slug))
              },
              onCapture = { text ->
                if (text.isNotBlank()) {
                  val fragment = Fragment(id = "captured-${System.currentTimeMillis()}", text = text, capturedAtMillis = System.currentTimeMillis())
                  fragments.add(0, fragment)
                  scope.launch { fragmentStore.save(fragment) }
                  toast = "Caught"
                }
              },
              requestFocusOnOpen = requestInboxFocus,
              onFocusConsumed = { requestInboxFocus = false },
            )

            Route.Pages -> {
              val pages = remember(posts.toList()) {
                posts.filter { it.kind == DocKind.Page }
                  .sortedByDescending { it.lastEditedAtMillis() ?: 0L }
              }
              PagesScreen(
                pages = pages,
                isRefreshing = isRefreshingPages,
                onRefresh = { scope.launch { refreshPages(explicit = true) } },
                onNewPage = {
                  val blank = newBlankPage(frontmatterType)
                  posts.add(blank)
                  persistIfLocal(blank)
                  go(Route.Editor(blank.slug))
                },
                onOpenPage = { page ->
                  go(if (page.repoPath != null) Route.Preview(page.slug, published = true) else Route.Editor(page.slug))
                },
                onOpenLive = { openUrl("https://${it.liveUrl(siteHost)}") },
              )
            }

            is Route.Media -> {
              val returnSlug = route.returnToEditorSlug
              // Real repo images plus whatever's staged this session, merged
              // into one list only while this screen is actually showing —
              // the same "not a top-level val" reason `drafts`/`published`
              // and `tagPool` aren't either (Milestone 10/12, PROGRESS.md).
              val displayedMedia = remember(mediaFiles, stagedMedia.toList()) {
                val stagedAsFiles = stagedMedia.map { media ->
                  MediaFile(
                    name = media.originalFileName,
                    seed = media.repoPath,
                    sitePath = media.sitePath,
                    localPath = media.localPath,
                  )
                }
                stagedAsFiles + mediaFiles
              }
              val onPickForReturn: ((MediaFile) -> Unit)? = if (returnSlug == null) null else { file ->
                if (file.isStaged) {
                  val index = stagedMedia.indexOfFirst { it.sitePath == file.sitePath }
                  if (index >= 0) stagedMedia[index] = stagedMedia[index].copy(claimedByPostSlug = returnSlug)
                }
                pendingInsertImage = file
                back()
              }
              MediaScreen(
                files = displayedMedia,
                connection = repoConnection,
                token = storedToken,
                stagedMedia = stagedMedia,
                onBack = if (returnSlug != null) ::back else null,
                onPick = onPickForReturn,
                onUpload = { uri ->
                  scope.launch {
                    val originalFileName = resolveDisplayName(context, uri)
                    val existingCount = stagedMedia.count { it.claimedByPostSlug == returnSlug }
                    val targetPath = stagedImagePath(
                      imagePath = repoConnection.imagePath,
                      slug = returnSlug,
                      originalFileName = originalFileName,
                      timestamp = System.currentTimeMillis(),
                      count = existingCount,
                    )
                    val staged = stageFromUri(context, uri, targetPath, originalFileName)
                      .copy(claimedByPostSlug = returnSlug)
                    stagedMedia.add(staged)
                    if (returnSlug != null) {
                      pendingInsertImage = MediaFile(
                        name = staged.originalFileName,
                        seed = staged.repoPath,
                        sitePath = staged.sitePath,
                        localPath = staged.localPath,
                      )
                      back()
                    } else {
                      toast = "Added to media"
                    }
                  }
                },
              )
            }

            Route.Settings -> SettingsScreen(
              connection = repoConnection,
              publishedCount = posts.count { it.state == PostState.Published && it.kind == DocKind.Post },
              draftCount = posts.count { it.state == PostState.Draft && it.kind == DocKind.Post },
              openPullRequestCount = posts.count { it.state == PostState.InReview },
              checkResult = connectionCheck,
              appVersion = appVersion,
              onOpenPage = { go(Route.SettingsDetail(it)) },
            )

            is Route.SettingsDetail -> when (route.page) {
              SettingsPage.Connection -> SettingsConnectionScreen(
                connection = repoConnection,
                storedToken = storedToken,
                checkResult = connectionCheck,
                isChecking = isCheckingConnection,
                onSave = { repository, branch, siteUrl, authorName, token ->
                  scope.launch {
                    repoConnectionRepository.setRepo(repository, branch, siteUrl, authorName)
                    if (token.isNotBlank()) repoConnectionRepository.setToken(token)
                    isCheckingConnection = true
                    connectionCheck = null
                    connectionCheck = gitHubClient.checkConnection(
                      repository = repository,
                      branch = branch,
                      token = repoConnectionRepository.getToken(),
                    )
                    isCheckingConnection = false
                  }
                },
                onClearToken = {
                  scope.launch { repoConnectionRepository.clearToken() }
                  connectionCheck = null
                  toast = "Token cleared"
                },
                onVisitSite = { openUrl(repoConnection.siteUrl.ifBlank { "https://${SampleData.sampleSiteHost}" }) },
                onBack = ::back,
              )

              SettingsPage.Repo -> SettingsRepoScreen(
                connection = repoConnection,
                onSavePostPath = { path -> scope.launch { repoConnectionRepository.setPostPath(path) } },
                onSaveImagePath = { path -> scope.launch { repoConnectionRepository.setImagePath(path) } },
                onSaveFrontmatterFields = { fields -> scope.launch { repoConnectionRepository.setFrontmatterFields(fields) } },
                onSaveFrontmatterType = { type -> scope.launch { repoConnectionRepository.setFrontmatterType(type) } },
                onBack = ::back,
              )

              SettingsPage.Publishing -> SettingsPublishingScreen(
                connection = repoConnection,
                onPublishActionChange = { action -> scope.launch { repoConnectionRepository.setPublishAction(action) } },
                onBack = ::back,
              )

              SettingsPage.Appearance -> SettingsAppearanceScreen(
                themeMode = themeMode,
                onThemeModeChange = { mode -> scope.launch { settingsRepository.setThemeMode(mode) } },
                onBack = ::back,
              )

              SettingsPage.Readability -> SettingsReadabilityScreen(
                readabilityChecks = readabilityChecks,
                onReadabilityChecksChange = { checks ->
                  scope.launch { settingsRepository.setReadabilityChecks(checks) }
                },
                onBack = ::back,
              )

              SettingsPage.ImportExport -> SettingsImportExportScreen(
                onExport = { exportSettingsLauncher.launch("bloggo-settings.json") },
                onImport = { importSettingsLauncher.launch(arrayOf("application/json")) },
                onBack = ::back,
              )
            }

            is Route.Editor -> {
              val post = postBySlug(route.slug)
              val isFragmentPreview = transientFragmentPost?.slug == route.slug
              // Computed once per visit to the editor, keyed on the slug — not a
              // top-level val, for the same reason `repoConfig` isn't one (see the
              // comment above): every keystroke in this post's body mutates `posts`
              // and would otherwise re-walk every post's frontmatter on each one.
              val tagPool = remember(route.slug) {
                posts.flatMap { it.markdown.parseFrontmatter()["tags"]?.parseTagList().orEmpty() }
                  .distinct()
                  .sorted()
              }
              // A stale result from a previous post's publish attempt must
              // never bleed into this one's Publish sheet.
              LaunchedEffect(route.slug) {
                publishResult = null
                pendingInsertImage = null
              }
              EditorScreen(
                post = post,
                tagPool = tagPool,
                connection = repoConnection,
                remoteSlugs = if (post.kind == DocKind.Page) remotePageSlugs else remotePostSlugs,
                stagedMediaForPost = stagedMedia.filter { it.claimedByPostSlug == route.slug },
                isPublishing = isPublishing,
                publishResult = publishResult,
                pendingInsertImage = pendingInsertImage,
                isFragmentPreview = isFragmentPreview,
                // The count comes from the editor, which already measured this
                // exact string to update its own toolbar. Recomputing it here was
                // a second full-document pass per keystroke.
                onMarkdownChange = { markdown, wordCount ->
                  updatePost(route.slug) {
                    it.copy(
                      markdown = markdown,
                      title = markdown.parseFrontmatter()["title"]?.takeIf { title -> title.isNotBlank() } ?: it.title,
                      wordCount = wordCount,
                      editedAgo = "just now",
                      updatedAt = System.currentTimeMillis(),
                    )
                  }
                },
                onBack = ::back,
                onPreview = {
                  // A page has no draft flag to read "already live" off — it's
                  // "already live" the moment it has a real repo path, whether
                  // that came from a fetch or from just having been published.
                  val alreadyLive = if (post.kind == DocKind.Page) post.repoPath != null else post.state == PostState.Published
                  go(Route.Preview(route.slug, published = alreadyLive), replace = true)
                },
                onFocus = { go(Route.Focus(route.slug)) },
                onReview = { go(Route.Review(route.slug)) },
                onToast = { toast = it },
                onDeletePost = {
                  val isPage = post.kind == DocKind.Page
                  val remoteSlugs = if (isPage) remotePageSlugs else remotePostSlugs
                  if (post.isPushed(remoteSlugs)) {
                    // Confirmed already: PostDetailsSheet's own delete dialog
                    // is what warned the writer this also removes the GitHub
                    // file, and this callback only runs after that confirm —
                    // "confirm-then-delete-both," never a second silent step.
                    scope.launch {
                      val path = post.repoPath
                        ?: if (isPage) resolvePagePath(post.slug) else resolvePostPath(repoConnection.postPath, post.slug)
                      val result = gitHubClient.deleteFile(
                        repository = repoConnection.repository,
                        branch = repoConnection.branch,
                        token = repoConnectionRepository.getToken(),
                        message = "Delete ${if (isPage) "page" else "post"}: ${post.title}",
                        path = path,
                      )
                      when (result) {
                        is CommitResult.Success -> {
                          posts.removeAll { it.slug == route.slug }
                          // The remote copy is gone; the cache row must go
                          // with it, or the next 304-served refresh would
                          // resurrect this post/page from a now-stale row.
                          if (isPage) {
                            pageLibraryRepository.evict(path)
                            remotePageSlugs = remotePageSlugs - route.slug
                          } else {
                            postLibraryRepository.evict(path)
                            remotePostSlugs = remotePostSlugs - route.slug
                          }
                          // A post/page created locally before it was ever
                          // pushed leaves its local_post row/file in place
                          // even after publish (see LocalPostStore). This is
                          // an explicit user delete of that same slug, so
                          // clearing it here is required, not optional —
                          // otherwise the next startup's loadAll() would
                          // resurrect the post the user just deleted.
                          localPostStore.delete(route.slug)
                          if (!isPage) ensureDraftExists()
                          toast = if (isPage) "Page deleted" else "Post deleted"
                          back()
                        }
                        is CommitResult.Failed -> {
                          // Local state is untouched on failure — deleting the
                          // in-memory post/cache row for a file that's still
                          // sitting in the repo would just resurrect it wrong
                          // on the next refresh.
                          toast = "Couldn't delete from GitHub: ${result.error.describe()}"
                        }
                      }
                    }
                  } else {
                    posts.removeAll { it.slug == route.slug }
                    // The one place this app currently lets a writer delete a
                    // local-only post/page — a direct tap on this button, not
                    // a side effect of sync or startup — so this is exactly
                    // the "explicit user action" LocalPostStore.delete
                    // requires. Without this, the file this post was
                    // persisted to would outlive the post it belongs to and
                    // resurrect it the next time loadAll() runs.
                    scope.launch { localPostStore.delete(route.slug) }
                    if (!isPage) ensureDraftExists()
                    toast = if (isPage) "Page deleted" else "Draft deleted"
                    back()
                  }
                },
                onMoveToInbox = {
                  val fragment = newFragmentFromPost(post)
                  fragments.add(0, fragment)
                  posts.removeAll { it.slug == route.slug }
                  // Same "explicit user action" invariant onDeletePost's
                  // local-only branch relies on above: this post is no
                  // longer a draft, its content lives on as an Inbox
                  // fragment instead, so its persisted file/row must go too
                  // — and the new fragment needs its own row so the demoted
                  // content survives a process death the same as any other
                  // capture would.
                  scope.launch {
                    localPostStore.delete(route.slug)
                    fragmentStore.save(fragment)
                  }
                  if (post.kind != DocKind.Page) ensureDraftExists()
                  toast = "Moved to Inbox"
                  back()
                },
                onPromoteToPost = {
                  // Only ever invoked while isFragmentPreview is true — see
                  // that param's own doc comment — so transientFragmentSource
                  // and transientFragmentPost are always non-null here.
                  val source = transientFragmentSource
                  val promoted = transientFragmentPost
                  if (source != null && promoted != null) {
                    posts.add(promoted)
                    persistIfLocal(promoted)
                    fragments.remove(source)
                    scope.launch { fragmentStore.delete(source.id) }
                    transientFragmentSource = null
                    transientFragmentPost = null
                    toast = "Promoted to Drafts"
                    back()
                  }
                },
                onDeleteFragment = {
                  // Only ever invoked while isFragmentPreview is true — see
                  // onPromoteToPost's identical comment above — so
                  // transientFragmentSource is always non-null here. A
                  // capture is never pushed (FragmentStore.kt), so this is a
                  // local-only delete, no GitHub call.
                  val source = transientFragmentSource
                  if (source != null) {
                    fragments.remove(source)
                    scope.launch { fragmentStore.delete(source.id) }
                    transientFragmentSource = null
                    transientFragmentPost = null
                    toast = "Capture deleted"
                    back()
                  }
                },
                onPublish = { message, date, setDraftFalse ->
                  scope.launch {
                    isPublishing = true
                    // Set exactly here, never before: PublishSheet's "Set date
                    // to Today" toggle (or its manual override) must not touch
                    // the frontmatter while the writer is still just editing a
                    // multi-day draft — only the actual Publish tap does.
                    var updatedMarkdown = post.markdown.withUpdatedDate(date)
                    // PublishSheet's "still a draft" prompt: when the writer
                    // chose to flip it, `draft: true` -> `false` rides along in
                    // this same commit.
                    if (setDraftFalse) updatedMarkdown = updatedMarkdown.withUpdatedDraft(false)
                    val claimed = stagedMedia.filter { it.claimedByPostSlug == route.slug }
                    val result = postPublishRepository.publish(
                      post = post.copy(markdown = updatedMarkdown),
                      connection = repoConnection,
                      token = repoConnectionRepository.getToken(),
                      message = message,
                      stagedMediaForThisPost = claimed,
                    )
                    publishResult = result
                    isPublishing = false
                    if (result is PublishResult.Success) {
                      updatePost(route.slug) {
                        it.copy(
                          repoPath = result.repoPath,
                          markdown = updatedMarkdown,
                          editedAgo = "just now",
                          updatedAt = System.currentTimeMillis(),
                        )
                      }
                      stagedMedia.removeAll { it.claimedByPostSlug == route.slug }
                      if (post.kind == DocKind.Page) {
                        remotePageSlugs = remotePageSlugs + route.slug
                      } else {
                        remotePostSlugs = remotePostSlugs + route.slug
                      }
                      toast = "Published"
                    }
                  }
                },
                onInsertImage = { go(Route.Media(returnToEditorSlug = route.slug)) },
                onPendingInsertConsumed = { pendingInsertImage = null },
              )
            }

            is Route.Preview -> {
              // A slug, not a Post: carrying the whole document in the back stack
              // retained a copy per entry and, worse, froze it at navigation time,
              // so Read mode could show content the editor had already changed.
              val post = postBySlug(route.slug)
              PreviewScreen(
                post = post,
                published = route.published,
                siteHost = siteHost,
                markdown = post.markdown,
                connection = repoConnection,
                token = storedToken,
                stagedMedia = stagedMedia.filter { it.claimedByPostSlug == route.slug },
                onBack = ::back,
                onEdit = { go(Route.Editor(route.slug), replace = true) },
                onShare = {
                  val url = "https://${post.liveUrl(siteHost)}"
                  val heading = if (post.kind == DocKind.Page) {
                    val author = repoConnection.authorName.trim()
                    if (author.isNotBlank()) "${post.title} · $author" else post.title
                  } else {
                    post.title
                  }
                  val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "$heading\n\n$url")
                  }
                  context.startActivity(Intent.createChooser(sendIntent, null))
                },
                onOpenLive = { openUrl("https://${post.liveUrl(siteHost)}") },
              )
            }

            is Route.Focus -> FocusScreen(
              slug = route.slug,
              markdown = postBySlug(route.slug).markdown,
              onMarkdownChange = { markdown, wordCount ->
                updatePost(route.slug) {
                  it.copy(
                    markdown = markdown,
                    wordCount = wordCount,
                    updatedAt = System.currentTimeMillis(),
                  )
                }
              },
              onExit = ::back,
            )

            is Route.Review -> {
              // Load the ignored-findings set before the first analysis so a
              // dismissed highlight never flashes back on reopen. Keyed by slug
              // so switching posts reloads; null means "still loading".
              var ignoredKeys by remember(route.slug) { mutableStateOf<Set<String>?>(null) }
              LaunchedEffect(route.slug) {
                ignoredKeys = readabilityIgnoreStore.load(route.slug)
              }
              val keys = ignoredKeys
              if (keys == null) {
                Box(Modifier.fillMaxSize().background(BloggoTheme.colors.paper))
              } else {
                ReviewScreen(
                  markdown = postBySlug(route.slug).markdown,
                  enabledChecks = readabilityChecks,
                  ignoredKeys = keys,
                  onIgnore = { key ->
                    ignoredKeys = keys + key
                    scope.launch { readabilityIgnoreStore.ignore(route.slug, key) }
                  },
                  onRecompute = {
                    ignoredKeys = emptySet()
                    scope.launch { readabilityIgnoreStore.clear(route.slug) }
                    toast = "Checks recomputed"
                  },
                  onBack = ::back,
                  onToast = { toast = it },
                )
              }
            }

            Route.Mastodon -> MastodonScreen(
              accounts = SampleData.accounts,
              initialText = SampleData.defaultToot,
              onBack = ::back,
              onToast = { toast = it },
            )
          }
        }

        if (current.isTab) {
          BloggoTabBar(
            tabs = TABS,
            selectedRoute = when (current) {
              Route.Library -> "library"
              Route.Inbox -> "inbox"
              Route.Pages -> "pages"
              else -> "settings"
            },
            onSelect = {
              go(
                when (it) {
                  "library" -> Route.Library
                  "inbox" -> Route.Inbox
                  "pages" -> Route.Pages
                  else -> Route.Settings
                }
              )
            },
            onCompose = {
              val blank = newBlankPost(defaultFrontmatterFields, frontmatterType)
              posts.add(blank)
              persistIfLocal(blank)
              go(Route.Editor(blank.slug))
            },
            modifier = Modifier.padding(
              bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            ),
          )
        }
      }

      BloggoToast(
        message = toast,
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 96.dp),
      )
    }
  }
}
