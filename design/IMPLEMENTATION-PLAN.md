# Bloggo — Implementation Plan

> Derived from `design/BloggoPRD.md`, `design/Bloggo-Technical-Design.md`, and `design/README.md`.
> Target: native Android, Kotlin + Jetpack Compose + Material 3. Package `com.rrajath.bloggo`.

## Locked decisions

| Concern | Choice |
|---|---|
| DI | **Hilt** (`@HiltAndroidApp` + ViewModels) |
| HTTP | **Retrofit + OkHttp + Moshi** (JSON converter) |
| YAML | **SnakeYAML** (generic `Map<String,Any>` fits the free-form raw front-matter block) |
| Markdown preview | **Markwon** bridged via `AndroidView` (edit field stays a Compose `BasicTextField`) |
| Local DB | **Room** (KSP compiler) |
| Secure storage | **EncryptedSharedPreferences** (Keystore-backed) for PAT; **DataStore** for non-secret settings |
| Navigation | **Navigation-Compose** |
| Image presets | **Small 480 / Medium 800 / Large 1600 px** (per Technical Design §10) |
| Sequencing | **Full v1 end-to-end**, feature by feature |

## Current state
Green-field on the "Empty Compose Activity" template. Present: Kotlin 2.0.21, AGP 9.0.1, Compose BOM 2024.09.00, minSdk 34 / target 36 / compile 36, `MainActivity` + stock theme. **Missing**: all app deps, permissions, Application class, architecture, navigation. No tests beyond placeholders.

## Architecture
Layered: **UI (Compose + ViewModels)** → **Domain (pure Kotlin: `PostDraft`, `SyncState`, slugify, front-matter assembly/parse)** → **Data (Room, EncryptedSharedPreferences/DataStore, Retrofit GitHub API)**. Hilt wires it. Two independent status axes (`SyncState`→tag, `draft`→section) never collapse.

---

## Milestones

### M0 — Project foundation & build setup
**Goal:** a building app with all deps wired and an `Application` class.
- Version catalog: add Room (runtime/ktx) + KSP plugin, Hilt (+ plugin, top & app), Navigation-Compose, Coroutines, `security-crypto`, Retrofit + OkHttp + Moshi (+ KSP codegen), SnakeYAML, Markwon, `androidx.biometric`, Material Icons Extended, DataStore.
- `AndroidManifest.xml`: add `INTERNET`, `ACCESS_NETWORK_STATE`, `USE_BIOMETRIC`; register `BloggoApplication` (`@HiltAndroidApp`).
- Package skeleton: `ui/`, `domain/`, `data/`, `di/`.
- **Verify:** `./gradlew :app:assembleDebug` green; app launches with empty Hilt-injected `MainActivity`.

