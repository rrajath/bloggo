package com.rrajath.bloggo.data

import kotlinx.serialization.Serializable

/**
 * A portable snapshot of every user-configurable setting, for the
 * Settings -> Import / Export screen.
 *
 * The GitHub token is deliberately absent: a backup file is something a writer
 * might sync, email, or check in, and the PAT must never travel with it. An
 * import therefore never touches the stored token.
 *
 * Every field is nullable and defaults to null. Export fills them all in;
 * import applies only the keys actually present in the file and leaves any
 * absent key untouched, so an older or partial file still imports cleanly.
 */
@Serializable
data class SettingsBackup(
  val version: Int = 1,
  val repository: String? = null,
  val branch: String? = null,
  val siteUrl: String? = null,
  val authorName: String? = null,
  val postPath: String? = null,
  val imagePath: String? = null,
  val hugoConfigFile: String? = null,
  val frontmatterFields: String? = null,
  /** [PublishAction] name. */
  val publishAction: String? = null,
  /** [ThemeMode] name. */
  val themeMode: String? = null,
  /** [com.rrajath.bloggo.designsystem.component.ArtMode] name. */
  val artMode: String? = null,
  /** [com.rrajath.bloggo.ui.review.ReadabilityCheck] names that are enabled. */
  val readabilityChecks: List<String>? = null,
)
