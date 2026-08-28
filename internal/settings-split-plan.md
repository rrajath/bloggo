# Settings page split — implementation plan

Status: implemented 2026-08-27 (see internal/LEARNINGS.md and internal/PROGRESS.md).
Date: 2026-08-27

## Goal

Break the single scrolling Settings screen into a menu of sub-pages (one per
section), add per-row subtitles, show the app version in a footer, give every
connection text field a placeholder, prefill `https://` in the Site URL field on
focus, remove the "Reading typeface" row, and add settings import/export to JSON.

## Decisions locked with the user

- **Page structure:** 6 sub-pages. Top-level Settings becomes a menu of
  navigable rows (each with a subtitle) plus the existing stat line and a
  version footer. Sub-pages: GitHub Connection, Repo Settings (was "Detected
  from your repo"), Publishing, Appearance, Readability Review, Import / Export.
- **Site URL:** store and show the full URL including scheme (not host-only).
  Migrate the existing host-only stored value.
- **Import:** apply every key present in the file; leave the PAT and any absent
  keys untouched; do NOT auto-run a connection check afterward.

## 1. Navigation

`app/src/main/java/com/rrajath/bloggo/BloggoApp.kt`

- Keep `Route.Settings` (the tab) as the new **menu** screen.
- Add a non-tab route for detail pages:
  ```kotlin
  enum class SettingsPage { Connection, Repo, Publishing, Appearance, Readability, ImportExport }
  data class SettingsDetail(val page: SettingsPage) : Route
  ```
- `isTab` unchanged (only `Settings` is a tab). Detail pages render with a back
  chevron via `BloggoAppBar(onBack = ::back)` and no tab bar, matching how
  `Editor` / `Review` already behave. Back returns to the menu.
- Tab-bar `selectedRoute` mapping needs no change (detail routes never show the
  tab bar).

## 2. Screens

Rename package `ui/repo` -> `ui/settings`; delete `RepoScreen.kt`, replace with:

- **`SettingsScreen.kt`** — the menu. App bar "Settings" (keeps the
  `connected · <branch>` subtitle), the existing `StatLine`
  (Posts / Drafts / Open PR), then one `CellGroup` of navigable `Cell`s
  (icon + title + subtitle + `BloggoIcons.ChevronRight` trailing, `onClick` ->
  `go(Route.SettingsDetail(...))`), then a centered version footer.
- **`SettingsDetailScreens.kt`** — six stateless composables, each an app bar
  with `onBack` plus the rows lifted from today's single page:

  | Page        | Title             | Content (styling unchanged)                                  |
  |-------------|-------------------|-------------------------------------------------------------|
  | Connection  | GitHub Connection | `ConnectionSection` + `ConnectionStatus` banner             |
  | Repo        | Repo Settings     | framework/Hugo config, post path, image path, frontmatter   |
  | Publishing  | Publishing        | default action segmented control                            |
  | Appearance  | Appearance        | theme, cover art (Reading typeface row REMOVED)             |
  | Readability | Readability Review| the 9 readability switches                                  |
  | ImportExport| Import / Export   | explanatory text + Export / Import rows                     |

  Shared privates (`SettingsField`, `ConnectionStatus`, `EditableCell`,
  `ToggleEditableCell`, `readabilityCheckRows`) move here.

Menu row subtitles:
- GitHub Connection — "Repository, branch, and access token"
- Repo Settings — "Hugo config, post and image paths, frontmatter"
- Publishing — "What happens when you publish a post"
- Appearance — "Theme and cover art"
- Readability Review — "Which checks run on your drafts"
- Import / Export — "Back up your settings to a file"

## 3. Version footer

In `SettingsScreen`, read via `PackageManager` (BuildConfig is disabled
project-wide):

```kotlin
context.packageManager.getPackageInfo(context.packageName, 0).versionName
```

The `debug` build type already sets `versionNameSuffix = " (debug)"`, so a debug
build reports `1.1.0 (debug)` and release reports `1.1.0` — the bracketed-suffix
requirement is met by displaying `versionName` as-is. Rendered centered,
`inkFaint`, version number in mono, "Bloggo" in UI font: `Bloggo 1.1.0 (debug)`.

## 4. Field placeholders + `https://` prefill

Rework the private `ConnectionField` -> `SettingsField`:
- Add `placeholder: String`, shown in `inkFaint` when the value is empty
  (matches prototype `.field input::placeholder{color:var(--ink-3)}`).
- Site URL field switches to `TextFieldValue`; on `Modifier.onFocusChanged`
  gaining focus while blank, set
  `TextFieldValue("https://", selection = TextRange(8))` so the caret sits right
  after the slashes.

Placeholders:
- Repository — `username/blog`
- Branch — `main`
- Site URL — `https://your-blog.com`
- Author name — `John Doe`
- Fine-grained PAT — `GitHub PAT to push your changes`

## 5. Site URL stores the full URL

- `RepoConnection`: replace `siteHost` with `siteUrl: String` (normalized
  `https://host`, no trailing slash); add derived
  `val siteHost get() = siteUrl.substringAfter("://").trimEnd('/')` so
  `Post.liveUrl(siteHost)` call sites are untouched.
- `RepoConnectionRepository`: new `site_url` key; `setRepo` normalizes (prepend
  `https://` if no scheme, strip trailing `/`). Migration on read:
  `prefs[siteUrlKey] ?: prefs[siteHostKey]?.let { "https://$it" } ?: ""`.
- `BloggoApp`: `onVisitSite` / `onSaveConnection` use `siteUrl`; the
  `val siteHost` fallback near line 520 still works via the derived property.
  `PreviewScreen` keeps its bare-host param.

## 6. Import / Export

- **`data/SettingsBackup.kt`** — `@Serializable data class SettingsBackup(version: Int = 1, ...)`
  covering repository, branch, siteUrl, authorName, postPath, imagePath,
  hugoConfigFile, frontmatterFields, publishAction, themeMode, artMode,
  readabilityChecks. NO token field.
- **Export:** SAF `CreateDocument("application/json")`, default name
  `bloggo-settings.json`, written through `contentResolver.openOutputStream`
  with `Json { prettyPrint = true }`. Launcher + assembly live in `BloggoApp`
  (it already has `repoConnection`, `themeMode`, `artMode`, `readabilityChecks`
  in scope); the screen exposes `onExport` / `onImport` callbacks only.
- **Import:** SAF `OpenDocument(arrayOf("application/json"))`, decode, apply every
  present key via existing repo setters (`setRepo`, `setPostPath`, ...,
  `setThemeMode`, `setArtMode`, `setReadabilityChecks`); PAT and absent keys
  untouched; NO connection re-check. Toast: "Settings imported — open GitHub
  Connection and Save to reconnect". Parse failure -> toast "Couldn't read that
  settings file".
- ProGuard: add a keep rule for the `SettingsBackup` serializer (release build
  is minified).

## 7. Tests

- New `SettingsBackupTest` (JVM) — JSON round-trip; assert the token is never
  serialized.
- `./gradlew :app:testDebugUnitTest` + `./gradlew assembleDebug`, then a release
  smoke build (`./gradlew assembleRelease`) since a new `@Serializable` type is
  added.
- Compose instrumented test: navigate Settings menu -> GitHub Connection -> back.
- Manual: version footer shows `(debug)`; placeholders render; `https://` caret
  position; export -> import round trip.

## 8. Docs (same commit as the code)

- `internal/docs/DESIGN_SYSTEM.md` — Settings is now a menu of nav `Cell`s +
  version footer; document the `SettingsField` placeholder / focus-prefill
  pattern; note the split has no prototype counterpart (deliberate divergence,
  built from existing components).
- `internal/docs/ANDROID_TDD.md` — settings screen description, `siteHost` ->
  `siteUrl`, Reading typeface row gone.
- `CLAUDE.md` (project) — `ui/repo` -> `ui/settings`, `Route` list,
  `RepoScreen` -> `SettingsScreen`.
- `README.md` — add settings import/export to the feature list.
- `CHANGELOG.md` — `### Added` multi-page settings / version display /
  import-export / field placeholders; `### Changed` site URL stores full URL;
  `### Removed` Reading typeface row.
- `internal/LEARNINGS.md` — PackageManager-for-version (BuildConfig disabled) +
  prototype divergence note.
- `internal/PROGRESS.md` — update.
