package com.rrajath.bloggo.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * The domain the app actually deals in.
 *
 * These are the shapes the P0 screens bind to. The GitHub layer described in
 * docs/ANDROID_TDD.md maps onto them; nothing here mentions the network, so the
 * UI can be built and previewed before any of that exists.
 */

/** Where a post stands relative to the repo and the published site. */
enum class PostState {
  /** Local only, or committed with `draft: true`. */
  Draft,

  /** Merged and live on the site. */
  Published,

  /** Open on a branch with a pull request. */
  InReview,
}

/** A post under `content/posts/`, or a top-level page under `content/` — see
 * docs/PROTOTYPE_NOTES.md's "Pages replaced Media". Both read and write the
 * same [Post] shape and the same Editor/Preview screens; [kind] is what a
 * handful of call sites (the editor header chip, the frontmatter sheet, the
 * commit path, the share text) branch on to tell them apart. */
enum class DocKind { Post, Page }

data class Post(
  val slug: String,
  val title: String,
  val state: PostState,
  val kind: DocKind = DocKind.Post,
  /** Body markdown including frontmatter, exactly as it sits in the repo. */
  val markdown: String = "",
  val wordCount: Int = 0,
  /** Human date for list rows, e.g. "Aug 4". Absent for unpublished work. */
  val date: String? = null,
  /** Relative edit time for drafts, e.g. "9m ago". */
  val editedAgo: String? = null,
  val pullRequest: Int? = null,
  /** Epoch millis of the last local edit. Null for a post that has never been
   * edited on this device — see [lastEditedAtMillis] for the fallback used then. */
  val updatedAt: Long? = null,
  /** [date] as epoch millis, parsed once where the post is built rather than on
   * every comparison. Sorting a library by recency invokes its selector O(n log n)
   * times, so a selector that re-parsed frontmatter did a full document pass per
   * comparison; this makes [lastEditedAtMillis] a field read. */
  val dateMillis: Long? = null,
  /** Where this post actually lives in the repo, e.g. `content/posts/on-agents.md`
   * — set once a post has been fetched from or committed to the repo, null for a
   * local draft or fragment-promoted post that has never been either. Once set,
   * publishing always writes back here rather than re-deriving a path from the
   * `{slug}` template: the frontmatter `slug:` and the filename are independent
   * in Hugo, so a slug edited after the first commit must not fork the post into
   * a second file. */
  val repoPath: String? = null,
) {
  /**
   * The slug Hugo (and the repo) actually knows this post by: the frontmatter
   * `slug:`/`slug =` field, not [slug] itself. [slug] is this app's own
   * internal identifier — unique across drafts by construction (a fresh
   * `-{timestamp}` suffix from `BloggoApp.kt`'s `newBlankPost`/
   * `newBlankPage`/`newPostFromFragment`), so it can key the in-memory post
   * list and the local-drafts table before a post has committed frontmatter
   * to read a real slug from — and is not what Hugo uses to build the page's
   * permalink, nor what the committed file should be named. Falls back to
   * [slug] only when the frontmatter has no `slug:` field at all (e.g. it was
   * dropped from [com.rrajath.bloggo.data.RepoConnection.frontmatterFields]).
   */
  val publicSlug: String
    get() = markdown.parseFrontmatter()["slug"]?.takeIf { it.isNotBlank() } ?: slug

  /**
   * The public URL, derivable before the post exists.
   *
   * Built from [publicSlug], not [slug] itself — see [publicSlug]'s own doc
   * comment for why those two differ.
   */
  fun liveUrl(host: String): String =
    if (kind == DocKind.Page) "$host/$publicSlug" else "$host/posts/$publicSlug"
}

data class Fragment(
  val id: String,
  val text: String,
  /** The instant this thought was actually captured — the one fact that must
   * never change again, including when the fragment is later opened, edited,
   * or promoted to a post. [capturedAt] and [bucket] are derived from this
   * against the current moment on every read rather than stored, which is
   * what keeps a capture from yesterday still reading "Today" a day later. */
  val capturedAtMillis: Long,
  val tag: String? = null,
) {
  /** Time-of-day for a capture made today ("08:12"), day-and-time otherwise
   * ("Fri 03:14") — recomputed against now, never frozen at capture time. */
  val capturedAt: String get() = fragmentTimeLabel(capturedAtMillis)

  /** Inbox grouping header: "Today", "Yesterday", "Earlier this week",
   * "Earlier" — recomputed against now for the same reason [capturedAt] is. */
  val bucket: String get() = fragmentBucket(capturedAtMillis)
}

