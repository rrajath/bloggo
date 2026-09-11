# Changelog

All notable changes to this project are documented here.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Changed

- Publishing a post now leaves the editor for the Library (or the Pages list,
  for a page) and refreshes it, so the writer lands back on the list with the
  post already moved out of Drafts instead of staying on the editor.

- Publishing a post that still has `draft: true` in its frontmatter now asks
  first whether to set the flag to `false`. Yes flips it in the same commit;
  No publishes it as a draft with the flag left as is.
- Pasting a link that already carries its own `http://` or `https://` into the
  URL slot the Link toolbar button prefills now replaces the prefilled
  `https://` instead of stacking on it.

### Fixed

- The Publish sheet's "Changes" list now shows the file path built from the
  frontmatter `slug:`, matching the file actually committed, so a slug edited
  in the editor is reflected there. Already-committed posts keep their original
  filename.
- The quote rule in the article preview now matches the height of the quote
  text instead of a fixed two-line rule that overhung a single-line quote.
- The editor now scrolls the cursor's line into view as the keyboard opens or
  the cursor moves while typing, instead of leaving it hidden under the IME.
- Returning to the editor from Preview or Review for the same post no longer
  jumps the scroll position to the bottom of the document. The editor now
  restores the cursor position the writer left it at, instead of always
  resetting the caret to the end of the document on every fresh mount.

## [1.2.0] - 2026-08-28

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
- Repo Settings has a Frontmatter type control (YAML / TOML). New posts and
  new pages open with the chosen fence style: YAML `---` / `key: value` or
  TOML `+++` / `key = value`. Existing files are still parsed either way.
- The Inbox shows a placeholder ("Nothing captured yet") when no fragments
  have been captured.

### Changed

- The release build now runs R8 code shrinking and resource shrinking
  (`isMinifyEnabled` / `isShrinkResources`), cutting the release APK from
  roughly 31 MB to about 5 MB. Keep rules added in `app/proguard-rules.pro`.
- Long-pressing a readability highlight now gives haptic feedback the moment
  the "ignore" confirmation opens.
- Library cards and rows are now typography-only: the hero card keeps its
  amber top rule and inline status chip, list rows gain full text width.
- The Library top bar subtitle now shows just the repository and branch, and
  only once the connection has verified.

### Fixed

### Removed

- Hugo config file detection. The connection check no longer lists the repo
  root to find a `hugo.toml` / `config.toml` candidate, `ConnectionCheck`
  no longer reports it, the Repo Settings "Framework" row (which edited the
  detected filename) is gone, the `hugo_config_file` setting and its
  `hugoConfigFile` backup field are removed, and the connection banner drops
  the "Hugo detected" clause.
- Generated cover art, everywhere it appeared: the library hero/thumbnail
  art, the post-details Cover section, the Appearance "Cover art" toggle, the
  `cover:` frontmatter field the app read and wrote, and the `cover` column in
  the post and local-draft caches (Room migration 7 -> 8). The `:coverart`
  module is kept but is no longer wired into the app.

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
