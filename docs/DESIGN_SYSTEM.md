# Bloggo Design System

Source of truth for color, typography, spacing, shape, icons, and reusable components.
When adding or styling UI, read the relevant section here first.

---

## 1. Color Tokens

### 1.1 Theme structure

The theme is built from two axes that compose independently:

| Axis | Options | Source |
|------|---------|--------|
| **Mode** | `LIGHT`, `DARK`, `SYSTEM` | `ThemeMode` enum |
| **Accent** | `INDIGO` (default), `GREEN`, `AMBER`, `VIOLET` | `Accent` enum |

`BloggoTheme(themeMode, accent)` wires both axes into a `MaterialTheme` + a companion `BloggoColors` object for semantic extras.

### 1.2 Surface palette (accent-independent)

These slots never change with the accent. Prefer these over `Color(0x…)` literals.

#### Light mode

| Token | Hex | Role |
|-------|-----|------|
| `surfaceContainerLowest` / `surface` | `#FCFBFE` | Base page background |
| `surfaceContainerLow` | `#F6F3F9` | Card background, inset panels |
| `surfaceContainer` | `#F0EDF3` | Search bar, mid-elevation surfaces |
| `surfaceContainerHigh` | `#EAE7EE` | Count badges, Local chip, toggle track |
| `surfaceContainerHighest` | `#E4E1E9` | Highest-elevation inert surface |
| `background` | `#E7E5EC` | Window/scaffold background |
| `onSurface` | `#1B1B1F` | Primary text |
| `onSurfaceVariant` | `#46464F` | Secondary text, icons, labels |
| `outline` | `#777680` | Borders, dividers |
| `outlineVariant` | `#C7C5D0` | Subtle borders (card outlines, dividers) |
| `scrim` | `#52000000` | Modal backdrop |

#### Dark mode

| Token | Hex | Role |
|-------|-----|------|
| `surfaceContainerLowest` / `surface` | `#131318` | Base page background |
| `surfaceContainerLow` | `#1A1A20` | Card background |
| `surfaceContainer` | `#1E1E24` | Search bar, mid-elevation surfaces |
| `surfaceContainerHigh` | `#29292F` | Count badges, toggle track |
| `surfaceContainerHighest` | `#34343A` | Highest-elevation inert surface |
| `background` | `#0C0C0F` | Window/scaffold background |
| `onSurface` | `#E4E1E9` | Primary text |
| `onSurfaceVariant` | `#C7C5D0` | Secondary text, icons, labels |
| `outline` | `#918F9A` | Borders |
| `outlineVariant` | `#46464F` | Subtle borders |
| `scrim` | `#99000000` | Modal backdrop |

### 1.3 Accent / primary palette

The `primary` / `onPrimary` / `primaryContainer` / `onPrimaryContainer` slots are provided by the active accent. `secondary` and `tertiary` are intentionally aliased to `primary` values (single-accent design).

| Accent | Light primary | Dark primary | Character |
|--------|---------------|--------------|-----------|
| Indigo | `#4F5BD5` | `#C0C1FF` | Default; cool blue-purple |
| Green | `#006B5B` | `#53DBC3` | Teal-green |
| Amber | `#8A5100` | `#FFB868` | Warm amber |
| Violet | `#6B4EA8` | `#D5BBFF` | Deep violet |

Access via `MaterialTheme.colorScheme.primary` — never hard-code an accent hex.

**Exception (known issue):** The FAB in `HomeScreen` is currently hardcoded to Amber (`#FFB868` / `#452100`). This is a bug — it should use `MaterialTheme.colorScheme.primary`.

### 1.4 Semantic palette (`MaterialTheme.bloggoColors`)

These are extras that Material 3 doesn't provide. Access via the `bloggoColors` extension on `MaterialTheme`.

