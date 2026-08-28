# Changelog

All notable changes to this project are documented here.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added

- Settings is now a menu of six sub-pages (GitHub Connection, Repo Settings,
  Publishing, Appearance, Readability Review, Import / Export), each with a
  one-line description on the menu row.
- Settings import/export: back up every setting except the access token to a
  JSON file, and restore it on this or another device.
- The installed app version is shown in a footer on the Settings menu.
- Every GitHub connection field now has a placeholder, and the Site URL field
  prefills `https://` the first time it is focused.
- Separate `debug` build variant: debug builds install alongside release as
  their own app (`com.rrajath.bloggo.debug`, "Bloggo Debug", version suffixed
  " (debug)"), with a matching debug copy of the launcher-shortcut config.

### Changed

- The release build now runs R8 code shrinking and resource shrinking
  (`isMinifyEnabled` / `isShrinkResources`), cutting the release APK from
  roughly 31 MB to about 5 MB. Keep rules added in `app/proguard-rules.pro`.

### Fixed

### Removed

## [1.1.0] - 2026-08-28

### Added

- Readability review screen: surfaces readability findings on a draft, lets the
  writer ignore individual findings per post, and recomputes on demand.
- Hand-managed release versioning: `bloggo.versionName` in `gradle.properties`
  drives both `versionName` and a derived `versionCode`.
- Release signing wired into the `:app` release build type, reading keystore
  credentials from environment variables.

### Changed

- Restructured the app into three Gradle modules: `:app`, `:designsystem`, and
  `:coverart`.
- Readability findings from the review screen are now washed onto the prose
  itself rather than shown only as a separate notes list.
- Centered `StatLine` labels.

### Fixed

### Removed