private val fragmentTimeOfDayFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val fragmentDayTimeFormatter = DateTimeFormatter.ofPattern("EEE HH:mm")

/** See [Fragment.capturedAt]. [now] is a parameter only so this is testable
 * without mocking the clock; every real caller uses the default. */
fun fragmentTimeLabel(capturedAtMillis: Long, now: Long = System.currentTimeMillis()): String {
  val zone = ZoneId.systemDefault()
  val captured = Instant.ofEpochMilli(capturedAtMillis).atZone(zone)
  val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
  return if (captured.toLocalDate() == today) {
    captured.format(fragmentTimeOfDayFormatter)
  } else {
    captured.format(fragmentDayTimeFormatter)
  }
}

/** See [Fragment.bucket]. [now] is a parameter only so this is testable
 * without mocking the clock; every real caller uses the default.
 *
 * "Earlier this week" is calendar-aware, not a fixed days-ago window: a capture
 * only lands there if its calendar date falls on or after the current week's
 * Monday. A fixed `daysAgo in 2..6` range doesn't respect that boundary — a
 * capture from, say, last Thursday would still read as "this week" on a
 * Tuesday, five days later, even though the calendar week has since turned
 * over. Weeks are taken to start on Monday, matching ISO-8601 (`DayOfWeek`'s
 * own ordering), since nothing else in the app already picks a convention. */
fun fragmentBucket(capturedAtMillis: Long, now: Long = System.currentTimeMillis()): String {
  val zone = ZoneId.systemDefault()
  val capturedDate = Instant.ofEpochMilli(capturedAtMillis).atZone(zone).toLocalDate()
  val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
  val daysAgo = ChronoUnit.DAYS.between(capturedDate, today)
  val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
  return when {
    daysAgo <= 0 -> "Today"
    daysAgo == 1L -> "Yesterday"
    !capturedDate.isBefore(startOfWeek) -> "Earlier this week"
    else -> "Earlier"
  }
}

/** A file under the repo's configured image path — real (already committed,
 * [repoPath] set) or staged (not yet committed, [localPath] set; see
 * [StagedMedia], which every staged [MediaFile] is built from). */
data class MediaFile(
  val name: String,
  val seed: String,
  /** Site-absolute path a post's markdown would reference this by. */
  val sitePath: String,
  val usedBy: String? = null,
  val repoPath: String? = null,
  val localPath: String? = null,
) {
  val isStaged: Boolean get() = repoPath == null
}

/** A connected Mastodon account. Limits differ per instance, so they travel with it. */
data class MastodonAccount(
  val id: String,
  val handle: String,
  val characterLimit: Int,
  val selected: Boolean = false,
  val visibility: TootVisibility = TootVisibility.Public,
) {
  val instance: String get() = handle.substringAfterLast('@')
  val avatarLetter: String get() = handle.removePrefix("@").take(1).uppercase()
}

enum class TootVisibility(val label: String) {
  Public("Public"),
  Unlisted("Unlisted"),
  Followers("Followers only"),
}

/** Hugo posts fence frontmatter with either `---` (YAML, `key: value`) or `+++`
 * (TOML, `key = value`). Both are common in the wild — a theme's archetype
 * decides which a given site uses — so both must parse the same fields out. */
private const val YAML_FENCE = "---"
private const val TOML_FENCE = "+++"

private val frontmatterRegex = Regex("^(?:---[\\s\\S]*?\\n---|\\+\\+\\+[\\s\\S]*?\\n\\+\\+\\+)\\n+")
private val wordRegex = Regex("[A-Za-z0-9'’]+")
private val markdownPunctuationRegex = Regex("[#>*`\\[\\]()_-]")

/** Word count of the body, frontmatter excluded. Shared so the editor toolbar, the
 * library row, and the article parser never drift out of sync on what a word is. */
fun String.markdownWordCount(): Int {
  val body = replace(frontmatterRegex, "")
  return wordRegex.findAll(body.replace(markdownPunctuationRegex, " ")).count()
}

/** The leading frontmatter block, fence to fence and including the blank lines
 * that follow it, or `""` when there is none. Shared with [markdownWordCount] so
 * Focus mode — which hides the block while editing and restores it on the way
 * out — never disagrees with the word count about where the body starts. */
fun String.frontmatterBlock(): String = frontmatterRegex.find(this)?.value.orEmpty()

