# Changelog

All notable changes to this project are documented here.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

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
