# Learnings

## 2026-08-27 — Settings screen split into a menu of sub-pages

**Files:** `app/BloggoApp.kt`, `app/ui/settings/*` (new, replaces `app/ui/repo/RepoScreen.kt`),
`app/data/RepoConnectionRepository.kt`, `app/data/SettingsBackup.kt` (new),
`app/proguard-rules.pro`, `app/src/test/.../SettingsBackupTest.kt` +
`RepoConnectionTest.kt` (new), `app/src/androidTest/.../SettingsNavigationTest.kt` (new),
docs.

**Problem / goal:** one long scrolling Settings screen became a menu of six navigable
sub-pages, plus a version footer, field placeholders, `https://` prefill on the Site URL
field, and settings import/export to JSON. Plan: `internal/settings-split-plan.md`.

**Decisions / fixes:**
- **App version without `BuildConfig`.** `buildConfig` is disabled project-wide, so the
  footer reads `context.packageManager.getPackageInfo(context.packageName, 0).versionName`.
  The `debug` build type's `versionNameSuffix = " (debug)"` means a debug build already
  reports `1.1.0 (debug)` here — no extra work to meet the "show the suffix" requirement.
- **`ActivityResultContracts.OpenDocument` must be instantiated** — `OpenDocument()`, not
  `OpenDocument`. `CreateDocument` takes a mime-type constructor arg. Passing the class
  object instead gave "Cannot infer type for type parameter 'I'/'O'" and "does not have a
  companion object".
- **Site URL now stores the full URL** (`RepoConnection.siteUrl`, scheme included, no
  trailing slash). `siteHost` became a derived `val` (`siteUrl.substringAfter("://")
  .trimEnd('/')`) so `Post.liveUrl` / permalink call sites did not change. Migration on
  read: `prefs[siteUrlKey] ?: prefs[siteHostKey]?.let { "https://$it" } ?: ""`.
- **No-prototype divergence.** The prototype has a single settings fold; the split menu +
  detail pages were built from existing `Cell`/`CellGroup`/`BloggoAppBar` components, noted
  as a deliberate divergence in `DESIGN_SYSTEM.md`.
