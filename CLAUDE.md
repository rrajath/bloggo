# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew assembleDebug

# Run all unit tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.rrajath.bloggo.domain.SlugifyTest"

# Lint
./gradlew lint

# Install on connected device
./gradlew installDebug
```

Tests use **JUnit 4 + Truth + MockK + Turbine** (for Flow testing) and run on **Robolectric** (no emulator required).

## Architecture

Standard Android layered architecture: UI → ViewModel → Repository → Data sources.

**Layers:**
- `ui/` — Jetpack Compose screens (`HomeScreen`, `EditorScreen`, `SettingsScreen`) with corresponding ViewModels and `UiState` sealed classes
- `domain/` — Pure Kotlin domain models (`PostDraft`, `SyncState`, `FrontMatter`, `Slug`, `HomeSection`). No Android dependencies.
- `data/` — Repositories, Room DAOs, Retrofit network layer, encrypted settings
- `di/` — Hilt modules (`DatabaseModule`, `NetworkModule`, `SettingsModule`)

**Navigation:** `BloggoNavHost` uses Jetpack Navigation Compose. Three routes: `HOME`, `EDITOR` (optional `postId` arg), `SETTINGS`.

## Key Domain Concepts

**`PostDraft`** is the central domain model. It holds structured fields (`title`, `slug`, `draft`, `slugAutoDerive`) separately from `rawFrontMatter` (everything else). Front matter is never stored in `body`. This separation prevents the title field from corrupting body text and simplifies preview rendering.

**Two-axis state model** — these axes are fully independent and must never be collapsed:
- `SyncState` (`LOCAL_ONLY`, `SYNCED`, `SYNCED_MODIFIED`) → drives the "Local/Synced" tag on the home screen
- `draft: Boolean` → drives the section grouping (Drafts vs Published)

**`FrontMatter` object** (`domain/FrontMatter.kt`) owns all YAML parsing/assembly. Always use it — never parse front matter with regex. It handles both YAML (`---`) and TOML (`+++`) fences. Owned keys (`title`, `slug`, `draft`) are stripped from `rawFrontMatter` on parse; structured fields win on assemble.

**Slug rules:** Auto-derived from title while `syncState == LOCAL_ONLY && slugAutoDerive == true`. Manually editing the slug freezes derivation permanently (`slugAutoDerive = false`). Crossing into `SYNCED` also freezes the slug to protect live URLs.

**SnakeYAML date caveat:** SnakeYAML auto-converts YAML timestamp literals to `java.util.Date`. `FrontMatter.parse()` extracts `date`/`lastmod` via raw text before YAML parsing to preserve the original string format. If you add date-related parsing, follow this same pattern.

## Data Flow

- **Home screen:** `HomeViewModel` observes `PostRepository.observeAllPosts()` (Room), triggers `GitHubRepository.refresh()` in background. Refresh fetches the repo tree via GitHub Trees API, fetches each `.md` file, parses front matter, and merges into Room — local `SYNCED_MODIFIED` posts are never overwritten.
- **Editor:** `EditorViewModel` autosaves to `AutosaveDao` continuously. On "Publish", calls `GitHubRepository.publish()` which calls GitHub Contents API PUT with the blob SHA. On 409/422, auto-refetches SHA and retries once.
- **Settings:** Stored in `DataStore` (non-sensitive) + `EncryptedSharedPreferences` backed by Android Keystore (PAT only). Accessed via `SettingsRepository`.

## Design System
Full reference: `docs/DESIGN_SYSTEM.md`
Whenever a task adds, modifies, or styles any UI element, read that file first and follow it. Don't invent colors, spacing, or components it doesn't cover — ask instead.

## Documentation Maintenance
Whenever a change is "significant," update documentation as part of
finishing the task — not as a separate step you wait to be asked for.

Significant changes include:
- Adding a new screen, feature, or public-facing component
- Adding/removing a dependency or changing a build/setup step
- Changing the component inventory in docs/design-system/ or docs/DESIGN_SYSTEM.md
- Changing the public behavior/usage of a documented module

What to update:
1. docs/ — update the specific file(s) affected. Don't rewrite unrelated sections.
2. README.md — only if the change affects setup steps, the feature list, or
   how someone would use the app.

IMPORTANT: Before considering any non-trivial task complete, check this list.
If you're unsure whether something counts as "significant," update the docs
anyway rather than skip it — over-documenting is cheaper than drift.

## Dependencies

- **DI:** Hilt (all `@Singleton` scoped repositories)
- **Network:** Retrofit + OkHttp + Moshi (JSON); GitHub REST API
- **Local DB:** Room with KSP code generation
- **YAML:** SnakeYAML (`SafeConstructor` to prevent unsafe deserialization)
- **Markdown preview:** Markwon
- **Build:** AGP 8.13.2, Kotlin 2.0.21, KSP for annotation processing
- **Crash reporting:** Sentry Android Gradle plugin. `defaultConfig.manifestPlaceholders["sentryRelease"]` feeds `io.sentry.release` in the manifest; it's set from the `SENTRY_RELEASE` env var (falls back to `versionName`). CI (`.github/workflows/build.yml`) computes one version string per run (`v1.0.<run number>`, or the pushed tag) and reuses it verbatim for `VERSION_NAME` (→ Gradle `versionName`), `SENTRY_RELEASE` (→ Sentry release), and the GitHub Release `tag_name`/`name` — all four are always the same string. `includeSourceContext.set(true)` means `sentry-cli` uploads proguard mappings and source context during the Gradle build; this needs `SENTRY_AUTH_TOKEN`, which CI passes from the `SENTRY_AUTH_TOKEN` repo secret (Settings > Secrets and Variables > Actions) into both build steps. Locally, the same token lives in the gitignored `sentry.properties` at the repo root.
