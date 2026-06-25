# HugoPad — Product Requirements Document (v1)

> Working title. This document is UI-forward so it can drive mockups in Claude Design: every screen lists its layout, components, states, and dialogs.

---

## 1. Summary

HugoPad is an Android app for writing, previewing, and publishing Hugo blog posts directly from a phone. Posts are authored in Markdown with YAML front matter and pushed straight to the blog's GitHub repository. It complements an existing laptop workflow (org-mode + ox-hugo) rather than replacing it.

**Guiding principle:** the phone is strictly for posts that are *started and finished on the phone*. These are a distinct class of post and are never round-tripped back through ox-hugo on the laptop, which avoids any source-of-truth conflict between the org source and the generated Markdown.

---

## 2. Problem

Today, writing a post requires sitting at the laptop to author, preview, commit, and push. There is no way to capture and publish a post when an idea strikes away from the desk. HugoPad removes that constraint: open the app, write Markdown, sanity-check the formatting, and publish — all from the phone.

Note on "preview": the goal is **only** to verify that the Markdown is well-formed, not to reproduce the live themed site. A plain Markdown render is sufficient.

---

## 3. Goals & non-goals

**Goals (v1)**
- Write and edit Markdown posts on the phone with a touch-friendly editor.
- Auto-manage YAML front matter without the user hand-typing boilerplate.
- Preview rendered Markdown (front matter hidden).
- Save drafts locally and publish to GitHub with a single, safe flow.
- See all posts — local and remote — in one cached, instantly-loading list.

**Non-goals (v1)** — explicitly out of scope:
- Underline formatting (no native Markdown equivalent).
- Hugo shortcode snippets.
- Editor syntax highlighting (deferred to v2).
- Future-dated scheduling (the user edits the date manually in front matter when needed).
- Rendering the actual themed Hugo site.

---

## 4. User & content model

Single user: the blog owner. The app talks to one GitHub repository using a Personal Access Token (PAT).

Each post has two **independent** status axes, which the UI must represent separately:

| Axis | Source of truth | Drives | Values |
|------|-----------------|--------|--------|
| **Draft status** | `draft:` field in front matter | Home **section** | Draft / Published |
| **Sync status** | Where the post lives vs. GitHub | Home **tag** (chip) | Local / Synced |

A post can be any combination — e.g. a `draft: true` post that already exists on GitHub appears in the **Draft** section with a **Synced** tag.

---

## 5. Information architecture

Three primary screens plus a set of dialogs:

1. **Home** — the post list.
2. **Post Detail** — the Markdown editor (create or edit).
3. **Settings** — configuration.

Navigation: Home → tap a post (or the new-post action) → Post Detail. Home → settings icon → Settings.

---

## 6. Screen: Home

### Purpose
A single list of every post, grouped by draft status, that loads instantly from cache and refreshes from GitHub on demand.

### Layout
- **Top app bar:** app title, a search affordance, and a settings icon.
- **Search/filter:** filters the visible list by title (and optionally body/tags).
- **Two collapsible sections, in this order:**
  - **Draft** (top, expanded by default) — every post with `draft: true`, *including both local-only drafts and drafts already on GitHub*.
  - **Published** (below) — every post with `draft: false`.
- **Post row** (per item):
  - Post title (primary text).
  - Secondary line: date and/or short excerpt.
  - **Tag chip:** `Local` or `Synced`. A `Synced` post with un-pushed local edits may show a subtle "edited" indicator.
  - Row actions: tap to open; **published rows** expose a **"View live"** action that opens the post's public URL; **draft rows** allow delete.
- **Pull-to-refresh:** re-pulls all posts from GitHub. Must **merge-preserve local unpushed posts** (a refresh never wipes local work).
- **New post:** a floating action button (or app-bar action) that opens an empty Post Detail.

### Behaviors
- **Instant paint:** the list renders from the local cache immediately on app open; the GitHub refresh runs in the background.
- **Tag is independent of section** — the chip communicates sync status, the section communicates draft status; the two never restate each other.

### States
- **Loading:** cached list shown immediately; a subtle refresh indicator while GitHub loads.
- **Empty:** friendly empty state with a prompt to create the first post.
- **Error (GitHub fetch fails):** show the cached list plus a non-blocking banner ("Couldn't refresh from GitHub").
- **Offline:** cached list fully usable; publish/refresh actions clearly disabled or queued.

---

## 7. Screen: Post Detail (Editor)

### Purpose
Create or edit a post. This is the core screen: structured fields, a Markdown body, a hidden-by-default raw front matter area, preview, a formatting toolbar, and the save/publish actions.

### Layout (top to bottom)
1. **Title field** — single-line text input.
2. **Collapsible "Front matter" section** (collapsed by default) — shows the **raw, editable** front matter (date, tags, custom keys). `title`, `slug`, and `draft` are managed by the app and are **not** duplicated here.
3. **Body area** — multiline Markdown editor. **Front matter never appears in the body.**
4. **Preview toggle** — switches the body area between edit and rendered-Markdown preview. Preview **hides front matter** (it renders the body only).
5. **Formatting toolbar** — docked directly above the on-screen keyboard.
6. **Action buttons** — **"Save to local"** and **"Publish to GitHub."**

