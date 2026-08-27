# Write & publish loop: direct commit, real media, real preview

## Context

Three related gaps, requested together because they're genuinely coupled:

1. **Publish is a stub.** The editor's Commit button just shows a toast ("Queued. It will push when you reconnect.") — there's no actual write path to GitHub. `PROGRESS.md`'s "Next" list has had "Commit through the Git Data API" sitting unbuilt since Milestone 13. The user wants to actually write a post and publish it, committing straight to whatever branch is configured in Settings (never hardcoded to `main`/`master`).
2. **Media is entirely fake.** `MediaScreen` shows `SampleData.media` with cover-art-generator thumbnails standing in for real photos; "Add from camera or files" is a toast. There's no way to get an image from the device into a post today.
3. **Preview never renders real media.** `ArticleParser` already parses `![caption](src)` into `ArticleBlock.Figure` and shortcode-rendered `<video>` tags into `ArticleBlock.Video` — but `PreviewScreen` renders both as a flat gray box with the filename as text. Nothing is ever actually shown.

These three are one feature in practice: an image only has somewhere to come from (Media) and somewhere to go (a real commit) once the commit path exists, and it's only worth looking at in Preview once it can render for real. Building them together also means the "staged, not-yet-committed" media concept only needs to be designed once.

**Confirmed scope for this pass** (via user Q&A):
- Publish v1 is **direct commit only** — no "open a pull request" flow, no line-level diff view. Just a commit message, a plain list of files about to change, and a Publish button.
- Video gets **preview playback only**, and only for a fully-qualified `http(s)://` URL. No new video upload/insert flow — recording, compression, and picking a video file are all out of scope.
- Media uploads (from the Media screen *or* the editor's Insert flow) **stage locally and are not committed immediately** — they commit only once a post actually references them, bundled into that post's own commit.
- Editor's Insert → Image **navigates into the Media screen** (pick an existing repo/staged image, or upload a new one there) rather than always launching a bare device picker — matches the HTML prototype's `data-go="media"` design and lets an image be reused across posts.
- Staged images get a **basic client-side downscale** before commit (cap the longest edge; no new dependency — `Bitmap.createScaledBitmap`), so the Media screen's existing "Resized, renamed by slug, committed with the post" copy stays true instead of becoming a lie the moment real uploads exist.

## How this was meant to work (answering "how was Media screen meant to be used")

The HTML prototype (`design/bloggo-prototype.html`) already specifies this, and the Android port never finished it:
- **Media** is a browser/manager over the repo's real `static/images/` (filters: All / Unused / Covers / Screenshots), not a standalone upload target.
- The editor's **Insert** sheet (`data-sheet="insert"`, never built in the Android app) has three rows — **Image**, **Code block**, **Divider** — plus a "Shortcodes in your repo" section (Callout / Aside / Figure, read from `layouts/shortcodes/`) that stays **out of scope this pass**; `MarkdownAction.Callout`/`.Aside`/`.figure(...)` already exist in the codebase with zero callers today, waiting for exactly this.
- Insert → **Image** (`data-go="media"`) jumps into the Media screen; picking a file there returns to the editor with the image inserted and staged against that specific post, to be committed together with the post text — this is the "committed with the post" the dropzone copy already promises.

## Build order

Each stage is independently testable before the next depends on it.

### Stage A — Git Data API write path (`data/github/`)

`GitHubApi.kt`: add DTOs for the write endpoints, matching the existing `<Thing>Dto` / `@SerialName` convention — `CreateBlobRequestDto(content, encoding)`, `CreateBlobResponseDto(sha)`, `TreeEntryInputDto(path, mode="100644", type="blob", sha)`, `CreateTreeRequestDto(base_tree, tree)` (reuses the existing `GitTreeResponseDto` for the response — it already has `sha`), `CreateCommitRequestDto(message, tree, parents)`, `CreateCommitResponseDto(sha)`, `UpdateRefRequestDto(sha, force=false)`, and a `BranchDto` (nested down to a tree sha) for one-call branch-head lookup. Add methods: `getBranch` (`GET .../branches/{branch}`), `createBlob` (`POST .../git/blobs`), `createTree` (`POST .../git/trees`), `createCommit` (`POST .../git/commits`), `updateRef` (`PATCH .../git/refs/heads/{branch}`). Same header/param style as every existing method (`@Header("Authorization") authorization: String?` first, `Response<T>` return).

`GitHubClient.kt`:
- Extend `GitHubApiError` with `data object Conflict` — a non-fast-forward `updateRef` failure (GitHub returns 422 specifically at that call site; don't make 422 globally mean Conflict since it's reused for generic validation errors elsewhere per §5.5's table — classify it only at the ref-update call site).
- Add `sealed interface FileContent { Text(value); Base64(bytes) }`, `data class CommitFile(path, content)`, `sealed interface CommitResult { Success(commitSha); Failed(error) }`.
- Add `suspend fun commitFiles(repository, branch, token, message, files: List<CommitFile>): CommitResult`, `withContext(Dispatchers.IO)`:
  1. `getBranch` → head commit sha + base tree sha in one call.
  2. Create one blob per file (concurrent, gated the same way `PostLibraryRepository.refresh` already gates its per-post fetches) — `utf-8` for text, `base64` for images.
  3. `createTree(base_tree, one entry per file)`.
  4. `createCommit(message, new tree sha, parents=[head sha])`.
  5. `updateRef(new commit sha, force=false)` — a rejected non-fast-forward maps to `Conflict`; anything else goes through the existing `classifyHttpError`.
- The branch is always the caller-supplied `RepoConnection.branch` — never a literal `"main"`, consistent with every existing method.

`GitHubClientTest.kt`: happy path (single file, asserts the full 5-call sequence, and specifically runs once with a non-`"main"` branch to catch any accidental hardcoding); multi-file commit (post + image) asserting one blob per file with the right encoding per type; ref-update 422 → `Conflict`; a mid-sequence failure (e.g. blob creation 401) short-circuits without calling later endpoints.

This stage ships and is fully tested with no UI involved.

### Stage B — Path resolution + publish orchestration

New `data/RepoPaths.kt` — pure, unit-testable functions shared by publish and media:
- `resolvePostPath(template, slug)` — resolves `RepoConnection.postPath`'s `{slug}` token.
- `sitePathToRepoPath(sitePath)` — reverse of the site-absolute-path convention `PostDetailsSheet.CoverSection` already uses (`"/" + repoPath.removePrefix("static/")`); resolves as `"static" + sitePath` rather than being tied to the configured `imagePath`, since a figure's `src` isn't guaranteed to live under it.

`model/Model.kt` — add `val repoPath: String? = null` to `Post`, populated from `PostCacheEntity.path` in `PostLibraryRepository.toPost()` (currently dropped on the floor), left `null` for local/fragment-promoted drafts. **Once set, never re-derive from the `{slug}` template again** — Hugo's filename and frontmatter `slug:` are independent, and a post whose frontmatter slug changed after it was already committed keeps committing to its original file path rather than silently forking into a second file.

New `data/publish/PostPublishRepository.kt` (thin orchestration, no Room — mirrors `PostLibraryRepository`'s shape without needing its caching):
- `sealed interface PublishResult { Success(repoPath, commitSha); Failed(error) }`
- `suspend fun publish(post, connection, token, message, stagedMediaForThisPost): PublishResult` — resolves `post.repoPath ?: resolvePostPath(connection.postPath, post.slug)`, builds one `CommitFile` for the markdown plus one per staged image (reading bytes from `StagedMedia.localPath`), calls `gitHubClient.commitFiles(...)`.

New `PostPublishRepositoryTest.kt` (same `MockWebServer` pattern as `PostLibraryRepositoryTest`): template-resolved path vs. reused `repoPath`; only media claimed for this post's slug are included; branch passes through unmodified.

### Stage C — Publish UI

New `ui/sheet/PublishSheet.kt` — same convention as `PostDetailsSheet` exactly: `@OptIn(ExperimentalMaterial3Api::class)`, `rememberModalBottomSheetState(skipPartiallyExpanded = true)`, local `dismiss(after:)` hide-then-callback helper, `ModalBottomSheet(containerColor = paperRaised, contentColor = ink)`. Content: title "Publish", a commit-message field defaulting to `"${if (draft) "Draft" else "Update"}: $title"`, a plain "Changes" list (post path; one row per staged-for-this-post image tagged "new" — no diff content), a Publish button, an inline error line + Retry when `publishResult is Failed` (conflicts are inherently refetch-and-retry-shaped, and Retry is nearly free to add here).

`draft:` frontmatter is **never touched by publish** — stays entirely under the existing "Keep as draft" switch in `PostDetailsSheet`, which is already the writer-controlled mechanism for that; inventing an auto-flip here would be new, uninvited behavior.

`EditorScreen.kt`: replace the `onCommit` stub with `var showPublish by remember { mutableStateOf(false) }`, composed the same way `showDetails`/`PostDetailsSheet` already is. New params `stagedMediaForPost`, `isPublishing`, `publishResult`, `onPublish: (message) -> Unit` — plain prop-down, same shape as `RepoScreen(checkResult=, isChecking=, ...)`; all suspend/IO work stays owned by `BloggoApp`.

`BloggoApp.kt`: instantiate `PostPublishRepository(gitHubClient)`; `isPublishing`/`publishResult` state; `onPublish` launches the publish call, and on success updates the post's `repoPath`/`updatedAt`, removes the claimed entries from `stagedMedia`, and toasts "Published."

### Stage D — Real preview rendering

No `ArticleParser`/`ArticleBlock` changes — parsing already works; this is purely a `PreviewScreen` rendering change.

Add Coil to the version catalog the same way `retrofit`/`room` are declared (`gradle/libs.versions.toml` + `app/build.gradle.kts`): `io.coil-kt.coil3:coil-compose` + `coil-network-okhttp` (reuses the already-declared `okhttp` dependency rather than pinning a second HTTP stack).

New `ui/preview/RepoImage.kt` — a small `RepoAsyncImage(sitePath, connection, token, stagedMedia, modifier)` composable:
1. If `sitePath` matches a `StagedMedia.sitePath`, load `File(localPath)` directly (Coil handles `File` models natively) — the not-yet-committed case.
2. Otherwise resolve the repo-relative path via `sitePathToRepoPath` and load through GitHub's authenticated raw-content endpoint (the same one `GitHubClient.getFileContent` already calls with `Accept: application/vnd.github.raw`) — try Coil3's per-request `httpHeaders` first; if that API doesn't fit the pinned Coil3 version, fall back to a `GitHubClient.getFileBytes` sibling of `getFileContent` (same call, `.bytes()` instead of `.string()`) wired through a small Coil `Fetcher.Factory`.
3. `placeholder`/`error` both keep today's existing gray box + filename — no visual regression on failure, only a real image added on success.

`PreviewScreen.kt`: `ArticleBlock.Figure` renders via `RepoAsyncImage` instead of the flat box. `ArticleBlock.Video` renders a real `android.widget.VideoView` (wrapped in `AndroidView`, with a `MediaController`) **only when `block.src` starts with `http://`/`https://`**; anything else keeps the current "▶ filename" placeholder. Needs `repoConnection`/`stagedMediaForPost` threaded down from `BloggoApp`, same as the Publish wiring.

### Stage E — Media: staging, listing, and the Insert → Image flow

**E1 — the staged-media shape.** New `model/Media.kt` (kept separate from the already-large `Model.kt`):
- `data class StagedMedia(localPath, repoPath, sitePath, originalFileName, claimedByPostSlug: String?, stagedAt)` — `localPath` stays a plain `String`, never `android.net.Uri`, matching `Model.kt`'s own pure/JVM-testable design.
- `MediaFile` gains `sitePath`, `repoPath: String?`, `localPath: String?`; `isStaged = repoPath == null`.
- `stagedImagePath(imagePath, slug, originalFileName, timestamp)` — pure and testable: known slug → `{imagePath}/{year}/{slug}-{n}.{ext}` (the "renamed by slug" case, matching the dropzone copy); no slug yet (a bare Media-screen upload with no post context) → `{imagePath}/{year}/{timestamp}-{sanitized filename}`.

**E2 — real repo image listing.** Refactor `GitHubClient.getPostsTree`'s body into a shared private `fetchFilteredTree(repository, branch, token, etag, predicate)` (keeps its existing truncated-tree fallback), add public `getImagesTree(repository, branch, imagePath, token, etag)` filtered to image extensions under `imagePath` — one recursive call, reusing the exact pattern already proven for posts rather than a slower per-directory listing. New `data/media/MediaRepository.kt` turns the resulting entries into `MediaFile`s.

**E3 — staging from the Photo Picker.** New `data/media/MediaStaging.kt` — `suspend fun stageFromUri(context, uri, targetRepoPath, originalFileName): StagedMedia`, on `Dispatchers.IO`: reads the picked `content://` Uri once via `BitmapFactory`, downscales to a capped longest-edge constant via `Bitmap.createScaledBitmap`, writes the result into `context.cacheDir/staged-media/`. Copying once up front (rather than holding the Uri) is deliberate — a `content://` Uri isn't guaranteed readable later.

**E4 — the registry.** `BloggoApp.kt`: `val stagedMedia = remember { mutableStateListOf<StagedMedia>() }` — in-memory only, lost on process death. This matches the codebase's current durability boundary rather than inventing a new one: post edits themselves aren't autosaved yet either (`PROGRESS.md`'s still-open "Editor autosave to Room"), so persisting staged media across a kill while concurrent post edits don't would be inconsistent, not safer. Sweep `cacheDir/staged-media/` on cold start so an unclaimed upload never lingers as an orphaned file after a restart — it simply doesn't survive one, same as an in-progress edit today.

**E5 — Media screen goes real.** `MediaScreen.kt`: `files` becomes the merged repo+staged list (merged upstream in `BloggoApp`, keeping the screen a pure render function). Staged rows get a small "not yet published" tag. New optional `onPick: ((MediaFile) -> Unit)? = null` — tapping a thumbnail selects and returns it when set. `onUpload` becomes real via `rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia())` (no runtime permission needed on modern API levels, so no manifest changes) → `MediaStaging.stageFromUri` → added to the registry, and in picker mode auto-returned via `onPick` immediately.

**E6 — Insert → Image navigation.** `Route.Media` becomes `data class Media(val returnToEditorSlug: String? = null) : Route`; `isTab` only counts a plain (non-return) visit, so a picker-mode push doesn't clear the back stack or show the tab bar — consistent with how `Editor`/`Preview`/`Focus` already behave. New `ui/editor/InsertSheet.kt` — the three prototype rows (Image / Code block / Divider; the "Shortcodes in your repo" section stays a one-line "not built yet" note, since `MarkdownAction.Callout`/`.Aside` already exist and are ready whenever that lands). Image pushes `Route.Media(returnToEditorSlug = post.slug)`; Code block/Divider call `edit { MarkdownAction.CodeBlock.applyTo(it) }` / `.Rule` directly. Returning the pick to the editor reuses the exact one-shot-signal pattern `requestInboxFocus`/`onFocusConsumed` already establishes: `EditorScreen` gets `pendingInsertImage: MediaFile?` + `onPendingInsertConsumed`, and a `LaunchedEffect` inserts `MarkdownAction.figure(sitePath, "")` at the cursor then clears the signal; `BloggoApp` marks the picked file's `claimedByPostSlug = post.slug` and pops the back stack to the editor.

## Known limitations going in (explicitly out of scope, not oversights)

- No "open a pull request" flow yet — `PublishAction.OpenPullRequest`/`AskEveryTime` stay unbranched; every publish is a direct commit to `RepoConnection.branch`. Tracked as its own future item.
- No line-level diff in the Publish sheet — just a file list.
- No video upload/insert — only http(s)-hosted video actually plays in Preview.
- No shortcode-from-repo section in the Insert sheet (Callout/Aside detection from `layouts/shortcodes/`).
- No conflict *resolution* UI — a rejected non-fast-forward commit surfaces as a clear error with Retry, not a merge tool.

## Verification

- `./gradlew :app:testDebugUnitTest` after each stage — new coverage: `GitHubClientTest` (Stage A), `PostPublishRepositoryTest` (Stage B), pure-function tests for `RepoPaths`/`stagedImagePath` (Stage B/E1), `PostLibraryRepositoryTest` still green after the `repoPath` change.
- `./gradlew :app:compileDebugKotlin :designsystem:compileDebugKotlin` after each stage.
- Manual/emulator pass at the end against the user's real connected repo (the only way to verify an actual commit lands correctly and the branch used matches Settings): write a new post, insert an image via Insert → Image, confirm it renders in Preview before publishing, Publish, and confirm on GitHub that exactly one commit landed on the configured branch containing both the markdown and the image at the expected paths.
