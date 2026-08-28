# Bloggo for Android — technical design document

Status: draft for review
Date: 2026-08-18
Reference UI: `internal/design/bloggo-prototype.html`
Design system: `internal/docs/DESIGN_SYSTEM.md`
Scaffold: `app/`, `designsystem/`, `coverart/` (Gradle modules at the repo root)

---

## 1. What this is

A phone app for writing and publishing posts to a git-backed Hugo blog, and
sharing them to several Mastodon accounts.

The premise worth stating plainly, because it shapes everything below: **the
phone never holds a git repository.** Every read and write goes through the
GitHub API. This buys a small, fast app with trivial auth, and it costs us a
local history, real merges, and any notion of "working tree". Sections 5 and 6
are mostly about paying that cost honestly.

### Success looks like

Someone drafts a post on a train with no signal, generates a cover for it, and
the moment they get a bar of reception it lands on `main` and the site rebuilds,
with no laptop involved.

### Explicit non-goals

- Not a git client. No branching UI beyond "open a pull request", no merge
  conflict resolution, no history browser, no rebase.
- Not a CMS. The repository is the source of truth and stays hand-editable.
- Not multi-site in v1. One repository, one blog.
- Not collaborative. Single author, single device at a time (see §6.4).

---

## 2. Decisions already taken

These were settled before writing and are inputs, not open questions.

| Decision | Choice | Consequence |
|---|---|---|
| Git access | GitHub REST + GraphQL, no local clone | §5 in full |
| GitHub auth | Fine-grained PAT, pasted by the user | §7.1 |
| UI toolkit | Jetpack Compose, Material 3 as substrate only | §4 |
| minSdk / compileSdk | 34 / 36 | Narrow device reach, accepted |
| Paper grain | Pre-baked tiling bitmap, not AGSL | §4.4 |
| Editor | Custom `VisualTransformation`, full fidelity | §8 |
| Read mode | Native Compose renderer | §9 |
| Cover art destination | Frontmatter `cover:` only, never the body | §10 |
| Fonts | Four OFL families bundled as assets | Design system §3 |
| Icons | Generated from the prototype's SVG paths | Design system §5 |
| P0 scope | The write-and-publish loop | §13 |

A note on minSdk 34: this is Android 14 and above, which is a small slice of the
installed base. That is fine for a personal tool and would not be for a public
release. It is recorded here as a deliberate choice rather than an oversight.

---

## 3. Module structure

```
android/
├── app/            screens, navigation, view models, GitHub + Mastodon clients
├── designsystem/   tokens, typography, icons, components, markdown highlighting
└── coverart/       the generator, standalone, no design system dependency
```

`:coverart` depends on nothing but Compose and `android.graphics`. It is a
publishable library, and the prototype's `paint()` is its only specification. It
must stay free of app concepts so it can be used from a build script or another
app, which was an explicit requirement.

`:designsystem` depends on `:coverart` (post rows render covers) and knows
nothing about GitHub, Mastodon, or navigation.

`:app` depends on both. If the data layer grows past a few thousand lines, split
`:data` out; do not do it pre-emptively.

### Dependencies

| Concern | Choice | Why |
|---|---|---|
| HTTP | Retrofit + OkHttp + kotlinx.serialization | ETag handling and interceptors are the whole job here |
| Local store | Room | The outbox is relational and needs transactions |
| Preferences | DataStore (Preferences) | Small, typed settings. Built with the key-value flavour rather than Proto: a single `theme_mode` enum does not earn a `.proto` schema and codegen step. Revisit if the settings surface grows enough that untyped keys start to hurt. |
| Secrets | EncryptedSharedPreferences on the Android Keystore | §7.3 |
| Background work | WorkManager | Survives process death, has the backoff we want |
| DI | Hilt | Standard, and the graph is small enough that manual DI would also be defensible |
| Image loading | Coil | Only for repo images; covers are generated, not loaded |

No git library. No Markdown library (see §9).

---

## 4. UI architecture

### 4.1 Shape

Unidirectional: a `ViewModel` exposes one immutable `UiState` through
`StateFlow`, the screen renders it, and events go back as method calls.

Screens are `@Composable` functions taking data and callbacks, never a view
model, with a thin stateful wrapper that supplies them. This is what makes every
screen previewable, and the scaffold in `app/src/main/java/com/rrajath/bloggo/ui/`
already follows it.

### 4.2 Navigation

