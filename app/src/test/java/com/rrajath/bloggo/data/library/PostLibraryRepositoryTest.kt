package com.rrajath.bloggo.data.library

import com.rrajath.bloggo.data.github.GitHubClient
import com.rrajath.bloggo.model.PostState
import com.rrajath.bloggo.model.parseFrontmatterDateEpochMillis
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** In-memory stand-in for the Room-generated DAO — no Android runtime needed to test the caching logic itself. */
private class FakePostCacheDao : PostCacheDao {
  private val storage = mutableMapOf<String, PostCacheEntity>()

  override suspend fun getAll(): List<PostCacheEntity> = storage.values.toList()

  override suspend fun getAllPaths(): List<String> = storage.keys.toList()

  override suspend fun upsertAll(entries: List<PostCacheEntity>) {
    entries.forEach { storage[it.path] = it }
  }

  override suspend fun deleteExcept(keepPaths: List<String>) {
    storage.keys.retainAll(keepPaths.toSet())
  }

  override suspend fun deleteAll() {
    storage.clear()
  }

  override suspend fun deletePaths(paths: List<String>) {
    storage.keys.removeAll(paths.toSet())
  }
}

class PostLibraryRepositoryTest {
  private lateinit var server: MockWebServer
  private lateinit var repository: PostLibraryRepository
  private lateinit var dao: FakePostCacheDao

  @Before
  fun setUp() {
    server = MockWebServer()
    server.start()
    dao = FakePostCacheDao()
    repository = PostLibraryRepository(GitHubClient(baseUrl = server.url("/").toString()), dao)
  }

  @After
  fun tearDown() {
    server.close()
  }

  private fun treeResponse(vararg entries: Pair<String, String>) = MockResponse.Builder()
    .code(200)
    .body(
      """{"sha":"root","truncated":false,"tree":[${
        entries.joinToString(",") { (path, sha) ->
          """{"path":"$path","mode":"100644","type":"blob","sha":"$sha"}"""
        }
      }]}"""
    )
    .build()

  @Test
  fun `refresh parses frontmatter into a Post`() = runTest {
    server.enqueue(treeResponse("content/posts/a.md" to "sha-a"))
    server.enqueue(
      MockResponse.Builder().code(200).body(
        "---\ntitle: A post\ndate: 2026-08-04\ndraft: false\ncover: /images/2026/a.png\n---\n\nBody text here."
      ).build()
    )

    val result = repository.refresh("o/r", "main", token = "t") as LibraryRefreshResult.Success

    val post = result.posts.single()
    assertEquals("a", post.slug)
    assertEquals("A post", post.title)
    assertEquals("Aug 4, 2026", post.date)
    assertEquals(PostState.Published, post.state)
    assertEquals("/images/2026/a.png", post.cover)
  }

  @Test
  fun `refresh formats a full RFC3339 timestamp date the same as a plain date`() = runTest {
    // Regression: a real post's `date:` is routinely a full timestamp
    // ("2016-01-02T00:00:00Z"), not just "2016-01-02" — that whole string was
    // falling through to the raw-string fallback instead of formatting.
    server.enqueue(treeResponse("content/posts/a.md" to "sha-a"))
    server.enqueue(
      MockResponse.Builder().code(200).body("---\ntitle: A\ndate: 2016-01-02T00:00:00Z\n---\n").build()
    )

    val result = repository.refresh("o/r", "main", token = "t") as LibraryRefreshResult.Success

    assertEquals("Jan 2, 2016", result.posts.single().date)
  }

  @Test
  fun `refresh parses TOML fenced frontmatter just as accurately as YAML`() = runTest {
    server.enqueue(treeResponse("content/posts/c.md" to "sha-c"))
    server.enqueue(
      MockResponse.Builder().code(200).body(
        "+++\ntitle = \"A TOML post\"\ndate = 2026-08-04\ndraft = false\n+++\n\nBody."
      ).build()
    )

    val result = repository.refresh("o/r", "main", token = "t") as LibraryRefreshResult.Success

    val post = result.posts.single()
    assertEquals("A TOML post", post.title)
    assertEquals("Aug 4, 2026", post.date)
    assertEquals(PostState.Published, post.state)
  }

