# Handoff: Bloggo — mobile Hugo/Markdown publisher

## Overview
Bloggo is an Android app for writing, editing, and publishing Hugo (Markdown) blog
posts directly from a phone, committing to a GitHub repository. This package documents
a high-fidelity interactive prototype covering the full app: a Home list of posts, a
Markdown Editor with a formatting toolbar and live preview, the Publish flow
(draft-flip → commit/diff confirm → push), a Settings screen, and supporting dialogs.

The source of truth for requirements is **`BloggoPRD.md`** (included). This README adds
the concrete visual + interaction spec realized in the prototype.

## About the Design Files
The files in this bundle are **design references created in HTML** — a working
prototype showing the intended look, layout, and behavior. They are **not production
code to copy directly**.

The app is specified as an **Android application** (see PRD). The task is to **recreate
these designs in the target Android environment** using its established patterns — the
recommended stack is **Jetpack Compose with Material 3**, which maps almost 1:1 onto the
prototype (the color tokens below are already Material 3 roles). If a different
environment is already in place, follow its conventions and use the prototype purely as
a visual/behavioral reference.

### How to run the prototype locally
`Bloggo.dc.html` is a "Design Component" and **requires `support.js`** (its runtime,
included in this folder) to be present in the same directory. Open `Bloggo.dc.html` in a
browser with both files side by side. `support.js` is **prototype tooling only — do not
port it**; it is the rendering runtime, not app logic. All app behavior lives in the
`<script data-dc-script>` class inside `Bloggo.dc.html`.

## Fidelity
**High-fidelity.** Final colors, typography, spacing, Material 3 component choices, and
interactions are all specified. Recreate the UI faithfully using the codebase's
Material 3 components and design tokens. The prototype's left-hand control bar, side
panel ("Home states", "App icon", "Try these flows" cards), and the desktop two-column
layout are **prototype scaffolding only** — the actual app is just the phone screen.

## Platform & Global Layout
- **Target:** Android phone, single-pane. Prototype phone frame is **392 × 828** (design
  px ≈ a 412dp-wide device). Status bar is faked at 30px tall — use the real system bar.
- **Theme:** Light / Dark / System. All colors are CSS variables that swap on a
  `data-theme` attribute; in Compose these are your `lightColorScheme` /
  `darkColorScheme`. **System** follows `prefers-color-scheme`.
- **Accent:** four selectable seed colors — Indigo (default), Green, Amber, Violet —
  each defining a full primary tonal set. This is Material You dynamic-color-style
  theming; ship Indigo as default and optionally expose the others.
- **Font:** Roboto for UI; **Roboto Mono** for all code/technical text (slugs, paths,
  dates, front matter, body editor, diffs, tokens).

---

## Screens / Views

### 1. Home (post list)
**Purpose:** Browse all posts, search, and jump into editing or creating.

**Layout (top → bottom):**
- **App bar:** title "Bloggo" (26px / 700 / letter-spacing −.5px) left; two 44×44 icon
  buttons right — Refresh (circular-arrow) and Settings (sliders/tune icon).
- **Search field:** full-width pill, `surfaceContainer` bg, 999px radius, 11×18px
  padding, leading search icon, trailing clear (×) icon that appears only when non-empty.
- **Refresh progress:** 3px indeterminate primary bar (shown while refreshing).
- **Banner (optional):** inset rounded (14px) strip below search; success =
  `successContainer`, warning = `warnContainer`, neutral = `surfaceHigh`. Has a text
  label + a trailing text action (Dismiss / Retry).
- **Sections:** two collapsible groups — **DRAFT** then **PUBLISHED**. Each header is a
  button: a rotating ▾ chevron (0° open / −90° closed), an uppercase 13px/600 label
  (letter-spacing .4px), and a count pill (`surfaceHigh`, 999px).
