package com.rrajath.bloggo.data

import com.rrajath.bloggo.model.DocKind
import com.rrajath.bloggo.model.Fragment
import com.rrajath.bloggo.model.MastodonAccount
import com.rrajath.bloggo.model.MediaFile
import com.rrajath.bloggo.model.Post
import com.rrajath.bloggo.model.PostState
import com.rrajath.bloggo.model.TootVisibility
import com.rrajath.bloggo.model.withDateFromFrontmatter

/**
 * The prototype's content, so the scaffold looks like the reference rather than
 * like lorem ipsum.
 *
 * This is the seam the real GitHub-backed repository replaces. Screens take the
 * data as parameters and never reach in here directly, so swapping the source is
 * a change in one place.
 */
object SampleData {

  val draftMarkdown = """
    ---
    title: On agents that actually ship
    date: 2026-08-17
    tags: [ai, tooling, craft]
    draft: true
    ---

    ## The gap nobody talks about

    Every agent demo ends at the moment of *generation*. The model writes
    the code, the video cuts, everyone claps. Then comes the part that
    **decides whether any of it matters**: getting the change reviewed,
    merged, and running in front of a person. That happens off camera.

    I have been keeping notes on this for six months.

    > A generated diff is a hypothesis. Shipping is the experiment.

    Three things separate the agents I keep from the ones I delete:

    1. They write to files I already own
    2. They show me the diff before it lands
    3. They fail loudly, in one place

    The third one sounds minor. It isn't. See [the tooling notes](/notes/tooling)
    for the longer version.
  """.trimIndent()

  val draft = Post(
    slug = "on-agents-that-actually-ship",
    title = "On agents that actually ship",
    state = PostState.Draft,
    markdown = draftMarkdown,
    wordCount = 1204,
    editedAgo = "9m ago",
    cover = "/images/2026/on-agents-that-actually-ship.png",
  ).withDateFromFrontmatter()

  val inReview = Post(
    slug = "six-months-with-a-folding-phone",
    title = "Six months with a folding phone",
    state = PostState.InReview,
    pullRequest = 42,
    wordCount = 960,
    markdown = """
      ---
      title: Six months with a folding phone
      date: 2026-08-16
      tags: [hardware, notes]
      draft: false
      ---

      ## The crease stops mattering around week three

      I did not expect that. Every review warns you about the crease, and for the
      first two weeks I saw it in every photo, every video, every time the screen
      caught the light wrong. Then it just stopped registering.

      What did not stop mattering:

      1. The hinge collects lint faster than any phone I have owned
      2. Folded, it is a genuinely better phone than my last unfolded one
      3. Unfolded, I still reach for my laptop for anything longer than a paragraph

      > A bigger screen did not make me a person who reads more on my phone. It
      > made me a person who reads the same amount, more comfortably.

      The real change was in what I stopped carrying. See [the hardware notes](/notes/hardware)
      for the rest of the six-month log.
    """.trimIndent(),
  ).withDateFromFrontmatter()