A plain back stack of a sealed `Route` type, held in the shell
(`BloggoApp.kt`). The graph is nine destinations and entirely internal; a
navigation library would add a serialization requirement and an indirection for
no benefit at this size. Revisit if deep links or process-death restoration of a
deep stack become requirements.

The four tab destinations clear the stack. Everything else pushes.

### 4.3 State that outlives a screen

Every post the app knows about (the draft, the one in review, the published
ones) lives in one list in the shell, keyed by slug. `Route.Editor(slug)`,
`Route.Focus(slug)` and `Route.Review(slug)` (the read-only readability pass)
look their post up by slug and write edits back to that
slug only — never to a single ambient "the draft" variable, which is what let
every route show the same content regardless of what was tapped (fixed in
Milestone 5, PROGRESS.md). Switching to preview and back does not lose the
buffer, because the buffer *is* that post's entry in the list. When the editor
gains autosave this list moves to a `DraftRepository` backed by Room; the
per-slug lookup shape does not change.

### 4.4 Rendering the paper

`Modifier.paperGrain()` once at the root. See design system §6 for why it is a
tiled bitmap rather than a shader at minSdk 34.

### 4.5 Keyboard insets

`MainActivity` calls `enableEdgeToEdge()`, which stops the system from
resizing the window around the IME — the app must consume that inset itself
or the keyboard overlays content instead of pushing it up. One
`Modifier.imePadding()` on the shell's root column handles this for every
screen. Do not re-add per-screen `imePadding()` calls; the shell already
covers it, and a second one just double-pads.

---

## 5. The GitHub layer

The heart of the app, and the part most likely to be wrong.

### 5.1 What each screen needs

| Screen | Call | Endpoint |
|---|---|---|
| Library | List posts | `GET /repos/{o}/{r}/git/trees/{branch}?recursive=1` |
| Library | Post state | `GET /repos/{o}/{r}/pulls?state=open` |
| Editor | Load a post | `GET /repos/{o}/{r}/contents/{path}` (raw media type) |
| Repo | Detect framework | `GET /repos/{o}/{r}/contents` at root, intersected with the Hugo config candidates |
| Media | List images | tree filtered to `static/images/` |
| Media | Thumbnails | `https://raw.githubusercontent.com/...` via Coil |
| Commit | Write | Git Data API, §5.3 |
| Commit | Pull request | `POST /repos/{o}/{r}/pulls` |
| Repo | Build status | `GET /repos/{o}/{r}/commits/{sha}/check-runs` |

### 5.2 Listing without a clone

One recursive tree call returns every path in the repo with its blob SHA. Filter
to `content/posts/**.md`. This is a single request and gives the whole library.

Two traps:

- The response has a `truncated` flag when the tree is very large. Handle it by
  falling back to a non-recursive walk of `content/posts/` only. A blog will not
  hit this; failing to check would still be a silent wrong-answer bug.
- The tree gives paths and SHAs, **not** titles. Titles live in frontmatter, so
  the library would need to fetch every post to render its list. Do not do that.
  Instead cache `path → (blobSha, title, date, draft, wordCount)` in Room, and on
  each refresh only fetch posts whose blob SHA changed. A blob SHA is content
  addressed, so an unchanged SHA guarantees unchanged frontmatter — and an
  unchanged word count, which is why the count is stored rather than recomputed
  per refresh.

The fetches that *are* needed run concurrently, gated at five in flight to match
OkHttp's default `maxRequestsPerHost`. Serially — which is how this was first
written — a first sync of an N-post blog was N round trips end to end, with the
library empty throughout.

The tree call is conditional: `PostLibraryRepository` keeps the last response's
ETag and replays it as `If-None-Match`, so an unchanged repo answers `304` with no
body and no rate-limit unit, and the cache serves the library. The ETag is held for
the process lifetime only; losing it across a restart costs exactly one tree call.

### 5.3 Committing, and why not the Contents API

The obvious call is `PUT /repos/{o}/{r}/contents/{path}`. It writes one file and
makes one commit. That is disqualifying: publishing a post with a generated cover
touches **two** files, and two Contents calls produce two commits, with a window
where the post is live and its cover is a 404.

So: the Git Data API, four calls per commit.

```
POST /repos/{o}/{r}/git/blobs          → blob SHA per file (base64 for the PNG)
POST /repos/{o}/{r}/git/trees          → new tree, base_tree = current commit tree
POST /repos/{o}/{r}/git/commits        → commit, parent = current head
PATCH /repos/{o}/{r}/git/refs/heads/{branch}   force = false
```