| Slot | Light | Dark | Use |
|------|-------|------|-----|
| `success` | `#3B6939` | `#A1D39A` | Success text/icon |
| `onSuccess` | `#FFFFFF` | `#1B1B1F` | Text on success fill |
| `successContainer` | `#BCF0B4` | `#1F4620` | "Synced" chip background |
| `onSuccessContainer` | `#1F4620` | `#BCF0B4` | Text/icons inside success container |
| `warn` | `#8F4C00` | `#FFB783` | "Edited" dot and label |
| `onWarn` | `#FFFFFF` | `#1B1B1F` | Text on warn fill |
| `warnContainer` | `#FFDCC2` | `#5E3500` | Warn banner background |
| `onWarnContainer` | `#5E3500` | `#FFDCC2` | Text inside warn container |

The Material error slots (`error`, `errorContainer`, `onErrorContainer`) handle all error states. Do not add new semantic slots without updating `BloggoColors` and both `lightBloggoColors()`/`darkBloggoColors()`.

### 1.5 Rules

- Never use `Color(0x…)` literals in composables. Use theme tokens.
- `onSurface` for body text, `onSurfaceVariant` for secondary/hint text.
- Use `successContainer`/`onSuccessContainer` for positive status chips.
- Use `warnContainer`/`onWarnContainer` for caution banners.
- Use `errorContainer`/`onErrorContainer` for inline error surfaces.

---

## 2. Typography Scale

Font: `FontFamily.Default` (system sans-serif) for prose; `FontFamily.Monospace` for code/technical strings.

| Token | Size | Weight | Line height | Letter spacing | Font | Typical use |
|-------|------|--------|-------------|----------------|------|-------------|
| `displayLarge` | 26 sp | Bold | 32 sp | −0.5 sp | Default | App title "Bloggo" in TopAppBar; empty-state icon letter |
| `displayMedium` | 22 sp | Bold | 28 sp | −0.3 sp | Default | — |
| `displaySmall` | 19 sp | SemiBold | 24 sp | −0.2 sp | Default | — |
| `headlineLarge` | 26 sp | Bold | 32 sp | −0.3 sp | Default | — |
| `headlineMedium` | 22 sp | Bold | 28 sp | −0.2 sp | Default | Editor title field |
| `headlineSmall` | 19 sp | SemiBold | 24 sp | −0.2 sp | Default | — |
| `titleLarge` | 19 sp | SemiBold | 24 sp | −0.2 sp | Default | — |
| `titleMedium` | 17 sp | SemiBold | 22 sp | 0 sp | Default | "No posts yet" empty state |
| `titleSmall` | 15.5 sp | SemiBold | 19 sp | 0 sp | Default | Post card title |
| `bodyLarge` | 15 sp | Normal | 24.75 sp | 0 sp | Default | General paragraph prose |
| `bodyMedium` | 14.5 sp | Normal | 23.9 sp | 0 sp | **Mono** | Editor body field; banner text; search field |
| `bodySmall` | 12.5 sp | Normal | 18 sp | 0 sp | **Mono** | Post card meta line; slug chip; front matter raw field; word count |
| `labelLarge` | 13 sp | SemiBold | 18 sp | 0.4 sp | Default | Section headers (uppercase); front matter card header |
| `labelMedium` | 12 sp | SemiBold | 16 sp | 0 sp | Default | Settings field labels; toggle segment text |
| `labelSmall` | 11 sp | Medium | 16 sp | 0 sp | Default | Status chips (Synced / Local / "edited"); count badge |

**Key conventions:**
- `bodyMedium` and `bodySmall` use `MonoFontFamily` — they are intentionally monospaced for editor/code contexts.
- `labelLarge` is rendered uppercase for section headers (`SectionHeader` in `HomeScreen`).
- The editor title placeholder is styled separately at 24 sp Bold inline — match this when adding similar large placeholder text.

---

## 3. Shape Scale

`BloggoShapes` maps to `MaterialTheme.shapes`. Named aliases exist for specific use cases — prefer the alias over a raw `RoundedCornerShape(Xdp)`.

