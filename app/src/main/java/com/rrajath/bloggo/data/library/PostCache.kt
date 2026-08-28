package com.rrajath.bloggo.data.library

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import android.content.Context
import com.rrajath.bloggo.data.inbox.FragmentDao
import com.rrajath.bloggo.data.inbox.FragmentEntity
import com.rrajath.bloggo.data.review.ReadabilityIgnoreDao
import com.rrajath.bloggo.data.review.ReadabilityIgnoreEntity

/**
 * ANDROID_TDD.md §5.2: the tree gives paths and blob SHAs, not titles, so the
 * library caches `path -> (blobSha, title, date, draft)` and only re-fetches a
 * post whose blob SHA changed. The full markdown rides along too, since fetching
 * it is what extracting frontmatter already requires — Editor and Preview read
 * it from here rather than making a second network round trip.
 */
@Entity(tableName = "post_cache")
data class PostCacheEntity(
  @PrimaryKey val path: String,
  val blobSha: String,
  val slug: String,
  val title: String,
  val date: String?,
  val draft: Boolean,
  val markdown: String,
  /** Word count of [markdown], measured once when the file was fetched. A post
   * whose blob SHA is unchanged cannot have a changed word count, so re-counting
   * every cached post on every refresh — a full document pass each — was work
   * whose answer was already known. [UNKNOWN_WORD_COUNT] marks a row written
   * before this column existed. */
  val wordCount: Int = UNKNOWN_WORD_COUNT,
) {
  companion object {
    const val UNKNOWN_WORD_COUNT = -1
  }
}

@Dao
interface PostCacheDao {
  @Query("SELECT * FROM post_cache")
  suspend fun getAll(): List<PostCacheEntity>

  /** Paths only. Working out what to delete does not need the corpus in memory. */
  @Query("SELECT path FROM post_cache")
  suspend fun getAllPaths(): List<String>

  @Upsert
  suspend fun upsertAll(entries: List<PostCacheEntity>)

  @Query("DELETE FROM post_cache WHERE path NOT IN (:keepPaths)")
  suspend fun deleteExcept(keepPaths: List<String>)

  @Query("DELETE FROM post_cache")
  suspend fun deleteAll()

  /** Replaces the cache with exactly [entries]: anything else cached is gone from the repo. */
  @Transaction
  suspend fun replaceAll(entries: List<PostCacheEntity>) {
    val keepPaths = entries.map { it.path }
    if (keepPaths.isEmpty()) {
      deleteAll()
    } else {
      // One bound SQLite parameter per path, against a SQLITE_MAX_VARIABLE_NUMBER
      // that is 999 on plenty of Android builds — a repo with a thousand posts
      // threw rather than degrading. Deleting in chunks means each statement
      // keeps only the paths in its own chunk, so the chunks have to intersect:
      // a path survives only if every chunk kept it.
      val survivors = keepPaths.toSet()
      getAllPaths()
        .filterNot { it in survivors }
        .chunked(DELETE_CHUNK)
        .forEach { deletePaths(it) }
    }
    upsertAll(entries)
  }

  @Query("DELETE FROM post_cache WHERE path IN (:paths)")
  suspend fun deletePaths(paths: List<String>)

  companion object {
    /** Comfortably under the 999-variable limit older Android SQLite builds enforce. */
    const val DELETE_CHUNK = 900
  }
}

/** Adds [PostCacheEntity.wordCount]. Existing rows are marked
 * [PostCacheEntity.UNKNOWN_WORD_COUNT] rather than defaulted to zero: the count
 * is recoverable from the markdown already cached, and the next refresh fills it
 * in without re-fetching a single file. */
private val MIGRATION_1_2 = object : Migration(1, 2) {
  override fun migrate(connection: SQLiteConnection) {
    connection.execSQL(
      "ALTER TABLE post_cache ADD COLUMN wordCount INTEGER NOT NULL DEFAULT ${PostCacheEntity.UNKNOWN_WORD_COUNT}"
    )
  }
}

/** Adds [PageCacheEntity]'s table — the Pages tab's own cache, alongside
 * `post_cache` rather than a column on it, since a page carries no draft flag
 * and no cover for that table's schema to grow to fit. */
private val MIGRATION_2_3 = object : Migration(2, 3) {
  override fun migrate(connection: SQLiteConnection) {
    connection.execSQL(
      """
      CREATE TABLE IF NOT EXISTS `page_cache` (
        `path` TEXT NOT NULL,
        `blobSha` TEXT NOT NULL,
        `slug` TEXT NOT NULL,
        `title` TEXT NOT NULL,
        `date` TEXT,
        `markdown` TEXT NOT NULL,
        `wordCount` INTEGER NOT NULL,
        PRIMARY KEY(`path`)
      )
      """.trimIndent()
    )
  }
}

/** Adds [LocalPostEntity]'s table — local-only posts/pages that have never
 * been pushed to the repo, tracked entirely separately from `post_cache`/
 * `page_cache` (see that entity's own doc comment for why: those two are
 * remote reconciliation's to reconcile, and this one is not). */