`force = false` on the ref update is the concurrency check: if the branch moved
since we read it, GitHub rejects the update rather than clobbering. That
rejection is the app's only conflict signal, so it must surface as a real UI
state, not a toast (§6.4).

For a pull request, create the branch ref first
(`POST /git/refs` with `refs/heads/post/{slug}`), commit onto it, then open the PR.

### 5.4 Caching and rate limits

Authenticated requests get 5,000 per hour, which is generous, but a poll loop can
still burn it. Rules:

- Every GET sends `If-None-Match` with a stored ETag. A `304` does not count
  against the limit.
- The tree and PR list refresh on screen entry and on manual pull-to-refresh.
  Nothing polls on a timer.
- Read the `x-ratelimit-remaining` header. Below 100, stop background refreshes
  and say so on the Settings screen rather than failing opaquely.
- Secondary rate limits exist for writes. Serialise the outbox: one commit in
  flight at a time, never a burst.

### 5.5 Errors

Map to a small sealed type, because the UI needs to behave differently for each:

| Case | Surface |
|---|---|
| No network | Queue it. This is the normal path, not an error. |
| 401 / 403 bad token | Settings screen, "reconnect", with the token flow |
| 403 rate limited | Settings screen, with the reset time |
| 409 / ref rejected | Conflict state (§6.4) |
| 422 validation | Developer error; log and show a generic failure |
| 5xx | Retry with backoff via WorkManager |

---

## 6. Offline

The prototype shows an offline queue on the library and repo screens. That is the
feature that makes the app usable, so it is P0, not a refinement.

### 6.1 The outbox

A Room table of intents, not of HTTP requests:

```kotlin
@Entity
data class OutboxEntry(
  @PrimaryKey val id: String,
  val kind: Kind,              // CommitPost, OpenPullRequest, UploadMedia
  val slug: String,
  val markdown: String,        // full file content to write
  val coverPngPath: String?,   // local file staged for upload
  val message: String,
  val baseCommitSha: String,   // what this edit was based on
  val createdAt: Instant,
  val state: State,            // Queued, Sending, Failed, Conflicted
  val lastError: String?,
)
```

Storing the intent rather than a serialized request means a queued commit can be
rebased onto a newer head without being rebuilt.

### 6.2 Draining it

A `CoroutineWorker` with a network constraint and exponential backoff, unique
work name so enqueuing twice does not double-post. It processes entries oldest
first, one at a time.

### 6.3 What the writer sees

The banner on the library screen and the count on the repo screen, both from the
same query. A queued post is not a failed post and must never look like one.

### 6.4 Conflicts

Someone edits the same post on their laptop and pushes. The queued commit's
`baseCommitSha` is now stale and the ref update is rejected.

There is no working tree, so a three-way merge is not available. The honest
options are limited, and the app should offer exactly two:

1. **Keep mine.** Refetch head, rebuild the tree on the new base with our
   content, commit. The other change to *this file* is lost; changes to other
   files survive because `base_tree` is the new tree.
2. **Keep theirs.** Discard the queued edit, reload from the remote.

Show the diff between the two versions before asking. Never resolve silently, and
never offer a "merge" the app cannot actually perform.

This is the sharpest edge of the no-clone decision, and it is worth revisiting if
it bites in practice.

---

## 7. Auth and secrets

### 7.1 GitHub

A fine-grained personal access token, pasted into the Settings screen. Required
permissions, which the setup copy should state exactly:

- Contents: read and write
- Pull requests: read and write
- Metadata: read (implied)

Scoped to the single blog repository. The app validates on paste with
`GET /repos/{o}/{r}` and reports what it found rather than just "invalid".

Tokens expire. Store the expiry when known and warn a week ahead on the Repo
screen; a token that dies silently mid-queue is a bad afternoon.

### 7.2 Mastodon

Full OAuth per instance, because unlike GitHub there is no paste-a-token path
that a normal Mastodon user has:

1. `POST https://{instance}/api/v1/apps` to register, once per instance, storing
   `client_id` and `client_secret`.
2. Authorisation Code with PKCE in a Custom Tab, redirect to
   `bloggo://oauth/{instance}`.
3. `POST /oauth/token`, scope `write:statuses read:accounts`.

One token per account, several accounts per instance permitted.

Advanced users can paste a token from an instance's own app settings instead. It
is a two-line code path and it rescues instances with unusual OAuth setups.

### 7.3 Storage

