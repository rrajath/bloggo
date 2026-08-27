# Bloggo prototype notes

Design rationale for `design/bloggo-prototype.html`. One self-contained file: no
build step, no dependencies. Open it in a browser, or on a phone where the device
frame drops away below 900px.

## What the prototype is

A git-backed markdown studio for a personal blog, shown as 12 screens driven by a
single in-memory document. Everything visible is real: the editor, preview, and
diff all read from the same string, so editing in one place changes the others.

## Decisions

### Cover art is optional

The rail carries a `Cover art: Generated / None` switch that sets `data-art` on the
root element. Off hides every generated image in the library, and the hero card
falls back to type plus a 3px amber rule marking the in-progress draft. The switch
exists so the two directions can be compared without maintaining two files.

The art itself is procedural, not a library. `paint()` seeds a PRNG from the post
slug, draws 3 to 5 overlapping shapes in `multiply` on light and `screen` on dark,
adds three hairline register rules, then per-pixel grain. Deterministic: the same
post always gets the same art.

### No word-goal bar in the library

An earlier version put a progress bar under the hero cover showing draft progress
toward a 2,000 word goal. A thin bar under a 16:9 image reads as reading progress
no matter what the label says, and reading progress is meaningless on a post you
wrote. The word goal now lives only in focus mode, where it belongs, as a ring.

### Hugo, not Astro

Every path and label in the prototype derives from Hugo:

| Thing | Value |
|---|---|
| Config | `hugo.toml` |
| Posts | `content/posts/{slug}.md` |
| Images | `static/images/{year}/` |
| Frontmatter source | `archetypes/default.md` |
| Taxonomies | `[taxonomies]` in `hugo.toml` |
| Components | shortcodes in `layouts/shortcodes/` |

Hugo has no machine-readable schema the way Astro's content collections do, so the
frontmatter sheet is honest about where each part comes from: the field list from
the archetype, the tag vocabulary from `[taxonomies]`.

> **Diverges in the Android app.** The prototype's provenance labels — "from
> archetypes/default.md" on the sheet, "[taxonomies] in hugo.toml" on the tags
> label — described where those fields would come from, but the app never
> actually fetched either file, which made the labels read as claims about
> live data rather than the static illustration they were. Removed from the
> Android Post details sheet (PROGRESS.md, Milestone 9); the Repo screen's
> frontmatter fields and taxonomies are instead derived from the frontmatter
> of posts actually fetched from the connected repo. The prototype HTML file
> itself is unchanged.

The Repo screen's framework row is a real button. It opens a sheet showing the
evidence for the detection, because every path below it is derived from that guess
and the user needs to be able to correct it.

### Shortcodes render

Inserting a shortcode writes `{{< callout type="note" >}}` into the document. The
editor tints those lines in accent, and the preview renders callouts, asides, and
figures as blocks rather than printing the raw shortcode. Without this the Read
mode would show template syntax, which would make the preview useless.

### Visiting the live page

Three placements, three different jobs:

1. **Preview permalink bar.** Always shows the URL. Live posts get an active Open
   button; drafts show the URL greyed with "Not published", so the permalink is
   predictable before it exists.
2. **Library row button.** A single icon on Live rows only, going straight to the
   page without a stop in Preview.
3. **Repo site cell.** `rrajath.dev` at the top of the Repo screen, for "show me my
   blog" rather than "show me this post".

### Focus mode

Distraction-free drafting with sprint mechanics: dimmed context, wider serif type,
a word goal ring, and a session timer. The rule at 38% is a typewriter sightline,
and `lockToGuide()` pulls the active line onto it so your eyes never travel down
the screen. Without that lock the rule is decoration, which is what it was before.

The active line is the one holding the caret. On entry the caret is placed at the
end of the document, so the last line is live and the view resumes where writing
stopped.

### Mastodon instead of a share card

The share card screen is gone. In its place is a composer that posts one message
to several accounts at once.

- Accounts are selectable, each with its own visibility (tap to cycle Public,
  Unlisted, Followers only) and its own character limit.
- With **Same text everywhere** on, the counter tracks the strictest selected
  instance and the banner names any instance the text is too long for.
- Turning it off seeds a per-account copy from the shared draft and shows a tab per
  account, with a dot on any tab that is over its own limit.
- No image is attached. Mastodon pulls the preview card from the site's own OG
  tags, which is both simpler and truer to how the platform works.

### Pages replaced Media

The Media tab from earlier prototype passes is gone. Attaching media to a post
already happens inline, through the toolbar's `+` (Insert sheet) or the
frontmatter sheet's cover-image field, both of which resize and commit to
`static/images/` with the post they're attached to. A screen for browsing
that folder never earned its keep next to that.

In its place is Pages: the top-level `content/*.md` files a Hugo site
actually has (`about.md`, `uses.md`, and so on), as distinct from
`content/posts/`. Each row opens the same Editor and Preview screens a post
does, reading and writing the same `doc`, so a page's frontmatter sheet, diff,
and commit sheet are just that document's real state rather than a parallel
path built to look similar.

Three differences from editing a post, all deliberate:

- The frontmatter sheet drops Reading time, Tags, and the Draft toggle. A page
  like About isn't tagged or drafted, and showing those fields would be
  asking the writer to fill in something meaningless.
- Publishing skips the offline-queue banner and the cover-image diff line. A
  page commits straight to `content/{slug}.md`, not
  `content/posts/{slug}.md`, and the branch/commit-message defaults follow:
  `page/{slug}` instead of `post/{slug}`.
- The editor header shows a neutral "Page" chip instead of Draft/Live, since
  pages don't carry a draft workflow the way posts do.

## Testing

`design/bloggo-prototype.html` has no test suite of its own. It was verified with a
jsdom pass that boots the page, walks every screen, and asserts 61 behaviours
covering navigation, the art toggle, the live link states, the Mastodon character
limit logic, shortcode rendering, and the diff.