- **`SettingsBackup` has no token field** and a test asserts every serializer-descriptor
  element name is free of token/pat/secret/credential/password (the earlier "JSON must not
  contain 'pat'" check was a false positive — `postPath` contains "pat").
- Added an R8 keep rule for `SettingsBackup$$serializer` since the release build is minified
  and a new `@Serializable` type was introduced.

**Verification:** `./gradlew :app:testDebugUnitTest` (9 new tests green), `assembleDebug`,
`assembleRelease` (R8, green), and `SettingsNavigationTest` on `emulator-5554` (2 tests
green). Three pre-existing instrumented tests remain flaky on the emulator
(`EditorScreenRapidInputTest` x2 — rapid ADB input; `InboxScreenFocusTest` — cold-launch
window-focus race, per its own doc comment); none touch settings.

## 2026-08-27 — Release v1.1.0: version number discrepancy after multi-module restructure

**Files:** `gradle.properties`, `.github/workflows/build.yml`, `CHANGELOG.md`, `CLAUDE.md`, `README.md`

**Problem:** Cutting the first release after the multi-module restructure. `gradle.properties`
had `bloggo.versionName=1.0.1`, but the remote already carried release tags through `v1.0.10`
(all ancestors of `main`, dated up to 2026-08-12). A `v1.0.1` tag also already existed. The
restructure commit (`3fcad30`) had reset the hand-managed version string to `1.0.1` without
accounting for the release history that survived on the same branch.

**Fix / decision:**
- Released as **v1.1.0** (minor bump), chosen to continue the existing sequence from `v1.0.10`
  while signalling the restructure. `1.0.11` would have worked too; `1.0.1` as-is was impossible
  (tag collision).
- Seeded `CHANGELOG.md` (Keep a Changelog) with the six commits since `v1.0.10` under
  `[Unreleased]`.
- Did **not** run the android-dev-workflow skill's `setup-changelog-ci` verbatim: its template
  writes a competing `.github/workflows/release.yml` that also triggers on `v*.*.*` and uses
  different signing-secret names (`ANDROID_KEYSTORE_BASE64` vs this repo's `KEYSTORE_BASE64`).
  Instead added the changelog-move step (awk) directly into the existing `build.yml` tag job,
  which commits the promoted section back to `main`.
- Added a short "Changelog" section to `CLAUDE.md` and updated `README.md` rather than merging
  the skill's full standing-rules block (most of it duplicated existing project conventions).

**Notes:**
- `versionCode` derives as `MAJOR*10000 + MINOR*100 + PATCH`, so `1.1.0` -> `10100`.
- The skill's stock changelog-move awk only works when a prior `## [` version section already
  exists below `[Unreleased]`; rewrote it to handle the first-release case (no prior section).
- After the tag build runs, `jj git fetch` to pick up CI's "Move CHANGELOG Unreleased entries
  into v1.1.0" commit on `main`.

## 2026-08-27 — Enabled R8 for the release build (31 MB -> 5 MB APK)

**Files:** `app/build.gradle.kts`, `app/proguard-rules.pro` (new), `CLAUDE.md`, `README.md`, `CHANGELOG.md`

**Problem:** The release APK was ~31 MB. `release { isMinifyEnabled = false }` meant R8 never
ran: all of Compose, Retrofit, kotlinx.serialization, OkHttp, Room, and the Kotlin stdlib were
packaged whole across three DEX files (~29 MB of code), plus unshrunk resources.

**Fix:**
- `isMinifyEnabled = true` and `isShrinkResources = true` on the `release` build type.
- Created `app/proguard-rules.pro` (the build already referenced it but the file did not exist;
  harmless only while minify was off). Contents: keep rules for the
  `com.rrajath.bloggo.data.github.**` DTO `$$serializer` classes and Companions, a
  kotlinx.serialization.json backstop, and `-dontwarn com.google.errorprone.annotations.**`.
- The errorprone `-dontwarn` is required: Tink (pulled in transitively by
  `androidx.security-crypto`, used for the encrypted PAT store) references
  `com.google.errorprone.annotations.{CanIgnoreReturnValue,CheckReturnValue,Immutable,RestrictedApi}`
  which are not on the runtime classpath. First R8 run failed with "Missing classes detected";
  AGP wrote the exact `-dontwarn` lines to
  `app/build/intermediates/mapping/release/minifyReleaseWithR8/missing_rules.txt`.

**Result:** release APK 31 MB -> 5.0 MB. `classes.dex` 29 MB (3 files) -> 3.5 MB (single dex,
multidex no longer triggered). `resources.arsc` 475 KB -> 132 KB. `./gradlew test` passes.

**Verification:** `assembleRelease` produces an unsigned APK, so signed it with the debug
keystore (`apksigner sign --ks ~/.android/debug.keystore --ks-pass pass:android ...`) purely for
a local smoke test, installed on `emulator-5554`, and exercised Library -> editor -> preview and
tab navigation. No crashes, no `SerializationException`/`ClassNotFound`/`LinkageError` in
logcat. Confirmed in `app/build/outputs/mapping/release/mapping.txt` that every GitHub DTO
`$$serializer` is kept and unobfuscated.

**Notes:**
- Retrofit 3, OkHttp 5, Room, Coil3, and the Kotlin serialization Gradle plugin all ship their
  own consumer/generated R8 rules, so the hand-written keep list stays small.
- Debug builds are not minified; a reflection/serialization regression will only surface in a
  release build. Smoke-test release after dependency bumps or DTO changes.
- The debug-signed test APK has a different signature from `installDebug`; uninstall
  `com.rrajath.bloggo` from the emulator before the next debug install.

## 2026-08-27 — create-app-variants: two AGP gotchas on this project

**Files:** `app/build.gradle.kts`, `app/src/main/res/values/strings.xml`,
`app/src/debug/res/xml/shortcuts.xml` (new), `CLAUDE.md`, `README.md`,
`internal/docs/ANDROID_TDD.md`, `CHANGELOG.md`

**Goal:** debug build type installs alongside release — `applicationIdSuffix = ".debug"`,
`versionNameSuffix = " (debug)"`, app name "Bloggo Debug".

**Gotcha 1 — `resValue` needs opting in here.** `resValue("string", "app_name", ...)`
failed configuration with `defaultConfig contains custom resource values, but the feature
is disabled`. This project's AGP defaults `resValues` off (same posture as `buildConfig = false`).
Fix: `buildFeatures { resValues = true }` in `:app`. Also moved `app_name` out of
`strings.xml` entirely and defined it via `resValue` in `defaultConfig` ("Bloggo") — defining
it in both `strings.xml` and `resValue` is a duplicate-resource error, so the string resource
has to live in exactly one place.

**Gotcha 2 — `${applicationId}` is not substituted in `res/xml`.** `res/xml/shortcuts.xml`
hardcodes `android:targetPackage="com.rrajath.bloggo"`. With the `.debug` suffix the static
"Capture a thought" launcher shortcut would not resolve on debug installs. Placeholders only
apply to the manifest, not arbitrary XML resources, so the fix is a source-set override:
`app/src/debug/res/xml/shortcuts.xml`, identical except `targetPackage` ends `.debug`. The two
files must be kept in sync by hand. `targetClass` stays `com.rrajath.bloggo.MainActivity` —
the suffix changes `applicationId`, not `namespace`.

**Verification:** `./gradlew assembleDebug assembleRelease test` all pass. `aapt dump badging`:
debug = `com.rrajath.bloggo.debug` / "Bloggo Debug" / `1.1.0 (debug)`; release =
`com.rrajath.bloggo` / "Bloggo" / `1.1.0` (untouched). Confirmed the debug APK's shortcuts.xml
carries the `.debug` targetPackage and release's still carries the plain one (resource-name
shortened to `res/Eq.xml` by the release resource optimizer, content intact). Installed the
debug variant on the emulator alongside nothing and launched it clean.