All tokens in `EncryptedSharedPreferences` with a Keystore-backed master key.
Never in DataStore, never in Room, never logged. The OkHttp logging interceptor
is debug-only and redacts `Authorization`. Built as a debug/release Kotlin
source-set split (`withDebugLogging()` in `data/github/`, two implementations)
rather than a `BuildConfig.DEBUG` check, since `:app` builds with
`buildConfig = false`. The `debug` build type is also a distinct app
(`com.rrajath.bloggo.debug`, "Bloggo Debug", version suffixed " (debug)") so a
dev build never overwrites a real install; `release` is unchanged.

The GitHub PAT field on the Settings screen shows the stored token, masked, with
an eye toggle to reveal it in plain text — a saved field that goes blank the
moment you save it is indistinguishable from one that never saved (Milestone
8, PROGRESS.md). This reverses an earlier "never redisplayed" stance from
Milestone 4; the storage guarantees above are unchanged, only the on-screen
field itself now round-trips the real value instead of a blank placeholder.

`android:allowBackup` is `false` (done in the milestone that added the GitHub
client — PROGRESS.md — since that is when a real secret first got stored).

---

## 8. The editor

The signature interaction, and the biggest UI risk. Already implemented in
`designsystem/editor/`.

### 8.1 The design that removes the risk

Markdown source is styled in place with a `VisualTransformation`. The prototype
*dims* its markers rather than hiding them, which means the transformation only
ever adds `SpanStyle`s and never changes the character count. So it can use
`OffsetMapping.Identity`, and the entire class of offset-mapping bugs that makes
rich markdown fields fragile simply does not arise.

This is worth protecting. If anyone later makes markers collapse to zero width,
they inherit a real mapping and a great many more tests. `MarkdownTransformation`
carries that warning in a comment.

### 8.2 Structure

`MarkdownHighlighter.spans(String): List<MarkdownSpan>` is pure, Compose-free,
and directly tested: 24 tests covering frontmatter detection, fences, shortcodes,
precedence between code and emphasis, links, and malformed input.

Precedence is code, then links, then bold, then italic, each claiming characters
so later patterns skip them. That is what keeps `` `a * b` `` from turning
italic.

### 8.3 Performance

`filter()` runs on every keystroke over the whole document. A 2,000 word post is
roughly 12 KB and the highlighter is linear with a handful of regex passes per
line. Measure before optimising, but the escape hatch if needed is to highlight
only the visible window plus a margin.

### 8.4 Formatting actions

`MarkdownAction` in the app module is a sealed type over `TextFieldValue`, split
out from the screen so the selection arithmetic is testable. Wrap actions select
the placeholder when there is no selection, so the writer can type straight over
it.

---

## 9. Read mode

`ArticleParser` turns markdown into blocks, rendered with the design system's
article styles.

Deliberately not a CommonMark library. It handles what the app writes plus what a
Hugo post normally contains, in about 150 lines, and it renders Hugo shortcodes
as real blocks. Printing `{{< callout >}}` in a preview would make the preview
useless, and no off-the-shelf Markdown renderer knows what a Hugo shortcode is.

Adopt a real parser the moment posts use tables, footnotes, or nested lists. That
is a known and accepted future rewrite, not an oversight.

Preview has two states, and the difference is load bearing: from the editor it is
an unpublished draft whose permalink does not resolve yet; from a Live row it is
the real page and the Open button works.

---

## 10. Cover generation

### 10.1 Where it lives in the UI

Post details sheet, under a Cover section, with Generate and then Shuffle.

### 10.2 What happens on Generate

1. Seed from the slug via FNV-1a. Same post, same cover, forever.
2. Render at 1200 × 630 through `CoverArtRenderer`, off the main thread.
3. Write the PNG to app-private storage, path recorded on the draft.
4. Set frontmatter `cover: /images/{year}/{slug}.png`.

Shuffle increments a variant counter rather than randomising, so the chosen cover
stays reproducible from `slug` plus one small integer. That integer is not stored
in the repo, which is a real limitation: regenerating a shuffled cover later
requires knowing the variant. Options if this matters are writing
`coverVariant: 3` into frontmatter, or accepting that the PNG in the repo is the
artefact of record. **Recommend the latter**, since the PNG is committed anyway.

### 10.3 What is committed

The post and its cover go up in **one** commit, which is the whole reason for the
Git Data API in §5.3.

The image is written to frontmatter only, never into the markdown body. Hugo
themes render the cover themselves; writing it twice is how posts end up showing
the image twice. Called out here because the original request was phrased as
"insert the cover image at the top", and this achieves that outcome the way Hugo
expects.

