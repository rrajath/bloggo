# Bloggo

A phone-first writing studio for a git-backed Hugo blog. Draft a post on a train
with no signal, generate a cover for it, and have it land on `main` the moment
you get reception, with no laptop involved.

This repository holds the design work and the Android foundation for that app.

## The problem

Publishing to a static site from a phone is unreasonably hard. The options are a
GitHub web editor that fights you on a touch screen, a CMS that takes ownership
of your files, or waiting until you are back at a desk. Meanwhile the useful half
of writing, the half where you catch a thought and get it down, happens away from
the desk.

Bloggo keeps the repository as the source of truth and puts a real writing
surface in front of it.

## What is here

| Path | What it is |
|---|---|
| `design/bloggo-prototype.html` | The interactive UI prototype. One file, no build step. Open it in a browser, or on a phone, where the device frame drops away. |
| `android/` | A buildable Compose project: the cover art generator, the design system, and every screen laid out against sample data. |
| `docs/DESIGN_SYSTEM.md` | Tokens, typography, icons, components. Read before touching any UI. |
| `docs/ANDROID_TDD.md` | Technical design for building the app: GitHub layer, offline outbox, auth, milestones, risks. |
| `docs/PROTOTYPE_NOTES.md` | Why the prototype is the way it is. |
| `tools/` | Icon extraction and Kotlin generation from the prototype's SVG. |

## Features

**In the prototype and laid out in the Android scaffold**

- Post library split by what needs attention: in progress, open pull request,
  published
- Markdown editor showing real source with live styling, dimmed syntax markers
- Read mode rendered with the site's typography, including Hugo shortcodes
- Frontmatter editing sourced from `archetypes/default.md` and `hugo.toml`
- Generated cover art, seeded from the post slug so it is stable forever
- Commit straight to the configured branch through the real Git Data API —
  message, file list, one commit for the post and any images it references.
  Opening a pull request instead, a line-level diff, and an offline queue are
  still ahead (P0 items 6–7)
- Media library over `static/images/`: real repo images plus anything staged
  this session but not committed yet, insertable into a post from the
  editor's Insert row
- Preview actually renders images (repo-hosted or still-staged) and
  `http(s)`-sourced video, not placeholder boxes
- Capture inbox for fragments that are not posts yet, persisted durably
  (Room) so a captured-but-unpromoted thought survives a process death.
  Capture is a FAB-triggered overlay (fixed-height text area, keyboard-
  avoiding, with list auto-continuation on Enter and Save/Discard), not an
  always-visible inline field. Opening a fragment previews it in the Editor
  without touching the Inbox; a "Promote to Post" button in Post details is
  what actually turns it into a real draft.
- "Capture a thought" app shortcut (long-press the launcher icon, Android
  only — no equivalent in the HTML prototype) opens straight to the Inbox
  with the capture overlay already open and focused
- Pages tab (Android only, in place of the old Media tab — see
  `docs/PROTOTYPE_NOTES.md`'s "Pages replaced Media"): the top-level
  `content/*.md` files a Hugo site has (`about.md`, `uses.md`, and so on),
  pulled straight from the connected repo. A page already pushed opens into
  Preview; one that only exists locally opens into Editor, same as a post's
  draft/published split. Editing a page stamps its frontmatter `lastmod:` to
  now.
- Share (from Preview, for either a post or a page) hands the title and the
  live URL to the native Android share sheet — a page's share text also
  includes the author name set on the Settings screen, when one is set
- Live page access from the library, the preview, and the repo screen
- Cover art can be switched off entirely, and the layouts hold up without it
- Light and dark, both complete palettes

**Deferred**

- Focus mode with a typewriter sightline and a session goal (P2)
- Voice capture in the inbox (P2)
- Share to several Mastodon accounts at once, with per-instance character
  limits (`MastodonScreen.kt`) — built, but no longer reachable from
  anywhere in the app now that Preview's Share button opens the native
  share sheet instead. Left in place rather than deleted; see PROGRESS.md.

## Setup

### The prototype

```sh
open design/bloggo-prototype.html
```

No dependencies, no server. Everything runs from the one file.

### The Android project

Requires JDK 17 or newer and the Android SDK (compileSdk 36, build-tools 36).

```sh
cd android
./gradlew assembleDebug          # build
./gradlew testDebugUnitTest      # 115 unit tests
./gradlew installDebug           # to a connected device or emulator
```

Or with the `android` CLI:

```sh
android emulator start Pixel_9_Pro
android run
```

minSdk is 34.

### Regenerating the icon set

Icons are generated from the prototype's inline SVG so they cannot drift. After
changing an icon there:

```sh
node tools/icons.js       # extract and normalise to path data
node tools/genicons.js    # write BloggoIcons.kt
```

## How the pieces relate

The prototype is the reference. The design system is the prototype's tokens made
into Kotlin. The Android scaffold is those components assembled into screens. When
they disagree, the prototype is right.

The cover art generator is the exception worth knowing about: it is a port of the
prototype's `paint()` verified against the JavaScript original's own output, so a
given post slug produces a byte-identical picture on both platforms. See
`docs/DESIGN_SYSTEM.md` §8.

## Status

The prototype is complete. The Android project builds, runs, and talks to a real
connected GitHub repository: validating the connection and detecting Hugo,
listing and caching posts and images, and committing a post (with any images it
references) straight to the configured branch through the Git Data API. 115
unit tests across `:app` alone, plus the cover generator's and the markdown
highlighter's own suites in `:coverart`/`:designsystem`.

Still ahead: opening a pull request instead of a direct commit, a line-level
diff before publishing, and an offline outbox for queued commits — see
`docs/ANDROID_TDD.md` §13/§16 for the exact scope.
