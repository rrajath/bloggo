package com.rrajath.bloggo.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RepoConnectionTest {

  @Test
  fun `normalizeSiteUrl prepends https when there is no scheme`() {
    assertEquals("https://rrajath.dev", normalizeSiteUrl("rrajath.dev"))
  }

  @Test
  fun `normalizeSiteUrl keeps an explicit scheme and strips a trailing slash`() {
    assertEquals("http://localhost:1313", normalizeSiteUrl("http://localhost:1313/"))
    assertEquals("https://rrajath.dev", normalizeSiteUrl("  https://rrajath.dev/  "))
  }

  @Test
  fun `normalizeSiteUrl keeps blank blank`() {
    assertEquals("", normalizeSiteUrl(""))
    assertEquals("", normalizeSiteUrl("   "))
  }

  @Test
  fun `siteHost is derived from the full url`() {
    assertEquals("rrajath.dev", RepoConnection(siteUrl = "https://rrajath.dev").siteHost)
    assertEquals("rrajath.dev", RepoConnection(siteUrl = "http://rrajath.dev").siteHost)
    assertEquals("", RepoConnection().siteHost)
  }
}