### 10.4 Fidelity

`:coverart` is verified against the prototype's own output: the golden values in
its tests were captured from the JavaScript. See design system §8 for the
double-precision hazard in the seed multiply, which is the kind of thing that
would otherwise have quietly changed every cover.

---

## 11. Mastodon

Compose once, post to several accounts.

The detail that makes naive cross-posting fail is that character limits are per
instance. A message that fits a self-hosted server 500s on mastodon.social. So:

- The counter tracks the strictest **selected** account while text is shared.
- Over-limit instances are named in the banner, not merely flagged in red.
- Turning off "same text everywhere" seeds a per-account copy from the shared
  draft and shows a tab per account, dotted when that copy is too long.
- Posting is per account and partial failure is normal: report "posted to 2 of
  3", keep the failure, offer a retry for that one.

No image is attached. Mastodon builds its preview card from the site's own Open
Graph tags, so the generated cover reaches the timeline anyway.

Posts go through the outbox like commits, for the same offline reasons.

---

## 12. Testing

| Layer | How | Where |
|---|---|---|
| Cover art | Golden values from the JS original | `:coverart` unit tests, 19 |
| Markdown highlighting | Pure function, table driven | `:designsystem` unit tests, 24 |
| Formatting actions | `TextFieldValue` in, out | `:app` unit tests, to write |
| Article parsing | Markdown in, blocks out | `:app` unit tests, 10 |
| GitHub client | MockWebServer | `:app` unit tests, 18 |
| Library cache | MockWebServer + an in-memory fake DAO | `:app` unit tests, 8 |
| Frontmatter / slugs | Pure function, table driven | `:app` unit tests, 13 |
| Editor slug-sync | Real `BasicTextField` via Compose test APIs | `:app` instrumented, 2 |
| Outbox | Room in-memory, WorkManager test runner | instrumented, to write |
| Components | Screenshot tests against the prototype | Roborazzi, to add |
| Flows | Compose UI tests for draft to commit | instrumented, to write |

The GitHub client row now also covers `getPostsTree` and `getFileContent`
(§5.2): every branch of the shared error mapping in §5.5, the `truncated`
fallback to a plain directory listing, and the raw-media-type request for a
post's content, and the single root listing framework detection reads. 409 (ref
conflicts) is still uncovered, because it belongs to the commit work in §5.3,
which is not built yet.

The library-cache row exercises `PostLibraryRepository` against a real
`GitHubClient` hitting `MockWebServer`, with a hand-written in-memory
`PostCacheDao` standing in for Room (no Android runtime needed to test the
caching logic itself): frontmatter parsing into a `Post`, an unchanged blob
SHA skipping the re-fetch entirely, a changed SHA replacing the cached entry,
a post removed from the tree dropping out of the cache, a `304` tree response
costing one request and serving the cache, request counts proving the per-post
fetches neither repeat nor serialise, and the truncated-tree
fallback.

Current state: **93 unit tests plus 2 instrumented, all passing.** The
`ArticleParser` suite includes regressions for two on-device bugs a
desktop-only check would have missed: unescaped `}` in a regex, which
Android's ICU engine rejects at pattern-compile time but desktop
`java.util.regex` accepts silently, and multi-line blockquotes rendering as
separate blocks instead of one merged quote. Neither showed up until real
(non-empty) markdown exercised those paths — see Milestone 5 in PROGRESS.md.

The 2 instrumented tests (`EditorScreenTest`, `testInstrumentationRunner`
now configured) are the project's first — written after manually verifying
the live title-to-slug sync (Milestone 8) by `adb shell input text` produced
a false failure: the tool silently dropped a literal space mid-string, which
made a genuinely working feature look broken under manual testing. Driving
the real `BasicTextField` through Compose's own test APIs sidesteps that
class of tooling artifact entirely, and is the right way to test anything
that depends on real IME/text-field behavior rather than reaching for ADB
key events.

Screenshot testing deserves its own note. The requirement is that the app matches
the prototype exactly, and no amount of code review verifies that. Roborazzi
golden images per component, in both themes and both art modes, is how that claim
stays true after the tenth change.

---

## 13. Milestones

### P0 — the write and publish loop

The bar: you would use it instead of opening a laptop.

1. Repo connection. Paste a token, validate, detect Hugo, store config.
2. Library. Tree listing, frontmatter cache, the three sections.
3. Editor. Load, edit with live styling, autosave to Room.
4. Post details. Frontmatter editing, draft toggle.
5. Read mode.
6. Commit. Git Data API, message, diff against the remote blob.
7. Outbox. Queue, drain, banner, conflict UI.

