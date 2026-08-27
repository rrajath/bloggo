# Progress

## Milestone 1 — prototype revisions (done, 2026-08-17)

- [x] Cover art on/off toggle in the rail, driven by `data-art` on the root
- [x] Artwork-free hero and rows: type-led, amber rule marks the in-progress draft
- [x] Removed the word-goal bar from the library hero
- [x] Switched the whole prototype from Astro to Hugo (paths, config, taxonomies)
- [x] Hugo shortcodes replace Astro components in the insert sheet
- [x] Shortcodes render as blocks in Preview instead of printing raw syntax
- [x] Framework row on the Repo screen is a real button, opens a detection sheet
- [x] Live page access: preview permalink bar, library row button, repo site cell
- [x] Preview distinguishes an unpublished draft from a live post
- [x] Typewriter sightline in focus mode actually locks the active line to it
- [x] Share card screen replaced with a multi-account Mastodon composer
- [x] Per-instance character limits, per-account text, visibility per account
- [x] jsdom smoke pass: 61 assertions, all green

## Milestone 2 — Android foundation (done, 2026-08-18)

### Cover art generator (`:coverart`)
- [x] `paint()` ported to Kotlin, split into a pure planner and a rasteriser
- [x] Golden tests against the JavaScript original's own output
- [x] Reproduced the JS seed-multiply precision loss, which would otherwise have
      silently changed every slug-seeded cover
- [x] FNV-1a slug seeding, stable across platforms; shuffle by variant counter
- [x] Compose bindings with per-seed caching; thumbnail, hero and export sizes
- [x] PNG export at 1200×630 for the repo and Open Graph
- [x] 19 unit tests

### Design system (`:designsystem`)
- [x] Full token set, light and dark, both complete
- [x] Four OFL font families bundled with licences
- [x] 41 icons generated from the prototype's SVG paths, size-aware stroke
- [x] Paper grain as a tiling bitmap
- [x] 15 components with previews, including no-art variants
- [x] Markdown highlighting with identity offset mapping
- [x] 24 unit tests

### App (`:app`)
- [x] Navigation shell, tab bar, toast host
- [x] All eight prototype screens laid out against sample data
- [x] Editor with live markdown styling and testable formatting actions
- [x] Post details sheet with Generate and Shuffle cover
- [x] Mastodon composer with working per-instance limit logic
- [x] Builds, installs, and runs on an emulator with no crashes

### Documentation
- [x] `docs/DESIGN_SYSTEM.md`
- [x] `docs/ANDROID_TDD.md`
- [x] `README.md`

## Milestone 3 — Settings screen (done, 2026-08-18)

- [x] Settings screen: theme (Auto / Light / Dark), reachable from a cog icon
      next to Library's search icon
- [x] Theme choice persisted via Preferences DataStore, resolved against
      `isSystemInDarkTheme()` in `BloggoApp` and applied app-wide
- [x] GitHub PAT stays on the Repo screen, per the TDD's §7.1 — Settings does
      not duplicate it
- [x] Verified on an emulator: switching themes updates every screen
      immediately, and the choice survives a force-stop and relaunch

## Milestone 4 — Repo connection, storage half (done, 2026-08-18)

- [x] "GitHub connection" section on the Repo screen: repository, branch, and
      a fine-grained PAT field
- [x] PAT stored in `EncryptedSharedPreferences` (Keystore-backed); repository
      and branch in Preferences DataStore; the token itself never touches
      DataStore, Room, or a log line
- [x] Save clears the PAT field immediately and shows a "Token saved" chip
      plus a Clear action; nothing about the stored token is ever redisplayed
- [x] Verified on an emulator: values and the saved-token state survive a
      force-stop and relaunch; Clear removes the token and reverts the chip
- [x] `GET /repos/{o}/{r}` validation and Hugo detection: done in Milestone 6

## Milestone 5 — making the scaffold actually work (done, 2026-08-18)

The eight screens laid out in Milestone 2 looked right but shared one `draft`
variable underneath, so most navigation was cosmetic: every route showed the
same sample post regardless of what was tapped. This milestone replaced that
with real per-post state and fixed a crash and a rendering bug found while
doing it.

- [x] **Crash, every post preview**: `ArticleParser`'s shortcode regexes had
      unescaped `}` characters. Android's ICU regex engine (unlike desktop
      `java.util.regex`) rejects that at pattern-compile time, so the class's
      static init threw and `PreviewScreen` crashed on open, every time.
      Fixed, with a regression test.
- [x] **Multi-line blockquotes rendered as separate boxes** instead of one
      merged quote — only visible once real content existed to expose it.
      Fixed, with a regression test.
- [x] **"New post" opened the sample draft**, not a new one. `BloggoApp` now
      holds every post (draft, in review, published) in one list keyed by
      slug; compose creates a genuinely blank post and navigates to it.
- [x] **Every `Route.Editor(slug)` and `Route.Focus(slug)` ignored `slug`**
      and always showed/edited the same outer `draft` variable. Both now look
      the post up by slug and write edits back to that post only.
- [x] **Inbox fragments all opened the same draft.** Tapping a fragment now
      promotes it to its own new draft, seeded with that fragment's text, and
      removes it from the inbox.
- [x] **The inbox capture field had no submit action at all** — typed text
      went nowhere. Wired to the existing `onCapture` callback via the
      keyboard's Done action.
- [x] **"Open live" and "visit site" only showed a toast** claiming to open a
      URL. Now they actually launch the browser (`Intent.ACTION_VIEW`).
- [x] **`PreviewScreen`'s Edit button was wired but never rendered** — dead
      code. Added the button; it now opens the Editor for that exact post,
      including already-published ones.
- [x] **The editor's toolbar (Bold, Italic, Focus mode, Commit…) was hidden
      behind the keyboard.** `MainActivity` calls `enableEdgeToEdge()`, which
      stops the window from auto-resizing around the IME; nothing consumed
      the inset. Added `Modifier.imePadding()` once at the app shell so every
      screen benefits, not just the editor.
- [x] Sample published/in-review posts got real markdown bodies instead of
      empty strings, since Preview now shows a post's own content rather than
      silently falling back to the draft's.
- [x] Consolidated three duplicate "count words in markdown" regex blocks
      (editor toolbar, `BloggoApp`, `ArticleParser`) into one
      `String.markdownWordCount()`.
- [x] 10 new `ArticleParser` tests, including regressions for both bugs above.
      53 unit tests total, all passing.
- [x] Manually exercised on an emulator: every sample post opens without
      crashing and shows its own content; compose opens a blank post; inbox
      promotion opens a fragment-seeded draft and removes it from the list;
      Focus mode and the formatting toolbar target the right post; open-live
      links launch a real browser.

Known gap, not fixed here: several Repo-screen cells (framework detection,
frontmatter fields, taxonomies, default publish action, build status, offline
queue) still only show a toast. They're part of the publish/PR/outbox flow
that P0 items 4–7 build; making them real needs the Git Data API work below,
not a UI fix.

## Milestone 6 — GitHub connection, the network half (done, 2026-08-18)

The rest of P0 item 1: validating the stored PAT against GitHub and detecting
Hugo, per ANDROID_TDD.md §7.1 and §5.1.

- [x] `GitHubClient` (`data/github/`): Retrofit + OkHttp + kotlinx.serialization,
      one call to `GET /repos/{owner}/{repo}` to validate, one to
      `GET /repos/{owner}/{repo}/contents/hugo.toml` to detect the framework
- [x] Errors mapped to the categories ANDROID_TDD.md §5.5 asks for:
      unauthorized (401, or 403 without a rate-limit header), rate-limited
      (403 with `x-ratelimit-remaining: 0`), not-found (404 — GitHub's own way
      of not distinguishing "doesn't exist" from "no access"), no network
      (`IOException`), and a fallback for anything else
- [x] Saving on the Repo screen now triggers a real check; the result shows as
      a banner (connected · branch · public/private · Hugo detected or not,
      or the specific error) and the screen header reflects it
- [x] Debug-only request logging that redacts `Authorization`, via a
      debug/release Kotlin source-set split (`withDebugLogging()`) rather than
      a runtime `BuildConfig.DEBUG` check, since this module builds with
      `buildConfig = false`
- [x] Added `android.permission.INTERNET` (never needed before this) and
      flipped `android:allowBackup` to `false`, since a real secret is now
      genuinely stored — ANDROID_TDD.md §7.3 and the item this closes out in
      the "Later" list below
- [x] **Crash found and fixed while testing**: a token with a trailing
      newline (routine when pasted from a clipboard) reached OkHttp as a raw
      `Authorization` header value and threw `IllegalArgumentException`
      — OkHttp correctly rejects control characters in header values,
      the app just never sanitized the input first. Fixed by trimming in
      `RepoConnectionRepository.setToken` (so a dirty value is never stored)
      and again where the header is built (defense in depth).
