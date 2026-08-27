# Bloggo design system

Extracted from `internal/design/bloggo-prototype.html` and implemented in
the `designsystem/` module. The prototype is the reference; where this document and
the prototype disagree, the prototype wins and this document is wrong.

Read this before adding or restyling any UI. Do not invent colours, spacing, or
components it does not cover.

---

## 1. Principles

**Paper and ink, not surfaces and primaries.** The palette is named for a printed
page. Tokens are `paper`, `ink`, `rule`, not `surface`, `onSurface`, `outline`.
Material 3 role names are derived from these for the sake of Material components,
but application code never reads them.

**Four typefaces, four jobs.** Every piece of text belongs to exactly one of
display, reading, UI, or mono. Mixing them up is the fastest way to stop looking
like the prototype.

**Machine facts are monospaced.** Paths, counts, dates, diffs, branch names,
character limits. If the app read it out of a file or computed it, it is mono. If
a person wrote it, it is not.

**Colour carries state, never decoration.** Amber is a draft, green is live, blue
is a pull request or a link, red is a deletion or a problem. There is no
"accent colour" available for making something look nice.

**The frame admits what it is connected to.** Screen headers carry a monospaced
subtitle naming the repo, the branch, the file count. Removing them makes screens
cleaner and the writer less informed.

---

## 2. Colour

Tokens in `BloggoColors.kt`. Both themes are complete and independent; dark is not
derived by inversion.

| Token | Light | Dark | Used for |
|---|---|---|---|
| `paper` | `#EDE9E0` | `#14130F` | Page ground, app bars, tab bar |
| `paperRaised` | `#F6F3EC` | `#1C1A15` | Cards, sheets, grouped cells, toolbar |
| `paperSunk` | `#E4DFD4` | `#0F0E0B` | Wells: diffs, code, focus mode ground |
| `paperArt` | `#E7E2D7` | `#100F0C` | Ground behind generated cover art |
| `ink` | `#191713` | `#EDE8DC` | Primary text, filled buttons |
| `inkMuted` | `#5C574D` | `#9C9484` | Secondary text, quotes |
| `inkFaint` | `#8B8477` | `#6E6759` | Metadata, labels, disabled |
| `rule` | `#D6CFC1` | `#2E2A22` | Dividers, borders, switch track |
| `ruleSoft` | `#E3DDD1` | `#241F19` | Hairlines inside cards and lists |
| `accent` | `#1D3F63` | `#8DB2D8` | Links, selection, ink 1 |
| `accentBright` | `#2F5B87` | `#A9C6E4` | Inline code text |
| `accentTint` | 10% accent | 13% accent | Icon wells, code background |
| `accentTintStrong` | 18% accent | 22% accent | Active nav, focus sightline |
| `mark` | `#8E3B3B` | `#C97F76` | Deletions, quote rule, ink 2 |
| `add` | `#3D6B4E` | `#84AE8E` | Additions, Live state, ink 4 |
| `amber` | `#8A6A24` | `#C9A44E` | Drafts, queued work, ink 3 |

Each of `mark`, `add`, `amber` has a matching `*Tint` for chip and banner
backgrounds: 10 to 12% in light, 14% in dark.

### State colours

| State | Tone | Where |
|---|---|---|
| Draft | amber | Post chips, hero rule, offline queue |
| Live | add | Post chips, build status, sync status |
| Pull request | accent | PR chips, branch references |
| Queued | mark | Outbox counts |
| Ghost | transparent + `rule` border | Neutral actions such as Change, Edit |
| Capture | `accentTintStrong` / `accentBright` | Editor header chip for an Inbox fragment preview — a bolder, distinct blue from Pull request's `accentTint`/`accent`, so an open capture and an open PR never read as the same state |

---

## 3. Typography

Four families, bundled as OFL assets in `designsystem/src/main/res/font/`. Not
downloadable fonts: the first frame would render in a fallback and the app would
flash on every cold start.

| Family | Face | Role |
|---|---|---|
| Display | Instrument Serif | Titles, and only titles |
| Reading | Newsreader (variable) | Body prose, post row titles, focus mode |
| UI | Archivo (variable) | Labels, buttons, chips, navigation |
| Mono | IBM Plex Mono | Metadata, paths, diffs, markdown source |

Sizes are the prototype's CSS pixel values read as sp. That mapping holds because
the prototype frame is 393 CSS px wide and phones are 360 to 412 dp.