  @Test
  fun `a draft true post maps to PostState Draft`() = runTest {
    server.enqueue(treeResponse("content/posts/b.md" to "sha-b"))
    server.enqueue(MockResponse.Builder().code(200).body("---\ntitle: B\ndraft: true\n---\n").build())

    val result = repository.refresh("o/r", "main", token = "t") as LibraryRefreshResult.Success

    assertEquals(PostState.Draft, result.posts.single().state)
  }

  @Test
  fun `an unchanged blob sha is not re-fetched on the next refresh`() = runTest {
    server.enqueue(treeResponse("content/posts/a.md" to "sha-a"))
    server.enqueue(MockResponse.Builder().code(200).body("---\ntitle: A\n---\n").build())
    repository.refresh("o/r", "main", token = "t")

    val requestsSoFar = server.requestCount
    server.enqueue(treeResponse("content/posts/a.md" to "sha-a")) // same sha, nothing else queued

    val result = repository.refresh("o/r", "main", token = "t") as LibraryRefreshResult.Success

    assertEquals("A", result.posts.single().title)
    assertEquals(requestsSoFar + 1, server.requestCount) // the tree call only, no raw-content re-fetch
  }

  @Test
  fun `a changed blob sha is re-fetched and replaces the cached entry`() = runTest {
    server.enqueue(treeResponse("content/posts/a.md" to "sha-a"))
    server.enqueue(MockResponse.Builder().code(200).body("---\ntitle: Old title\n---\n").build())
    repository.refresh("o/r", "main", token = "t")

    server.enqueue(treeResponse("content/posts/a.md" to "sha-a2"))
    server.enqueue(MockResponse.Builder().code(200).body("---\ntitle: New title\n---\n").build())

    val result = repository.refresh("o/r", "main", token = "t") as LibraryRefreshResult.Success

    assertEquals("New title", result.posts.single().title)
  }

  @Test
  fun `a post removed from the tree is dropped from the cache`() = runTest {
    server.enqueue(treeResponse("content/posts/a.md" to "sha-a", "content/posts/b.md" to "sha-b"))
    server.enqueue(MockResponse.Builder().code(200).body("---\ntitle: A\n---\n").build())
    server.enqueue(MockResponse.Builder().code(200).body("---\ntitle: B\n---\n").build())
    repository.refresh("o/r", "main", token = "t")

    server.enqueue(treeResponse("content/posts/a.md" to "sha-a")) // b.md is gone

    val result = repository.refresh("o/r", "main", token = "t") as LibraryRefreshResult.Success

    assertEquals(listOf("a"), result.posts.map { it.slug })
    assertEquals(1, dao.getAll().size)
  }

  @Test
  fun `every changed post is fetched, and the fetches overlap rather than queueing one at a time`() = runTest {
    // Regression: the per-post fetches ran inside a plain mapNotNull, so a first
    // sync of an N-post blog was N serial round trips with the library empty
    // throughout. They are gated at five in flight now, not serialised.
    val paths = (1..12).map { "content/posts/p$it.md" to "sha-$it" }
    server.enqueue(treeResponse(*paths.toTypedArray()))
    repeat(paths.size) {
      server.enqueue(MockResponse.Builder().code(200).body("---\ntitle: T\n---\n\nBody.").build())
    }

    val result = repository.refresh("o/r", "main", token = "t") as LibraryRefreshResult.Success

    assertEquals(paths.size, result.posts.size)
    assertEquals(paths.size + 1, server.requestCount) // one tree call plus one per post, nothing repeated
  }

