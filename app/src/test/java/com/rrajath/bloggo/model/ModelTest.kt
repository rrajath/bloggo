package com.rrajath.bloggo.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelTest {

  @Test
  fun `parseFrontmatter strips surrounding quotes from values`() {
    val markdown = "---\ntitle: \"A quoted title\"\ntags: 'not a list'\n---\nBody."

    val frontmatter = markdown.parseFrontmatter()

    assertEquals("A quoted title", frontmatter["title"])
    assertEquals("not a list", frontmatter["tags"])
  }

  @Test
  fun `parseFrontmatter returns empty map without a leading fence`() {
    assertEquals(emptyMap<String, String>(), "Just a body, no frontmatter.".parseFrontmatter())
  }

  @Test
  fun `parseFrontmatter ignores lines without a colon`() {
    val markdown = "---\ntitle: A post\nthis line has no colon\n---\n"

    val frontmatter = markdown.parseFrontmatter()

    assertEquals(setOf("title"), frontmatter.keys)
  }

  @Test
  fun `parseFrontmatter reads TOML fenced frontmatter`() {
    val markdown = "+++\ntitle = \"A TOML post\"\ndraft = true\ndate = 2026-08-17\n+++\n\nBody."

    val frontmatter = markdown.parseFrontmatter()

    assertEquals("A TOML post", frontmatter["title"])
    assertEquals("true", frontmatter["draft"])
    assertEquals("2026-08-17", frontmatter["date"])
  }

  @Test
  fun `parseFrontmatter ignores lines without an equals sign in TOML`() {
    val markdown = "+++\ntitle = \"A post\"\nthis line has no equals\n+++\n"

    val frontmatter = markdown.parseFrontmatter()

    assertEquals(setOf("title"), frontmatter.keys)
  }

  @Test
  fun `parseFrontmatter stops at the closing fence, not at a horizontal rule in the body`() {
    // The parser walks the string rather than splitting it, so this pins the one
    // thing that walk has to get right: `---` means "close the block" only until
    // the block is closed, and means "horizontal rule" everywhere after.
    val markdown = "---\ntitle: A post\n---\n\nIntro paragraph.\n\n---\n\nnot: frontmatter\n"

    val frontmatter = markdown.parseFrontmatter()

    assertEquals(setOf("title"), frontmatter.keys)
    assertEquals("A post", frontmatter["title"])
  }

  @Test
  fun `parseFrontmatter reads an unterminated block for whatever it does contain`() {
    val markdown = "---\ntitle: A post\ndraft: true\n"

    assertEquals("A post", markdown.parseFrontmatter()["title"])
    assertEquals("true", markdown.parseFrontmatter()["draft"])
  }

  @Test
  fun `parseFrontmatter handles a document that is nothing but a fence`() {
    assertEquals(emptyMap<String, String>(), "---".parseFrontmatter())
  }

  @Test
  fun `frontmatterBlock returns the whole block including the blank line after it`() {
    val markdown = "---\ntitle: A post\n---\n\nBody starts here."

    assertEquals("---\ntitle: A post\n---\n\n", markdown.frontmatterBlock())
    assertEquals("Body starts here.", markdown.removePrefix(markdown.frontmatterBlock()))
  }

  @Test
  fun `frontmatterBlock is empty for markdown that has no frontmatter`() {
    assertEquals("", "Just a body.\n\n---\n\nAnd a rule.".frontmatterBlock())
  }

  @Test
  fun `lastEditedAtMillis prefers a local edit over the frontmatter date`() {
    val post = Post(
      slug = "a",
      title = "A",
      state = PostState.Draft,
      markdown = "---\ntitle: A\ndate: 2026-08-04\n---\n",
      dateMillis = parseFrontmatterDateEpochMillis("2026-08-04"),
      updatedAt = 1_800_000_000_000L,
    )

    assertEquals(1_800_000_000_000L, post.lastEditedAtMillis())
    assertEquals(post.dateMillis, post.copy(updatedAt = null).lastEditedAtMillis())
  }

  @Test
  fun `withDateFromFrontmatter fills the sort key in from the markdown`() {
    val post = Post(
      slug = "a",
      title = "A",
      state = PostState.Published,
      markdown = "---\ntitle: A\ndate: 2026-08-04\n---\n\nBody.",
    ).withDateFromFrontmatter()

    assertEquals(parseFrontmatterDateEpochMillis("2026-08-04"), post.dateMillis)
  }

  @Test
  fun `markdownWordCount strips TOML frontmatter too`() {
    val markdown = "+++\ntitle = \"A post\"\n+++\n\nOne two three."

    assertEquals(3, markdown.markdownWordCount())
  }

  @Test
  fun `slugify collapses punctuation and case`() {
    assertEquals("on-agents-that-ship", slugify("On Agents That Ship!"))
  }

  @Test
  fun `slugify falls back to untitled for punctuation only input`() {
    assertEquals("untitled", slugify("---"))
  }

  @Test
  fun `syncSlugToTitle updates an auto-tracked slug when the title changes`() {
    val old = "---\ntitle: Old Title\nslug: old-title\ndraft: true\n---\n"
    val new = "---\ntitle: New Title\nslug: old-title\ndraft: true\n---\n"

    val result = syncSlugToTitle(old, new)

    assertEquals("new-title", result.parseFrontmatter()["slug"])
  }

  @Test
  fun `syncSlugToTitle leaves a manually edited slug alone`() {
    val old = "---\ntitle: Old Title\nslug: my-custom-slug\ndraft: true\n---\n"
    val new = "---\ntitle: New Title\nslug: my-custom-slug\ndraft: true\n---\n"

    val result = syncSlugToTitle(old, new)

    assertEquals("my-custom-slug", result.parseFrontmatter()["slug"])
  }

  @Test
  fun `syncSlugToTitle is a no-op when the title hasn't changed`() {
    val markdown = "---\ntitle: Same Title\nslug: same-title\ndraft: true\n---\n"

    assertEquals(markdown, syncSlugToTitle(markdown, markdown))
  }

  @Test
  fun `syncSlugToTitle reproduces the real new-post shape with date and tags between title and slug`() {
    val old = "---\ntitle: Untitled\ndate: 2026-08-18T10:22:51-07:00\ntags: []\nslug: untitled\ndraft: true\n---\n\n"
    val new = "---\ntitle: UntitledPost\ndate: 2026-08-18T10:22:51-07:00\ntags: []\nslug: untitled\ndraft: true\n---\n\n"

    val result = syncSlugToTitle(old, new)

    assertEquals("untitledpost", result.parseFrontmatter()["slug"])
  }

  @Test
  fun `syncSlugToTitle tracks the title in TOML frontmatter too`() {
    val old = "+++\ntitle = \"Old Title\"\nslug = \"old-title\"\n+++\n"
    val new = "+++\ntitle = \"New Title\"\nslug = \"old-title\"\n+++\n"

    val result = syncSlugToTitle(old, new)

    assertEquals("new-title", result.parseFrontmatter()["slug"])
  }

  @Test
  fun `withFrontmatterEdits rewrites all five fields in YAML frontmatter`() {
    val markdown = "---\ntitle: Old Title\nslug: old-title\ndate: 2026-08-01\ntags: [old]\ndraft: false\n---\n\nBody text."
    val edits = FrontmatterEdits(
      title = "New Title",
      slug = "new-title",
      date = "2026-08-18",
      tags = listOf("ai", "tooling"),
      draft = true,
    )

    val result = markdown.withFrontmatterEdits(edits)
    val frontmatter = result.parseFrontmatter()

    assertEquals("New Title", frontmatter["title"])
    assertEquals("new-title", frontmatter["slug"])
    assertEquals("2026-08-18", frontmatter["date"])
    assertEquals(listOf("ai", "tooling"), frontmatter["tags"]?.parseTagList())
    assertEquals("true", frontmatter["draft"])
    assertEquals(true, result.endsWith("\n\nBody text."))
  }

  @Test
  fun `withFrontmatterEdits rewrites all five fields in TOML frontmatter`() {
    val markdown = "+++\ntitle = \"Old Title\"\nslug = \"old-title\"\ndate = 2026-08-01\ntags = [\"old\"]\ndraft = false\n+++\n\nBody text."
    val edits = FrontmatterEdits(
      title = "New Title",
      slug = "new-title",
      date = "2026-08-18",
      tags = listOf("ai", "tooling"),
      draft = true,
    )

    val result = markdown.withFrontmatterEdits(edits)
    val frontmatter = result.parseFrontmatter()

    assertEquals("New Title", frontmatter["title"])
    assertEquals("new-title", frontmatter["slug"])
    assertEquals("2026-08-18", frontmatter["date"])
    assertEquals(listOf("ai", "tooling"), frontmatter["tags"]?.parseTagList())
    assertEquals("true", frontmatter["draft"])
  }

  @Test
  fun `withFrontmatterEdits preserves fields it doesn't know about and their order`() {
    val markdown = "---\ntitle: Old Title\ncustom_field: keep me\nslug: old-title\n---\n\nBody."

    val result = markdown.withFrontmatterEdits(
      FrontmatterEdits(title = "New Title", slug = "old-title", date = "", tags = emptyList(), draft = false),
    )

    assertEquals("keep me", result.parseFrontmatter()["custom_field"])
    assertEquals(
      listOf("title", "custom_field", "slug", "date", "tags", "draft"),
      result.parseFrontmatter().keys.toList(),
    )
  }

  @Test
  fun `withFrontmatterEdits appends a field the frontmatter didn't already have`() {
    val markdown = "---\ntitle: Old Title\nslug: old-title\n---\n\nBody."

    val result = markdown.withFrontmatterEdits(
      FrontmatterEdits(title = "Old Title", slug = "old-title", date = "2026-08-18", tags = listOf("craft"), draft = true),
    )
    val frontmatter = result.parseFrontmatter()

    assertEquals("2026-08-18", frontmatter["date"])
    assertEquals(listOf("craft"), frontmatter["tags"]?.parseTagList())
    assertEquals("true", frontmatter["draft"])
  }

  @Test
  fun `withFrontmatterEdits is a no-op without a frontmatter block`() {
    val markdown = "Just a body, no frontmatter."

    val result = markdown.withFrontmatterEdits(
      FrontmatterEdits(title = "New Title", slug = "new-slug", date = "2026-08-18", tags = emptyList(), draft = true),
    )

    assertEquals(markdown, result)
  }

  @Test
  fun `effectiveDate prefers date when both date and lastmod are present`() {
    val frontmatter = mapOf("date" to "2026-07-16T19:21:00-07:00", "lastmod" to "2026-07-16T19:25:04-07:00")
    assertEquals("2026-07-16T19:21:00-07:00", frontmatter.effectiveDate())
  }

  @Test
  fun `effectiveDate falls back to lastmod when date is missing`() {
    val frontmatter = mapOf("lastmod" to "2026-07-16T19:25:04-07:00")
    assertEquals("2026-07-16T19:25:04-07:00", frontmatter.effectiveDate())
  }

  @Test
  fun `effectiveDate falls back to lastModifiedDate when date and lastmod are missing`() {
    val frontmatter = mapOf("lastModifiedDate" to "2026-07-16T19:25:04-07:00")
    assertEquals("2026-07-16T19:25:04-07:00", frontmatter.effectiveDate())
  }

  @Test
  fun `effectiveDate is null when none of the date fields are present`() {
    assertEquals(null, mapOf("title" to "A post").effectiveDate())
  }

  private fun searchablePost(
    slug: String = "a-post",
    title: String = "A post",
    markdown: String = "---\ntitle: A post\ntags: [ai, tooling]\n---\n\nA body about agents.",
  ) = Post(slug = slug, title = title, state = PostState.Draft, markdown = markdown)

  @Test
  fun `publicSlug reads the frontmatter slug field, not the internal timestamp-suffixed slug`() {
    // Regression: a fragment promoted to a post (BloggoApp.kt's
    // newPostFromFragment) gets a Post.slug like "on-agents-1693521600000"
    // for uniqueness in the local drafts table, while the frontmatter
    // `slug:` stays the clean "on-agents". publicSlug (and everything that
    // reads it, e.g. liveUrl and the repo commit path) must follow the
    // clean frontmatter slug.
    val post = Post(
      slug = "on-agents-1693521600000",
      title = "On agents",
      state = PostState.Draft,
      markdown = "---\ntitle: On agents\nslug: on-agents\n---\n\nBody.",
    )

    assertEquals("on-agents", post.publicSlug)
    assertEquals("https://example.com/posts/on-agents", post.liveUrl("https://example.com"))
  }

  @Test
  fun `publicSlug falls back to the internal slug when frontmatter has no slug field`() {
    val post = Post(
      slug = "untitled-1693521600000",
      title = "Untitled",
      state = PostState.Draft,
      markdown = "---\ntitle: Untitled\n---\n\nBody.",
    )

    assertEquals("untitled-1693521600000", post.publicSlug)
  }

  @Test
  fun `isPushed is false for a local draft that has never been fetched or committed`() {
    // The bug isPushed exists to fix: PostState.Draft alone can't answer this
    // — it's set at creation and never flipped by a publish (BloggoApp.kt's
    // onPublish updates repoPath, not state) — so a never-pushed local draft
    // must read false here regardless of its PostState.
    val post = searchablePost().copy(repoPath = null)

    assertEquals(false, post.isPushed(remoteSlugs = emptySet()))
  }

  @Test
  fun `isPushed is true once the slug is on the remote, even before repoPath is set`() {
    val post = searchablePost(slug = "on-agents").copy(repoPath = null)

    assertEquals(true, post.isPushed(remoteSlugs = setOf("on-agents")))
  }

  @Test
  fun `isPushed is true once repoPath is set, even if the last refresh predates it`() {
    // The moment PostPublishRepository.publish succeeds, onPublish sets
    // repoPath before the next library refresh has a chance to add this
    // post's slug to remotePostSlugs/remotePageSlugs — isPushed must not
    // report "still local" in that window.
    val post = searchablePost().copy(repoPath = "content/posts/a-post.md")

    assertEquals(true, post.isPushed(remoteSlugs = emptySet()))
  }

  @Test
  fun `isPushed is false for a draft with draft-true frontmatter that was never actually pushed`() {
    // The exact conflation the bug report calls out: a `draft: true`
    // frontmatter field says nothing about whether this post ever reached
    // the repo at all.
    val post = searchablePost(
      markdown = "---\ntitle: A post\ndraft: true\n---\n\nBody.",
    ).copy(repoPath = null)

    assertEquals(false, post.isPushed(remoteSlugs = emptySet()))
  }

  @Test
  fun `matchesSearchQuery matches the title case-insensitively`() {
    val post = searchablePost(title = "On Agents That Ship")

    assertEquals(true, post.matchesSearchQuery("agents"))
    assertEquals(true, post.matchesSearchQuery("AGENTS"))
    assertEquals(false, post.matchesSearchQuery("obsidian"))
  }

  @Test
  fun `matchesSearchQuery matches the slug`() {
    val post = searchablePost(slug = "why-i-left-obsidian", title = "Something else entirely")

    assertEquals(true, post.matchesSearchQuery("obsidian"))
  }

  @Test
  fun `matchesSearchQuery matches a tag from frontmatter`() {
    val post = searchablePost(markdown = "---\ntitle: A post\ntags: [ai, tooling, craft]\n---\n\nBody.")

    assertEquals(true, post.matchesSearchQuery("tooling"))
  }

  @Test
  fun `matchesSearchQuery matches a word only in the body`() {
    val post = searchablePost(markdown = "---\ntitle: A post\n---\n\nA paragraph about plain files and Obsidian.")

    assertEquals(true, post.matchesSearchQuery("plain files"))
  }

  @Test
  fun `matchesSearchQuery is true for a blank query regardless of content`() {
    val post = searchablePost()

    assertEquals(true, post.matchesSearchQuery(""))
    assertEquals(true, post.matchesSearchQuery("   "))
  }

  @Test
  fun `searchLibrary returns the list unchanged for a blank query`() {
    val posts = listOf(searchablePost(slug = "a"), searchablePost(slug = "b"))

    assertEquals(posts, posts.searchLibrary(""))
  }

  @Test
  fun `searchLibrary filters to only the posts that match`() {
    val match = searchablePost(slug = "match", title = "Agents that ship")
    val miss = searchablePost(slug = "miss", title = "Something unrelated", markdown = "---\ntitle: Something unrelated\n---\n\nNo overlap here.")
    val posts = listOf(match, miss)

    assertEquals(listOf(match), posts.searchLibrary("agents"))
  }

  @Test
  fun `searchLibrary is empty when nothing matches`() {
    val posts = listOf(searchablePost(slug = "a"), searchablePost(slug = "b"))

    assertEquals(emptyList<Post>(), posts.searchLibrary("no such thing anywhere"))
  }

  @Test
  fun `matchingSnippet returns a window of lines around the one that matched`() {
    val post = searchablePost(
      markdown = "---\ntitle: A post\n---\n\nFirst line.\nA paragraph about plain files and Obsidian.\nLast line.",
    )

    assertEquals(
      "First line. A paragraph about plain files and Obsidian. Last line.",
      post.matchingSnippet("obsidian"),
    )
  }

  @Test
  fun `matchingSnippet is null when the query only matched the title`() {
    val post = searchablePost(title = "On Agents That Ship", markdown = "---\ntitle: On Agents That Ship\n---\n\nNo overlap here.")

    assertEquals(null, post.matchingSnippet("agents"))
  }

  @Test
  fun `matchingSnippet is null when the query only matched frontmatter`() {
    val post = searchablePost(markdown = "---\ntitle: A post\ntags: [ai, tooling]\n---\n\nA body about writing.")

    assertEquals(null, post.matchingSnippet("tooling"))
  }

  @Test
  fun `matchingSnippet is null for a blank query`() {
    val post = searchablePost()

    assertEquals(null, post.matchingSnippet(""))
  }

  // --- stagedImagePath ---

  @Test
  fun `stagedImagePath renames by slug when one is known`() {
    val path = stagedImagePath(
      imagePath = "static/images/",
      slug = "on-agents",
      originalFileName = "IMG_0231.JPG",
      timestamp = java.time.Instant.parse("2026-08-18T00:00:00Z").toEpochMilli(),
    )

    assertEquals("static/images/2026/on-agents-1.jpg", path)
  }

  @Test
  fun `stagedImagePath increments the counter for a second image on the same post`() {
    val timestamp = java.time.Instant.parse("2026-08-18T00:00:00Z").toEpochMilli()

    val path = stagedImagePath("static/images/", "on-agents", "photo.png", timestamp, count = 1)

    assertEquals("static/images/2026/on-agents-2.png", path)
  }

  @Test
  fun `stagedImagePath falls back to a timestamp and sanitized filename with no slug`() {
    val timestamp = java.time.Instant.parse("2026-08-18T00:00:00Z").toEpochMilli()

    val path = stagedImagePath("static/images/", slug = null, originalFileName = "My Photo #1.jpg", timestamp = timestamp)

    assertEquals("static/images/2026/$timestamp-my-photo-1.jpg", path)
  }

  // --- fragmentBucket ---

  /** Noon on [year]-[month]-[day] in the system's own zone, so DST transitions
   * near midnight can't push a date-only test onto the wrong calendar day. */
  private fun noonMillis(year: Int, month: Int, day: Int): Long =
    java.time.LocalDate.of(year, month, day)
      .atTime(12, 0)
      .atZone(java.time.ZoneId.systemDefault())
      .toInstant()
      .toEpochMilli()

  @Test
  fun `fragmentBucket labels a capture from earlier today as Today`() {
    val today = noonMillis(2026, 8, 26) // Wednesday

    assertEquals("Today", fragmentBucket(capturedAtMillis = today, now = today))
  }

  @Test
  fun `fragmentBucket labels a capture from one calendar day ago as Yesterday`() {
    val now = noonMillis(2026, 8, 26) // Wednesday
    val capturedAt = noonMillis(2026, 8, 25) // Tuesday

    assertEquals("Yesterday", fragmentBucket(capturedAt, now))
  }

  @Test
  fun `fragmentBucket labels Monday of the current calendar week as Earlier this week`() {
    // Today is Wednesday Aug 26; Monday Aug 24 started this calendar week, two
    // days ago — not "yesterday," but still within the current week.
    val now = noonMillis(2026, 8, 26) // Wednesday
    val capturedAt = noonMillis(2026, 8, 24) // Monday, this week

    assertEquals("Earlier this week", fragmentBucket(capturedAt, now))
  }

  @Test
  fun `fragmentBucket does not label last Thursday as Earlier this week when today is Tuesday`() {
    // The bug this test pins: last Thursday is five days before this Tuesday,
    // which used to fall inside the fixed `daysAgo in 2..6` window even though
    // it's actually in the previous calendar week (Monday-start).
    val now = noonMillis(2026, 8, 25) // Tuesday
    val capturedAt = noonMillis(2026, 8, 20) // Thursday, the week before

    assertEquals("Earlier", fragmentBucket(capturedAt, now))
  }

  @Test
  fun `fragmentBucket labels the Sunday just before this week as Earlier, not Earlier this week`() {
    // The calendar-week boundary itself: the Sunday right before this week's
    // Monday is the day before the cutoff, not inside it.
    val now = noonMillis(2026, 8, 26) // Wednesday
    val capturedAt = noonMillis(2026, 8, 23) // Sunday, the week before

    assertEquals("Earlier", fragmentBucket(capturedAt, now))
  }

  @Test
  fun `fragmentBucket still labels this week's Monday as Earlier this week when today is Sunday`() {
    // The widest possible span within one calendar week: Monday is six days
    // before this week's Sunday, but it's still this week.
    val now = noonMillis(2026, 8, 30) // Sunday
    val capturedAt = noonMillis(2026, 8, 24) // Monday, this week

    assertEquals("Earlier this week", fragmentBucket(capturedAt, now))
  }

  @Test
  fun `fragmentBucket labels a capture from two weeks ago as Earlier`() {
    val now = noonMillis(2026, 8, 26) // Wednesday
    val capturedAt = noonMillis(2026, 8, 10) // Monday, two weeks before

    assertEquals("Earlier", fragmentBucket(capturedAt, now))
  }
}