| Named alias | Radius | Use |
|-------------|--------|-----|
| `ChipShape` | 999 dp (pill) | Status chips, search bar, toggle segment container, theme selector segments |
| `FabShape` | 20 dp | FAB (if you add a custom FAB shape) |
| `DialogShape` | 28 dp | `AlertDialog` corners |
| `SheetShape` | 28 dp top-only | `ModalBottomSheet` (PushConfirmSheet) |
| `CardShape` | 18 dp | Post row cards; `PostRowCard` |
| `RowShape` | 16 dp | FrontMatterCard; SettingsSection card |
| `IconTileShape` | 12 dp | Empty-state icon tile; heading flyout items; slug chip |
| `extraSmall` (shapes) | 10 dp | (reserved; not currently used inline) |
| `small` (shapes) | 12 dp | Body field border, small surface items |
| `medium` (shapes) | 16 dp | (alias of RowShape in practice) |
| `large` (shapes) | 18 dp | (alias of CardShape in practice) |
| `extraLarge` (shapes) | 28 dp | (alias of DialogShape / SheetShape) |

**Rules:**
- Cards → `CardShape` (18 dp) or `RowShape` (16 dp) for denser settings-style cards.
- Chips & pill-shaped containers → `ChipShape` (999 dp).
- Dialogs → `DialogShape` (28 dp).
- Bottom sheets → `SheetShape` (28 dp top-only).
- Small inset surfaces (code blocks, slugs, diff blocks) → 8 dp inline or `IconTileShape` (12 dp).
- Banner surfaces → 14 dp inline (match `HomeBanner` and `HeadingFlyout`).

---

## 4. Spacing

There is no named spacing scale yet. The following values appear consistently across screens and should be treated as the de-facto grid:

| Value | Common use |
|-------|------------|
| 4 dp | Minimum internal padding; icon-to-label gap inside chips |
| 6 dp | Chip vertical padding; toggle segment padding |
| 8 dp | Card-to-card vertical gap; icon-to-text horizontal gap; standard inner padding |
| 12 dp | Horizontal screen margin (list items, cards); card inner padding |
| 16 dp | Primary horizontal content margin; card content padding |
| 24 dp | Dialog button area spacing |
| 28 dp | Section gaps in settings |
| 32 dp | Empty state outer padding |
| 88 dp | `LazyColumn` bottom content padding (clears the FAB) |

**Margin convention:** Horizontal list/card margins are **12 dp** (`padding(horizontal = 12.dp)`). Internal card content uses **16 dp** horizontal padding.

---

## 5. Icon Conventions

All icons come from `androidx.compose.material.icons`. No custom drawables are used in the UI layer.

| Icon | Vector | Screen / context |
|------|--------|-----------------|
| Add | `Icons.Default.Add` | FAB — new post |
| ArrowBack | `Icons.AutoMirrored.Filled.ArrowBack` | Editor, Settings back nav |
| Check | `Icons.Default.Check` | "Synced" chip |
| Clear | `Icons.Default.Clear` | Search field clear button |
| Delete | `Icons.Default.Delete` | Post card delete action |
| FormatBold | `Icons.Default.FormatBold` | Editor formatting toolbar |
| FormatItalic | `Icons.Default.FormatItalic` | Editor formatting toolbar |
| Image | `Icons.Default.Image` | Editor formatting toolbar |
| Link | `Icons.Default.Link` | Editor formatting toolbar |
| Lock | `Icons.Default.Lock` | Frozen slug indicator |
| OpenInNew | `Icons.Default.OpenInNew` | "View live" button |
| Refresh | `Icons.Default.Refresh` | Home screen sync |
| Save | `Icons.Default.Save` | Save local button |
| Search | `Icons.Default.Search` | Search field leading icon |
| Send | `Icons.Default.Send` | Publish button |
| Settings | `Icons.Default.Settings` | Home → Settings |
| Upload | `Icons.Default.Upload` | "Local" chip |
| Visibility / VisibilityOff | both `Icons.Default` | PAT field reveal/hide |

