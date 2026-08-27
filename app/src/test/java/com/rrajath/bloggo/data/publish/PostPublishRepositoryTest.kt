package com.rrajath.bloggo.data.publish

import com.rrajath.bloggo.data.RepoConnection
import com.rrajath.bloggo.data.github.GitHubApiError
import com.rrajath.bloggo.data.github.GitHubClient
import com.rrajath.bloggo.model.Post
import com.rrajath.bloggo.model.PostState
import com.rrajath.bloggo.model.StagedMedia
import java.io.File
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PostPublishRepositoryTest {
  private lateinit var server: MockWebServer
  private lateinit var repository: PostPublishRepository

  @Before
  fun setUp() {
    server = MockWebServer()
    server.start()
    repository = PostPublishRepository(GitHubClient(baseUrl = server.url("/").toString()))
  }

  @After
  fun tearDown() {
    server.close()
  }

  private fun connection(branch: String = "main", postPath: String = "content/posts/{slug}.md") = RepoConnection(
    repository = "o/r",
    branch = branch,
    postPath = postPath,
  )

  private fun post(slug: String = "on-agents", repoPath: String? = null) = Post(
    slug = slug,
    title = "On agents",
    state = PostState.Draft,
    markdown = "---\ntitle: On agents\n---\nBody.",
    repoPath = repoPath,
  )

  private fun enqueueSuccessfulCommit(blobCount: Int) {
    server.enqueue(
      MockResponse.Builder().code(200)
        .body("""{"name":"b","commit":{"sha":"head-sha","commit":{"tree":{"sha":"base-tree-sha"}}}}""")
        .build()
    )
    repeat(blobCount) {
      server.enqueue(MockResponse.Builder().code(201).body("""{"sha":"blob-sha-$it"}""").build())
    }
    server.enqueue(MockResponse.Builder().code(201).body("""{"sha":"new-tree-sha"}""").build())
    server.enqueue(MockResponse.Builder().code(201).body("""{"sha":"new-commit-sha"}""").build())
    server.enqueue(MockResponse.Builder().code(200).body("{}").build())
  }

  @Test
  fun `publish resolves the postPath template for a post that has never been committed`() = runTest {
    enqueueSuccessfulCommit(blobCount = 1)

    val result = repository.publish(
      post = post(slug = "on-agents", repoPath = null),
      connection = connection(),
      token = "t",
      message = "Draft: On agents",
      stagedMediaForThisPost = emptyList(),
    ) as PublishResult.Success

    assertEquals("content/posts/on-agents.md", result.repoPath)
  }

  @Test
  fun `publish names the committed file after the frontmatter slug, not the timestamp-suffixed internal slug`() = runTest {
    // Regression: a post promoted from a fragment (BloggoApp.kt's
    // newPostFromFragment) gets a Post.slug like "on-agents-1693521600000" —
    // unique-by-construction for the local drafts table — while its
    // frontmatter `slug:` stays the clean "on-agents". The committed
    // filename must follow the frontmatter slug, matching the permalink
    // Post.liveUrl() already builds, not the internal identifier.
    enqueueSuccessfulCommit(blobCount = 1)

    val result = repository.publish(
      post = post(slug = "on-agents-1693521600000", repoPath = null).copy(
        markdown = "---\ntitle: On agents\nslug: on-agents\n---\nBody.",
      ),
      connection = connection(),
      token = "t",
      message = "New post: On agents",
      stagedMediaForThisPost = emptyList(),
    ) as PublishResult.Success

    assertEquals("content/posts/on-agents.md", result.repoPath)
  }

  @Test
  fun `publish reuses an already-known repoPath even if the slug changed since`() = runTest {
    enqueueSuccessfulCommit(blobCount = 1)

    val result = repository.publish(
      // The frontmatter slug moved on after the first commit; the file it
      // actually lives at must not move with it.
      post = post(slug = "on-agents-v2", repoPath = "content/posts/on-agents.md"),
      connection = connection(),
      token = "t",
      message = "Update: On agents",
      stagedMediaForThisPost = emptyList(),
    ) as PublishResult.Success

    assertEquals("content/posts/on-agents.md", result.repoPath)
  }

  @Test
  fun `publish sends one blob per staged image, in addition to the post itself`() = runTest {
    val temp1 = File.createTempFile("staged1", ".jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
    val temp2 = File.createTempFile("staged2", ".jpg").apply { writeBytes(byteArrayOf(4, 5, 6)) }
    enqueueSuccessfulCommit(blobCount = 3) // post + two images

    val result = repository.publish(
      post = post(),
      connection = connection(),
      token = "t",
      message = "msg",
      stagedMediaForThisPost = listOf(
        StagedMedia(temp1.absolutePath, "static/images/2026/on-agents-1.jpg", "/images/2026/on-agents-1.jpg", "photo1.jpg", stagedAt = 1L),
        StagedMedia(temp2.absolutePath, "static/images/2026/on-agents-2.jpg", "/images/2026/on-agents-2.jpg", "photo2.jpg", stagedAt = 2L),
      ),
    )

    assertEquals(true, result is PublishResult.Success)
    assertEquals(7, server.requestCount) // branch + 3 blobs + tree + commit + ref
    temp1.delete()
    temp2.delete()
  }

  @Test
  fun `publish passes the configured branch through, never a hardcoded main`() = runTest {
    enqueueSuccessfulCommit(blobCount = 1)

    repository.publish(
      post = post(),
      connection = connection(branch = "content-v2"),
      token = "t",
      message = "msg",
      stagedMediaForThisPost = emptyList(),
    )

    assertEquals("/repos/o/r/branches/content-v2", server.takeRequest().target)
  }

  @Test
  fun `publish surfaces the underlying commit failure`() = runTest {
    server.enqueue(MockResponse.Builder().code(401).build())

    val result = repository.publish(
      post = post(),
      connection = connection(),
      token = "bad",
      message = "msg",
      stagedMediaForThisPost = emptyList(),
    ) as PublishResult.Failed

    assertEquals(GitHubApiError.Unauthorized, result.error)
  }
}