  val published = listOf(
    Post(
      "why-i-left-obsidian", "Why I left Obsidian for plain files", PostState.Published,
      wordCount = 840, date = "Aug 4, 2026",
      markdown = """
        ---
        title: Why I left Obsidian for plain files
        date: 2026-08-04
        tags: [tools, writing]
        draft: false
        ---

        ## The vault stopped being mine

        Not because of anything Obsidian did wrong. It is a good editor. But
        somewhere in the second year I noticed I was writing *for* the graph view
        instead of writing to think, and every new note came with a small tax:
        which folder, which tags, which links back.

        Plain files removed the tax. A directory of markdown, one editor, no app
        holding the format hostage. What I lost was the backlinks panel. What I
        got back was not thinking about the backlinks panel.

        - Nothing renders my notes but me
        - Grep is a better search than I expected to need
        - The friction I removed was mine to remove

        I do not think this is the right call for everyone. It was the right call
        for what I was actually using it for.
      """.trimIndent(),
    ),
    Post(
      "a-small-note-on-taste", "A small note on taste", PostState.Published,
      wordCount = 310, date = "Jul 28, 2026",
      markdown = """
        ---
        title: A small note on taste
        date: 2026-07-28
        tags: [craft]
        draft: false
        ---

        ## Taste is a rate, not a level

        People talk about taste like it is a stat you level up. It behaves more
        like a rate: how fast you notice the gap between what you made and what
        you meant, and how honestly you act on it before you ship.

        > The gap never closes. Good taste is noticing it faster than last time.

        That is the whole note. Short today on purpose.
      """.trimIndent(),
    ),
    Post(
      "the-cost-of-a-good-abstraction", "The cost of a good abstraction", PostState.Published,
      wordCount = 1650, date = "Jul 12, 2026",
      markdown = """
        ---
        title: The cost of a good abstraction
        date: 2026-07-12
        tags: [engineering, craft]
        draft: false
        ---

        ## It costs a rewrite you did not plan for

        A good abstraction is not free at the moment you write it. It is free
        later, at every call site that never has to know what changed underneath.
        The cost is up front, and it looks exactly like scope creep until the
        second or third caller shows up and the shape turns out to be right.

        The bad version of this is abstracting after one caller, on a guess. I
        have shipped that mistake more than once.

        1. Wait for the second caller before you generalise
        2. Name the abstraction after what it guarantees, not what it wraps
        3. Delete it the moment it stops paying for itself

        None of this is new. It is just easy to forget mid-deadline, which is
        exactly when it matters most.
      """.trimIndent(),
    ),
    Post(
      "rewriting-my-blog", "What I learned rewriting my blog for the fourth time", PostState.Published,
      wordCount = 2120, date = "Jun 30, 2026",
      markdown = """
        ---
        title: What I learned rewriting my blog for the fourth time
        date: 2026-06-30
        tags: [meta, writing]
        draft: false
        ---

        ## Four rewrites, one honest lesson

        Every rewrite was framed as a technical decision. Every rewrite was
        actually about lowering the friction between having a thought and it
        being published. The static site generator was never the bottleneck.

        > The tool you keep rewriting is rarely the tool that is broken.

        This one is a git-backed Hugo site edited from a phone, which sounds like
        a downgrade until you notice the actual metric: time from thought to
        published post, on the day the thought happened.

        - Rewrite one: chased a faster build
        - Rewrite two: chased a nicer theme
        - Rewrite three: chased owning the whole stack
        - Rewrite four: chased actually finishing posts

        Only the fourth one changed how much I wrote.
      """.trimIndent(),
    ),
    // Sorting the library by recency reads Post.dateMillis, not the markdown, so
    // sample posts have to carry the same parsed date a cached repo post does.
  ).map { it.withDateFromFrontmatter() }

  // Illustrative only, before a real connection has fetched the repo's real
  // top-level content/*.md files — the Pages tab's counterpart to `draft`/
  // `published` above. Every one is treated as already pushed (repoPath set),
  // matching what a real `content/{slug}.md` fetched from the tree would be.
  val pages = listOf(
    Post(
      slug = "about", title = "About", state = PostState.Published, kind = DocKind.Page,
      repoPath = "content/about.md",
      markdown = """
        ---
        title: About
        slug: about
        date: 2026-03-02
        ---

        I'm Rajath. I write here about the tools I build and the ones I
        throw away, mostly the second kind.

        This site runs on Hugo, checked into a repo I own, rendered by a
        theme I can read start to finish in an evening.
      """.trimIndent(),
    ),
    Post(
      slug = "uses", title = "Uses", state = PostState.Published, kind = DocKind.Page,
      repoPath = "content/uses.md",
      markdown = """
        ---
        title: Uses
        slug: uses
        date: 2026-01-18
        ---

        ## Hardware

        A folding phone and a keyboard from 2019 I refuse to replace.

        ## Software

        Plain text everywhere it can be. This blog is markdown files in a
        git repo, and the pattern holds until it doesn't.
      """.trimIndent(),
    ),
    Post(
      slug = "now", title = "Now", state = PostState.Published, kind = DocKind.Page,
      repoPath = "content/now.md",
      markdown = """
        ---
        title: Now
        slug: now
        date: 2026-07-30
        ---

        Shipping the git-backed editor this site is written in. Writing
        more, publishing less, which is a different problem than it sounds.
      """.trimIndent(),
    ),
    Post(
      slug = "contact", title = "Contact", state = PostState.Published, kind = DocKind.Page,
      repoPath = "content/contact.md",
      markdown = """
        ---
        title: Contact
        slug: contact
        date: 2025-11-09
        ---

        The fastest way to reach me is Mastodon, linked at the bottom of
        every post. Email works too, slower, and I read all of it.
      """.trimIndent(),
    ),
  ).map { it.withDateFromFrontmatter() }