| Style | Family | Size / line | Weight | Used for |
|---|---|---|---|---|
| `displayMedium` | Display | 27 / 29.7 | 400 | Screen titles |
| `displayHero` | Display | 29 / 32.5 | 400 | Hero card title |
| `displaySmall` | Display | 23 / 26.5 | 400 | Sheet titles |
| `articleTitle` | Display | 36 / 38.9 | 400 | Rendered post h1 |
| `articleHeading` | Display | 25 / 29.5 | 400 | Rendered post h2 |
| `articleSubheading` | Reading | 18 / 25 | 600 | Rendered post h3 |
| `articleBody` | Reading | 17.5 / 29 | 400 | Rendered prose |
| `articleQuote` | Reading | 18.5 / 27.8 | 400 italic | Pull quotes |
| `rowTitle` | Reading | 17 / 21.8 | 500 | Post row titles |
| `captureBody` | Reading | 15.5 / 23.3 | 400 | Inbox fragments |
| `focusBody` | Reading | 19 / 33.8 | 400 | Focus mode |
| `body` | UI | 16 / 24 | 400 | Default UI text |
| `cellTitle` | UI | 14 / 18.2 | 500 | Settings rows |
| `cellSubtitle` | UI | 11.5 / 15.5 | 400 | Settings descriptions |
| `button` | UI | 14.5 / 20 | 600 | Buttons |
| `chip` | UI | 10.5 / 14 | 600 | Chips, pills, segments |
| `eyebrow` | UI | 9.5 / 13 | 700, +1.62 sp | Section headings |
| `fieldLabel` | UI | 9.5 / 13 | 700, +1.52 sp | Form labels |
| `tabLabel` | UI | 10 / 13 | 600 | Tab bar |
| `meta` | Mono | 11 / 15 | 400 | Metadata |
| `metaSmall` | Mono | 10.5 / 14 | 400 | App bar subtitles |
| `editorSource` | Mono | 14 / 24.1 | 400 | Markdown source |
| `diff` | Mono | 11.5 / 18.6 | 400 | Diff rows |
| `monoField` | Mono | 13 / 18 | 400 | Tag field / slug / date form values |
| `statNumber` | Mono | 17 / 18.7 | 500 | Stat line counters |

Eyebrow and field labels are always uppercased at the call site, not in the font.

`displayLarge` (34 / 34, Display) also exists, for a rail/sidebar brand mark. It
has no call site in current app chrome; keep it if you're wiring up a wider
layout, drop it if a design system audit finds it's still unused.

---

## 4. Shape, spacing, motion

| Token | Value | Used for |
|---|---|---|
| `small` | 8 dp | Icon wells, tab hit areas |
| `medium` | 14 dp | Cards, cell groups, banners, buttons |
| `large` | 22 dp | Hero card |
| `thumbnail` | 10 dp | Row thumbnails, media tiles, avatars |
| `field` | 11 dp | Text fields |
| `sheet` | 26 dp top | Bottom sheets |
| `pill` | full | Chips, segments, toasts, icon buttons |

Screen gutter is 18 dp. List rows are 15 dp vertical. Cards inset 14 to 18 dp.
Section spacing comes from the eyebrow's own 22 dp top margin, not extra padding.

Transitions are 280 ms. Sheets use 340 ms on a `cubic-bezier(.22, .9, .3, 1)`
curve. Everything honours reduced-motion.

Touch targets are 48 dp even where the prototype's visual size is smaller. A 34 dp
icon button in the design gets a 48 dp hit area.

---

## 5. Icons

45 icons in `BloggoIcons.kt`. 41 are generated from the prototype's inline SVG
by `tools/icons.js` and `tools/genicons.js`: every path is the literal `d`
attribute, with `<circle>`, `<rect>` and `<line>` converted to equivalent path
data. Nothing in that set was redrawn by eye.

The remaining 4 — `Refresh`, `Trash`, `Eye`, `EyeOff`, added in Milestone 8
(PROGRESS.md) — have no prototype source to generate from; the prototype has
no refresh, delete, or reveal-secret affordance at all. These are Feather
Icons' `refresh-cw`, `trash-2`, `eye` and `eye-off`, hand-transcribed rather
than generated, chosen because Feather already shares this set's spec: 24
unit grid, stroked, round caps and joins. They're marked as such in
`BloggoIcons.kt` rather than blended silently into the generated block.

Stroked, never filled. 24 unit grid, round caps and joins.

