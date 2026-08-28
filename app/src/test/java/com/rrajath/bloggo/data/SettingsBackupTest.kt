package com.rrajath.bloggo.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsBackupTest {

  private val json = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }

  @Test
  fun `round trips every field`() {
    val backup = SettingsBackup(
      repository = "rrajath/blog",
      branch = "main",
      siteUrl = "https://rrajath.dev",
      authorName = "Rajath",
      postPath = "content/posts/{slug}.md",
      imagePath = "static/images/",
      hugoConfigFile = "hugo.toml",
      frontmatterFields = "title, date, tags",
      frontmatterType = FrontmatterType.Toml.name,
      publishAction = PublishAction.OpenPullRequest.name,
      themeMode = ThemeMode.Dark.name,
      readabilityChecks = listOf("Adverbs", "PassiveVoice"),
    )

    val decoded = json.decodeFromString(SettingsBackup.serializer(), json.encodeToString(SettingsBackup.serializer(), backup))

    assertEquals(backup, decoded)
  }

  @Test
  fun `has no field that could carry a credential`() {
    val descriptor = SettingsBackup.serializer().descriptor
    val fieldNames = (0 until descriptor.elementsCount).map { descriptor.getElementName(it) }

    fieldNames.forEach { name ->
      val lower = name.lowercase()
      assertFalse(
        "SettingsBackup.$name looks like it could carry a secret",
        lower.contains("token") || lower == "pat" || lower.contains("secret") ||
          lower.contains("credential") || lower.contains("password"),
      )
    }
  }

  @Test
  fun `serialized form never contains the word token`() {
    val text = json.encodeToString(
      SettingsBackup.serializer(),
      SettingsBackup(repository = "rrajath/blog", authorName = "Rajath"),
    )

    assertFalse(text, text.contains("token", ignoreCase = true))
  }

  @Test
  fun `absent keys decode as null so an import leaves them untouched`() {
    val decoded = json.decodeFromString(
      SettingsBackup.serializer(),
      """{ "version": 1, "repository": "rrajath/blog" }""",
    )

    assertEquals("rrajath/blog", decoded.repository)
    assertNull(decoded.branch)
    assertNull(decoded.siteUrl)
    assertNull(decoded.themeMode)
    assertNull(decoded.readabilityChecks)
  }

  @Test
  fun `unknown keys from a newer file are ignored`() {
    val decoded = json.decodeFromString(
      SettingsBackup.serializer(),
      """{ "version": 2, "repository": "rrajath/blog", "somethingNew": true }""",
    )

    assertEquals("rrajath/blog", decoded.repository)
  }
}