**Size conventions:**
- Toolbar icons: 20 dp (inside a 40 dp `IconButton`)
- Chip icons: 14 dp
- Trailing / action icons: 16–20 dp
- TopAppBar icons: default (24 dp via `IconButton`)

**Tint rule:** Use `MaterialTheme.colorScheme.onSurface` for active/primary icons; `onSurfaceVariant` for secondary/inactive icons. Icons inside colored chips use the chip's `onXxxContainer` color.

---

## 6. Component Inventory

### 6.1 `HomeScreen` — `ui/home/HomeScreen.kt`

Full-screen scaffold with TopAppBar, `LazyColumn`, and Extended FAB.

---

#### `SearchField` (private)

Pill-shaped search input pinned above the post list.

- **Shape:** `CircleShape` (pill)
- **Background:** `surfaceContainer`
- **Padding:** 12 dp horizontal, 16 dp top, 8 dp bottom (outer); 16 dp H / 6 dp V (inner row)
- **Leading icon:** `Search` at 20 dp, tinted `onSurfaceVariant`
- **Input text:** `bodyMedium`; placeholder at 14 sp
- **Clear button:** `Clear` icon at 18 dp — only visible when query is non-empty

**When to use:** Any single-screen search bar over a list. Do not use `SearchBar` (M3) — use this pattern for visual consistency.

---

#### `SectionHeader` (private)

Collapsible section label with a count badge.

- **Typography:** `labelLarge` uppercase for the title; `labelSmall` for the count
- **Chevron:** `▾` (expanded) / `▸` (collapsed) rendered as text
- **Count badge:** `CircleShape` surface with `surfaceContainerHigh` background
- **Padding:** 12 dp H / 10 dp V (entire row is clickable)

**When to use:** Grouping homogeneous list items into named, collapsible sections.

---

#### `PostRowCard` (private)

Primary list item for a blog post.

- **Shape:** `CardShape` (18 dp)
- **Background:** `surfaceContainerLow`
- **Content padding:** 16 dp H / 12 dp V
- **Title:** `titleSmall`, max 1 line with ellipsis
- **Meta line:** `bodySmall`, `onSurfaceVariant`, max 1 line
- **Status chips:** see `SyncChip` pattern below

Contains two status chip variants and optional action buttons:
- **"Synced" chip:** `CircleShape`, `successContainer` bg, check icon + text in `onSuccessContainer`
- **"Local" chip:** `CircleShape`, `surfaceContainerHigh` bg, upload icon + text in `onSurfaceVariant`
- **"edited" dot:** 6 dp circle in `warn`; label text in `warn`
- **Delete button:** 36 dp touch target, `Delete` icon in `onSurfaceVariant` (drafts only)
- **"Live" button:** `TextButton` + `OpenInNew` icon (published posts with a slug only)

**When to use:** Displaying any post in a list. Don't inline post cards elsewhere — navigate to the editor.

---

#### `HomeBanner` (private)

Inline status banner below the search field and progress indicator.

- **Types:** `SUCCESS`, `WARN`, `NEUTRAL`
- **SUCCESS:** `successContainer` bg / `onSuccessContainer` fg
- **WARN:** `warnContainer` bg / `onWarnContainer` fg
- **NEUTRAL:** `surfaceContainerHigh` bg / `onSurface` fg
- **Shape:** 14 dp rounded
- **Padding:** 12 dp H / 4 dp V (outer); 16 dp H / 12 dp V (inner)
- **Action:** `TextButton` tinted with the banner's fg color

**When to use:** Transient feedback that doesn't block interaction (sync status, errors, warnings). Dismiss via action or `viewModel.dismissBanner()`.

---

#### `EmptyState` (private)

Centered illustration + copy shown when the post list is empty.

- **Icon tile:** 84 dp × 84 dp `Surface`, 14 dp radius, `primaryContainer` bg, letter "B" in `displayLarge`
- **Heading:** `titleMedium`
- **Body:** `bodyMedium`, `onSurfaceVariant`
- **Outer padding:** 32 dp

**When to use:** Zero-item states for any list screen. Adapt icon and copy; keep the tile size and typography.