/**
 * The frontmatter fence this markdown opens with, or null if it has none.
 *
 * Reads only as far as the first newline. It used to be `lines().firstOrNull()`,
 * which split the *entire* document to look at line 1 — and since every
 * frontmatter helper below starts by calling this, reading one field out of a
 * 5,000-word post allocated a line list of the whole post before it began.
 */
private fun String.frontmatterFence(): String? {
  val firstLineEnd = indexOf('\n').takeIf { it >= 0 } ?: length
  return substring(0, firstLineEnd).trim().takeIf { it == YAML_FENCE || it == TOML_FENCE }
}

/** Parses the `key: value` (YAML) or `key = value` (TOML) lines of a leading
 * frontmatter block, unquoted. Shared so the library cache and
 * [com.rrajath.bloggo.ui.preview.ArticleParser] never read a post's frontmatter
 * differently.
 *
 * Walks the string to the closing fence rather than splitting it, so this is
 * O(frontmatter) rather than O(document): it runs on every keystroke in the
 * editor, and the body it never reads is the part that grows. */
fun String.parseFrontmatter(): Map<String, String> {
  val fence = frontmatterFence() ?: return emptyMap()
  val separator = if (fence == YAML_FENCE) ':' else '='
  val frontmatter = mutableMapOf<String, String>()
  // The fence itself is line 0; a document that is nothing but a fence has no
  // fields and no closing fence to look for.
  var start = indexOf('\n').takeIf { it >= 0 }?.plus(1) ?: return emptyMap()
  while (start <= length) {
    val newline = indexOf('\n', start)
    val lineEnd = if (newline >= 0) newline else length
    val line = substring(start, lineEnd)
    // An unterminated block still yields whatever it did contain, as before.
    if (line.trim() == fence) break
    val sep = line.indexOf(separator)
    if (sep > 0) {
      val key = line.take(sep).trim()
      val value = line.drop(sep + 1).trim().trim('"', '\'')
      frontmatter[key] = value
    }
    if (newline < 0) break
    start = newline + 1
  }
  return frontmatter
}

/** A frontmatter `tags:`/`tags =` value like `[ai, tooling, craft]` parsed into
 * individual tag strings. Shared so the post-details sheet and the preview
 * eyebrow never read a post's tags differently. */
fun String.parseTagList(): List<String> =
  trim('[', ']').split(',').map { it.trim().trim('"', '\'') }.filter { it.isNotEmpty() }

/** The frontmatter date to show and sort by. `date:` wins when it's present;
 * a post that only records when it was last touched — `lastmod:` or the
 * `lastModifiedDate:` some themes use instead — falls back to that rather
 * than showing no date at all. Shared so the library, the post-details sheet
 * and the preview never disagree on which post is newest. */
fun Map<String, String>.effectiveDate(): String? =
  listOf("date", "lastmod", "lastModifiedDate").firstNotNullOfOrNull { key ->
    this[key]?.takeIf { it.isNotBlank() }
  }

/** Rewrites one or more frontmatter lines in place, preserving fence style,
 * field order, and everything outside the frontmatter block. Each pair's
 * second element is the literal right-hand side to write (already quoted or
 * bracketed as needed — callers differ on that, so this doesn't guess). Keys
 * already present are updated in place; keys that aren't get appended just
 * before the closing fence, in the order given, but only when [appendMissing]
 * says a caller actually wants that (a plain rename never should — silently
 * inventing a field a rename didn't ask about would be its own bug). A no-op
 * if the markdown has no frontmatter block at all. */
private fun String.rewriteFrontmatterFields(
  edits: List<Pair<String, String>>,
  appendMissing: Boolean,
): String {
  val lines = lines().toMutableList()
  val fence = frontmatterFence() ?: return this
  val isToml = fence == TOML_FENCE
  val separator = if (isToml) '=' else ':'
  val remaining = LinkedHashMap<String, String>().apply { edits.forEach { (k, v) -> put(k, v) } }
  var index = 1
  var closeIndex = -1
  while (index < lines.size) {
    if (lines[index].trim() == fence) {
      closeIndex = index
      break
    }
    val line = lines[index]
    val sep = line.indexOf(separator)
    if (sep > 0) {
      val key = line.take(sep).trim()
      remaining.remove(key)?.let { value ->
        lines[index] = if (isToml) "$key = $value" else "$key: $value"
      }
    }
    index++
  }
  if (closeIndex == -1) return this
  if (appendMissing && remaining.isNotEmpty()) {
    val appended = edits
      .filter { (key, _) -> key in remaining }
      .map { (key, value) -> if (isToml) "$key = $value" else "$key: $value" }
    lines.addAll(closeIndex, appended)
  }
  return lines.joinToString("\n")
}