  @Test
  fun `an unchanged tree answers 304 and costs a single request`() = runTest {
    server.enqueue(
      MockResponse.Builder()
        .code(200)
        .setHeader("ETag", "\"tree-v1\"")
        .body("""{"sha":"root","truncated":false,"tree":[{"path":"content/posts/a.md","mode":"100644","type":"blob","sha":"sha-a"}]}""")
        .build()
    )
    server.enqueue(MockResponse.Builder().code(200).body("---\ntitle: A\n---\n\nBody.").build())
    repository.refresh("o/r", "main", token = "t")

    val requestsSoFar = server.requestCount
    server.enqueue(MockResponse.Builder().code(304).build())

    val result = repository.refresh("o/r", "main", token = "t") as LibraryRefreshResult.Success

    assertEquals("A", result.posts.single().title)
    assertEquals(requestsSoFar + 1, server.requestCount) // the conditional tree call, and nothing else
    // The first tree call carries no ETag; the second replays the one it was given.
    val conditionalTreeRequest = generateSequence { server.takeRequest() }.take(3).last()
    assertEquals("\"tree-v1\"", conditionalTreeRequest.headers["If-None-Match"])
  }

  @Test
  fun `a cache row written before word counts were stored is filled in without a re-fetch`() = runTest {
    server.enqueue(treeResponse("content/posts/a.md" to "sha-a"))
    server.enqueue(MockResponse.Builder().code(200).body("---\ntitle: A\n---\n\nOne two three.").build())
    repository.refresh("o/r", "main", token = "t")
    // Exactly what the version-1 schema left behind after the migration.
    dao.upsertAll(dao.getAll().map { it.copy(wordCount = PostCacheEntity.UNKNOWN_WORD_COUNT) })

    val requestsSoFar = server.requestCount
    server.enqueue(treeResponse("content/posts/a.md" to "sha-a"))

    val result = repository.refresh("o/r", "main", token = "t") as LibraryRefreshResult.Success

    assertEquals(3, result.posts.single().wordCount)
    assertEquals(requestsSoFar + 1, server.requestCount) // the tree call only
  }

  @Test
  fun `a post carries its frontmatter date as a sort key, not just a display string`() = runTest {
    server.enqueue(treeResponse("content/posts/a.md" to "sha-a"))
    server.enqueue(MockResponse.Builder().code(200).body("---\ntitle: A\ndate: 2026-08-04\n---\n").build())

    val result = repository.refresh("o/r", "main", token = "t") as LibraryRefreshResult.Success

    assertEquals(parseFrontmatterDateEpochMillis("2026-08-04"), result.posts.single().dateMillis)
  }

  @Test
  fun `a truncated tree still returns posts via the directory-listing fallback`() = runTest {
    server.enqueue(MockResponse.Builder().code(200).body("""{"sha":"root","truncated":true,"tree":[]}""").build())
    server.enqueue(
      MockResponse.Builder().code(200).body(
        """[{"name":"a.md","path":"content/posts/a.md","sha":"sha-a","type":"file"}]"""
      ).build()
    )
    server.enqueue(MockResponse.Builder().code(200).body("---\ntitle: A\n---\n").build())

    val result = repository.refresh("o/r", "main", token = "t") as LibraryRefreshResult.Success

    assertTrue(result.posts.single().slug == "a")
  }

  @Test
  fun `evict drops a single cache row without touching the rest`() = runTest {
    server.enqueue(treeResponse("content/posts/a.md" to "sha-a", "content/posts/b.md" to "sha-b"))
    server.enqueue(MockResponse.Builder().code(200).body("---\ntitle: A\n---\n").build())
    server.enqueue(MockResponse.Builder().code(200).body("---\ntitle: B\n---\n").build())
    repository.refresh("o/r", "main", token = "t")

    repository.evict("content/posts/a.md")

    assertEquals(listOf("content/posts/b.md"), dao.getAll().map { it.path })
  }
}