| Size | Value | Stroke |
|---|---|---|
| Medium | 20 dp | 1.6 |
| Small | 16 dp | 1.6 |
| Tiny | 13 dp | 1.9 |

The stroke thickens at Tiny because a 1.6 stroke scaled to 13 dp reads as grey
rather than as a line. `BloggoIconDefaults.strokeFor` applies this automatically.

Icons hold path data rather than a prebuilt `ImageVector` precisely so stroke
width can vary with size. To add one: add it to the prototype first, then
regenerate. Do not hand write path data into the Kotlin file.

### App shortcut icon

`res/drawable/ic_shortcut_capture.xml` (the "Capture a thought" long-press
shortcut, `res/xml/shortcuts.xml`) is the one icon asset outside
`BloggoIcons.kt`. Static shortcuts are read by the launcher/OS before any
Compose code runs, so they need a real Android vector drawable resource, not
an `ImageVector` — there's nowhere for the Kotlin-side generation pipeline to
plug in. It reuses `BloggoIcons.Pen`'s exact path data (white stroke, 2.2 dp
at this drawable's scale) on a 48×48 dp `accent` (`#1D3F63`, light theme)
circle rather than being redrawn, keeping it in the same spirit as the
generated set even though it lives outside it.

---

## 6. Texture

Two separate mechanisms, often confused:

**Paper grain** (`Modifier.paperGrain()`) is the app's background texture, a 140 px
tiling noise bitmap drawn with multiply on light and screen on dark. Apply once,
high in the tree, on the surface representing paper. Applying it per component
stacks the texture and reads as dirt.

It is a `DrawModifierNode`, not the deprecated `composed {}` factory, which opts a
modifier out of Compose's reuse and skipping. It stays `@Composable` because it
reads the theme, and hands the node a plain boolean. The two noise tiles, light and
dark, are module-level `by lazy` values: building one runs a 19,600-iteration loop
and allocates a 78 KB bitmap, and neither tile depends on anything but its own seed,
so neither is ever rebuilt — including across a theme flip.

**Cover grain** is per pixel noise applied inside a generated cover at render
time, so an exported PNG carries its own grain. See `CoverArtRenderer`.

minSdk 34 would allow an AGSL `RuntimeShader` for the paper grain. The tile is
used instead: at this opacity the two are indistinguishable and the tile costs one
texture upload rather than a fragment program per frame.

---

## 7. Components

All in `designsystem/component/`. Each has an `@Preview`.

### Primitives

| Component | Notes |
|---|---|
| `BloggoChip` | Tone carries meaning; never takes a free-form colour. Shows a dot for states describing the post itself, not for counts or PR numbers. Optional `onRemove` adds a trailing `×` at 60% opacity, matching `.tag button{opacity:.6}` in the prototype. |
| `Eyebrow` | Uppercase label plus a rule to the edge. The rule is what stops sections reading as a wall of text. |
| `MetaText` | Monospaced metadata. |
| `Banner` | Inline notice, info or warning, with optional action. |
| `StatLine` | Three counters, hairline separated. |
| `BloggoTagField` | Editable tag chips (`BloggoChip` + `onRemove`) plus a "+ add" affordance that reveals an inline autocomplete panel. Takes the caller's already-computed suggestion pool — it only filters, never recomputes the pool itself — and offers a "Create '…'" row when nothing matches. The chip and "+ add" look follow `.tag` / `.tag-add` in the prototype; the autocomplete panel has no prototype counterpart, since the prototype's `.tag-add` is inert. Used by `PostDetailsSheet` for frontmatter `tags:`. |

### Controls

| Component | Notes |
|---|---|
| `BloggoButton` | Filled or ghost. One filled button per view: it is the commit action. |
| `BloggoIconButton` | Circular, 48 dp target, optional filled variant. |
| `SegmentedControl` | Two or three options. More than three is a list, not a switch. |
| `BloggoPill` | Scrolling filters. Unlike the segmented control this can hold many. |
| `BloggoSwitch` | 46 × 27 track, 21 dp thumb. |
| `ToolbarButton` | Editor formatting toolbar slot. |
| `BloggoFab` | 56 dp `ink` circle, `paper`-tinted icon — the same dark-circle-on-paper language as `BloggoTabBar`'s raised compose action, sized and positioned for a screen corner instead. Used by the Inbox tab to open its capture overlay. |

### Rows and containers