/** Rewrites one frontmatter field's value in place, preserving fence style,
 * field order, and everything outside the frontmatter block. A no-op if the
 * markdown has no frontmatter or the key isn't present in it. */
private fun String.replaceFrontmatterField(key: String, value: String): String {
  val isToml = frontmatterFence() == TOML_FENCE
  val rendered = if (isToml) "\"$value\"" else value
  return rewriteFrontmatterFields(listOf(key to rendered), appendMissing = false)
}

/** The frontmatter fields [com.rrajath.bloggo.ui.sheet.PostDetailsSheet] lets
 * a writer edit directly, bundled so one save rewrites the whole block in a
 * single pass. */
data class FrontmatterEdits(
  val title: String,
  val slug: String,
  val date: String,
  val tags: List<String>,
  val draft: Boolean,
)

/** Applies a Post Details sheet save to [this] markdown's frontmatter block:
 * title, slug, date, tags and draft are rewritten (or, for a field the site's
 * archetype didn't already have, appended) while every other field, the
 * fence style, and the whole body are left untouched. TOML strings are
 * quoted, as [replaceFrontmatterField] already does for a slug rename; a
 * TOML date is left bare, matching the native, unquoted offset-date-time
 * this app's own archetype produces. A no-op if the markdown has no
 * frontmatter block. */
fun String.withFrontmatterEdits(edits: FrontmatterEdits): String {
  val isToml = frontmatterFence() == TOML_FENCE
  fun quotedIfToml(value: String) = if (isToml) "\"$value\"" else value
  val tags = edits.tags.joinToString(", ") { if (isToml) "\"$it\"" else it }
  val fields = listOf(
    "title" to quotedIfToml(edits.title),
    "slug" to quotedIfToml(edits.slug),
    "date" to edits.date,
    "tags" to "[$tags]",
    "draft" to edits.draft.toString(),
  )
  return rewriteFrontmatterFields(fields, appendMissing = true)
}

/** The frontmatter fields [com.rrajath.bloggo.ui.sheet.PostDetailsSheet] lets a
 * writer edit for a page — no tags or draft flag, since a page like About isn't
 * tagged or drafted (docs/PROTOTYPE_NOTES.md, "Pages replaced Media"). */
data class PageFrontmatterEdits(
  val title: String,
  val slug: String,
  val date: String,
)

/** [withFrontmatterEdits]'s counterpart for a page: title, slug, date and
 * [lastmod] are rewritten (or appended), nothing else — a page's frontmatter
 * sheet never touches tags or draft. */
fun String.withPageFrontmatterEdits(edits: PageFrontmatterEdits, lastmod: String): String {
  val isToml = frontmatterFence() == TOML_FENCE
  fun quotedIfToml(value: String) = if (isToml) "\"$value\"" else value
  val fields = listOf(
    "title" to quotedIfToml(edits.title),
    "slug" to quotedIfToml(edits.slug),
    "date" to edits.date,
    "lastmod" to lastmod,
  )
  return rewriteFrontmatterFields(fields, appendMissing = true)
}

/** Upserts a `lastmod`/`lastmod =` frontmatter field to [timestamp] — the
 * "when was this page last touched" signal Hugo itself reads, per the "add
 * lastmod on every edit" requirement for pages. Left bare, unquoted, in both
 * fence styles, matching how `date` is already written. */
fun String.withUpdatedLastmod(timestamp: String): String =
  rewriteFrontmatterFields(listOf("lastmod" to timestamp), appendMissing = true)

/** Upserts a `date`/`date =` frontmatter field to [timestamp] — what
 * [com.rrajath.bloggo.ui.sheet.PublishSheet]'s "Set date to Today" toggle (or
 * its manual override) writes at the moment Publish is actually tapped, never
 * before: a multi-day draft's `date:` must stay whatever it was captured or
 * last set to right up until publishing actually happens. Left bare,
 * unquoted, in both fence styles, matching how `date` is already written
 * elsewhere. */
fun String.withUpdatedDate(timestamp: String): String =
  rewriteFrontmatterFields(listOf("date" to timestamp), appendMissing = true)