- [x] Verified against the real GitHub API on an emulator: `octocat/Hello-World`
      (public, no token needed) validates correctly and reports "no hugo.toml
      found at the root"; a nonexistent repo reports "not found"; an invalid
      token reports "reconnect"; the previously-crashing newline-terminated
      token no longer crashes and is stored clean.
- [x] 10 `GitHubClient` unit tests against a `MockWebServer`, covering every
      branch in the error mapping above plus the newline-token regression.
      63 unit tests total, all passing.

Still sample data, deliberately: the "Sample repo" and "Detected from your
repo" sections below the connection banner. Wiring real detection into what
those sections *display* — replacing `SampleData.repo` — is Library/Repo UI
work for later, not part of this milestone.

## Milestone 7 — Library backed by the git trees API (done, 2026-08-18)

The rest of P0 item 2: real posts in the Library screen instead of only
`SampleData`, per ANDROID_TDD.md §5.2. First use of Room in the project.

- [x] Room + KSP wired into the project (`androidx.room` 2.8.4, KSP 2.3.11 —
      KSP's versioning decoupled from Kotlin's own at 2.3.0, so any KSP
      release from there on pairs with this project's Kotlin 2.3.20)
- [x] `GitHubClient.getPostsTree`: one recursive `GET .../git/trees/{branch}`
      call, filtered to markdown files under `content/posts/`; falls back to
      a plain directory listing when the response is `truncated`, the trap
      §5.2 calls out by name
- [x] `GitHubClient.getFileContent`: a post's raw markdown via the
      `application/vnd.github.raw` media type
- [x] The §5.5 error taxonomy refactored into a shared `GitHubApiError` +
      `classifyHttpError`, so `checkConnection` and the new tree/content
      calls classify the same response the same way — `ConnectionCheck`'s
      existing shape and tests were untouched
- [x] `PostCacheEntity`/`PostCacheDao`/`BloggoDatabase` (`data/library/`):
      caches `path -> (blobSha, title, date, draft, cover, markdown)`. An
      unchanged blob SHA — content-addressed proof the frontmatter hasn't
      changed — skips the re-fetch entirely; a post dropped from the repo is
      dropped from the cache on the next refresh
- [x] `PostLibraryRepository.refresh`: ties the tree call, the cache, and
      per-post content fetches together into a `List<Post>`. A per-file
      fetch failure keeps the stale cached entry rather than dropping the
      post from view
- [x] Shared frontmatter parsing: `String.parseFrontmatter()` extracted into
      `model/Model.kt` from what was inline logic in `ArticleParser`, now
      also quote-stripping values (`title: "A post"` → `A post`) — a real
      fix, not just a dedup, since `ArticleParser`'s own tests never
      exercised a quoted value
- [x] `BloggoApp` refreshes the library whenever a usable connection
      (repository + token) appears, merging results into the shared post
      list by slug — matching slugs get real content, new slugs get added,
      remote-sourced slugs no longer in the repo get dropped, local
      not-yet-committed posts are left alone. The Library header's
      repo/branch/Hugo line now reflects the real connection once one exists
      instead of always reading `SampleData.repo`
- [x] Scoped down per two decisions going in: PR listing (§5.1) is a
      separate follow-up, so "Open pull request" keeps showing the sample
      post until it exists; ETags/rate-limit headers (§5.4) are noted as an
      open follow-up rather than built here
- [x] 15 new unit tests (6 `GitHubClient`, 6 `PostLibraryRepository`, 3
      `Model.parseFrontmatter`) — 78 total, all passing
- [x] Verified against the real GitHub API on an emulator: the tree call
      fires with the exact URL §5.2 specifies, a bad token 401s without
      crashing and leaves the sample data in place untouched, and clearing
      the token correctly skips the tree call on the next save (connection
      check alone still runs)

## Milestone 8 — real-usage bug pass (done, 2026-08-18)

Milestone 7 shipped the git-trees library, but connecting a real repository
surfaced eight gaps a demo with sample data never would have. All eight
reported in one pass, fixed together:

- [x] **No way to sync on demand.** A refresh icon in the Library app bar and
      a pull-to-refresh gesture, both calling the same `refreshLibrary()` now
      shared with the auto-refresh effect. Refreshing with no repository or
      token connected shows a toast instead of doing nothing silently.
- [x] **"No hugo.toml found" on a real Hugo site.** `detectHugo` checked one
      filename; Hugo has shipped `config.toml`/`.yaml`/`.json` for years
      before `hugo.toml` became the preferred name, and most existing sites
      still carry the old one. Now checks 8 candidate filenames in order and
      only reports "no Hugo config" once none of them exist.
- [x] **The PAT field went blank after saving**, indistinguishable from a
      token that never saved. It's now seeded from the stored token (masked)
      so the field always shows what's actually persisted, with an eye
      toggle to reveal it in plain text on demand. This is a deliberate
      reversal of Milestone 4's "never redisplayed" stance — the user asked
      for it directly — the token is still never logged and still lives only
      in `EncryptedSharedPreferences`.
- [x] **No way to delete a draft.** A "Delete draft" action in Post details
      (Draft-state posts only) with a confirm dialog. Scoped to local,
      not-yet-committed drafts — a draft that came from the real repo via
      the tree listing shows "not supported yet" instead of silently
      vanishing until the next refresh brings it back, since there's no
      delete-via-commit yet (that's write-loop work, not this pass).
- [x] **YAML-only frontmatter parsing.** Hugo supports `---`/YAML and
      `+++`/TOML frontmatter interchangeably, and both show up in the wild.
      `parseFrontmatter`, `markdownWordCount`'s frontmatter strip,
      `ArticleParser`, and Focus mode's frontmatter-hiding regex all now
      detect and handle either fence.
- [x] **Editor ↔ Preview toggling grew the back stack forever.** `go()`
      gained a `replace` parameter, used exactly where Edit/Read toggle
      between the same post's two views, so the stack never grows past one
      combined entry for that pair. Repeated toggling then hitting back now
      returns to Library in one step.
- [x] **Found while fixing the above: hardware/gesture back did nothing
      the app's own navigation.** There was no `BackHandler` at all — the
      in-app "‹" buttons called `back()` correctly, but the system back
      action fell straight through to finishing the Activity regardless of
      navigation depth. Added one `BackHandler(enabled = backStack.size > 1)`
      so hardware back and the in-app button now agree.
- [x] **Status bar icons were dark-on-dark in dark mode.** `enableEdgeToEdge()`
      only reacts to the *system* theme; it knew nothing about the in-app
      Auto/Light/Dark override. A `SideEffect` keyed on the resolved
      `darkTheme` now sets `isAppearanceLightStatusBars` to match whichever
      theme is actually in effect.
- [x] **The offline-queue banner was permanently stubbed on**, showing "2
      posts waiting" with no outbox to back it. Library now gets a real `0`
      until the outbox milestone gives it something true to report, per
      "shown only when it's actually true."
- [x] **New posts had bare-minimum frontmatter.** `title`/`draft` only. New
      posts (compose and inbox-promote) now also get `date` (current RFC3339
      timestamp), an empty `tags: []`, and a `slug:` field. That slug tracks
      the title live as you type — the way a CMS permalink field does —
      until it stops matching what auto-tracking would produce, at which
      point a manual edit is assumed and it's left alone.
- [x] Real date parsing gap caught along the way: a post's frontmatter
      `date:` is routinely a full RFC3339 timestamp, not a bare
      `yyyy-MM-dd` — that whole value was falling through to raw-string
      display instead of formatting as "Aug 4". Fixed by keying off the
      `yyyy-MM-dd` prefix every format shares rather than matching the whole
      string.
- [x] First instrumented Compose UI test in the project
      (`EditorScreenTest`, `testInstrumentationRunner` now configured):
      written after `adb shell input text` proved unreliable for verifying
      the live slug-sync by hand — it silently dropped a literal space
      mid-string during manual testing, which made a working feature look
      broken. Drives the real `BasicTextField` through Compose's test APIs
      instead of synthetic ADB key events. 2 tests, both passing.
- [x] 15 new unit tests (93 unit total) plus the 2 instrumented tests above
      — 95 tests overall, all passing.
- [x] Verified live against the user's real `rrajath/blog` repository on an
      emulator: real dates, refresh button and pull-to-refresh both trigger
      real fetches, PAT reveal toggle confirmed via content-desc state
      change (without displaying the real secret in a screenshot), delete
      flow confirmed end-to-end (dialog → removal → back to Library), and
      the Edit/Read/back-stack fix confirmed with both the in-app button and
      hardware back after multiple toggles.

## Milestone 9 — stubbed-data bug pass (done, 2026-08-18)

Another round of real-usage review, this time focused on screens that still
showed fixed sample values dressed up as real data. Nine issues, fixed
together:

- [x] **Post details sheet was entirely stubbed.** Title, slug, date,
      description, and tags were hardcoded regardless of the post actually
      open. Now every field reads `post.markdown.parseFrontmatter()` —
      title, slug, date, tags, and the "Keep as draft" switch's initial
      state all reflect the real frontmatter.
- [x] **Removed three elements that implied machinery the app doesn't
      have**: the "from archetypes/default.md" provenance chip, the
      Description field (added nothing — Hugo posts don't require one and
      the sheet never wrote it back), and the "· [taxonomies] in hugo.toml"
      suffix on the tags label. `docs/PROTOTYPE_NOTES.md`'s "Hugo, not
      Astro" section documented these as a deliberate prototype decision
      (showing frontmatter provenance rather than implying an invented
      schema); the user asked for them removed from the real app as it
      shipped, so the prototype rationale no longer describes current
      behavior for this sheet.
- [x] **The bottom sheet's Cancel/Save/Delete buttons skipped the close
      animation.** Each flipped the caller's `showDetails` state directly,
      unmounting `ModalBottomSheet` before its own hide animation could
      run — swipe-to-dismiss and scrim-tap animate fine since Material3
      drives those internally. Fixed with the hide-then-callback pattern
      from the official Material3 samples: `sheetState.hide()`, then the
      real callback once `isVisible` goes false.
- [x] **Preview screen's "rendered with your site theme" was never true.**
      The preview renders through `ArticleParser`'s own block renderer, not
      the site's Hugo theme. Removed.
- [x] **Preview's date was raw RFC3339** (`2026-08-18T11:01:19-07:00`).
      New shared `formatFrontmatterDate` in `model/Model.kt` renders it as
      "Aug 18, 2026 11:01", falling back to a date-only format for a plain
      `yyyy-MM-dd` value and to the raw string if neither parses.
- [x] **Preview's permalink used the wrong slug.** `Post.liveUrl` built the
      URL from `Post.slug` — this app's own internal identifier, unique
      per draft by a timestamp suffix (`untitled-1787...`) so it can key the
      in-memory post list — instead of the frontmatter `slug:` field Hugo
      actually uses for the permalink. Fixed to prefer the frontmatter slug,
      falling back to the internal one only if frontmatter has none.
- [x] **Preview's Edit/Read toggle turned into a different control.** The
      segmented control on the Editor screen became a plain pencil icon on
      Preview — the same conceptual toggle vanishing and reappearing as
      different chrome depending on which screen you're on. `EditorMode`
      is now shared between both screens; Preview shows the same segmented
      control, in the Read position.
- [x] **Editor header and Library's draft card both stuck on "Untitled."**
      `Post.title` was set once at creation and never refreshed — editing
      the frontmatter `title:` field changed what Preview showed (it always
      read frontmatter directly) but not `post.title` itself, which the
      Editor header and Library's hero card both bind to. `title` now
      updates alongside `wordCount` on every markdown change.
- [x] **Library's hero card showed "N unsaved."** That number was a
      per-keystroke edit counter, not a meaningful stat — removed, along
      with the now-dead `Post.unsavedChanges` field.
- [x] **Repo screen real-data pass**, after user Q&A on how to handle the
      pieces with no honest source yet:
      - The repo/site card is slimmed to repository + branch + site host —
        the fabricated "last push 2h ago · 3 unsynced" and "deployed 2h
        ago" claims are gone; there's no local/remote diffing or deploy
        integration to back them.
      - Added a "Site URL" field to the GitHub connection section,
        persisted in `RepoConnectionRepository` (plain DataStore, not the
        encrypted token store). There's no reliable way to detect a Hugo
        site's live host from the repo alone — GitHub Pages is the only
        host the API exposes, and most Hugo sites deploy elsewhere — so
        it's entered by hand and used everywhere a live URL is opened
        (Library, Preview, Repo's "Visit site").
      - The Posts/Drafts/Open-PR stat line now counts the real in-memory
        post list instead of a fixed "38 / 2 / 1".
      - Frontmatter fields and taxonomies are derived from posts actually
        fetched from the connected repo (union of frontmatter keys; array-
        valued fields as the taxonomy heuristic) instead of a hardcoded
        list claimed to come from `archetypes/default.md` and
        `[taxonomies]` in `hugo.toml` — neither file was ever actually
        fetched. Falls back to `SampleData.repo`'s lists only while
        nothing real has loaded.
      - `GitHubClient.detectHugo` now returns which config candidate
        matched (`hugo.toml`, `config.toml`, ...) instead of a bare
        boolean, so the "Detected from your repo" framework row can show
        the real file name.
      - Removed the "Offline queue" and "Build status" rows outright —
        no outbox and no deploy-status integration exist yet (§6, §5.1),
        so a fixed count and a fabricated "Cloudflare Pages · passing ·
        41s" were pure invention rather than degraded-but-honest data.
- [x] Verified the full module still builds and the existing unit test
      suite (`GitHubClientTest`, `ModelTest`, and the rest) passes
      unchanged; `GitHubClient.ConnectionCheck.Connected.hugoDetected`
      kept as a derived property over the new `hugoConfigFile` so existing
      call sites and tests didn't need touching.

## Milestone 10 — library sorting, editor input/perf, and a self-service Repo screen (done, 2026-08-18)

- [x] **Library posts weren't sorted.** Published posts now sort reverse-
      chronologically by `Post.lastEditedAtMillis()` (new: a real local-edit
      timestamp when there is one, else the frontmatter `date:` parsed to
      epoch millis — both new in `model/Model.kt`).
- [x] **Published dates dropped the year** ("Aug 4"). `formatPostDate`
      (library cache) and `SampleData`'s hardcoded display dates both now
      read "Aug 4, 2026".
- [x] **"In progress" assumed exactly one draft ever existed.** `currentDraftSlug`
      — a single tracked slug the app kept nudging around on every compose,
      promote, delete, and refresh — is gone. The hero card is now whichever
      draft was *actually* edited most recently (same `lastEditedAtMillis`
      sort as above), and every other draft appears in a new "Drafts" list
      underneath. `LibraryScreen.onOpenDraft` now takes the `Post` being
      opened instead of assuming there's only one.
- [x] **Editor's keyboard used default capitalization/autocorrect.**
      `BasicTextField` now sets `KeyboardCapitalization.Sentences` and
      `autoCorrectEnabled = true`.
- [x] **Typing and holding-delete in the editor lagged, worst in
      frontmatter.** Root cause: Milestone 9's new `repoConfig` derived
      "frontmatter fields" and "taxonomies" by calling `parseFrontmatter()`
      over *every* post's full markdown, recomputed on **every keystroke**
      of every edit anywhere in the app, because it lived as a plain `val`
      in `BloggoApp()`'s top-level scope — which recomposes in full on any
      `posts` mutation, editor keystrokes included. Fixed as a side effect
      of this milestone's Repo screen rework (below): that whole derivation
      is gone, not just relocated. What's left recomputing per keystroke
      (draft/published sort, `siteHost`, field-list parsing) is proportional
      to post count, not post *content*, and stayed fast in testing.
- [x] **Repo screen: frontmatter fields, post path, and image path are now
      real settings**, not detected/hardcoded text — persisted on
      `RepoConnection` (`RepoConnectionRepository`, plain DataStore, same as
      repository/branch/site host):
      - Frontmatter fields (default `title, date, tags, slug, draft`) now
        drive what a *new* post's frontmatter actually contains —
        `frontmatterFor()` (`BloggoApp.kt`) builds each line from this list,
        giving recognized keys (`title`, `date`, `slug`, `tags`, `draft`) a
        real default and anything else an empty line to fill in by hand.
        Edited via the row's "Edit" button, which was previously wired to
        nothing.
      - Post path (default `content/posts/{slug}.md`) and image path
        (default `static/images/`) are directly editable text fields. Image
        path is live: `PostDetailsSheet`'s cover Generate/Shuffle now builds
        both the repo-relative path (shown, and where a real upload would
        land) and the frontmatter `cover:` site path from this setting and
        the current year, instead of a hardcoded `/images/2026/...`.
      - Taxonomies row removed outright — it claimed to read
        `[taxonomies]` in `hugo.toml`, a file the app has never parsed.
      - The Hugo row's "Change" button, previously wired to nothing, now
        toggles the config-filename field into an editable text field
        (default `config.toml`). The framework name itself (Hugo / Not
        detected) is untouched — still the real detection result.
      - "Default action" is a real 3-way `SegmentedControl`
        (`PublishAction.CommitToMain` / `OpenPullRequest` / `AskEveryTime`,
        new enum in `RepoConnectionRepository.kt`) instead of a row that
        only toasted when tapped.
      - The "Sample repo" / "Repository" card (repo + branch + site cells)
        is gone — redundant with the connection section and the app bar's
        "connected · branch" subtitle. Its "visit site" button moved onto
        the connection section's Site URL field as a trailing icon button.
      - `RepoConfig` (the model type) and `SampleData.repo` are deleted —
        everything they described is now either a live `ConnectionCheck`
        result or a `RepoConnection` setting with its own default.
- [x] Verified the full module builds (`compileDebugKotlin`,
      `compileDebugAndroidTestKotlin`, `assembleDebug`) and the unit test
      suite passes, after updating three `PostLibraryRepositoryTest`
      assertions for the new "MMM d, yyyy" date format.

## Milestone 11 — real-usage bug pass on the editor and frontmatter sheet (done, 2026-08-18)

A fourth round of real-usage feedback, this time from actually writing in the
app rather than reading code. Four items, three fixed as bugs, the fourth
(commit/push toolbar button) confirmed as already-scoped future work rather
than a regression — it's the Milestone 12→ "Commit through the Git Data API"
item under "Next", not built yet, so its no-op is expected.

- [x] **Formatting toolbar overlapped the bottom gesture-navigation bar.**
      `EditorToolbar` in `ui/editor/EditorScreen.kt` had no bottom system-bar
      padding of its own — the shell's single `imePadding()` (§4.5) only
      covers the keyboard inset, not the nav-bar inset from
      `enableEdgeToEdge()`. Added `Modifier.navigationBarsPadding()` to the
      toolbar's own column, matching the pattern the tab bar already used in
      `BloggoApp.kt` for the same reason. Verified visually on
      `emulator-5554`: clean gap above the gesture bar, no overlap.
- [x] **The H2 toolbar button always inserted a fixed `##`**, regardless of
      the line's current heading level. `MarkdownAction.Heading` now points
      to a new `CycleHeading`, which reads the active line's existing
      `^#{1,6} ` marker (if any) and advances it: none → H1 → H2 → … → H6 →
      wraps back to H1. New table-driven tests in
      `ui/editor/MarkdownActionTest.kt` cover the full cycle including
      wrap-around, per the existing `MarkdownAction` testing pattern
      (§8.4/§12).
- [x] **Press-and-hold backspace stalled after a letter or two, and fast
      individual keystrokes dropped characters** — reported on a brand-new
      post's `Untitled` title field. Root cause was *not* Compose dropping
      input (confirmed via real back-to-back `KeyEvent`s with no idling: it
      never does) but per-keystroke cost: `EditorScreen`'s toolbar
      double-computed `markdownWordCount()`, `onValueChange` fired
      `onMarkdownChange` on pure cursor/selection moves as well as real
      edits, and — the dominant cost for a short/new post —
      `BloggoApp.kt` recomputed `drafts`/`inReviewPost`/`publishedPosts` as
      unconditional top-level `val`s on every keystroke of every edit
      anywhere in the app, across the *entire* library, regardless of which
      screen was visible. That's the same "expensive work recomputed in a
      hot recomposition path" shape Milestone 10 already fixed once for
      `repoConfig` — just not caught there for this call site. Moved those
      three lists into only the `Route.Library`/`Route.Repo` branches that
      use them, deduped the word-count calc, and gated `onMarkdownChange` on
      an actual text change. New instrumented
      `EditorScreenRapidInputTest.kt` fires real `KeyEvent`s back-to-back to
      assert no character is ever dropped under rapid typing or backspacing,
      and that selection-only changes don't fan out an edit callback.
      Flagged, not fixed here: on a very large document (~2,000 words),
      `BasicTextField`'s own full-document re-layout plus multi-span
      highlighting is a real remaining architectural cost (measured
      ~35-100ms/keystroke in that case) — the escape hatch §8.3/§14 already
      names (window the highlighter to the visible viewport, or migrate to
      `TextFieldState`) is a follow-up, not attempted here since it doesn't
      explain the reported bug on a short/new post.
- [x] **`PostDetailsSheet`'s "Keep as draft" toggle — and every other field
      in the sheet — was a no-op.** Root cause was bigger than the one
      switch the user noticed: `onSave: () -> Unit` took zero parameters, so
      title, slug, date, tags, and draft were all local composable state
      with no path back to the actual `Post`, regardless of which field you
      edited. Fixed with a real round trip: `SheetField` and the draft
      switch became controlled (value/`onValueChange`), `onSave` now carries
      `(title, slug, date, tags, draft)`, and a new
      `String.withFrontmatterEdits(FrontmatterEdits(...))` in `model/Model.kt`
      (generalized from the existing `replaceFrontmatterField`, the rewrite
      counterpart to `frontmatterFor`'s "build fresh" path) rewrites just
      those fields in place — preserving fence style (YAML/TOML), every
      other field, field order, and the body. `EditorScreen.kt` routes the
      rewritten markdown through the existing `onMarkdownChange` callback
      (the same pipe Milestone 9 already uses to keep `post.title`/
      `wordCount`/`updatedAt` in sync) rather than a second, divergence-prone
      path. Six new table-driven `ModelTest` cases cover
      `withFrontmatterEdits`. Verified live on `emulator-5554`: edited a
      real draft's title and toggled draft off in the sheet, saved, and
      confirmed both the raw markdown and the Library card reflected the
      change. Known minor gap: unescaped `"` inside a TOML title/slug value
      would produce invalid TOML — matches a pre-existing gap in
      `frontmatterFor`, not new here.
- [x] Full unit suite and the new instrumented tests all pass; no
      regressions in existing `EditorScreenTest`.

## Milestone 12 — editable tags in Post details (done, 2026-08-18)

- [x] Tag chips in `PostDetailsSheet`'s TAGS row are now editable, not
      read-only: each carries a remove `×` (new optional `onRemove` on
      `BloggoChip`, designsystem `Primitives.kt`), and a "+ add" affordance
      reveals an inline autocomplete panel.
- [x] New reusable `BloggoTagField` (designsystem `component/TagField.kt`)
      owns the chip row, the "+ add" toggle, and the panel: filtered
      suggestions from a caller-supplied pool, tapping one inserts it, and a
      query matching nothing offers "Create '…'" to add a genuinely new tag
      — consistent with this app's "no invented schema, only what's real"
      stance (Milestones 9–10), since here the writer is the one asking for
      the new tag, not the app inventing it.
- [x] The suggestion pool is the distinct union of `tags` already used
      across every post the writer has (`post.markdown.parseFrontmatter()`),
      not hugo.toml `[taxonomies]` — that concept was removed in Milestone
      10 for being unbacked. New `PostDetailsSheet` parameter `tagPool`,
      threaded through `EditorScreen`, computed once in `BloggoApp.kt`'s
      `Route.Editor` branch via `remember(route.slug)` — deliberately not a
      top-level val, for the same per-keystroke-recomputation reason
      `repoConfig` wasn't one (Milestone 10).
- [x] `PostDetailsSheet`'s tag state is now a `var` fed by the new field
      instead of a read-only `remember`; `onSave`'s `tags` list reflects
      whatever the writer added, removed, or created before tapping "Save
      details".
- [x] Verified end to end on `emulator-5554`: opened a real draft's details
      sheet, added an existing tag from another post via autocomplete,
      created a brand-new tag via "Create '…'", removed a tag, saved, and
      confirmed the persisted markdown's `tags:` frontmatter line matched
      the final chip list exactly; reopening the sheet reloaded the same
      chips from that markdown.
- [x] Compared the chip and "+ add" look against
      `design/bloggo-prototype.html`'s `.tag` / `.tag-add` CSS — tinted
      pill, 60%-opacity `×`, bordered add affordance. The autocomplete panel
      itself has no prototype counterpart (the prototype's `.tag-add` has no
      click handler at all), so its visuals were designed fresh, reusing
      only existing tokens (`paperSunk`, `ruleSoft`, `shapes.small`) rather
      than the prototype's dashed border, which isn't part of this app's
      existing border vocabulary.
- [x] `docs/DESIGN_SYSTEM.md` updated: `BloggoChip`'s new `onRemove` and the
      new `BloggoTagField` component are documented under Components.
- [x] Verified `compileDebugKotlin`, `compileDebugAndroidTestKotlin`,
      `testDebugUnitTest`, and `connectedDebugAndroidTest` (targeting
      `emulator-5554` specifically, since a physical device was also
      attached) all pass.

## Milestone 13 — performance pass from PERF_IMPROVEMENT.md (done, 2026-08-18)

Every finding in `PERF_IMPROVEMENT.md`'s runtime-hot-path scope, in the order
that file recommends. Build configuration, R8, baseline profiles and APK size
stay deferred there, untouched.

- [x] #8 + #7 — `ArticleParser` hoists the five per-line and per-call regexes it
      was still compiling fresh (a 150-block article was compiling ~600
      throwaway `Pattern`s), and each `matches()` + `find()!!` pair collapses
      into one `find()`, removing five `!!`s with it. Focus mode's two inline
      `Regex(...)` constructions are gone, replaced by the shared
      `frontmatterBlock()` and `markdownWordCount()` in `model/Model.kt`.
- [x] #7's real bug: Focus mode keyed its editing state on `markdown`, which
      `onMarkdownChange` rewrites per keystroke, so the caret was reset to the
      end of the document on every character. Keyed on the post's slug now,
      exactly as `EditorScreen` already was. New `FocusScreenCaretTest` guards
      it — confirmed failing against the old key before the fix.
- [x] #1 — `frontmatterFence()`/`parseFrontmatter()` walk the string with
      `indexOf('\n')` instead of splitting the whole document twice to read
      line 1. Every caller gets O(frontmatter) for free: `Post.liveUrl`,
      `syncSlugToTitle`, the editor's per-keystroke title read, and the tag
      pool. The duplicate `markdownWordCount()` the caller was recomputing is
      gone — `onMarkdownChange` is `(String, Int)` now and carries the count the
      editor already measured. `MarkdownTransformation` memoises its last
      styled source, and `MarkdownHighlighter` skips the `BooleanArray` and four
      regex passes for lines with no `` ` ``, `[` or `*`.
- [x] #2 — `RepoConnectionRepository.getToken()` is `suspend` on
      `Dispatchers.IO`. It was a plain function read during composition, so
      every recomposition of the shell while the Repo tab was open ran a
      Keystore unwrap and an `EncryptedSharedPreferences` decrypt on the main
      thread. The Repo screen reads state loaded in a `LaunchedEffect` instead.
- [x] #6 — `Post.dateMillis` is parsed once where the post is built, so
      `lastEditedAtMillis()` is a field read rather than a full document parse
      invoked once per sort *comparison*. Both library lists are `remember`ed
      against the post list.
- [x] #3 — library refresh fetches changed posts concurrently, gated at five in
      flight (OkHttp's own `maxRequestsPerHost`), and the tree call sends
      `If-None-Match` so an unchanged repo costs one `304`.
- [x] #4 — Hugo detection is one root listing intersected with the eight config
      candidates, not up to eight sequential probes.
- [x] #5 — `CoverArtCache`, an 8 MB byte-sized `LruCache` keyed on
      `(seed, width, height, palette)`; misses render on `Dispatchers.Default`
      behind a flat paper placeholder. `applyGrain` unpacks pixels by hand
      rather than through `Color.argb`/`red`/`green`/`blue` — same bytes, which
      `MulberryRngTest` and `CoverArtPlannerTest` still pin.
- [x] #10 — `Route.Preview` carries a slug, not a whole `Post`. That also fixes
      the stale-content bug: the snapshot was frozen at navigation time.
- [x] #11 — `Modifier.paperGrain()` is a `DrawModifierNode`, not the deprecated
      `composed {}`, and both noise tiles are module-level `by lazy` so a theme
      flip reuses them.
- [x] #9 (partly) — `PostCacheEntity.wordCount` is stored and migrated in
      (schema v2, `UNKNOWN_WORD_COUNT` backfilled from cached markdown without a
      re-fetch), and `deleteExcept` is chunked at 900 against the 999-variable
      SQLite limit that used to throw outright. **Not done:** loading markdown
      lazily on Editor/Preview open. The projection queries that pays for only
      help once `Post.markdown` stops being eagerly populated, which needs a
      load-on-open path through Editor, Preview and Focus — a data-layer change
      of its own, and the least urgent item in the audit.
- [x] Verified: `testDebugUnitTest` across all three modules (72 app tests, all
      green) and `connectedDebugAndroidTest` on `emulator-5554` (7 tests). New
      coverage: horizontal-rule-in-body and unterminated-block cases for
      `parseFrontmatter`, `frontmatterBlock`, `lastEditedAtMillis`, request
      counts for the parallel fetch and the `304`, one-call Hugo detection, and
      the Focus caret regression.
- [ ] Not verified: an end-to-end pass against a real connected repository, and
      the StrictMode `detectDiskReads()` check for #2 — both need a real PAT.

## Milestone 14 — Settings/Repo merge and body-matching search (done, 2026-08-18)

- [x] **Removed the cog icon from Library's app bar.** The standalone Settings
      destination it opened (theme only) is gone — `SettingsScreen.kt` and
      `ui/settings/` deleted outright, not left as dead code.
- [x] **Bottom nav's last tab is now "Settings", not "Repo".** Same
      destination (`RepoScreen.kt`, still the file/composable name since it's
      still mostly the GitHub connection screen), retitled "Settings" in its
      own app bar; the tab icon swapped from the branch glyph to
      `BloggoIcons.Settings`. `Route.Repo` renamed to `Route.Settings` (the old
      `Route.Settings` name was free since that screen is gone); every
      user-facing "Repo screen"/"Repo tab" string and code comment updated to
      say "Settings" for consistency, in `BloggoApp.kt`, `docs/ANDROID_TDD.md`,
      `docs/DESIGN_SYSTEM.md`, and a handful of other source comments.
- [x] **Theme (Auto/Light/Dark) moved into the Settings screen's existing
      "Appearance" section**, above Cover art and Reading typeface, as a new
      `SegmentedControl` row — same three-way control Milestone 3's
      `SettingsScreen` used, just relocated.
- [x] **Search results only ever showed the title**, even when the query only
      matched somewhere in the post body — no way to tell *why* a result
      surfaced. New `Post.matchingSnippet(query)` (`model/Model.kt`) returns
      the first body line (frontmatter excluded via a new private
      `String.bodyOnly()`) containing the query, and `LibraryScreen`'s search
      results show that line in place of the usual date/word-count meta text
      when it exists, falling back to the old meta line when the match was
      only in the title/slug/frontmatter.
- [x] 4 new `ModelTest` cases for `matchingSnippet` (title-only match, tag-only
      match, blank query, and the real body-match case) — full suite still
      green.
- [x] Verified `compileDebugKotlin` (`:app`, `:designsystem`) and
      `testDebugUnitTest` (`:app`) both pass. Not verified on an emulator —
      no device attached in this environment.

## Milestone 15 — direct-commit publish, real media, real preview rendering (done, 2026-08-18)

Full plan in `docs/WRITE_PUBLISH_MEDIA_PLAN.md`. Three gaps closed together
because a photo only has somewhere to come from (Media) and somewhere to go
(a real commit) once the write path exists, and Preview was only worth fixing
once there was something real to render.

- [x] **`GitHubClient.commitFiles`**: the real Git Data API sequence — one
      blob per file (`utf-8` for the post, `base64` for images), a tree
      layered on the branch's current base tree, a commit parented on the
      branch's head, then a ref update with `force = false` always. A
      rejected non-fast-forward (the branch moved since the read) maps to a
      new `GitHubApiError.Conflict`, never a silent overwrite. Always the
      branch `RepoConnection` has configured — never a literal `main`.
- [x] **`Post.repoPath`**, populated from `PostCacheEntity.path` (previously
      dropped on the floor) — once a post has a real repo path, publishing
      always writes back there instead of re-deriving one from the `{slug}`
      template, so a frontmatter slug edited after the first commit can't
      fork the post into a second file.
- [x] New `PostPublishRepository` + `PublishSheet` (editor's Commit button,
      previously `onToast("Queued...")`): commit message, a plain list of
      files about to change, Publish. **v1 scope, deliberately**: direct
      commit only, no "open a pull request" toggle, no line-level diff.
- [x] **Real preview rendering.** `ArticleParser`'s already-working
      `Figure`/`Video` parsing finally renders: new Coil dependency
      (`coil-compose` + `coil-network-okhttp`, reusing the existing OkHttp
      dep) loads a figure's image from wherever it actually lives — a local
      staged file with no network call, or the repo's authenticated
      raw-content endpoint otherwise, same `vnd.github.raw` header
      `getFileContent` already uses. Video only actually plays for a
      fully-qualified `http(s)://` source (`VideoView`) — uploading/recording
      video stayed out of scope, confirmed with the user directly.
- [x] **Media screen goes real.** New `GitHubClient.getImagesTree` (shares a
      refactored-out `fetchTree` with `getPostsTree` — same
      recursive-call-plus-truncated-tree-fallback shape, now proven twice)
      lists real images under the configured path; `MediaScreen` merges them
      with whatever's staged this session. Uploads (dropzone or the editor's
      new Insert row) downscale to a 2000px longest edge, copy out of the
      picked `content://` Uri once (it isn't guaranteed readable later), and
      **stage rather than commit immediately** — confirmed with the user —
      committing only once a post actually references them, bundled into
      that post's own commit.
- [x] **Insert -> Image** pushes `Route.Media` in a new picker mode
      (`returnToEditorSlug` set, not a tab visit) rather than a bare device
      picker, so an existing repo/staged image can be reused across posts —
      matches the HTML prototype's own `data-go="media"` design, confirmed
      with the user over the simpler alternative. New `InsertSheet` also
      wires up `MarkdownAction.CodeBlock`/`.Rule`, unused since they were
      added. The prototype's shortcode-detection row stays unbuilt.
- [x] Staged media is in-memory only (`BloggoApp`'s `stagedMedia`), swept
      from the cache dir on every cold start — matches the fact that a
      post's own in-progress edits aren't autosaved yet either (still open,
      below), rather than inventing a new, inconsistent durability guarantee.
- [x] 25 new tests: `GitHubClientTest` (the full commit sequence, branch
      passthrough, per-file encoding, the conflict mapping, `getImagesTree`
      and its truncated fallback), `PostPublishRepositoryTest`, and pure
      unit tests for `RepoPaths`/`stagedImagePath`. 115 `:app` tests total,
      all green.
- [x] Verified `compileDebugKotlin`/`compileDebugAndroidTestKotlin`
      (`:app`, `:designsystem`) and `assembleDebug` all pass. **Not
      verified**: an emulator/device pass, or a real commit against a real
      connected repository — no device in this environment.

## Milestone 16 — Pages tab replaces Media in the HTML prototype (done, 2026-08-19)

`design/bloggo-prototype.html` only — the Android app (`android/`) is untouched
by this milestone. Full rationale in `docs/PROTOTYPE_NOTES.md` under "Pages
replaced Media".

- [x] Media tab/screen removed from the prototype. Attaching media to a post
      still works exactly as before, through the toolbar's `+` (Insert sheet)
      and the frontmatter sheet's cover-image field — those no longer route to
      a media-library screen (there isn't one to route to now), they toast the
      resize/commit behavior directly.
- [x] New Pages tab: reads top-level `content/*.md` files (`about.md`,
      `uses.md`, `now.md`, `contact.md` as the illustrative set) as opposed to
      `content/posts/`. Rows open the same Editor/Preview screens a post does.
- [x] "New page" affordance writes to `content/{slug}.md` at the top level,
      never `content/posts/`.
- [x] Commit/Publish sheet is kind-aware: `content/{slug}.md` +
      `page/{slug}` branch + no offline-queue banner + no cover-image diff
      line for a page, vs. the existing `content/posts/{slug}.md` + `post/{slug}`
      + offline queue for the post. Frontmatter sheet drops Reading time,
      Tags, and Draft for a page. Editor header shows a neutral "Page" chip
      instead of Draft/Live.
- [x] jsdom smoke pass (34 assertions) covering: tab bar/nav rail no longer
      reference `media`, Pages rows render from the top-level files, opening a
      page loads it into the shared Editor/Preview/commit/frontmatter state,
      switching back to the post restores its own state with no bleed between
      the two, and a brand-new page diffs as a new file.
- [ ] **Divergence to be aware of, not a bug**: Milestone 15 (`Insert ->
      Image`) deliberately matched the Android app's real, repo-aware media
      picker to the prototype's old `data-go="media"` design. The prototype no
      longer has that screen to route to, so its Insert/Cover-image actions
      are now a plain toast instead of a picker. The Android app's behavior is
      unchanged and is still correct — it has a real Media screen backed by
      real repo images, which is strictly more capable than anything the
      static prototype could demonstrate. Nothing to fix here; noted so the
      two aren't compared 1:1 later.

## Milestone 17 — Pages tab in the Android app, native share, editor toolbar tweak (done, 2026-08-19)

Brings Milestone 16's Pages tab into the real Android app (the prototype-only
milestone deliberately left `android/` untouched), plus two smaller,
unrelated UI requests bundled into the same pass: the editor toolbar's
heading button and Preview's Share button.

- [x] **Pages tab replaces Media in the bottom nav.** `Route.Pages` (new) +
      `PagesScreen.kt` (new); `Route.Media` still exists and still works, but
      only as a push destination from the editor's Insert -> Image row —
      never a tab any more, so nothing about Milestone 15's real media picker
      changed.
- [x] **Pages pulled from the real repo**: `GitHubClient.getPagesTree` (one
      recursive tree call, same shape as `getPostsTree`, filtered to `.md`
      files that are direct children of `content/` — excludes
      `content/posts/` and any other subdirectory by construction, not by
      name-matching "posts") + `PageLibraryRepository` + a new `page_cache`
      Room table (`PageCacheEntity`/`PageCacheDao`, migration 2→3) — the same
      cache-by-blob-SHA shape `PostLibraryRepository` already uses.
- [x] `Post` gained a `kind: DocKind` field (`Post`/`Page`, default `Post`) —
      posts and pages live in the same in-memory list and share the Editor,
      Preview, Publish sheet, and details sheet, exactly as
      `docs/PROTOTYPE_NOTES.md`'s "Pages replaced Media" describes; `kind` is
      what those shared call sites branch on.
- [x] **Already-pushed opens in Preview, not-yet-pushed opens in Editor** —
      for a page this is keyed on `Post.repoPath != null` (a page has no
      draft flag the way a post does), not on `Post.state`.
- [x] **`lastmod:` stamped on every edit to a page.** Reuses the editor's
      existing debounced settle-after-typing effect (the same one that
      re-syncs a post's `slug:` to its `title:`) rather than a rewrite per
      keystroke — `String.withUpdatedLastmod` (new, `model/Model.kt`) upserts
      it whenever the settled document has actually moved on from what was
      last stamped. Also stamped on a Page details sheet save
      (`String.withPageFrontmatterEdits`, new).
- [x] Page details sheet: Tags, "Keep as draft", and Cover are hidden for a
      page (`PostDetailsSheet.kt`, gated on `post.kind`); sheet title reads
      "Page details"; Title/Slug/Date stay editable. Editor header shows a
      neutral "Page" chip instead of Draft/In review/Live.
      `PostPublishRepository`/`PublishSheet` resolve a page's commit path to
      `content/{slug}.md` (`resolvePagePath`, new — not user-configurable,
      unlike a post's path template) and default the commit message to "Add
      page: …" / "Update page: …".
- [x] **Share button opens the native Android share sheet** instead of the
      in-app Mastodon composer, for both a post and a page, from Preview.
      Post text: title, blank line, URL. Page text: `<title> · <author>`
      (falling back to just the title when no author name is set), blank
      line, URL — sourced from a new "Author name" field on the Settings
      screen (`RepoConnection.authorName`), since nothing in the app or the
      repo (frontmatter, `config.toml`) already carries this. Confirmed with
      the user: `config.toml`'s `[params.author].name` was considered and
      explicitly declined in favour of the simpler Settings field.
  - **Open item, not a bug**: `Route.Mastodon`/`MastodonScreen.kt` are no
    longer reachable from anywhere in the app — Share was their only entry
    point. Kept in place rather than deleted, per the user's explicit call;
    revisit if the Mastodon composer should get a new entry point or should
    be removed outright.
- [x] Editor toolbar's heading button now shows `#` instead of `H2` — same
      style, same `MarkdownAction.CycleHeading` behavior (Milestone 11),
      just the label text.
- [x] `docs/DESIGN_SYSTEM.md` untouched: nothing new was added to
      `designsystem/component/` — `PagesScreen` composes existing components
      (`Cell`, `BloggoAppBar`, `Eyebrow`) rather than a new one.
      `README.md`'s feature list updated (Pages tab, the native-share
      behavior, Mastodon composer moved to "Deferred").
- [x] Verified: `:app:compileDebugKotlin`, `:designsystem:compileDebugKotlin`,
      `:app:compileDebugAndroidTestKotlin`, `:app:testDebugUnitTest`, and
      `:app:assembleDebug` all pass. **Not verified**: an emulator/device
      pass — no device in this environment, same gap prior milestones have
      flagged.

## Milestone 18 — real-device bug pass: publish 422, preview headings, Settings copy, stale PR stub (done, 2026-08-19)

Four bugs reported from an actual device with a real repo connected
(`rrajath/blog`), reproduced live over adb (uiautomator taps + logcat, since
no prior report pinned down a root cause) rather than guessed at from code
alone.

- [x] **Publish always failed with "Unexpected error."** Root cause: the
      `Json` instance in `GitHubClient.kt` never set `encodeDefaults = true`,
      so `TreeEntryInputDto.mode`/`.type` — both default-valued — were
      silently dropped from every `POST .../git/trees` body. GitHub's real
      response (captured by temporarily bumping the debug `HttpLoggingInterceptor`
      to `BODY` level, reverted after): `"Must supply a valid tree.mode"`,
      HTTP 422 — which `classifyHttpError` has no case for, so it fell through
      to `GitHubApiError.Unknown` → "unexpected error." This broke **every**
      publish, not just this device's. Fixed by adding `encodeDefaults = true`.
      Verified end to end: published a real test post to `rrajath/blog` and
      watched `git/trees` return 201 instead of 422.
- [x] **Preview headings only had two distinguishable sizes.** `PreviewScreen.kt`
      mapped `level <= 2 -> articleHeading else -> articleSubheading` — h1 and
      h2 looked identical (both got the smaller `articleHeading` style; h1
      never got `articleTitle` at all), and h3 through h6 were all identical
      too. Fixed to `1 -> articleTitle, 2 -> articleHeading, else -> articleSubheading`,
      matching `docs/DESIGN_SYSTEM.md`'s three defined heading styles (the
      prototype's `.read` CSS only styles h1/h2/h3 — no h4-h6 rules exist
      there either, so levels 3+ sharing one style matches the prototype,
      not a gap introduced here).
- [x] **Settings showed "not yet validated" as the app bar subtitle** any time
      `checkResult` wasn't `Connected` — including on a normal cold start
      before the connection check has re-run, which read as the app not
      recognizing an already-configured repo. Subtitle is now `null` (hidden)
      until a check actually succeeds, instead of a hardcoded placeholder string.
- [x] **A fake "Six months with a folding phone #42" open PR sat on the Library
      screen permanently**, even fully connected to a real repo. `SampleData.inReview`
      was deliberately excluded from the sample-cleanup slug sets because PR
      listing (`GET /pulls?state=open`, still in "Next" below) isn't wired up —
      there was never a real fetch to replace it with. Added a `LaunchedEffect`
      keyed on `repoConnection.repository`/`hasToken` that removes it outright
      once a real connection exists, so the placeholder only shows before a
      repo is configured, per the user's explicit call.
- [x] Verified live on an attached device (`adb`), not just compiled: all four
      fixes exercised through the actual UI after reinstalling — publish
      succeeded (real GitHub commit), preview showed three distinct heading
      sizes, Settings subtitle was blank pre-check, and Library's PR section
      no longer showed the stub once connected.
- [x] `docs/DESIGN_SYSTEM.md` untouched — the heading fix corrected a mapping
      bug against styles the doc already documents; no new tokens.
- [!] **Left-over real commit**: verifying the publish fix required actually
      publishing to `rrajath/blog` (branch `master`) — a real "Draft: Untitled"
      commit with a throwaway `content/posts/untitled-*.md` test post (six
      heading levels, no real content) landed on the user's live repo.
      Flagged to the user; not reverted here since that's a real git history
      change on their repo, not something to undo unasked.

## Next — P0, the write and publish loop

- [ ] PR listing (`GET /pulls?state=open`) for the Library screen's "Open pull request" section
- [ ] Editor autosave to Room
- [x] Commit through the Git Data API — direct-commit half done, Milestone 15 above; PR flow still open
- [ ] Diff against the remote blob, in the Publish sheet
- [ ] Offline outbox: Room table, WorkManager drain, conflict UI (a live conflict now surfaces as a plain error + Retry, not a merge tool)

## In progress — "Capture a thought" app shortcut

- [x] Static shortcut (`res/xml/shortcuts.xml`, `shortcutId="capture_a_thought"`)
      wired to `MainActivity` via `<meta-data android:name="android.app.shortcuts">`.
      Icon reuses the existing `BloggoIcons.Pen` path data as a vector drawable
      (`res/drawable/ic_shortcut_capture.xml`) rather than inventing new art.
- [x] `MainActivity` set to `launchMode="singleTask"` + `onNewIntent` so a
      warm-started app (already in the recent-tasks list) still receives the
      shortcut's intent instead of it being silently dropped, which is what
      plain "standard" launch mode does when the task already exists.
- [x] Custom action `dev.rrajath.bloggo.action.CAPTURE_THOUGHT` read in
      `BloggoApp(launchIntent)`; when present, the back stack seeds straight to
      `Route.Inbox` (no flash of Library first) and `InboxScreen` gets a
      one-shot `requestFocusOnOpen` that normal tab navigation never sets.
- [x] `compileDebugKotlin`, `compileDebugAndroidTestKotlin`, and
      `testDebugUnitTest` all pass. Added `InboxScreenFocusTest`
      (`app/src/androidTest/.../ui/inbox/`) covering `requestFocusOnOpen` —
      compiles, but instrumented tests need a device/emulator to run, which
      this environment doesn't have.
- [x] `docs/DESIGN_SYSTEM.md` §5 and `README.md`'s feature list updated for
      the new shortcut icon and feature.
- [ ] Not verified (no emulator/device in this environment): the shortcut
      actually appears on long-press, the icon renders as intended, the
      keyboard actually pops on cold start, and `InboxScreenFocusTest`
      actually passes when run. Static XML/Kotlin review + successful
      compile only.

## Milestone 19 — Inbox rework: FAB capture overlay, durable fragments, promote-not-auto-promote (done, 2026-08-19)

Closes out the "In progress — Capture a thought app shortcut" item above and
fixes the last known Inbox data-loss/UX gaps, mirroring the pattern
`LocalPostStore` already established for local posts/pages.

- [x] **Inline "capture a thought" text box replaced with a FAB + overlay.**
      `InboxScreen.kt` no longer has an always-visible composer; a
      `BloggoFab` (new `designsystem/component/Controls.kt` — 56 dp `ink`
      circle, `paper` icon, same visual language as `BloggoTabBar`'s raised
      compose button) opens a new `CaptureSheet` (`ui/inbox/CaptureSheet.kt`).
      Overlay is a `ModalBottomSheet` (same mechanism `PostDetailsSheet`
      already uses) with a ~30%-screen-height text area, `imePadding()` plus
      Material3's own IME-aware sheet behavior for keyboard avoidance, an
      auto-scroll-to-cursor effect, `Sentences`/autocorrect keyboard options
      matching the editor body field, and Save/Discard buttons — Discard
      only confirms (`AlertDialog`, styled like `PostDetailsSheet`'s delete
      dialog) when there's actually text to lose.
- [x] **List auto-continuation on Enter**, new `continueListOnEnter`
      (`ui/inbox/CaptureListContinuation.kt`), built on the same insertion-
      diff shape as `EditorScreen.kt`'s `capitalizeHeadingFirstLetter`: Enter
      on a non-empty bullet/numbered line continues the list; Enter on an
      *empty* marker line removes the marker instead of continuing (single-
      Enter list exit). 9 unit tests.
- [x] **Fragments are now durably persisted.** New `data/inbox/FragmentCache.kt`
      (`FragmentEntity`/`FragmentDao`) + `FragmentStore.kt`, the Inbox's
      counterpart to `LocalPostStore`/`LocalPostDao` — same restraint
      (`getAll`/`upsert`/`deleteById` only, no bulk-delete method exists at
      all). Added to the existing `BloggoDatabase` (`PostCache.kt`,
      migration 4→5). `BloggoApp.kt` wires `fragmentStore.save` into
      `onCapture` and `onMoveToInbox`, a startup `LaunchedEffect` merges
      `loadAll()` into `fragments` (add-or-replace, never remove, matching
      the `posts`/`LocalPostStore` restore), and `SampleData.fragments` no
      longer seeds the live list — a fresh install's Inbox starts genuinely
      empty. 4 unit tests (`FragmentStoreTest`, fake-DAO pattern matching
      `PostLibraryRepositoryTest`).
- [x] **Fixed: opening an Inbox item no longer promotes it.** Previously,
      tapping a fragment immediately called `posts.add(...)` /
      `fragments.remove(...)` before the Editor even opened — closing the
      Editor left the fragment gone from Inbox and sitting in Drafts
      regardless of what the writer actually wanted. `Route.Inbox`'s
      `onOpen` now only sets transient nav-scoped state
      (`transientFragmentSource`/`transientFragmentPost`, new in
      `BloggoApp.kt`); `postBySlug` falls back to it when a slug isn't in
      `posts`, and `updatePost` writes edits into it in place rather than
      `persistIfLocal`-ing anything, so Editor/Preview/Focus all keep
      working against a fragment preview with zero per-route special-casing.
- [x] **New "Promote to Post" button** in `PostDetailsSheet.kt`, gated on a
      new `isFragmentPreview` param (threaded through `EditorScreen.kt`,
      analogous to `isPushed`) — mutually exclusive with "Move to Inbox"
      and the delete button, both hidden for a fragment preview. Tapping it
      adds the post to `posts`, `persistIfLocal`s it, deletes the source
      fragment (`fragments.remove` + `fragmentStore.delete`), clears the
      transient state, and returns to Inbox via the existing `back()` +
      toast pattern `onMoveToInbox`/`onDeletePost` already use.
- [x] **Transient-state cleanup lives in `back()`, not composable disposal.**
      `EditorScreen`'s `onBack` and `FocusScreen`'s `onExit` are both
      literally `::back`, and Editor↔Preview/Editor↔Focus toggle via `go(...)`
      without ever calling it — a `DisposableEffect`-based first attempt at
      this cleanup fired (and crashed) on those toggles too, since Compose
      tears down the `Route.Editor` branch on *every* route change, not just
      a genuine "leave the fragment preview" one. `back()` now checks, after
      popping, whether the new stack top still shows the same previewed
      slug (Editor/Preview/Focus) before clearing — a genuine "closed the
      whole preview" pop clears; a Focus-mode round trip or an Edit/Read
      toggle doesn't.
      **Known minor gap**: switching tabs directly out of a fragment-preview
      Editor (rather than pressing back) bypasses `back()` entirely — the
      transient state is left set but unreachable (no route can display that
      slug again except by reopening the same fragment, which resets it
      anyway), so this is inert, not a correctness bug, just not swept
      immediately.
- [x] Commit (the toolbar's "Commit or open a pull request" action) is
      disabled for a fragment preview — `onToast("Promote to Post first")`
      instead of opening the Publish sheet — since publishing a post that
      was never added to `posts`/`LocalPostStore` would either crash Focus/
      Preview navigation for it later or orphan a real GitHub commit `posts`
      never heard about. Insert Image and Focus mode are left enabled: both
      already work correctly against a transient post via the `postBySlug`
      fallback and `stagedMedia`'s slug-keyed claiming, which survives
      promotion unchanged since the slug doesn't change.
- [x] `InboxScreenFocusTest.kt` updated for the new design: the shortcut
      case now asserts the overlay opens and focuses itself; the ordinary-
      navigation case asserts the capture field doesn't exist at all
      (rather than "exists but unfocused") when the overlay never opens.
      Two pre-existing, already-broken androidTest call sites
      (`EditorScreenTest.kt`, `EditorScreenRapidInputTest.kt` — missing
      `remoteSlugs`/`onMoveToInbox` from an earlier milestone that added
      them without updating these two files) fixed in passing since this
      milestone touches the same `EditorScreen` call sites anyway.
- [x] `docs/DESIGN_SYSTEM.md` Controls table gained `BloggoFab`.
      `README.md`'s feature list updated for the new capture overlay and
      promote-not-auto-promote behavior.
- [x] Verified: `:app:compileDebugKotlin`, `:designsystem:compileDebugKotlin`,
      `:app:compileDebugAndroidTestKotlin`, `:app:testDebugUnitTest` (140
      tests, all green — 13 new: 9 `CaptureListContinuationTest`, 4
      `FragmentStoreTest`), and `:app:assembleDebug` all pass. **Not
      verified**: an emulator/device pass — no device in this environment,
      same recurring gap prior milestones have flagged. In particular, the
      overlay's real keyboard-avoidance behavior and auto-scroll feel, and
      the actual shortcut → focused-overlay flow, are unverified beyond
      compile + the instrumented test's static assertions.

## Milestone 20 — capture/promote bug pass (done, 2026-08-20)

- [x] **Promoted-fragment title/slug bugs, one root cause.** `newPostFromFragment`
      (`BloggoApp.kt`) picked the fragment's first *sentence* (split on
      `[.!?]`) as `title:`, which for a fragment with no sentence-ending
      punctuation for a while meant several raw lines landing in what has to
      be a single-line frontmatter field — visually "way too long" in the
      Library, and worse, silently corrupting every downstream read of it:
      `parseFrontmatter` only reads up to the first newline of a `key: value`
      line, so the title it handed back (and the slug-sync baseline built
      from it in `PostDetailsSheet`/`EditorScreen`) diverged from the slug
      actually stored (derived from the *full* multi-line title), which
      permanently failed `syncSlugToTitle`'s "has the writer hand-edited the
      slug away from what the title would generate" guard — the slug then
      never followed the title again, no matter how many times it was
      retyped. Fixed at the source: title is now just the fragment's first
      non-blank line, trimmed and capped at 80 chars. Backstopped by
      `singleLine = true` on the Title/Slug/Date fields in
      `PostDetailsSheet.kt`'s `SheetField`, so a paste or Enter keypress
      can't reintroduce the same corruption by hand.
- [x] **Sample posts/pages no longer flash on cold start.** `posts` was
      seeded synchronously with `SampleData` at composable init, and only
      swapped for the real library once `refreshLibrary`/`refreshPages`'s
      network round trip resolved — so every launch briefly showed stub
      content before the real posts/pages replaced it. Added
      `PostLibraryRepository.loadCached()`/`PageLibraryRepository.loadCached()`,
      reading the last-synced Room cache with no network call, and a
      `LaunchedEffect(Unit)` in `BloggoApp.kt` (alongside the existing
      `localPostStore`/`fragmentStore` restores) that merges it into `posts`
      immediately — the same slug-keyed add-or-replace shape
      `refreshLibrary`/`refreshPages` already do post-network, just running
      off a local read that resolves in milliseconds instead of a live
      network fetch.
- [x] **Editing a capture and closing without promoting now keeps the edit.**
      `back()`'s fragment-preview cleanup discarded `transientFragmentPost`
      outright once every view of it was popped — by design, opening a
      fragment must never silently promote it, but that same code path was
      also throwing away in-place edits the writer had just made, reverting
      `fragments`/`FragmentStore` to the untouched pre-edit source. `back()`
      now diffs the transient post's body against the source fragment's text
      before clearing, and if it changed, writes it back via
      `fragments[index] = updated` + `fragmentStore.save(updated)` — still
      never adds it to `posts`/`LocalPostStore`, so it's still not a draft,
      just no longer a silent data-loss trap.
- [x] **Fragment preview in the Editor shows a "Capture" chip, not "Draft".**
      Editor's header chip only branched on `post.kind`/`post.state`, so a
      fragment preview (`isFragmentPreview`) — not a real draft, nothing
      persisted yet — showed the same "Draft · saved locally" a genuine
      draft gets, which was actively misleading about what tapping the
      inbox item had actually done. Added `ChipTone.Capture` to the design
      system (`accentTintStrong`/`accentBright` — a distinct, bolder blue
      from `PullRequest`'s `accentTint`/`accent`, so an open capture and an
      open pull request never read as the same state) and gated the header
      on `isFragmentPreview` first: "Capture" chip, "not yet promoted to a
      draft" subtitle.
- [x] **Captures can now be deleted from the post details bottom sheet.**
      `PostDetailsSheet`'s delete button/dialog was gated `!isFragmentPreview`
      off entirely — a stale gate from before captures were persisted via
      `FragmentStore`, per that param's doc comment. Added a
      `isFragmentPreview`-only "Delete capture" button (own confirm-dialog
      copy, no "also deletes from GitHub" branch since a capture is never
      pushed) and a new `onDeleteFragment` callback threaded
      `PostDetailsSheet` → `EditorScreen` → `BloggoApp`, wired next to
      `onPromoteToPost` using the same `transientFragmentSource` — local-only
      `fragments.remove` + `fragmentStore.delete`, no `GitHubClient` call.
      The existing "Delete draft"/"Delete page" button is unchanged and still
      only shows for a real, non-fragment-preview draft/page.
- [x] `docs/DESIGN_SYSTEM.md` Chip tone table gained `Capture`.
- [x] Verified: `:app:compileDebugKotlin`, `:designsystem:compileDebugKotlin`,
      and the existing `dev.rrajath.bloggo.data.inbox.*`/`dev.rrajath.bloggo.model.*`
      unit tests all pass. **Not verified**: an emulator/device pass — no
      device in this environment, same recurring gap prior milestones have
      flagged.

## Later

- [ ] P1: media, cover commit flow, pull requests, inbox, Mastodon OAuth
- [ ] P2: focus mode, voice capture
- [ ] **Focus mode toolbar button hidden/unreachable in the Editor.**
      2026-08-25: the `ToolbarIcon(BloggoIcons.FocusMode, ...)` entry in
      `EditorScreen.kt`'s formatting toolbar was gated behind `if (false)`
      (left in place, commented as intentionally dead, rather than deleted)
      so users can no longer reach it. As a result `ui/focus/FocusScreen.kt`
      and its androidTest `FocusScreenCaretTest.kt` are now unreferenced dead
      code. Future pass should either finish wiring Focus Mode back in (see
      P2 above) or remove `FocusScreen.kt`/`FocusScreenCaretTest.kt` and the
      `onFocus` plumbing entirely.
- [ ] Roborazzi screenshot tests against the prototype
- [ ] **Keep `docs/DESIGN_SYSTEM.md` in sync with `:designsystem`/`:coverart` automatically.**
      2026-08-24 audit found real drift: two typography styles
      (`articleSubheading`, `monoField`) existed in `BloggoTypography.kt` but
      were missing from the doc's table, and the state-color tint note ("10 to
      12%") was wrong for dark theme (~14%) — both fixed manually that session.
      Two options considered, not yet built:
      1. Cheap presence check (pre-commit hook or CI step): warn/block a commit
         that touches `android/designsystem/**` or `android/coverart/**` without
         also touching `docs/DESIGN_SYSTEM.md`. Catches "forgot to update the
         doc," not "updated it wrong."
      2. A real generator, following the `tools/icons.js`/`genicons.js`
         precedent: parse `BloggoColors.kt`/`BloggoTypography.kt` and
         mechanically regenerate the color/typography tables between marker
         comments, verified in CI by re-running and diffing. Would have caught
         both issues above. Higher build-out cost; only covers tabular
         sections, not the doc's prose (principles, texture rationale, the
         FNV-1a rounding hazard).
      Recommendation was to start with (1) — most drift is "forgot to open the
      file," not "typed the wrong number" — and escalate to (2) only if (1)
      keeps letting inaccuracies through. No CI workflow or git hooks exist in
      this repo yet, so either option needs that scaffolding first.
- [x] Set `android:allowBackup="false"` before any real token is stored — done
      in Milestone 6, alongside the GitHub client that made it necessary
