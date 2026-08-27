# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A phone app for writing and publishing posts to a git-backed Hugo blog, and sharing them to Mastodon. The phone never holds a git repository: every read and write goes through the GitHub REST API. Full design rationale is in `internal/docs/ANDROID_TDD.md`.

## Commands

```bash
# Build the debug APK
./gradlew assembleDebug

# Build the release APK (needs signing env vars, see below)
./gradlew assembleRelease

# Run all JVM unit tests
./gradlew test

# Run one test class
./gradlew :app:testDebugUnitTest --tests "com.rrajath.bloggo.model.ModelTest"

# Run instrumented (on-device) tests, needs a connected device or emulator
./gradlew connectedDebugAndroidTest

# Lint
./gradlew lint

# Install the debug build on a connected device
./gradlew installDebug
```

## Module layout

Three Gradle modules (`settings.gradle.kts`):

- `:app` — the application: navigation shell, screens, data layer. namespace `com.rrajath.bloggo`.
- `:designsystem` — Compose theme, typography, bundled OFL fonts, shared components, the markdown editor's `VisualTransformation`. namespace `com.rrajath.bloggo.designsystem`. Depends on `:coverart`.
- `:coverart` — deterministic generative cover-image rendering (seeded RNG, palettes, plan, renderer). namespace `com.rrajath.bloggo.coverart`. No other project dependencies.

`:app` depends on both. `:designsystem` re-exports its Compose deps with `api(...)` so `:app` picks them up transitively.

Build config: AGP/Kotlin versions in `gradle/libs.versions.toml`, `minSdk 34`, `compileSdk 36`, Java 17 toolchain, `buildConfig` disabled in every module.

## Architecture

Single-Activity Compose app. There is no Navigation Compose and there are no ViewModels.

- **`MainActivity`** — thin shell. `singleTask` launch mode plus `onNewIntent` so the "Capture a thought" launcher shortcut (`ACTION_CAPTURE_THOUGHT`) reaches the already-composed app instead of starting a second Activity.
- **`BloggoApp`** (`BloggoApp.kt`) — owns everything: a hand-rolled back stack (`backStack: SnapshotStateList<Route>` plus a `go()` helper), all screen state hoisted as `remember { mutableStateOf(...) }`, and manual dependency construction (`remember { SettingsRepository(context) }` and friends). No DI framework.
- **`Route`** — `sealed interface` with `Library`, `Inbox`, `Pages`, `Media`, `Settings` (renders `RepoScreen`), `Editor(slug)`, `Preview(slug, published)`, `Focus(slug)`, `Mastodon`.
- **`ui/<feature>/`** — one package per screen (`editor`, `focus`, `inbox`, `library`, `mastodon`, `media`, `pages`, `preview`, `repo`, `sheet`). Screens are stateless composables that take data and callbacks from `BloggoApp`.
- **`data/`** — repositories talk to GitHub, stores/caches hold local state:
  - `github/GitHubClient`, `github/GitHubApi` — the Retrofit + kotlinx.serialization GitHub client.
  - `library/PostLibraryRepository`, `library/PageLibraryRepository` — list/fetch posts and pages, backed by a Room cache (`PostCache`, `PageCache`).
  - `library/LocalPostStore` — drafts that exist only on the device.
  - `inbox/FragmentStore` — captured thoughts ("fragments").
  - `media/MediaRepository`, `media/MediaStaging` — repo images and pending uploads.
  - `publish/PostPublishRepository` — the GitHub Contents API PUT with retry on stale SHA.
  - `RepoConnectionRepository`, `SettingsRepository`, `RepoPaths`, `SampleData`.
- **`model/Model.kt`** — pure-Kotlin domain: `Post`, `Fragment`, `MediaFile`, `MastodonAccount`, `PostState`, and all frontmatter parsing/assembly and slug helpers. Parse frontmatter through these helpers, never with ad-hoc regex elsewhere.

Room is used only as a local cache for the library listing, not as a source of truth. The repository is the source of truth.

## Persistence

- Room (`BloggoDatabase.get(context)`) — post/page frontmatter cache, local drafts, fragments.
- DataStore Preferences — theme mode and other small typed settings, via `SettingsRepository`.
- `EncryptedSharedPreferences` (Android Keystore) — the GitHub PAT only, via `RepoConnectionRepository`.

## Testing

- **JVM unit tests** (`app/src/test`, plus `:coverart` and `:designsystem`): plain JUnit 4 (`org.junit.Assert.*`), `kotlinx-coroutines-test` (`runTest`), and OkHttp `MockWebServer` for the GitHub client. No Robolectric, Truth, MockK, or Turbine.
- **Instrumented tests** (`app/src/androidTest`): Compose UI tests with `createComposeRule`, covering editor input, focus-screen caret behavior, and inbox focus.

## Design system

Reference: `internal/docs/DESIGN_SYSTEM.md`. Prototype: `internal/design/bloggo-prototype.html`.
Whenever a task adds, modifies, or styles any UI element, read the design system doc first and follow it. Do not invent colors, spacing, or components it does not cover. Ask instead.

## Documentation maintenance

When a change is significant, update docs as part of finishing the task, not as a separate step:

- Adding a screen, feature, or public component
- Adding/removing a dependency or changing a build/setup step
- Changing the component inventory in `internal/docs/DESIGN_SYSTEM.md`
- Changing the public behavior of a documented module

Update the specific file(s) in `internal/docs/` that are affected, and `README.md` only if setup steps, the feature list, or usage changed. If unsure whether something counts, update the docs anyway.

## Working notes

`internal/PROGRESS.md` and `internal/PERF_IMPROVEMENT.md` are local working notes. `PROGRESS.md` is gitignored.

## Release build and CI

`.github/workflows/build.yml` builds debug and release APKs on every push to `main` and on `v*` tags, and cuts a GitHub Release. The workflow still decodes a keystore and passes `KEYSTORE_PATH`, `KEY_STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, `BUILD_NUMBER`, `VERSION_NAME`, `SENTRY_RELEASE`, and `SENTRY_AUTH_TOKEN` into the Gradle build.

Known gap after the multi-module import: `app/build.gradle.kts` no longer reads any of those. The Sentry Gradle plugin, the `signingConfigs` block, and the `System.getenv("BUILD_NUMBER" / "VERSION_NAME")` version wiring were all dropped. Until that is restored, CI release APKs are not release-signed and are not versioned from the run number. The previous wiring is in `git show 479c8bb:app/build.gradle.kts`.
