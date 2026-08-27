Performance Audit — Bloggo Android — 2026-08-18

 Status: implemented 2026-08-18. Findings #1-#8 and #10-#11 landed in full;
 #9 landed except for lazy markdown loading (the stored word count and the
 chunked deleteExcept are in, the projection queries are not — they only pay off
 once Post.markdown stops being eagerly populated, which needs a load-on-open
 path through Editor, Preview and Focus). The "Deferred" and "Out of scope"
 sections below are untouched. See PROGRESS.md, Milestone 13.

 Context

 Ran the android-dev-workflow skill's performance-audit command against android/
 (~4,600 lines of main source across :app, :designsystem, :coverart).

 The app is a git-backed Hugo post editor: a Compose editor over raw markdown, a
 library backed by the GitHub trees API with a Room frontmatter cache, and a
 deterministic generated cover-art renderer. Everything below was found by reading
 the code, not by profiling; nothing has been measured on a device, and the
 "Needs data" section says so where it matters.

 Scope, as decided after the audit: runtime hot paths only. Build configuration,
 R8, baseline profiles and APK size are recorded under "Deferred" but are not part
 of this plan. The two correctness bugs the performance read surfaced (#7's caret
 reset, #10's stale Preview) are folded in, since the fixes touch the same lines the
 performance work already changes.

 Bootstrap notes:
 - Model: running as Opus 5, which is what the skill recommends for this command.
 - git pull was skipped: 22 files are modified/added and uncommitted, and the
 skill says to surface that rather than pull over it.
 - internal/ does not exist and there is no root CLAUDE.md, so init has never
 been run. Running it first would set up the shared workspace these commands
 normally write to. This audit is written to the plan file instead.

 ---

 Summary

 The codebase is careful and well-commented, and several past per-keystroke
 regressions were clearly found and fixed by hand. But the fixes treated symptoms:
 the underlying primitives (parseFrontmatter, markdownWordCount,
 MarkdownHighlighter.spans) are all full-document passes, so every caller that
 looks reasonable in isolation still costs O(document) each time. Typing one
 character in the editor currently triggers at least four full splits or regex
 scans of the entire post, one of which (markdownWordCount) is the exact
 duplicate the comment at EditorScreen.kt:81 says was removed — it moved to the
 caller rather than going away.

 The three highest-value items: collapse the per-keystroke document passes
 (#1), get getToken()'s Keystore-backed disk read off the main thread and out
 of composition (#2), and parallelise the library refresh's serial per-post
 fetches (#3).

 ---

 Findings

 1. Every keystroke in the editor makes four or more full-document passes — Impact: High, Confidence: High

 - Location: model/Model.kt:106-136, ui/editor/EditorScreen.kt:90-104,
 BloggoApp.kt:449-459, designsystem/editor/MarkdownHighlighter.kt:55-108

 Issue. One character typed runs, in order:

 ┌────────────────────────────────┬─────────────────────────────────┬───────────────────────────────────────────────────────────────────────────────────┐
 │              Pass              │              Where              │                                       Cost                                        │
 ├────────────────────────────────┼─────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────┤
 │ parseFrontmatter(previousText) │ syncSlugToTitle, Model.kt:250   │ full lines() split + List<String> alloc                                           │
 ├────────────────────────────────┼─────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────┤
 │ parseFrontmatter(newText)      │ syncSlugToTitle, Model.kt:251   │ full lines() split + alloc                                                        │
 ├────────────────────────────────┼─────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────┤
 │ markdownWordCount()            │ EditorScreen.kt:101             │ 2 whole-doc regex replaces (each allocating a full copy) + a findAll().count()    │
 ├────────────────────────────────┼─────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────┤
 │ parseFrontmatter()             │ BloggoApp.kt:453                │ full lines() split (third)                                                        │
 ├────────────────────────────────┼─────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────┤
 │ markdownWordCount() again      │ BloggoApp.kt:455                │ same three passes, same string, second time                                       │
 ├────────────────────────────────┼─────────────────────────────────┼───────────────────────────────────────────────────────────────────────────────────┤
 │ MarkdownHighlighter.spans()    │ via                             │ full split("\n"), then per line up to 4 anchored finds, a                         │
 │                                │ MarkdownTransformation.filter   │ BooleanArray(line.length) alloc, and up to 4 findAll scans                        │
 └────────────────────────────────┴─────────────────────────────────┴───────────────────────────────────────────────────────────────────────────────────┘
 
  The root cause is smaller than it looks. String.frontmatterFence()
 (Model.kt:112) does lines().firstOrNull() — it splits the entire document
 to read line 1. parseFrontmatter (Model.kt:119-120) then calls lines() a
 second time and iterates only to the closing fence. So reading a frontmatter field
 from a 5,000-word post allocates two full line lists of that post. Four call sites
 depend on it: parseFrontmatter, rewriteFrontmatterFields,
 replaceFrontmatterField, withFrontmatterEdits.

 Recommended fix, in the order they pay off:

 1. Model.kt — stop splitting the document to read its head. Rewrite
 frontmatterFence() and parseFrontmatter() to walk the string with
 indexOf('\n') from offset 0 and stop at the closing fence. parseFrontmatter
 becomes O(frontmatter) instead of O(document), and every one of its callers gets
 that for free — including Post.liveUrl, Post.lastEditedAtMillis,
 PostDetailsSheet, and the tagPool computation at BloggoApp.kt:440-444,
 which currently walks every post's full markdown.
 2. Delete the duplicate word count. EditorScreen.edit() already computes it
 at line 101. Widen onMarkdownChange to (String, Int) -> Unit and pass it,
 so BloggoApp.kt:455 uses the value instead of recomputing it. FocusScreen
 (:125) and the PostDetailsSheet save path (EditorScreen.kt:189-192) call
 the same callback, so both need the count too — FocusScreen already computes
 one at :69.
 3. Memoise the highlighter. Give MarkdownTransformation a one-entry memo
 (last source → last AnnotatedString) so repeated filter() calls for
 unchanged text are free. MarkdownHighlighter.spans itself can also skip the
 BooleanArray allocation for lines with no inline markers by checking for the
 presence of `, [, * before entering inlineSpans.

 Risk of not fixing. Cost scales linearly with post length, so it is invisible
 on the sample data and worst on exactly the long-form posts this app exists to
 write. This is also the third time this shape of bug has been fixed here (see the
 comments at EditorScreen.kt:81-85, BloggoApp.kt:236-239,
 PostDetailsSheet.kt:57-61); fixing the primitives is what stops it recurring.

 ---

 2. getToken() does a Keystore-backed disk read on the main thread, from inside composition — Impact: High, Confidence: High

 - Location: data/RepoConnectionRepository.kt:69-77,138; called from
 BloggoApp.kt:402, :309, :414

 Issue. getToken() is a plain (non-suspend) function that reads
 EncryptedSharedPreferences — an AES-SIV/GCM decrypt over a SharedPreferences
 file, behind a by lazy whose first touch also runs
 EncryptedSharedPreferences.create and unwraps the Keystore master key.

 BloggoApp.kt:402 calls it during composition:

 storedToken = if (repoConnection.hasToken) repoConnectionRepository.getToken() else null,

 That argument is evaluated on every recomposition of BloggoApp while Route.Repo
 is current. The other two call sites (:309 inside refreshLibrary, reached from
 a LaunchedEffect; :414 inside scope.launch from rememberCoroutineScope)
 both run on Dispatchers.Main too, because neither of those scopes switches
 dispatcher.

 Recommended fix. Make getToken() a suspend fun wrapped in
 withContext(Dispatchers.IO) — GitHubClient already uses exactly this pattern
 (GitHubClient.kt:97, :147, :192). Then load the token for the Repo screen in
 a LaunchedEffect(repoConnection.hasToken) into a var storedToken by remember,
 so composition reads state rather than disk. Move the refreshLibrary and
 onSaveConnection calls onto the same suspend function.

 Risk of not fixing. A StrictMode disk-read-on-main violation, and a Keystore
 unwrap (tens of ms) blocking the frame the first time the Repo tab is opened. It
 also silently re-decrypts the token on every unrelated recomposition of that
 screen.

 ---

 3. Library refresh fetches every changed post one at a time — Impact: High, Confidence: High

 - Location: data/library/PostLibraryRepository.kt:38-50

 Issue. After the single tree call, each post whose blob SHA changed is fetched
 inside entries.mapNotNull { ... } — a sequential suspend call per file. On first
 sync every post is "changed", so an N-post blog is N serial HTTPS round trips. At a
 200ms RTT that is ~12s for 60 posts, with the library empty the whole time.

 There is also no conditional-request support: neither the tree call nor the file
 fetches send If-None-Match, so every refresh re-downloads the full tree and
 spends a rate-limit unit even when nothing changed. The class KDoc
 (GitHubClient.kt:78-80) already names this as deferred work.

 Recommended fix. Parallelise with bounded concurrency:

 val updated = coroutineScope {
   val gate = Semaphore(5)   // OkHttp's default maxRequestsPerHost is 5
   entries.map { entry -> async { gate.withPermit { fetchOne(entry, cached) } } }.awaitAll()
 }.filterNotNull()

 Keep the existing per-file fallback-to-cache behaviour inside fetchOne — it is
 correct and worth preserving. Separately, store the tree response's ETag and send
 it back as If-None-Match so an unchanged repo costs one 304 instead of a full
 tree download.

 Risk of not fixing. First-run sync feels broken on any real blog, and every
 manual refresh burns proportionally more of the 5,000/hr authenticated rate limit
 than it needs to.

 ---

 4. Hugo detection makes up to eight sequential network calls — Impact: Medium, Confidence: High

 - Location: data/github/GitHubClient.kt:130-139

 Issue. detectHugo loops the eight entries of HUGO_CONFIG_CANDIDATES and
 issues a getContents request per candidate, stopping at the first hit. A repo
 with hugo.toml costs one call; a repo with config.yaml costs six; a repo with
 no Hugo config at all costs eight sequential round trips before the Repo screen
 can say "Not detected" — and eight rate-limit units.

 Recommended fix. One listDirectory(authorization, owner, repo, "", branch)
 call on the repo root, then intersect the returned file names with
 HUGO_CONFIG_CANDIDATES in the order the list already declares. getPostsFromDirectory
 (GitHubClient.kt:172-188) already shows the call shape and response type. One
 request, always, and it preserves the candidate-precedence the current loop encodes.

 Risk of not fixing. The "Save connection" spinner on the Repo screen is
 slowest in exactly the case where the answer is a negative — the case a user is
 most likely to retry.

 ---

 5. Cover-art bitmaps are rasterised on the main thread during composition, with no cache across item recycling — Impact: Medium-High, Confidence: Medium

 - Location: coverart/CoverArt.kt:30-32, coverart/CoverArtRenderer.kt:155-173;
 used from Rows.kt:75,153, MediaScreen.kt:102, PostDetailsSheet.kt:223

 Issue. Two problems in one line:

 val image = remember(seed, size, palette) { CoverArtRenderer.render(seed, size, palette).asImageBitmap() }

 1. remember is not a cache here. It is scoped to the composable instance, and
 LazyColumn/LazyVerticalGrid dispose items that scroll off-screen. Every row
 that scrolls back into view re-rasterises from scratch. The KDoc at
 CoverArt.kt:18-20 — "cached against seed, size and palette, so scrolling a
 library of posts does not re-render anything" — does not hold for lazy layouts.
 2. It runs on the composition (main) thread. render allocates an
 ARGB_8888 bitmap, runs the canvas draws, then applyGrain does
 getPixels into an IntArray(w*h), a per-pixel Kotlin loop calling
 Color.argb/red/green/blue, and setPixels back. Thumbnail is 14,400
 pixels (57.6KB bitmap + a same-size transient IntArray); Hero is 129,600 pixels
 (~518KB + ~518KB transient). MediaScreen composes a 3-wide grid of them.

 Recommended fix.
 - Add a module-level LruCache<CoverKey, ImageBitmap> in :coverart, keyed on
 (seed, width, height, palette) and sized in bytes (sizeOf = byteCount), so
 scroll-back is a map hit. Cap it at a few MB.
 - On a miss, render via produceState/LaunchedEffect on Dispatchers.Default
 and draw a flat paperSunk placeholder for the frame or two before it lands, so
 composition never rasterises.
 - Cheap win inside applyGrain: skip the getPixels/setPixels round trip by
 having render keep the IntArray it drew into, or apply grain via a
 pre-baked tiling ImageShader the way Grain.kt already does for paper.

 Uncertainty. I have not measured a single render call. The KDoc's claim that
 a thumbnail is "well under a frame" is plausible; whether the aggregate during a
 fling causes dropped frames needs a trace. The missing cache is certain regardless.

 ---

 6. Library sorting re-parses frontmatter O(n log n) times, on every recomposition — Impact: Medium, Confidence: High

 - Location: BloggoApp.kt:346-350, model/Model.kt:294-295

 Issue.

 drafts = posts.filter { it.state == PostState.Draft }
   .sortedByDescending { it.lastEditedAtMillis() ?: 0L },

 Three compounding problems:

 1. Post.lastEditedAtMillis() falls back to markdown.parseFrontmatter() for any
 post with a null updatedAt — which is every post fetched from the repo,
 since PostLibraryRepository.toPost() (:72-80) never sets it. So the selector
 is a full-document parse.
 2. sortedByDescending is sortedWith(compareByDescending(selector)), and that
 comparator invokes the selector on each comparison — O(n log n) selector
 calls, not O(n).
 3. Neither list is inside a remember, so both recompute on every recomposition of
 BloggoApp. The file's own comment at :236-239 notes that scope is invalidated
 by every editor keystroke.

 Recommended fix. PostCacheEntity.date is already parsed to a display string in
 formatPostDate (PostLibraryRepository.kt:88). Parse it to epoch millis in the
 same place and store it on Post as a real field, so lastEditedAtMillis()
 becomes updatedAt ?: dateMillis — a field read. Then wrap both lists in a
 remember(posts.toList()). Finding #1's parseFrontmatter fix reduces the damage
 but does not remove the O(n log n).

 Risk of not fixing. Grows super-linearly with post count while the Library tab
 is visible.

 ---

 7. FocusScreen recompiles two regexes per keystroke, and resets the caret to the end of the document — Impact: Medium, Confidence: High

 - Location: ui/focus/FocusScreen.kt:61-71, reached from BloggoApp.kt:492-504

 Issue. Two constant patterns are constructed inside remember blocks:

 val frontmatter = remember(markdown) { Regex("^(?:---[\\s\\S]*?...").find(markdown)?.value.orEmpty() }
 val words = remember(value.text) { Regex("[A-Za-z0-9'’]+").findAll(value.text).count() }

 Regex(...) compiles a Pattern every time the remember key changes — i.e. per
 keystroke. The first pattern is character-for-character identical to
 Model.kt:100's frontmatterRegex, which is already a top-level val.

 The larger problem is the key. markdown comes from
 postBySlug(route.slug).markdown (BloggoApp.kt:493), which onMarkdownChange
 rewrites on every keystroke. So var value by remember(markdown) re-keys every
 keystroke and rebuilds TextFieldValue(body, TextRange(body.length)) — the caret
 jumps to the end of the document on every character typed anywhere but the end.
 That is a correctness bug surfaced by the performance read, not just a slow path.

 Recommended fix. Hoist both patterns to top-level vals (reuse Model.kt's
 frontmatterRegex and markdownWordCount() rather than duplicating a third word
 definition — the KDoc at Model.kt:104-105 says that is the point of sharing
 them). Key the editing state on post identity rather than content: pass the slug
 into FocusScreen and use remember(slug), exactly as EditorScreen.kt:78 does.

 Risk of not fixing. Focus mode is unusable for editing anything but the tail of
 a document, which defeats the mode.

 ---

 8. ArticleParser compiles five throwaway regexes per line and per block — Impact: Medium, Confidence: High

 - Location: ui/preview/ArticleParser.kt:114, :149-153

 Issue. Lines 36-42 correctly hoist seven patterns to private vals. Five more
 were missed:

 - :114 — trimmed.matches(Regex("^-{3,}$")) compiles a fresh Regex for every
 body line of every article parsed.
 - :149-153 — inline() compiles four fresh Regex objects on every call,
 and it is called once per paragraph, heading, quote, list item and callout. A
 150-block article compiles roughly 600 throwaway Patterns.

 Separately, :108/:110, :119/:121, :129/:131 and :135/:137 each run
 matches() and then find() with the same anchored pattern — every matching line
 is scanned twice.

 Recommended fix. Hoist the five patterns up beside the existing ones. Replace
 each matches() + find()!! pair with a single find() and a null check —
 which also removes five !!s.

 Risk of not fixing. Read mode's first frame after tapping "Read" is slower than
 it needs to be, worst on long posts. Purely mechanical to fix.

 ---

 9. The post cache is unbounded and holds every post's full markdown in memory — Impact: Medium, Confidence: High on mechanism, Medium on whether it bites

 - Location: data/library/PostCache.kt:22-49, PostLibraryRepository.kt:37,52-53,72-80

 Issue.
 - PostCacheEntity.markdown stores the entire file, and getAll()
 (PostCache.kt:35) selects every column of every row — the whole corpus into a
 List — on each refresh, purely to compare blob SHAs.
 - Every returned Post then lives in BloggoApp's posts SnapshotStateList for
 the process lifetime, full markdown included.
 - toPost() (:77) calls markdownWordCount() — the three-pass scan from
 finding #1 — for every post on every refresh, including the unchanged
 ones whose count cannot have changed.
 - deleteExcept(keepPaths: List<String>) (PostCache.kt:41) binds one SQLite
 parameter per path. SQLITE_MAX_VARIABLE_NUMBER is 999 on many Android builds,
 so a repo past that many posts throws rather than degrading.

 Recommended fix. Add a projection query returning only
 path, blobSha for the SHA comparison, and a second returning the library-row
 columns without markdown; fetch markdown by path only when the Editor or
 Preview actually opens. Persist wordCount on the cache row so it is computed once
 at fetch time. Chunk deleteExcept into batches of ~900, or invert it to a
 delete-by-diff against the fresh path set.

 Uncertainty. A 200-post blog at 8KB per post is ~1.6MB — real but survivable.
 This is a "correct before it is urgent" item; the SQLite variable limit is the part
 that fails outright rather than degrading.

 ---

 10. Route.Preview carries a full Post snapshot in the back stack — Impact: Low, Confidence: High

 - Location: BloggoApp.kt:83, used at :481-490

 Issue. data class Preview(val post: Post, val published: Boolean) retains a
 copy of the whole document per back-stack entry, and PreviewScreen reads
 route.post.markdown — a snapshot taken at navigation time. Route.Editor and
 Route.Focus both correctly carry only a slug.

 Recommended fix. Change it to data class Preview(val slug: String, val published: Boolean)
 and resolve through the existing postBySlug helper (:221), matching the other
 two routes.

 Risk of not fixing. Minor memory retention, plus Preview can render stale
 content if the post changes after navigation. The replace = true behaviour keeps
 the stack short enough that memory alone would not justify the change.

 ---

 11. Modifier.paperGrain() uses the deprecated composed {} factory — Impact: Low, Confidence: High

 - Location: designsystem/Grain.kt:53-65, applied at BloggoApp.kt:335

 Issue. composed opts a modifier out of Compose's modifier reuse and skipping.
 It is applied exactly once here, high in the tree, so the runtime cost is
 negligible — but it is the only composed in the codebase and it is deprecated in
 favour of Modifier.Node. Separately, buildNoiseTile runs a 19,600-iteration
 loop and allocates a 78KB bitmap on the main thread during first composition, and
 again on every light/dark flip.

 Recommended fix. Rewrite as a DrawModifierNode via ModifierNodeElement, and
 hoist the two tiles (light, dark) into module-level by lazy values so toggling
 the theme reuses them instead of rebuilding.

 ---

 Deferred: build, startup and APK size (not in this plan)

 Scoped out by decision — recorded here so they are not rediscovered later. Each is
 its own piece of work with its own verification cycle.

 - Release builds ship unminified and unshrunk. app/build.gradle.kts:20-24:
 isMinifyEnabled = false, no isShrinkResources, and the referenced
 proguard-rules.pro does not exist on disk. Enabling R8 needs keep rules for the
 Retrofit interface and Room entity, plus verification on a real release install.
 - No baseline profile. No androidx.profileinstaller, no baselineProfile
 block, no benchmark module. This is the largest cold-start and first-scroll win
 available to a Compose app, and it is also the harness that would let findings #1
 and #5 be measured rather than reasoned about. Worth revisiting first if the
 appetite for build work returns.
 - Gradle flags. gradle.properties has none of org.gradle.parallel,
 org.gradle.caching, org.gradle.configuration-cache, and -Xmx2048m is low
 for a KSP + Room + Compose-compiler build. (Credit where due: Room already uses
 KSP, not kapt.)
 - Fonts total ~2.3MB uncompressed. archivo_variable.ttf 658KB,
 newsreader_italic_variable.ttf 496KB, newsreader_variable.ttf 452KB, four IBM
 Plex Mono statics 556KB combined. Latin subsetting typically cuts variable fonts
 60-80%; BloggoFonts.Mono also ships four statics where one variable file would
 do. APK size only.
 - Template leftover. The manifest still declares
 android:theme="@style/Theme.MyApplication" (AndroidManifest.xml:13).

 ---

 Out of scope / needs more data

 Named rather than guessed at:

 - Whether any per-keystroke work is actually perceptible. Finding #1 is
 arithmetically certain; the frame budget it consumes is not measured. Needs a
 Macrobenchmark FrameTimingMetric or a Perfetto capture while typing into a
 realistic 5,000-word post — and the benchmark module that would provide it sits
 in the deferred bucket. Until it exists, treat #1 and #5 as sound reasoning
 rather than measured wins.
 - Recomposition counts. No Compose compiler metrics have been generated. The
 claims here about what recomposes on each keystroke come from reading the
 SnapshotStateList reads in BloggoApp's scope, not from a report. Run with
 -Pandroidx.compose.compiler.plugins.kotlin.reportsDestination=... to get the
 restartable/skippable and parameter-stability breakdown before acting on
 anything stability-related.
 - Cover-art render time per bitmap. Finding #5's missing cache is certain; the
 per-render cost is not. Needs a microbenchmark at Thumbnail and Hero sizes.
 - Dead code and unused resources. ./gradlew lintRelease was not run —
 plan mode is read-only and no build was executed. Worth running before acting on
 anything size-related.
 - Real-scale behaviour of the Room cache. Finding #9's limits need an actual
 repo with several hundred posts connected to observe.

 ---

 Suggested implementation order

 Low-risk and high-value first; each numbered group is its own commit.

 1. #8 + #7 — regex hoisting and the Focus-mode caret. Purely mechanical, and
 ArticleParserTest / MarkdownHighlighterTest already cover the parsing
 behaviour that must not change. #7 also fixes the caret reset: add a test for
 typing mid-document in Focus mode, modelled on EditorScreenRapidInputTest.
 2. #1 — frontmatterFence/parseFrontmatter rewrite, plus dropping the
 duplicate word count. Highest value in the list, and the one that stops this
 class of bug recurring. ModelTest covers YAML/TOML fences, missing frontmatter
 and unterminated blocks; keep it green and add a case for a body containing a
 --- horizontal rule.
 3. #2 — getToken() off the main thread. Small and self-contained.
 4. #6 — precomputed sort keys, and remember the two library lists. Land after
 #1; the Post.dateMillis field it adds is populated in the same
 PostLibraryRepository.toPost() that #1 touches.
 5. #3 + #4 — parallel library refresh, single-call Hugo detection.
 6. #5 — cover-art LruCache, then the off-thread render. Do the cache first
 and look at it before adding the async path; the cache alone may be enough.
 7. #10 + #11 — Preview route carries a slug, grain becomes a Modifier.Node.
 Tidy-ups, and #10 removes the stale-Preview bug.
 8. #9 — Room cache projections, persisted wordCount, chunked deleteExcept.
 Last because it is the least urgent and the most invasive to the data layer.

 ---

 Verification

 - Unit tests, per step: cd android && ./gradlew :app:testDebugUnitTest :designsystem:testDebugUnitTest :coverart:testDebugUnitTest. Existing coverage
 is genuinely useful here — ModelTest, ArticleParserTest,
 MarkdownHighlighterTest, PostLibraryRepositoryTest, GitHubClientTest,
 MarkdownActionTest, CoverArtPlannerTest, MulberryRngTest.
 - Instrumented: ./gradlew :app:connectedDebugAndroidTest.
 EditorScreenRapidInputTest is exactly the regression guard for finding #1 —
 make sure it still passes and consider extending it to Focus mode for #7.
 - Determinism guard for #5: CoverArtPlannerTest and MulberryRngTest pin the
 RNG draw order. Any change to applyGrain must keep them green — the cover for a
 given slug has to stay byte-identical, which the comments in
 MulberryRng.kt:30-45 explain at length. This is the finding most likely to
 break something silently; the LruCache step alone cannot, which is the argument
 for doing it first and stopping there if it is enough.
 - Main-thread I/O (#2): enable StrictMode detectDiskReads() +
 penaltyLog() in MainActivity for the debug variant, open the Repo tab, and
 confirm the log is clean.
 - Network call counts (#3, #4): assert MockWebServer.requestCount in
 GitHubClientTest and PostLibraryRepositoryTest.
 - End to end: connect a real repo on the Repo screen, refresh the library, open
 a long post, type into it, switch to Read, then Focus. That is the path every
 finding above sits on.