---

### 6.2 `EditorScreen` — `ui/editor/EditorScreen.kt`

Full-screen editing surface. Handles the keyboard pan/scroll, pinned formatting toolbar, and preview mode.

---

#### `EditPreviewToggle` (private)

Segmented toggle in the TopAppBar switching between Edit and Preview modes.

- **Outer container:** `ChipShape` (pill), `surfaceContainerHigh`
- **Selected segment:** `ChipShape`, `primary` bg, `onPrimary` text
- **Unselected segment:** transparent, `onSurfaceVariant` text
- **Typography:** `labelMedium`
- **Padding:** 12 dp H / 6 dp V per segment; 2 dp outer inset

**When to use:** Toggling between two mutually exclusive view modes inside a TopAppBar. For three or more modes, use a full-width `ThemeSelector`-style row instead.

---

#### `SlugRow` (private)

Displays the current post slug with a frozen indicator.

- **Label "slug:":** `bodySmall`, `onSurfaceVariant`
- **Slug chip:** 8 dp rounded, `primaryContainer` bg, `onPrimaryContainer` text, monospace font
- **Lock icon:** `Lock` at 16 dp, `onSurfaceVariant` — shown only when `slugFrozen == true`

---

#### `FrontMatterCard` (private)

Collapsible card exposing the raw YAML front matter and draft toggle.

- **Shape:** `RowShape` (16 dp)
- **Background:** `surfaceContainerLow`
- **Border:** 1 dp, `outlineVariant`
- **Header row:** 16 dp H / 6 dp V, always visible and clickable
- **Header typography:** `labelLarge` (title) + `labelSmall` `onSurfaceVariant` (hint)
- **Dividers:** `outlineVariant`
- **Draft switch:** scaled to 0.7× via `LocalMinimumInteractiveComponentSize = 0.dp`
- **Raw text field:** `bodySmall`, min 4 lines, `OutlinedTextField`

**When to use:** Any key→value metadata panel that should be accessible but not prominent.

---

#### `FormattingToolbar` / `ToolbarButton` / `TextToolbarButton` (private)

Horizontal toolbar for Markdown formatting actions, embedded inside the body field box.

- **Background:** `surfaceContainerLow` via `Surface`
- **Buttons:** 40 dp × 40 dp `IconButton`; icon at 20 dp, tinted `onSurface`
- **"H" heading button:** `TextToolbarButton` — 16 sp Bold text, same 40 dp size
- **Word count:** `bodySmall`, `onSurfaceVariant`, right-aligned

The toolbar appears both inline (top of the body box) and as a pinned overlay when the body field has scrolled above the viewport. The pinned copy adds `HorizontalDivider` below.

---

#### `HeadingFlyout` (private)

Dropdown that appears when the "H" button is tapped, listing H1–H6.

- **Container:** 14 dp rounded, `surfaceContainerHigh`, 8 dp shadow elevation
- **Items:** 8 dp rounded, `surfaceContainerLow`, 12 dp H / 8 dp V
- **Prefix text:** monospace `bodySmall`, `onSurfaceVariant`
- **Heading label:** size steps from 19 sp (H1) down to 14 sp (H6) using `(20 - level).sp`

---

#### `SavePublishRow` (private)

Two-button row at the bottom of the editor.

- **"Save local":** `OutlinedButton`, full width weight 1, `Save` icon
- **"Publish":** filled `Button`, full width weight 1, `primary` container, `Send` icon

**When to use:** Any two-action footer where one action is secondary (outlined) and one is primary (filled). Use equal weight unless one action is more consequential.

---

#### `PreviewPane` (private)

Markdown preview rendered via Markwon into a `TextView`.

- **Text size:** 15 sp (set on the View directly)
- **Text color:** `onSurface` (updated on theme change via `update` lambda)
- **Padding:** 48 px H / 32 px V (View pixels, not dp — set via `setPadding`)
- **Line spacing:** +8 px extra

---