### P1 — the rest of the prototype

8. Media library, upload, insert.
9. Cover generation, committed with the post.
10. Pull request flow, branch naming, checks.
11. Inbox capture, promote a fragment to a post.
12. Mastodon: OAuth, multi-account, per-instance limits.

### P2 — deferred by request

13. Focus mode: typewriter sightline, dimmed context, goal ring, session timer.
14. Voice capture in the inbox: record, transcribe, file as a fragment.

Focus mode and the voice control are already laid out in the scaffold, inert, so
the shape is visible and the work is additive.

---

## 14. Risks

| Risk | Severity | Response |
|---|---|---|
| Conflicts are unresolvable without a working tree (§6.4) | High | Two honest options, always show the diff. Revisit the no-clone decision if it bites. |
| Editing a large post re-highlights per keystroke | Medium | Measure. Window the highlighter if needed. |
| `ArticleParser` meets markdown it does not know | Medium | Bounded by what the app writes. Adopt a real parser when tables appear. |
| Token expiry mid-queue | Medium | Store expiry, warn ahead, keep the outbox intact. |
| minSdk 34 excludes most devices | Low, accepted | Deliberate for a personal tool. |
| Cover art drifts from the prototype | Low | Golden tests fail loudly. |
| GitHub secondary rate limits on writes | Low | Serialise the outbox. |

---

## 15. Open questions

1. **Autosave granularity.** Every keystroke to Room, or debounced? Debounced is
   cheaper; every keystroke survives a crash mid-sentence. Leaning debounced at
   500 ms plus on-pause.
2. **Does the library need search?** The prototype has the affordance. 38 posts
   does not need it; 300 does.
3. **Should the shuffle variant be written to frontmatter?** §10.2. Recommending
   no.
4. **Multiple repositories.** Out of scope for v1, but the config is already a
   record rather than loose keys, so it would not be a rewrite.
5. **What happens to a post deleted on the remote while queued locally?** Not yet
   specified. Probably the same conflict UI.

---

## 16. What already exists

Everything below builds and runs on an emulator today.

- `:coverart` — the generator, ported and verified against the prototype, with
  Compose bindings. 19 tests.
- `:designsystem` — tokens, four bundled families, 41 generated icons, paper
  grain, 15 components with previews, markdown highlighting. 24 tests.
- `:app` — navigation shell and all eight prototype screens laid out against
  sample data, including the editor with live styling, the cover generation
  section, and the Mastodon composer with working per-instance limits.
- `:app` — a Settings screen, matching the prototype's "Repo + settings"
  fold: the bottom nav's last tab (formerly labelled "Repo") now opens this
  screen, titled "Settings", and it is the sole destination for both the
  GitHub connection and app-wide appearance. Theme is Auto/Light/Dark, backed
  by Preferences DataStore. There is no separate cog icon on Library any more;
  a standalone Settings destination existed briefly but was folded back into
  this screen once it had somewhere better to live.
  - Later split (see `internal/settings-split-plan.md`): the single scrolling
    screen became a menu of six navigable sub-pages — GitHub Connection, Repo
    Settings, Publishing, Appearance, Readability Review, Import / Export —
    each a non-tab `Route.SettingsDetail(page)` with a back chevron. The menu
    keeps the stat line and adds a version footer (read from `PackageManager`,
    since `BuildConfig` is disabled). The "Reading typeface" row was removed.
    The site URL is stored as a full URL including scheme (`RepoConnection.
    siteUrl`); `siteHost` is a derived property so `Post.liveUrl` call sites
    are unchanged, and a pre-existing host-only stored value migrates to
    `https://<host>` on read. Import / Export round-trips every setting except
    the PAT through `data/SettingsBackup.kt` and the Storage Access Framework.
- `:app` — the storage half of §7.1 and §7.3: a "GitHub connection" section on
  the Settings screen that saves a repository, branch, and fine-grained PAT. The
  token goes to `EncryptedSharedPreferences` on the Keystore master key, never
  to DataStore or Room; repository and branch go to Preferences DataStore.