  val fragments = listOf(
    Fragment("1", "The difference between a tool that respects your files and one that quietly owns them.", System.currentTimeMillis() - 2 * 3_600_000L, "promote to post"),
    Fragment("2", "AI code review is good at nits and bad at judgment. Worth working out why that is.", System.currentTimeMillis() - 3 * 3_600_000L, "2 links saved"),
    Fragment("3", "3am idea: frontmatter as a query language. Every field becomes a filter on the archive page.", System.currentTimeMillis() - 3 * 86_400_000L, "voice note · 0:22"),
    Fragment("4", "Bret Victor on immediate connection. Pull the exact wording before quoting it.", System.currentTimeMillis() - 4 * 86_400_000L, "needs a source"),
  )

  val media = listOf(
    MediaFile("agents-ship.png", "agents-ship", "/images/2026/agents-ship.png", usedBy = "on-agents-that-actually-ship", repoPath = "static/images/2026/agents-ship.png"),
    MediaFile("ledger-2.png", "ledger-2", "/images/2026/ledger-2.png", repoPath = "static/images/2026/ledger-2.png"),
    MediaFile("desk-aug.jpg", "desk-aug", "/images/2026/desk-aug.jpg", repoPath = "static/images/2026/desk-aug.jpg"),
    MediaFile("fold-hinge.jpg", "fold-hinge", "/images/2026/fold-hinge.jpg", repoPath = "static/images/2026/fold-hinge.jpg"),
    MediaFile("taste-01.png", "taste-01", "/images/2026/taste-01.png", usedBy = "a-small-note-on-taste", repoPath = "static/images/2026/taste-01.png"),
    MediaFile("abstraction.png", "abstraction", "/images/2026/abstraction.png", repoPath = "static/images/2026/abstraction.png"),
    MediaFile("obsidian-vault.png", "obsidian-vault", "/images/2026/obsidian-vault.png", repoPath = "static/images/2026/obsidian-vault.png"),
    MediaFile("rewrite-04.jpg", "rewrite-04", "/images/2026/rewrite-04.jpg", repoPath = "static/images/2026/rewrite-04.jpg"),
    MediaFile("notes-scan.jpg", "notes-scan", "/images/2026/notes-scan.jpg", repoPath = "static/images/2026/notes-scan.jpg"),
  )

  val accounts = listOf(
    MastodonAccount("hachy", "@rrajath@hachyderm.io", 500, selected = true),
    MastodonAccount("indie", "@rrajath@indieweb.social", 500, selected = true, visibility = TootVisibility.Unlisted),
    MastodonAccount("self", "@rajath@social.rrajath.dev", 2000),
    MastodonAccount("foss", "@writing@fosstodon.org", 500),
  )

  // Illustrative only, shown before a real connection exists — the Settings screen's
  // GitHub connection section and its own defaults now own the rest of what
  // `RepoConfig` used to describe.
  val sampleRepository = "rrajath/blog"
  val sampleBranch = "main"
  val sampleSiteHost = "rrajath.dev"

  val defaultToot = """
    New post: what separates an agent demo from an agent that actually lands a change.

    https://rrajath.dev/posts/on-agents-that-actually-ship
  """.trimIndent()
}