### Front matter behavior (hybrid model)
- **Structured fields are the source of truth** for `title`, `slug`, and `draft`.
- The **raw front matter area** owns everything else and is freely hand-editable.
- The final file is **assembled at save time** from the structured fields + raw block + body.
- On a **new** post, the raw front matter is seeded from the **Settings template**, with the **date auto-filled** to the creation time.
- If the user types an app-owned key (e.g. `title:`) into the raw area, the structured field wins and the app surfaces a warning.

### Slug behavior
- The slug is derived from the title **on focus change from title → body**.
- This happens **only for local, not-yet-pushed posts.** Once a post is synced (or the slug is hand-edited), the slug **freezes** so live URLs never break. The title remains editable; title ≠ slug is acceptable.
- Slug rule: lowercase; spaces → dashes; any run of non-alphanumeric characters collapses to a **single** dash; no leading/trailing dash.

### Formatting toolbar
Buttons, left to right: **Bold**, **Italic**, **Link**, **Heading (H)**.
- The **H** button opens a **flyout menu** listing the six headings (H1–H6) as a selectable list; choosing one applies that heading to the current line.
- (No underline button — out of scope.)

### Image insertion
- An **insert-image** action lets the user pick an image; before upload, a small dialog offers **three size presets — Small / Medium / Large** — which resize the image to a fixed max width while preserving aspect ratio.
- The image is uploaded to the repo's configured image path, and the **correct public URL path** (not the repo path) is inserted into the body as Markdown.

### Save & publish

**Save to local**
- Writes the post to local storage only and lists it on Home with a `Local` tag.
- Never modifies the `draft:` flag.

**Publish to GitHub** — ordered flow:
1. **Draft-flip dialog** — shown **only if `draft: true`**: "Flip this post out of draft mode?" If yes, set `draft: false`.
2. **Front matter validation** — block publish on malformed YAML.
3. **Confirm-what-I'll-push** — show the commit message (`New post: <title>`) and, for an existing post, a diff against what's on GitHub. (For a brand-new post there's nothing to diff, so this is a simple confirm.)
4. **Push** — commit the `<slug>.md` file to the configured path/branch.

### Autosave & recovery
- The in-progress post **autosaves continuously** as a recovery buffer, independent of "Save to local," so a killed app or interruption never loses work.

### Leaving the editor
- If there are unsaved changes, prompt to **confirm before discarding**.

### States
- **New (empty):** title and body empty; front matter seeded on first title entry/save.
- **Editing local draft / editing synced post.**
- **Preview mode.**
- **Validation error**, **push in progress**, **push success/failure.**

---

## 8. Screen: Settings

All fields persist locally; the PAT is stored securely (see Technical Doc).

- **GitHub PAT** — masked field (dots) with a **reveal toggle** to switch between dots and plain text.
- **GitHub repository** — owner/repo.
- **Branch** — defaults to `main`.
- **Content path** — where `<slug>.md` files are pushed (e.g. `content/posts`).
- **Image repo path** — where image files are committed (e.g. `static/images`).
- **Image URL base** — the public path used when referencing images in Markdown (e.g. `/images`).
- **Blog base URL** — used to build the "View live" link for published posts.
- **Default front matter template** — applied to every new post; supports a date token that is auto-filled at creation.
- **Theme** — Light / Dark / System (dark mode required).
- **(Recommended)** App lock — biometric lock, since the app holds a write-capable token.

---

## 9. Dialogs & popups (inventory)

| Dialog | Trigger | Content |
|--------|---------|---------|
| Draft-flip confirm | Publish, when `draft: true` | "Flip out of draft mode?" Yes/No |
| Push confirm / diff | After validation, before push | Commit message + diff (or simple confirm for new posts) |
| Image size preset | Insert image | Small / Medium / Large |
| Delete confirm | Delete a draft | Confirm destructive action |
| Discard changes | Leaving editor with unsaved edits | Keep editing / Discard |

---

## 10. Key user flows

**A. Write a new post and publish**
New post → type title → on leaving the title, front matter is seeded and slug derived → write body, format via toolbar → preview → Save to local (optional) → Publish to GitHub → draft-flip dialog → validation → confirm → pushed; row moves to Published / tag becomes Synced.

**B. Edit a post already on GitHub**
Open from Home (Synced tag) → edit → slug stays frozen → Publish → diff confirm → pushed.

**C. Insert an image**
Body → insert image → pick file → choose Small/Medium/Large → upload → Markdown image inserted with the public URL path.

---

## 11. Non-functional requirements

- **Security:** the PAT is write-capable; store it in encrypted storage and gate the app with an optional biometric lock. Never display the token unless the reveal toggle is on.
- **Offline-first:** the post list and editor work fully offline from cache; only refresh/publish require connectivity.
- **Performance:** Home paints from cache instantly; network work is backgrounded.
- **Accessibility & theme:** support light/dark/system; toolbar and chips meet touch-target and contrast guidance.

---

## 12. Out of scope (v1) / future

Out of scope for v1: underline, Hugo shortcode snippets, editor syntax highlighting, scheduled publishing, full themed-site preview.

Candidate future work: syntax highlighting, shortcode quick-insert, multi-repo support, scheduled dates, richer diffing.