- `:app` — the network half: `data/github/GitHubClient.kt` makes a real
  `GET /repos/{o}/{r}` call to validate the saved connection, and a real
  `GET .../contents/{candidate}` call against 8 known Hugo config filenames
  to detect the framework, per §7.1 and §5.1. Errors map to the categories in
  §5.5 and show as a banner on the Settings screen (connected · branch ·
  visibility · Hugo detected or not; or the specific failure). Verified
  against the live GitHub API, not just mocks. The rest of the Settings screen —
  "Sample repo", "Detected from your repo" — still reads `SampleData.repo`;
  wiring real detection results into what those sections *display* is
  separate UI work, not done here. (Superseded by Milestone 10, PROGRESS.md:
  the "Sample repo" card is gone — the GitHub connection section already
  says as much — and "Detected from your repo" is no longer read-only.
  Framework detection (Hugo yes/no) still comes from this real check, but
  the config filename, post path, image path, and frontmatter field list are
  now `RepoConnection` settings the writer corrects, per §7.1's own framing
  that a Hugo site has no schema to trust blindly. `SampleData.repo` no
  longer exists as a type; only the illustrative repository/branch/site-host
  strings shown before any connection remain, as plain `SampleData`
  constants.)
- `:app` — the library half of §5.2: `GitHubClient.getPostsTree` lists every
  markdown post under `content/posts/` via one recursive git-trees call
  (falling back to a plain directory listing when the response is
  `truncated`), and `data/library/PostLibraryRepository` caches
  `path -> (blobSha, title, date, draft, cover, markdown)` in Room, keyed on
  blob SHA — a post is only re-fetched when its SHA no longer matches the
  cache. `BloggoApp` refreshes from this repository whenever a usable
  connection (repository + token) appears, merging the result into the
  shared post list by slug: matching slugs get real content, new slugs get
  added, and remote-sourced slugs no longer in the repo get dropped. Local,
  not-yet-committed posts are left alone. Verified against the real GitHub
  API on an emulator: the tree endpoint fires with the exact URL §5.2
  specifies, a bad token 401s without crashing and leaves the sample data in
  place, and the Library header's repo/branch/Hugo line reflects the real
  connection once one exists. PR listing (§5.1's other Library call) is not
  built yet, so "Open pull request" still shows the sample placeholder.
- `:app` — a real-usage bug pass (Milestone 8, PROGRESS.md), once a real
  repository was actually connected: on-demand refresh (button and
  pull-to-refresh) alongside the existing auto-refresh; Hugo config
  detection across 8 candidate filenames instead of one; the PAT field
  round-trips the stored token (masked, with a reveal toggle) instead of
  going blank on save; a "Delete draft" action, scoped to local
  not-yet-committed drafts; YAML *and* TOML frontmatter parsed identically
  everywhere frontmatter is read; Editor/Preview toggling no longer grows
  the back stack, and a `BackHandler` was added since hardware/gesture back
  had never been wired to the app's own navigation at all; dark mode's
  status bar icons now match the theme actually in effect; the offline-queue
  banner shows a real (currently always zero) count instead of a fixed
  sample one; and new posts get a fuller frontmatter default — `date`,
  empty `tags`, and a `slug` that tracks the title live until edited by
  hand.

- `:app` — a fourth real-usage bug pass (Milestone 11, PROGRESS.md), from
  actually writing in the app: the formatting toolbar no longer overlaps the
  gesture-navigation bar (it lacked its own `navigationBarsPadding()`); the
  H2 toolbar button now cycles heading levels 1 through 6 instead of always
  inserting a fixed `##`; a keystroke-performance bug (unconditional
  library-wide list recomputation on every editor keystroke, the same shape
  Milestone 10 fixed once already for `repoConfig`) that caused backspace
  stalls and dropped fast typing on the title field is fixed; and P0 item
  4's other half — the post-details bottom sheet's Save button, which
  previously took no parameters and so persisted nothing (not just the
  "Keep as draft" switch the user noticed, every field) — now writes
  title/slug/date/tags/draft back into the post's frontmatter via a new
  `String.withFrontmatterEdits()` in `model/Model.kt`, the rewrite
  counterpart to the existing "build fresh frontmatter" `frontmatterFor()`.
- `:app`/`:designsystem` — editable tags (Milestone 12, PROGRESS.md): the
  post-details sheet's tag chips gained a remove `×` and a "+ add"
  affordance with autocomplete, sourced from the distinct union of tags
  already used across the writer's own posts (not `hugo.toml`
  `[taxonomies]`, a concept Milestone 10 removed for being unbacked), plus
  a "Create '…'" option for a genuinely new tag. New reusable
  `BloggoTagField` in `:designsystem`, documented in DESIGN_SYSTEM.md.