/** RFC3339, seconds precision — what Hugo's own archetypes write to `date:`/
 * `lastmod:`. Shared so a new post's `date:` (`BloggoApp.kt`) and a page's
 * `lastmod:` stamp are generated the same way. */
fun currentFrontmatterTimestamp(): String =
  OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

/** [currentFrontmatterTimestamp]'s counterpart for a specific instant rather
 * than now — a captured fragment's own [Fragment.capturedAtMillis], so
 * promoting it to a post writes the moment it was actually captured to
 * `date:` instead of whenever the writer happened to open or promote it. */
fun frontmatterTimestampFromMillis(epochMillis: Long): String =
  Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toOffsetDateTime()
    .truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

private val slugPunctuationRegex = Regex("[^a-z0-9]+")

/** A URL-safe slug from free text: lowercase, non-alphanumerics collapsed to a
 * single hyphen, no leading/trailing hyphen. Shared so a new post's slug and its
 * frontmatter `slug:` field are always derived the same way. */
fun slugify(text: String): String {
  val slug = text.lowercase().replace(slugPunctuationRegex, "-").trim('-')
  return slug.ifEmpty { "untitled" }
}

/**
 * Keeps a post's frontmatter `slug:`/`slug =` field tracking its `title` as the
 * writer types, the way a CMS permalink field follows the title until it's
 * edited by hand. The moment the stored slug stops matching `slugify(oldTitle)`
 * — because the writer changed it themselves — this stops touching it, so a
 * deliberate edit is never silently overwritten by the next keystroke in the
 * title.
 */
fun syncSlugToTitle(oldMarkdown: String, newMarkdown: String): String {
  val oldTitle = oldMarkdown.parseFrontmatter()["title"] ?: return newMarkdown
  val newFrontmatter = newMarkdown.parseFrontmatter()
  val newTitle = newFrontmatter["title"] ?: return newMarkdown
  if (oldTitle == newTitle) return newMarkdown
  val currentSlug = newFrontmatter["slug"] ?: return newMarkdown
  if (currentSlug != slugify(oldTitle)) return newMarkdown
  return newMarkdown.replaceFrontmatterField("slug", slugify(newTitle))
}

private val dateOnlyFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")

/** A frontmatter `date:` value, human-readable. Hugo dates are RFC3339
 * (`2026-08-18T11:01:19-07:00`) once written by this app's own archetype, but a
 * plain `2026-08-18` is just as valid frontmatter, so both must render rather
 * than one falling back to raw ISO text. Anything that parses as neither is
 * shown as written. */
fun formatFrontmatterDate(raw: String): String = try {
  OffsetDateTime.parse(raw).format(dateTimeFormatter)
} catch (e: DateTimeParseException) {
  try {
    LocalDate.parse(raw.take(10)).format(dateOnlyFormatter)
  } catch (e: DateTimeParseException) {
    raw
  }
}

/** A frontmatter `date:` value as a short library-row date ("Aug 4, 2026"),
 * keying only off the `yyyy-MM-dd` prefix every RFC3339 or plain-date value
 * shares, since a full RFC3339 timestamp is at least as common in the wild as
 * a bare date. Shared by [com.rrajath.bloggo.data.library.PostLibraryRepository]
 * and its Pages counterpart so a post row and a page row format dates the same
 * way. Anything that doesn't parse is shown as written rather than dropped. */
fun formatShortFrontmatterDate(raw: String): String = try {
  LocalDate.parse(raw.take(10), DateTimeFormatter.ISO_LOCAL_DATE).format(dateOnlyFormatter)
} catch (e: DateTimeParseException) {
  raw
}

/** A frontmatter `date:` value as epoch millis, for sorting — RFC3339 or a plain
 * `yyyy-MM-dd` both parse; anything else sorts as unknown (null). */
fun parseFrontmatterDateEpochMillis(raw: String): Long? = try {
  OffsetDateTime.parse(raw).toInstant().toEpochMilli()
} catch (e: DateTimeParseException) {
  try {
    LocalDate.parse(raw.take(10)).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
  } catch (e: DateTimeParseException) {
    null
  }
}