- **Post row card:** margin 0 12px 8px, padding 14×16, radius 18px, bg `surfaceLow`
  (hover `surfaceContainer`). Contains:
  - Title — 15.5px / 600, line-height 1.25.
  - Meta — 12.5px Roboto Mono, `onSurfaceVariant`: formatted date, plus
    "· not pushed" when unsynced.
  - **Status chips** row: `Synced` (filled `successContainer`, check icon) **or**
    `Local` (outlined, upload icon); plus an `edited` marker (warn-color dot + text)
    when a synced post has local changes.
  - **Trailing action:** draft rows show a delete (trash) icon button (hover turns red);
    published rows show a "Live" text+icon button (primary) to open the live URL.
- **FAB:** extended FAB pinned bottom-right (right 20, bottom 24), 60px tall, radius
  20px, `primary` bg, "+ New post", shadow `0 8px 22px -4px`.

**States (the prototype's "Home states" panel switches these):**
- *Normal* — populated list.
- *Empty* — centered illustration tile (84px, `primaryContainer`, document icon),
  "No posts yet" (19px/600) + helper line, max-width 230.
- *Fetch error* — warn banner "Couldn't refresh from GitHub. Showing cached posts." with
  **Retry** action; list still shows cached posts.
- *Offline* — neutral banner "Offline — cached posts available. Publish is disabled.";
  Refresh and Publish are blocked and surface a banner instead.

### 2. Editor
**Purpose:** Write/edit a post in Markdown and publish it.

**Layout:**
- **App bar:** back arrow (triggers discard-guard if dirty), title "New post" or
  "Edit post" (17px/600), and a segmented **Edit / Preview** toggle (pill, `surfaceHigh`
  track, active segment = `primary`/`onPrimary`).
- **Title input:** 24px / 700, placeholder "Post title".
- **Slug line:** "slug:" (mono) + the slug rendered as a `primaryContainer` chip. A lock
  icon appears when the slug is **frozen**. Rule: slug is auto-derived from the title
  (`lowercase`, non-alphanumerics → `-`, trimmed) on blur for new posts; once a post has
  been pushed (synced) the slug **freezes** so published URLs never break.
- **Front matter** (collapsible card, 1px `outlineVariant` border, radius 16): header row
  "Front matter" + hint "title · slug · draft managed"; expanded body is a Roboto Mono
  textarea (13px, line-height 1.6) on `surfaceLow`. Title/slug/draft are managed by the
  app and not duplicated here.
- **Body:** full-width Roboto Mono textarea, 14.5px, line-height 1.65, placeholder
  "Write in Markdown…".
- **Preview mode:** replaces the editing fields with rendered Markdown (see Markdown
  rules below). 15px / line-height 1.65.
- **Bottom bar (sticky):** on `surfaceLow`, two tiers:
  - **Formatting toolbar** (Edit mode only): 40×40 icon buttons (radius 11) — **B**
    (bold), **I** (italic, serif), **Link**, **Image**, **H** (heading). Right-aligned
    live "{n} words" counter (Roboto Mono).
  - **Actions:** "Save local" (outlined button, primary text, save icon) and "Publish"
    (filled `primary`, send icon, flex-weighted slightly larger).

**Heading flyout:** tapping **H** opens a small popover above the toolbar
(`surfaceHigh`, radius 14, shadow, `sheetUp` animation). Lists **Heading 1–6**, each with
its `#`-hash count (mono) and a label whose size steps down per level. Picking one
prefixes the current body line with the right number of `#` (replacing any existing
heading marks). The H button shows a `primaryContainer` active state while open.

**Formatting behavior:** B/I wrap the selection in `**…**` / `*…*` (inserting
placeholder text if no selection) and restore the selection. Link wraps as
`[text](https://)` and selects the URL. All edits mark the editor **dirty**.

### 3. Insert-image dialog
Bottom sheet (`surfaceHigh`, radius 28 top, drag handle, `sheetUp`). Title
"Insert image" + helper. Three size options, each a row (radius 16, `surfaceLow`):
- **Small** — max-width 480px
- **Medium** — max-width 768px
- **Large** — max-width 1200px

Picking one inserts `![alt text](<imgUrl>/<slug>-<size>.jpg)` at the cursor. (PRD: image
is resized to the chosen max width, aspect preserved, uploaded to the image path.)

### 4. Publish flow
1. **Publish** tapped. If the post is `draft: true`, show the **Draft-flip dialog**
   (centered, radius 28): explains it's marked `draft: true` and publishing flips it to
   `draft: false`. Actions: **Keep as draft** (text) / **Flip & continue** (filled).
2. **Push confirm / diff** bottom sheet (radius 28 top, drag handle, up to 82% tall):
   - Title "Confirm new post" / "Confirm push" + target file **path** (mono):
     `<contentPath>/<slug>.md`.
   - **Commit message** in a bordered mono box.
   - **Diff** in a bordered mono box: new posts show all-green `+` lines (front matter +
     first body lines); edits show context + red `-` / green `+` (e.g. the
     `draft: true → draft: false` flip). Added lines use a translucent success tint,
     removed lines a translucent red tint.
   - Actions: **Cancel** (outlined) / **Push to GitHub** | **Push changes** (filled,
     shows a spinner while pushing ~1.5s).
   - On success → return Home, mark post **Synced**, clear `edited`, show success banner
     "Published to GitHub · …".

### 5. Settings
Back arrow + "Settings" title. Sections, each a grouped `surfaceLow` card (radius 16)
with stacked rows divided by `outlineVariant` hairlines:
- **GitHub** — Personal access token (masked `••••`, eye/eye-off reveal toggle, mono);
  Repository (`meghal/blog`); Branch (`main`).
- **Paths** — Content path (`content/posts`); Image repo path (`static/images`); Image
  URL base (`/images`); Blog base URL (`https://blog.meghal.dev`). All mono inputs.
- **Default front matter** — multiline mono template; helper "{date} is auto-filled at
  creation".
- **Appearance** — Theme segmented control (Light / Dark / System).
- **Security** — "App lock" row: 40px `primaryContainer` shield tile, title +
  "Biometric unlock · token is write-capable" subtitle, and a Material switch (track
  `primary` when on; 22px knob translating 3px↔23px).

---

## Interactions & Behavior
- **Navigation:** Home ⇄ Editor ⇄ Settings, all in-app screen swaps (no URL routing).
  FAB / row tap → Editor; gear → Settings; back arrows → Home.
- **Discard guard:** leaving the editor while `dirty` opens a "Discard changes?" dialog
  (Keep editing / Discard).
- **Delete:** trash on a draft → "Delete draft?" confirm (Cancel / Delete in error red
  `#BA1A1A`) → row removed + "Draft deleted" banner.
- **Refresh:** spins the refresh icon + indeterminate bar ~1.3s → success banner. Blocked
  when offline.
- **Scrim:** dialogs sit over a `scrim` overlay; tapping the scrim dismisses the push and
  image sheets (but not destructive confirms).
- **Animations:** `fadeIn` 150ms (scrim/banner), `sheetUp` 140–200ms ease-out (sheets &
  flyout), `spin` 0.7–0.9s linear (loaders), switch/chevron transitions ~200ms,
  indeterminate progress 1s linear loop.

## State Management
Per the prototype's single component (translate to ViewModel/state holders):
- **Global:** `theme` (light|dark|system), `accent`, `screen` (home|editor|settings),
  `posts[]`, `settings{}`, `banner`, `dialog` (draftFlip|push|image|delete|discard|null),
  `pushing`, `refreshing`, `homeState`, `search`, section open flags, `patReveal`,
  `appLock`, `headingFlyout`.
- **Post model:** `{ id, title, body, fm, slug, date, draft, synced, edited }`.
  - `synced` — exists on GitHub.  `edited` — synced but has local changes.
  - `local` (derived) = `!synced`.
- **Editor (`ed`) model:** `{ id, title, body, fm, slug, draft, isNew, preview, fmOpen,
  dirty, slugFrozen }`. Slug derives on title blur unless `slugFrozen`.
- **Data fetching (real app, per PRD):** read/list/commit posts via the GitHub Contents
  API using the PAT; image upload to the image repo path. The prototype simulates these
  with timeouts.

## Design Tokens

**Light** — surface `#FCFBFE`, surfaceLow `#F6F3F9`, surfaceContainer `#F0EDF3`,
surfaceHigh `#EAE7EE`, surfaceHighest `#E4E1E9`, onSurface `#1B1B1F`, onSurfaceVariant
`#46464F`, outline `#777680`, outlineVariant `#C7C5D0`, canvas `#E7E5EC`,
scrim `rgba(0,0,0,.32)`.

**Dark** — surface `#131318`, surfaceLow `#1A1A20`, surfaceContainer `#1E1E24`,
surfaceHigh `#29292F`, surfaceHighest `#34343A`, onSurface `#E4E1E9`, onSurfaceVariant
`#C7C5D0`, outline `#918F9A`, outlineVariant `#46464F`, canvas `#0C0C0F`,
scrim `rgba(0,0,0,.6)`.

**Accent / primary (light → dark):**
- Indigo (default): primary `#4F5BD5 → #C0C1FF`, onPrimary `#FFFFFF → #1B2178`,
  primaryContainer `#E1E0FF → #373B91`, onPrimaryContainer `#11144B → #E1E0FF`.
- Green: `#006B5B → #53DBC3`, container `#71F8E0 → #005045`.
- Amber: `#8A5100 → #FFB868`, container `#FFDCBE → #693C00`.
- Violet: `#6B4EA8 → #D5BBFF`, container `#EBDDFF → #523885`.

**Semantic:** success `#3B6939`/successContainer `#BCF0B4` (dark `#A1D39A`/`#1F4620`);
warn `#8F4C00`/warnContainer `#FFDCC2` (dark `#FFB783`/`#5E3500`);
error `#BA1A1A` (destructive buttons, removed diff lines).

**Type scale:** display title 26/700; screen title 17–19/600; post title 15.5/600; body
14.5–15; meta/labels 11–13; mono everywhere for technical strings. Letter-spacing −.2 to
−.5px on large headings.

**Radii:** rows/cards 16–18; sheets/dialogs 28; pills/chips/FAB-extended 999/20; icon
buttons 999; small tiles 10–14. **App icon** corner ≈ 30% (Android adaptive squircle).

**Shadows:** cards `0 1px 3px`; FAB `0 8px 22px -4px`; sheets/flyout `0 10px 30px -6px`;
icon tile `0 4px 12px -2px`. (All use the theme `--shadow` color.)

## App Icon
**Chosen mark: "Mono dot"** (the only variant to ship — earlier exploration variants
were removed at the user's request). It is a lowercase **`b`** with a trailing **dot**,
on a near-black `onSurface` field with `surface`-colored glyph; the **dot is the accent
color** (`primary`), so it adapts with the theme/seed. Adaptive icon, ~30% rounded
corner. Provide standard Android adaptive-icon layers (foreground glyph + solid
background) at all densities; the small mark also appears as the Home app-bar logo
treatment.

## Assets
- **Icons:** all UI icons are inline SVG line icons (24×24, ~2px stroke, round caps) —
  search, settings/tune, refresh, back arrow, plus, trash, external-link, image,
  link, eye/eye-off, lock, shield-check, send, save, list. Replace with the codebase's
  Material Symbols equivalents.
- **App icon:** the Mono-dot mark is pure type + a colored dot — reproducible as a vector
  drawable, no raster asset needed.
- **Fonts:** Roboto + Roboto Mono (Google Fonts) — both standard on Android.
- **No photographic/raster assets.** Images in posts are shown as placeholder tiles in
  preview.

## Files
- `Bloggo.dc.html` — the interactive prototype (all screens, states, dialogs). App logic
  is in the `<script data-dc-script>` class near the bottom.
- `support.js` — **prototype runtime only** (required to open the HTML; do not port).
- `BloggoPRD.md` — the original product requirements document.