| Component | Notes |
|---|---|
| `HeroCard` | The in-progress post. With `ArtMode.None` the art becomes a 3 dp amber top rule and the chip moves inline. Never simply hide the image: the card loses its top edge. |
| `PostRow` | Thumbnail, title, chip, meta, optional live-page button. |
| `CaptureRow` | Inbox fragment, marked by an oversized opening quote. |
| `CellGroup` / `Cell` | Grouped settings rows. |

### Chrome

| Component | Notes |
|---|---|
| `BloggoAppBar` | Serif title, mono subtitle, trailing actions. |
| `BloggoTabBar` | Four tabs around a raised compose action. The compose button is not a tab and has no selected state: it starts a post rather than navigating. |
| `LinkBar` | Permalink strip. Shows the URL in both states; a draft's is greyed with "Not published". |
| `BloggoToast` | Transient confirmation. Never for anything requiring action. |
| Search app bar (`LibraryScreen.kt`, private) | The Library search icon swaps `BloggoAppBar` for a row of the same height: back chevron, a `BasicTextField` in `type.body` with an `inkFaint` placeholder, and a `Close` "clear" icon once there's a query. Not promoted to `designsystem/component/` yet — it's used by one screen — but it's the pattern to reuse (not reinvent) the next time a screen needs search-in-app-bar. Results replace the grouped draft/PR/published sections with a flat, cross-section list; a query that matches nothing shows a plain "No posts found" message in `cellTitle`/`meta`, since the app has no illustrated empty-state to match instead. |

---

## 8. Cover art

The `coverart/` module is standalone with no dependency on the design
system, so it can be dropped into any app or run from a build script.

Ported from the prototype's `paint()`, and verified against it: the golden values
in `MulberryRngTest` and `CoverArtPlannerTest` are the JavaScript implementation's
own output, so a seed produces the same picture on both platforms.

- **Seed.** FNV-1a over the post slug, so the same post always gets the same
  cover. `String.hashCode` was rejected because its value is not stable across
  languages. Shuffle increments a variant counter rather than randomising, which
  keeps a chosen cover reproducible from `slug` plus one small integer.
- **Composition.** Three to five overlapping forms drawn from discs, rotated
  bands, and wedges, in four inks at 18 to 48% alpha, multiplied on light and
  screened on dark, then three hairline register marks, then per pixel grain.
- **Sizes.** Thumbnail 120², Hero 480 × 270, Export 1200 × 630.
- **One rendering path.** Thumbnails and exports both rasterise through
  `CoverArtRenderer`. An earlier design drew thumbnails with cheap Compose
  primitives and only rasterised on export, which meant the picture you approved
  was not the picture you shipped.
- **Rendered once, cached process-wide.** `CoverArtCache` is an `LruCache` keyed
  on `(seed, width, height, palette)` and sized in bytes, capped at 8 MB.
  `remember` is not a cache for this: it is scoped to a composable instance, and
  lazy layouts dispose rows that scroll off-screen, so every row scrolling back
  into view was re-rasterising from scratch.
- **Never rasterised in composition.** A cache miss renders on
  `Dispatchers.Default` and the composable shows flat `palette.paper` for the
  frame or two until it lands. A hit is synchronous, so a cover drawn once never
  flashes its placeholder again.

### A porting hazard worth knowing

The JavaScript seed multiply, `seed * 2654435761`, exceeds the 53 bit double
mantissa for any seed above roughly two million, so the original is working from
an already-rounded value. Slug-derived seeds are always in that range. Doing the
multiply exactly in `Long` is more correct in the abstract and produces a
different generator, which would have silently changed every cover. `MulberryRng`
reproduces the rounding deliberately.

---

## 9. The cover-art switch

The prototype and the app both carry generated art behind one switch
(`ArtMode.Generated` / `ArtMode.None`), settable on the Settings screen.

With art off, typography carries the hierarchy: the hero gets an amber top rule
and an inline status chip, rows lose their thumbnails and gain text width. The
media library is unaffected, because those tiles stand for the writer's own
photographs rather than generated decoration.

---

## 10. Adding to the system

1. Add it to `internal/design/bloggo-prototype.html` first. The prototype is the reference.
2. Implement it in `designsystem/`, reading tokens through `BloggoTheme`.
3. Give it an `@Preview`, and a second one for the state that is easy to get
   wrong (no art, over limit, empty).
4. Update this document.

If you find yourself needing a colour or a size this document does not have, that
is a design decision, not an implementation detail. Raise it rather than picking
a hex value.