### 6.3 `SettingsScreen` — `ui/settings/SettingsScreen.kt`

Scrollable settings surface grouped into labeled `SettingsSection` cards.

---

#### `SettingsSection` (private)

Labeled card grouping settings fields.

- **Section label:** `labelLarge`, `onSurfaceVariant`, 4 dp H / 8 dp V padding
- **Card shape:** `RowShape` (16 dp)
- **Card background:** `surfaceContainerLow`
- **Content padding:** 16 dp

---

#### `SettingsTextField` (public)

Single-line `OutlinedTextField` with a label above it.

- **Label:** `labelMedium`, `onSurfaceVariant`
- **Spacing:** 4 dp between label and field

---

#### `SettingsSecretField` (public, approx name)

Like `SettingsTextField` but for PAT — includes `PasswordVisualTransformation` and a show/hide `IconButton`.

- **Text style:** `bodyMedium` with `FontFamily.Monospace`
- **Trailing icon:** `Visibility` / `VisibilityOff`

---

#### `SettingsMultilineField` (private)

`OutlinedTextField` with an optional helper line, used for the front matter template.

- **Label:** `labelMedium`, `onSurfaceVariant`
- **Helper:** `labelSmall`, `onSurfaceVariant` (optional, shown between label and field)

---

#### `ThemeSelector` (private)

Full-width segmented control for LIGHT / DARK / SYSTEM theme mode.

- **Track:** `ChipShape` (pill), `surfaceContainerHigh`
- **Selected segment:** `primary` bg, `onPrimary` text
- **Unselected segment:** `surfaceContainerHigh` bg, `onSurface` text
- **Typography:** `labelMedium`, centered

Pattern identical to `EditPreviewToggle` but full-width with three segments.

---

#### `SettingsDivider` (private)

`HorizontalDivider` with `outlineVariant` color and 8 dp vertical padding. Use to separate fields within a `SettingsSection` card.

---

### 6.4 Publish dialogs — `ui/editor/PublishDialogs.kt`

#### `DraftFlipDialog`

`AlertDialog` warning that draft is `true` before publishing.

- Confirm: `TextButton`
- Dismiss: `TextButton`

#### `DiscardDialog`

`AlertDialog` confirming discard of unsaved changes (defined inline in `EditorScreen`).

- Confirm text: `colorScheme.error`
- Dismiss: `TextButton`

#### `DeleteDraftDialog`

`AlertDialog` confirming post deletion.

- Confirm text: `colorScheme.error`
- Dismiss: `TextButton`

**Rule for destructive dialogs:** Confirm button is always `TextButton` with `colorScheme.error` text. Never use a filled red button.

#### `PushConfirmSheet`

`ModalBottomSheet` (shape = `SheetShape`) showing commit message + diff before push.

- Commit message surface: 8 dp rounded, `surfaceContainerLow`, monospace `bodySmall`
- Diff viewer: 8 dp rounded, `surfaceContainerLow`, 1 dp `outlineVariant` border
- Error surface (if present): 8 dp rounded, `errorContainer` bg, `onErrorContainer` text
- Buttons: `OutlinedButton` (Cancel) + filled `Button` (Push / "Pushing…" with `CircularProgressIndicator`)

---

## 7. Accent Color Selector

Settings exposes a future accent selector (wired but accent is persisted in `SettingsRepository`). When building the accent picker UI, follow this pattern:

- Show swatches as 40–48 dp circles or pill chips.
- Active accent: 2 dp `primary`-colored border or a checkmark overlay.
- Labels: `labelSmall`, `onSurfaceVariant`.

---

## 8. What's Not Covered — Ask Before Inventing

If a task requires any of the following, align before implementing:

- A new semantic color (beyond success/warn/error)
- A shape radius not in the scale
- Typography sizes outside the defined scale
- New icon sources (custom drawables, third-party icon packs)
- Elevation / shadow beyond the 8 dp used in `HeadingFlyout`
- Animation beyond `AnimatedVisibility(fadeIn/fadeOut)` used on the search clear button
