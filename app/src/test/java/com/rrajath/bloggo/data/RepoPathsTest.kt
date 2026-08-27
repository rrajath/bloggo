package com.rrajath.bloggo.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RepoPathsTest {
  @Test
  fun `resolvePostPath substitutes the slug token`() {
    assertEquals("content/posts/on-agents.md", resolvePostPath("content/posts/{slug}.md", "on-agents"))
  }

  @Test
  fun `resolvePostPath is a no-op when the template has no slug token`() {
    assertEquals("content/posts/fixed.md", resolvePostPath("content/posts/fixed.md", "on-agents"))
  }

  @Test
  fun `sitePathToRepoPath prefixes static back on`() {
    assertEquals("static/images/2026/slug.png", sitePathToRepoPath("/images/2026/slug.png"))
  }

  @Test
  fun `repoPathToSitePath strips the static prefix Hugo serves at the root`() {
    assertEquals("/images/2026/slug.png", repoPathToSitePath("static/images/2026/slug.png"))
  }

  @Test
  fun `sitePathToRepoPath and repoPathToSitePath round-trip`() {
    val repoPath = "static/images/2026/on-agents-1.jpg"
    assertEquals(repoPath, sitePathToRepoPath(repoPathToSitePath(repoPath)))
  }
}