### M1 — Theme & design system
**Goal:** Bloggo's look, tokens, and app icon.
- `BloggoTheme`: light/dark/**system** + 4 accent seed `ColorScheme`s (Indigo default, Green, Amber, Violet).
- Map all README tokens: `surfaceLow/Container/High/Highest`, `outline/outlineVariant`, semantic `success/warn/error` + containers, scrim.
- Typography: **Roboto** (UI) + **Roboto Mono** (slugs/paths/dates/front matter/body/diffs); type scale from README.
- App icon: "Mono-dot" adaptive vector drawable (lowercase `b` + accent dot).
- **Verify:** app launches; light/dark/system + each accent render correctly.

### M2 — Navigation & screen shells
**Goal:** navigate between all three screens with no real data.
- Routes: Home, Editor (arg: `postId?`), Settings.
- Scaffold top bars, back nav, Home FAB `+ New post`.
- Empty `@HiltViewModel` shells per screen.
- **Verify:** Home ⇄ Editor ⇄ Settings navigation works.

### M3 — Domain layer (pure Kotlin, unit-tested)
**Goal:** the editor model and front-matter logic, framework-free.
- `PostDraft`, `SyncState { LOCAL_ONLY, SYNCED, SYNCED_MODIFIED }`, `HomeSection`.
- `slugify()` + reconciliation: `onTitleCommitted` (derive only when `slugAutoDerive && LOCAL_ONLY`), `onSlugManuallyEdited` (freezes).
- Front-matter **assemble/parse** via SnakeYAML: split `---` fence; parse YAML→Map; lift `title`/`slug`/`draft` to structured fields; re-serialize remainder as raw block; on assemble, emit owned keys from structured fields, strip them from raw, **warn** on conflict.
- Filename rule `<contentPath>/<slug>.md`.
- **Verify:** unit tests for slugify edge cases, FM round-trip, owned-key strip+warn.

### M4 — Data: Room persistence
**Goal:** posts cached locally, Home can paint instantly.
- `PostDraftEntity` ↔ `PostDraft` (localId PK, repoPath, blobSha, syncState, structured fields, rawFrontMatter, body, updatedAt).
- `PostDao`: `observeAll() Flow`, `getById`, `upsert`, `delete`.
- Separate **autosave** table (recovery buffer, distinct from "Save to local").
- `BloggoDatabase` + Hilt module; `PostRepository` over Dao.
- **Verify:** instrumented DAO tests (insert/observe/update/delete).

### M5 — Data: secure settings
**Goal:** persisted config with the PAT encrypted.
- `Settings` model (PAT, repo, branch, contentPath, imageRepoPath, imageUrlBase, blogBaseUrl, fmTemplate, theme, accent, appLock).
- **EncryptedSharedPreferences** (Keystore) for PAT; **DataStore** for the rest.
- `SettingsRepository` + Hilt module. Reveal toggle is UI-only.
- **Verify:** round-trip test; PAT stored encrypted (not in plaintext prefs).

### M6 — GitHub sync (network)
**Goal:** list/refresh/publish against the GitHub REST API.
- `GitHubService` (Retrofit): Contents API `GET` (file+sha) / `PUT` (create/update with sha); Trees API `GET` (recursive) for listing; auth interceptor (Bearer PAT).
- Map tree → fetch each file's content+`blobSha` → parse FM → `PostDraft` (loaded posts get `slugAutoDerive=false`).
- `refresh()`: merge remote with **local unpushed** (never drop locals); update syncState.
- `publish()`: assemble file, `PUT` with `blobSha` for updates; **stale-SHA → refetch + retry**, surface diff on unexpected remote change.
- `NetworkMonitor` (ConnectivityManager) → offline blocks refresh/publish with a banner.
- Commit message: `New post: <title>`.
- **Verify:** unit tests with mocked `GitHubService` (refresh merge, create, update-with-sha, stale retry).

### M7 — Home screen (full)
**Goal:** the cached, instantly-loading post list.
- `HomeViewModel`: `Flow<List<PostDraft>>` from Room (instant) + refresh/search/banner state.
- App bar (Bloggo, refresh, settings); search pill + clear; 3px indeterminate refresh bar; banner (success/warn/neutral + action).
- Collapsible **Draft** / **Published** sections (chevrons + count pills).
- Post row card: title; mono meta (`date · not pushed`); `Synced`/`Local` chip; `edited` marker; trailing **delete** (draft) / **Live** (published → open `blogBaseUrl`+permalink externally).
- States: normal, empty, fetch-error banner, offline banner.
- Pull-to-refresh (merge-preserving).
- **Verify:** all four states render; cached paint is instant; search filters.

### M8 — Editor screen (full)
**Goal:** the Markdown authoring surface.
- `EditorViewModel`: editor state, dirty, `slugFrozen`, preview toggle, `fmOpen`, autosave.
- App bar: back (**discard-guard** if dirty), `New post`/`Edit post`, **Edit/Preview** segmented toggle.
- Title input; slug line with chip + lock when frozen; auto-derive on title→body focus (local/unpushed only).
- Collapsible Front matter card (mono textarea, hint `title · slug · draft managed`).
- Body: `BasicTextField` (Roboto Mono).
- Preview: Markwon `render(body)` in `AndroidView` (FM hidden by construction).
- Formatting toolbar: **B**, **I**, **Link**, **Image**, **H** (flyout H1–H6, replaces existing `#` marks on current line). Word count (mono).
- **Save local** / **Publish** actions; continuous autosave to Room.
- **Risk:** Compose `TextFieldValue` selection manipulation for wrap/link/heading is the trickiest part — dedicate time here.
- **Verify:** all formatting ops insert correct Markdown at cursor; preview matches; slug freeze works.

### M9 — Publish flow & dialogs
**Goal:** the safe, ordered publish path.
- **Draft-flip dialog** (when `draft:true`) → `draft:false`.
- **Front-matter validation**: block publish on malformed YAML with a clear message.
- **Push confirm/diff bottom sheet**: title (`Confirm new post`/`Confirm push`), target path (mono), commit message box, diff box (new = all green `+`; edit = context + red `-`/green `+`), push spinner, Cancel/Push.
- On success → Home, mark **Synced**, clear `edited`, success banner.
- **Delete-confirm** (draft rows, error-red action), **Discard-changes** dialogs.
- Scrim: dismisses push/image sheets, not destructive confirms.
- **Verify:** new-post and edit-post publish paths end-to-end against a test repo.

### M10 — Image pipeline
**Goal:** pick → resize → upload → insert.
- Android Photo Picker.
- Insert-image bottom sheet: **Small 480 / Medium 800 / Large 1600**.
- Resize (`Bitmap`, max width, preserve aspect) + re-encode JPEG ~85%.
- Upload base64 to `imageRepoPath` via Contents API `PUT`; collision handling (timestamp/suffix).
- Insert `![alt](<imageUrlBase>/<file>)` at cursor.
- Disable/queue when offline.
- **Verify:** image appears in repo; correct public URL inserted; preview shows placeholder tile.

### M11 — Settings screen (full)
**Goal:** full config UI, live theme/accent.
- GitHub: PAT (masked `••••` + reveal eye), repo, branch.
- Paths: content path, image repo path, image URL base, blog base URL (all mono).
- Default front matter template (multiline mono, `{date}` auto-filled helper).
- Appearance: theme segmented (Light/Dark/System) + accent seed picker.
- Security: app lock switch.
- Persist via `SettingsRepository`; theme/accent apply live.
- **Verify:** settings round-trip; theme/accent switch immediately.

### M12 — Biometric app lock
**Goal:** gate the app (token is write-capable).
- `BiometricPrompt` on launch when `appLock` enabled; re-prompt on resume-from-background.
- **Verify:** lock engages/disengages with the Settings toggle.

### M13 — Polish & hardening
**Goal:** match the prototype's motion and a11y specs.
- Animations: `fadeIn` 150ms (scrim/banner), `sheetUp` 140–200ms (sheets/flyout), `spin` 0.7–0.9s (loaders), chevron/switch ~200ms, indeterminate progress 1s linear loop.
- Touch targets ≥48dp, contrast, content descriptions; final empty/error/offline copy.
- **Verify:** `./gradlew build` + lint clean; manual a11y pass.

### M14 — Testing & verification
**Goal:** confidence to ship v1.
- Unit: domain (slugify, FM assemble/parse, owned-key warn), repo mapping, slug-freeze.
- Instrumented: Room DAO, EncryptedSharedPreferences, Hilt wiring.
- Manual end-to-end checklist: new→publish→synced; edit synced→diff→push; image insert; offline; delete; settings round-trip; biometric; theme/accent; "View live".
- **Verify:** `./gradlew build` green.

---

## Risks / notes
- **AGP 9.0.1 is very new** — watch for KSP/Hilt compatibility quirks during M0; pin versions if needed.
- **Compose `TextFieldValue` selection math** (M8) is the highest-effort UI task — the prototype leans on raw textarea selection APIs; budget extra time.
- **minSdk 34** (Android 14+) — fine for a personal blog tool, excludes older devices by design.
- **Diff generation** (M9): a simple line-diff suffices for v1; pull in a small diff lib if the hand-rolled one gets messy.
- **`support.js` / `Bloggo.dc.html` are reference-only** — not ported; design tokens + the README screen spec are the source of truth.
