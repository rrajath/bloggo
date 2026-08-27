package com.rrajath.bloggo.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** How a new post reaches the repo once it's committed. */
enum class PublishAction {
  CommitToMain,
  OpenPullRequest,
  AskEveryTime,
}

/** What the app knows about the connected repo, before any GitHub call is made. */
data class RepoConnection(
  val repository: String = "",
  val branch: String = "main",
  val hasToken: Boolean = false,
  /** The live site's host, e.g. `rrajath.dev`. There is no reliable way to detect
   * this from the repo alone — GitHub Pages is the only host discoverable via the
   * API, and plenty of Hugo sites deploy elsewhere — so it is set by hand. */
  val siteHost: String = "",
  /** Read into a page's share text ("<title> · <author>") — there is no
   * frontmatter or repo-detected source for this, so it is set by hand, same
   * as [siteHost]. Blank means the share text drops the "· <author>" part
   * entirely rather than showing an empty one. */
  val authorName: String = "",
  /** Where a new post is written, as a template — `{slug}` is filled in via
   * `resolvePostPath` (`data/RepoPaths.kt`) when a post is actually committed
   * (§5.3), but only for a post that has never been committed before; see
   * [com.rrajath.bloggo.model.Post.repoPath] for why an already-committed post
   * skips this template on every later publish. */
  val postPath: String = "content/posts/{slug}.md",
  /** Where a generated or uploaded cover is written, repo-relative. */
  val imagePath: String = "static/images/",
  /** The Hugo config filename the app treats as canonical. Corrects what
   * detection found, or stands in for it before a connection exists. */
  val hugoConfigFile: String = "config.toml",
  /** Comma-separated frontmatter field names a new post is seeded with. */
  val frontmatterFields: String = "title, date, tags, slug, draft",
  val publishAction: PublishAction = PublishAction.AskEveryTime,
)

private val Context.repoConnectionDataStore by preferencesDataStore(name = "repo_connection")

/**
 * Stores the fine-grained PAT and the repo it unlocks.
 *
 * This is storage only: no `GET /repos/{o}/{r}` call, no Hugo detection. Those
 * are the rest of ANDROID_TDD.md §7.1 and land with the GitHub client.
 *
 * The token lives only in [EncryptedSharedPreferences], Keystore-backed, per
 * §7.3 — never in DataStore, never in Room, never logged. `hasToken` is a
 * plain boolean mirror in DataStore so the UI can react to it without reading
 * the encrypted store on every recomposition.
 */
class RepoConnectionRepository(private val context: Context) {
  private val repositoryKey = stringPreferencesKey("repository")
  private val branchKey = stringPreferencesKey("branch")
  private val hasTokenKey = booleanPreferencesKey("has_token")
  private val siteHostKey = stringPreferencesKey("site_host")
  private val authorNameKey = stringPreferencesKey("author_name")
  private val postPathKey = stringPreferencesKey("post_path")
  private val imagePathKey = stringPreferencesKey("image_path")
  private val hugoConfigFileKey = stringPreferencesKey("hugo_config_file")
  private val frontmatterFieldsKey = stringPreferencesKey("frontmatter_fields")
  private val publishActionKey = stringPreferencesKey("publish_action")

  private val masterKey by lazy {
    MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
  }

  private val securePrefs by lazy {
    EncryptedSharedPreferences.create(
      context,
      "github_secrets",
      masterKey,
      EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
      EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
  }

  val connection = context.repoConnectionDataStore.data.map { prefs ->
    RepoConnection(
      repository = prefs[repositoryKey].orEmpty(),
      branch = prefs[branchKey]?.takeIf { it.isNotBlank() } ?: "main",
      hasToken = prefs[hasTokenKey] ?: false,
      siteHost = prefs[siteHostKey].orEmpty(),
      authorName = prefs[authorNameKey].orEmpty(),
      postPath = prefs[postPathKey]?.takeIf { it.isNotBlank() } ?: "content/posts/{slug}.md",
      imagePath = prefs[imagePathKey]?.takeIf { it.isNotBlank() } ?: "static/images/",
      hugoConfigFile = prefs[hugoConfigFileKey]?.takeIf { it.isNotBlank() } ?: "config.toml",
      frontmatterFields = prefs[frontmatterFieldsKey]?.takeIf { it.isNotBlank() } ?: "title, date, tags, slug, draft",
      publishAction = prefs[publishActionKey]?.let { saved ->
        runCatching { PublishAction.valueOf(saved) }.getOrNull()
      } ?: PublishAction.AskEveryTime,
    )
  }

  suspend fun setRepo(repository: String, branch: String, siteHost: String, authorName: String) {
    context.repoConnectionDataStore.edit { prefs ->
      prefs[repositoryKey] = repository.trim()
      prefs[branchKey] = branch.trim().ifBlank { "main" }
      prefs[siteHostKey] = siteHost.trim().removePrefix("https://").removePrefix("http://").trim('/')
      prefs[authorNameKey] = authorName.trim()
    }
  }

  suspend fun setPostPath(path: String) {
    context.repoConnectionDataStore.edit { prefs -> prefs[postPathKey] = path.trim() }
  }

  suspend fun setImagePath(path: String) {
    context.repoConnectionDataStore.edit { prefs -> prefs[imagePathKey] = path.trim() }
  }

  suspend fun setHugoConfigFile(file: String) {
    context.repoConnectionDataStore.edit { prefs -> prefs[hugoConfigFileKey] = file.trim() }
  }

  suspend fun setFrontmatterFields(fields: String) {
    context.repoConnectionDataStore.edit { prefs -> prefs[frontmatterFieldsKey] = fields.trim() }
  }

  suspend fun setPublishAction(action: PublishAction) {
    context.repoConnectionDataStore.edit { prefs -> prefs[publishActionKey] = action.name }
  }

  suspend fun setToken(token: String) {
    // Pasted tokens routinely carry a trailing newline or space from the
    // clipboard. Untrimmed, that reaches OkHttp as a raw Authorization header
    // value and OkHttp throws on the control character — better to never
    // store the dirty value than to crash on first use.
    securePrefs.edit().putString(TOKEN_KEY, token.trim()).apply()
    context.repoConnectionDataStore.edit { prefs -> prefs[hasTokenKey] = true }
  }

  suspend fun clearToken() {
    securePrefs.edit().remove(TOKEN_KEY).apply()
    context.repoConnectionDataStore.edit { prefs -> prefs[hasTokenKey] = false }
  }

  /**
   * Never logged, never shown back in the UI — only read to build an
   * Authorization header.
   *
   * `suspend` on Dispatchers.IO because this is disk I/O twice over: reading
   * [EncryptedSharedPreferences] is an AES-SIV/GCM decrypt over a preferences
   * file, and the first touch of the lazy also unwraps the Keystore master key.
   * It used to be a plain function, which meant the Settings screen read it during
   * composition — on the main thread, once per recomposition.
   */
  suspend fun getToken(): String? = withContext(Dispatchers.IO) {
    securePrefs.getString(TOKEN_KEY, null)
  }

  private companion object {
    const val TOKEN_KEY = "github_pat"
  }
}
