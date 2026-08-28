package com.rrajath.bloggo.data

/** [RepoConnection.postPath]'s `{slug}` token, resolved for a real post. Only
 * meaningful for a post that has never been committed — see [com.rrajath.bloggo.model.Post.repoPath]
 * for why an already-committed post never runs back through this. */
fun resolvePostPath(template: String, slug: String): String = template.replace("{slug}", slug)

/** Where a new page is written: `content/{slug}.md`, always — not
 * [RepoConnection.postPath]'s template, and not user-configurable, since a
 * top-level page's location in Hugo is fixed the same way `content/posts/`
 * is for a post. Only meaningful for a page that has never been committed
 * before; see [com.rrajath.bloggo.model.Post.repoPath] for why an
 * already-committed page skips this on every later publish. */
fun resolvePagePath(slug: String): String = "content/$slug.md"

/**
 * The inverse of the site-absolute path Hugo serves an asset at (e.g.
 * `/images/2026/slug.png`, the convention every `![](...)` figure already uses)
 * back to where it actually lives in the repo (`static/images/2026/slug.png`).
 *
 * Built as `"static/" + sitePath`, not joined against the configured image
 * path specifically — a figure's `src` isn't guaranteed to live under
 * [RepoConnection.imagePath], but every site-absolute path Hugo can serve
 * does live somewhere under `static/`.
 */
fun sitePathToRepoPath(sitePath: String): String = "static/" + sitePath.trimStart('/')

/** The forward half of the same convention — what a repo-relative path under
 * `static/` is served as once Hugo builds the site. */
fun repoPathToSitePath(repoPath: String): String = "/" + repoPath.removePrefix("static/").removePrefix("static").trimStart('/')