/**
 * Whether this post/page actually exists in the repo, as opposed to only on
 * this device.
 *
 * [PostState.Draft] is not this signal: it's set the moment a post is created
 * locally and never flipped afterward (see `BloggoApp.kt`'s `onPublish`,
 * which updates [Post.repoPath] on a successful publish but not [Post.state]),
 * so it conflates "never pushed" with "pushed, but committed with a
 * `draft: true` frontmatter field." The real "is this on the remote" signal is
 * this: either the last library refresh actually saw this slug on the repo
 * ([remoteSlugs], `BloggoApp.kt`'s `remotePostSlugs`/`remotePageSlugs`), or
 * this post has already been committed at least once this session
 * ([Post.repoPath] set, whether from a fetch or from `onPublish`'s own
 * update). Shared so a delete-confirmation flow and a "Move to Inbox" button
 * (both later work) don't each re-derive it slightly differently.
 */
fun Post.isPushed(remoteSlugs: Set<String>): Boolean = slug in remoteSlugs || repoPath != null

/** Best-effort "last touched" instant, for sorting drafts and published posts by
 * recency. A real local edit's [Post.updatedAt] wins when there is one; a post
 * fetched from the repo but never edited on this device falls back to its
 * frontmatter `date:`, the closest signal available without a per-file commit
 * history call.
 *
 * A field read, deliberately: [Post.dateMillis] is parsed once where the post is
 * built (see [withDateFromFrontmatter] and the library cache). Parsing here
 * instead made it a full document pass per sort comparison. */
fun Post.lastEditedAtMillis(): Long? = updatedAt ?: dateMillis

/** Fills [Post.dateMillis] in from the post's own frontmatter. For posts built
 * from literal markdown — sample data, a promoted fragment — where there is no
 * cache row to carry an already-parsed date. */
fun Post.withDateFromFrontmatter(): Post =
  copy(dateMillis = markdown.parseFrontmatter().effectiveDate()?.let(::parseFrontmatterDateEpochMillis))

/**
 * Library search, title first but genuinely full-text: [Post.markdown] is the
 * whole document — frontmatter and body both — and every post's copy of it is
 * already sitting in the Room cache from the last refresh (see
 * `PostLibraryRepository`), not fetched on demand. So a query matching a word
 * buried in a post's body, or in a tag, is a real hit rather than a metadata-only
 * approximation of one; it costs nothing beyond the substring scan itself.
 *
 * A plain case-insensitive `contains`, not a Room FTS4 table: a personal blog's
 * library is tens to low hundreds of posts, not a corpus large enough for
 * indexing or ranking to earn their keep, and every post is already in memory
 * as a [Post] by the time this runs.
 */
fun Post.matchesSearchQuery(query: String): Boolean {
  if (query.isBlank()) return true
  return title.contains(query, ignoreCase = true) ||
    slug.contains(query, ignoreCase = true) ||
    markdown.contains(query, ignoreCase = true)
}

/** Every post in [this] whose title, slug, tags or body match [query] — see
 * [matchesSearchQuery]. A blank query is "no filter": the list back unchanged,
 * in the order it was given, so an empty search box always means "everything." */
fun List<Post>.searchLibrary(query: String): List<Post> =
  if (query.isBlank()) this else filter { it.matchesSearchQuery(query) }

/** [Post.markdown] with any leading frontmatter block stripped off — just the
 * prose a reader would recognize as the post itself, not its `title:`/`tags:`
 * fields. */
private fun String.bodyOnly(): String {
  val fence = frontmatterFence() ?: return this
  var start = indexOf('\n').takeIf { it >= 0 }?.plus(1) ?: return ""
  while (start <= length) {
    val newline = indexOf('\n', start)
    val lineEnd = if (newline >= 0) newline else length
    if (substring(start, lineEnd).trim() == fence) return if (newline < 0) "" else substring(newline + 1)
    if (newline < 0) return ""
    start = newline + 1
  }
  return ""
}

/** A short window of lines from the post's body around the first line that
 * contains [query] — the matching line plus a line of context before and
 * after it, trimmed — what a search result shows under the title so a hit
 * buried in the text reads as an actual quote from the post, with enough
 * surrounding context that the highlighted term is actually visible, rather
 * than a title-only match with no clue why it surfaced. Null when [query] is
 * blank, or only matched the title, slug, or a frontmatter field. */
fun Post.matchingSnippet(query: String): String? {
  if (query.isBlank()) return null
  val lines = markdown.bodyOnly().lineSequence()
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .toList()
  val matchIndex = lines.indexOfFirst { it.contains(query, ignoreCase = true) }
  if (matchIndex < 0) return null
  val start = (matchIndex - 1).coerceAtLeast(0)
  val end = (matchIndex + 1).coerceAtMost(lines.lastIndex)
  return lines.subList(start, end + 1).joinToString(" ")
}