Framework detection is that one root listing, intersected with the eight valid
Hugo config names in their precedence order (`hugo.toml` before `config.toml`, and
so on). Probing the candidates one at a time — the first implementation — cost a
round trip and a rate-limit unit per miss, up to eight of each, and was slowest
exactly when the answer was "no Hugo config here": the case a writer retries.

What is still stubbed: PR listing (§5.1), rate-limit headers (§5.4), and the
Mastodon client in §6/§7.2 — no calls made yet, on purpose. The tree call's
ETag half of §5.4 is built (§5.2); the per-file `If-None-Match` and the
`x-ratelimit-remaining` backoff are not. The connection-check call was the
first real HTTP request the app made, to prove the token/repo/branch the
writer entered actually works before anything downstream depends on it; the
tree and content calls above are the second.

- `:app` — P0 item 6, the commit flow (direct-commit half only; see
  `internal/docs/WRITE_PUBLISH_MEDIA_PLAN.md`), plus P1 item 8 (media library, upload,
  insert), built together since a photo only has somewhere to go once the
  commit path exists:
  - `GitHubClient.commitFiles`: the real Git Data API sequence — one blob per
    file (`utf-8` for the post, `base64` for images), a new tree layered on
    the branch's current base tree, a commit parented on the branch's head,
    then a ref update with `force = false` always, so a branch that moved in
    the meantime comes back as a real `GitHubApiError.Conflict` rather than
    being silently overwritten. Runs on whatever branch `RepoConnection`
    has configured — never a literal `main`.
  - New `PostPublishRepository` resolves a post's real repo path (a newly
    committed post's path is now remembered on `Post.repoPath`, populated
    from `PostCacheEntity.path`, and never re-derived from the `{slug}`
    template again once set — a frontmatter slug edited after the first
    commit must not fork the post into a second file).
  - New `PublishSheet` (editor's Commit button, previously a toast) shows the
    commit message and the files about to change, and calls `commitFiles`
    directly. **Not built this pass, on purpose**: the "open a pull request"
    flow §5.3/P0 item 6 also names, and the line-level diff against the
    remote blob — v1 is a plain file list, no diff renderer.
  - `ArticleParser`'s already-working `Figure`/`Video` parsing finally
    renders for real in `PreviewScreen` — Coil (new dependency,
    `coil-compose` + `coil-network-okhttp`, reusing the existing OkHttp dep)
    loads a figure's image from wherever it actually lives: a local staged
    file with no network call, or the repo's authenticated raw-content
    endpoint otherwise. Video only actually plays for a fully-qualified
    `http(s)://` source (`VideoView`); uploading/recording video is out of
    scope, per the user's own explicit scoping.
  - `GitHubClient.getImagesTree` — the Media screen's real half, same
    one-recursive-call-plus-truncated-tree-fallback shape §5.2 already
    validated for posts, filtered to image extensions under the configured
    image path instead of markdown under `content/posts/`.
  - Uploads (Media screen's dropzone, or the editor's new Insert -> Image
    row) stage locally — downscaled, capped at a 2000px longest edge, copied
    once out of the picked `content://` Uri into the app's cache dir since
    that Uri isn't guaranteed readable later — and are **not** committed
    until a post actually references them, bundled into that post's own
    commit. The in-memory `StagedMedia` registry (`BloggoApp`) is
    process-lifetime only, on purpose: post edits themselves aren't
    autosaved yet either (P0 item 3 below), so persisting staged media
    across a kill while concurrent edits don't would be a new, inconsistent
    guarantee. The cache directory is swept clean on every cold start.
  - Insert -> Image pushes `Route.Media` in a new picker mode
    (`returnToEditorSlug` set — not a tab visit, doesn't clear the back
    stack) rather than a bare device picker, so an existing repo or staged
    image can be reused across posts, matching the HTML prototype's own
    `data-go="media"` design. New `InsertSheet` also wires up
    `MarkdownAction.CodeBlock`/`.Rule`, which had zero callers before this.
    The prototype's fourth Insert row — shortcodes detected from
    `layouts/shortcodes/` — is still not built; `MarkdownAction.Callout`/
    `.Aside` exist and are ready whenever it is.
  - 25 new `GitHubClientTest` cases (the full commit sequence, branch
    passthrough, per-file encoding, the conflict mapping, the images-tree
    listing and its truncated fallback), a new `PostPublishRepositoryTest`,
    and pure-function tests for `RepoPaths`/`stagedImagePath` in
    `model/Model.kt`.
  - Not verified on an emulator or against a real repository — compile and
    unit-test verified only in this environment.