private val MIGRATION_3_4 = object : Migration(3, 4) {
  override fun migrate(connection: SQLiteConnection) {
    connection.execSQL(
      """
      CREATE TABLE IF NOT EXISTS `local_post` (
        `slug` TEXT NOT NULL,
        `kind` TEXT NOT NULL,
        `title` TEXT NOT NULL,
        `filePath` TEXT NOT NULL,
        `wordCount` INTEGER NOT NULL,
        `date` TEXT,
        `dateMillis` INTEGER,
        `cover` TEXT,
        `updatedAt` INTEGER,
        `editedAgo` TEXT,
        PRIMARY KEY(`slug`)
      )
      """.trimIndent()
    )
  }
}

/** Adds [FragmentEntity]'s table — captured-but-unpromoted Inbox fragments,
 * the durable counterpart to `local_post` for the Inbox tab (see that
 * entity's own doc comment, `data/inbox/FragmentCache.kt`, for why it's a
 * plain Room row with no mirrored file the way `local_post` has). */
private val MIGRATION_4_5 = object : Migration(4, 5) {
  override fun migrate(connection: SQLiteConnection) {
    connection.execSQL(
      """
      CREATE TABLE IF NOT EXISTS `fragment` (
        `id` TEXT NOT NULL,
        `text` TEXT NOT NULL,
        `capturedAt` TEXT NOT NULL,
        `tag` TEXT,
        `bucket` TEXT NOT NULL,
        PRIMARY KEY(`id`)
      )
      """.trimIndent()
    )
  }
}

/** Replaces the free-form `capturedAt`/`bucket` text columns with a single
 * `capturedAtMillis` instant — those two were written once at capture time and
 * never touched again, so a fragment captured yesterday still read "Today"
 * forever after. [com.rrajath.bloggo.model.Fragment.capturedAt] and
 * [com.rrajath.bloggo.model.Fragment.bucket] are now derived from the instant
 * against the current moment on every read instead. There's no reliable
 * source to recover a real historical instant for already-captured rows from
 * their old free-form strings, so they're stamped with the migration's own
 * "now" — the same fallback those rows already displayed. */
private val MIGRATION_5_6 = object : Migration(5, 6) {
  override fun migrate(connection: SQLiteConnection) {
    connection.execSQL(
      """
      CREATE TABLE IF NOT EXISTS `fragment_new` (
        `id` TEXT NOT NULL,
        `text` TEXT NOT NULL,
        `capturedAtMillis` INTEGER NOT NULL,
        `tag` TEXT,
        PRIMARY KEY(`id`)
      )
      """.trimIndent()
    )
    connection.execSQL(
      "INSERT INTO `fragment_new` (`id`, `text`, `capturedAtMillis`, `tag`) " +
        "SELECT `id`, `text`, ${System.currentTimeMillis()}, `tag` FROM `fragment`"
    )
    connection.execSQL("DROP TABLE `fragment`")
    connection.execSQL("ALTER TABLE `fragment_new` RENAME TO `fragment`")
  }
}

/** Adds the per-post store of readability findings the writer has chosen to
 * ignore on the review screen. Keyed by post slug plus an opaque
 * `category + text` string; a row is only ever removed by the screen's
 * recompute action. */
private val MIGRATION_6_7 = object : Migration(6, 7) {
  override fun migrate(connection: SQLiteConnection) {
    connection.execSQL(
      """
      CREATE TABLE IF NOT EXISTS `readability_ignore` (
        `slug` TEXT NOT NULL,
        `ignoreKey` TEXT NOT NULL,
        PRIMARY KEY(`slug`, `ignoreKey`)
      )
      """.trimIndent()
    )
  }
}

/** Drops the `cover` column from `post_cache` and `local_post` — generated
 * cover art was removed from the app entirely, so the frontmatter `cover:`
 * value is no longer read, displayed, or written anywhere. `DROP COLUMN` is
 * available on the SQLite that ships with `minSdk 34`. */
private val MIGRATION_7_8 = object : Migration(7, 8) {
  override fun migrate(connection: SQLiteConnection) {
    connection.execSQL("ALTER TABLE `post_cache` DROP COLUMN `cover`")
    connection.execSQL("ALTER TABLE `local_post` DROP COLUMN `cover`")
  }
}

@Database(
  entities = [
    PostCacheEntity::class, PageCacheEntity::class, LocalPostEntity::class,
    FragmentEntity::class, ReadabilityIgnoreEntity::class,
  ],
  version = 8,
  exportSchema = false,
)
abstract class BloggoDatabase : RoomDatabase() {
  abstract fun postCacheDao(): PostCacheDao
  abstract fun pageCacheDao(): PageCacheDao
  abstract fun localPostDao(): LocalPostDao
  abstract fun fragmentDao(): FragmentDao
  abstract fun readabilityIgnoreDao(): ReadabilityIgnoreDao

  companion object {
    @Volatile private var instance: BloggoDatabase? = null

    fun get(context: Context): BloggoDatabase = instance ?: synchronized(this) {
      instance ?: Room.databaseBuilder(context, BloggoDatabase::class.java, "bloggo.db")
        .addMigrations(
          MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
          MIGRATION_6_7, MIGRATION_7_8,
        )
        .build()
        .also { instance = it }
    }
  }
}
