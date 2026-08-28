package com.rrajath.bloggo.data.github

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GitHubClientTest {
  private lateinit var server: MockWebServer
  private lateinit var client: GitHubClient

  @Before
  fun setUp() {
    server = MockWebServer()
    server.start()
    client = GitHubClient(baseUrl = server.url("/").toString())
  }

  @After
  fun tearDown() {
    server.close()
  }

  private fun repoResponse(defaultBranch: String = "main", private: Boolean = false) = MockResponse.Builder()
    .code(200)
    .body("""{"full_name":"o/r","default_branch":"$defaultBranch","private":$private}""")
    .build()

  @Test
  fun `connected reports the default branch and visibility in one call`() = runTest {
    server.enqueue(repoResponse(defaultBranch = "main", private = true))

    val result = client.checkConnection("o/r", "main", token = "t")

    val connected = result as ConnectionCheck.Connected
    assertEquals("main", connected.defaultBranch)
    assertTrue(connected.isPrivate)
    assertEquals(1, server.requestCount) // getRepo only — no root listing
  }

  @Test
  fun `401 maps to Unauthorized`() = runTest {
    server.enqueue(MockResponse.Builder().code(401).build())

    val result = client.checkConnection("o/r", "main", token = "bad")

    assertEquals(ConnectionCheck.Unauthorized, result)
  }

  @Test
  fun `403 without rate limit header maps to Unauthorized`() = runTest {
    server.enqueue(MockResponse.Builder().code(403).build())

    val result = client.checkConnection("o/r", "main", token = "bad")

    assertEquals(ConnectionCheck.Unauthorized, result)
  }

  @Test
  fun `403 with exhausted rate limit maps to RateLimited`() = runTest {
    server.enqueue(
      MockResponse.Builder()
        .code(403)
        .addHeader("x-ratelimit-remaining", "0")
        .build()
    )

    val result = client.checkConnection("o/r", "main", token = "t")

    assertEquals(ConnectionCheck.RateLimited, result)
  }

  @Test
  fun `404 maps to NotFound`() = runTest {
    server.enqueue(MockResponse.Builder().code(404).build())

    val result = client.checkConnection("o/r", "main", token = null)

    assertEquals(ConnectionCheck.NotFound, result)
  }

  @Test
  fun `5xx maps to Unknown with the http code`() = runTest {
    server.enqueue(MockResponse.Builder().code(500).build())

    val result = client.checkConnection("o/r", "main", token = "t")

    assertEquals(ConnectionCheck.Unknown(500), result)
  }

  @Test
  fun `a repository without an owner slash name maps to NotFound without a call`() = runTest {
    val result = client.checkConnection("not-a-valid-repo-string", "main", token = "t")

    assertEquals(ConnectionCheck.NotFound, result)
    assertEquals(0, server.requestCount)
  }

  @Test
  fun `a token with a trailing newline is trimmed before it reaches the request`() = runTest {
    // Regression test: an untrimmed token crashed OkHttp with
    // IllegalArgumentException ("Unexpected char 0x0a ... in Authorization
    // value") the first time a real pasted-from-clipboard token was used.
    server.enqueue(repoResponse())

    client.checkConnection("o/r", "main", token = "t\n")

    val recorded = server.takeRequest()
    assertEquals("Bearer t", recorded.headers["Authorization"])
  }

  @Test
  fun `no token omits the Authorization header`() = runTest {
    server.enqueue(repoResponse())

    client.checkConnection("o/r", "main", token = null)

    val recorded = server.takeRequest()
    assertEquals(null, recorded.headers["Authorization"])
  }

  // --- getPostsTree: ANDROID_TDD.md §5.2 ---

  @Test
  fun `getPostsTree keeps only markdown blobs under content posts`() = runTest {
    server.enqueue(
      MockResponse.Builder().code(200).body(
        """
        {"sha":"root","truncated":false,"tree":[
          {"path":"content/posts/a.md","mode":"100644","type":"blob","sha":"sha-a"},
          {"path":"content/posts/drafts","mode":"040000","type":"tree","sha":"sha-dir"},
          {"path":"content/posts/img.png","mode":"100644","type":"blob","sha":"sha-img"},
          {"path":"hugo.toml","mode":"100644","type":"blob","sha":"sha-config"}
        ]}
        """.trimIndent()
      ).build()
    )

    val result = client.getPostsTree("o/r", "main", token = "t") as PostsTreeResult.Success

    assertEquals(listOf(PostTreeEntry("content/posts/a.md", "sha-a")), result.entries)
  }

  @Test
  fun `getPostsTree falls back to a directory listing when truncated`() = runTest {
    server.enqueue(
      MockResponse.Builder().code(200).body("""{"sha":"root","truncated":true,"tree":[]}""").build()
    )
    server.enqueue(
      MockResponse.Builder().code(200).body(
        """[{"name":"a.md","path":"content/posts/a.md","sha":"sha-a","type":"file"}]"""
      ).build()
    )

    val result = client.getPostsTree("o/r", "main", token = "t") as PostsTreeResult.Success

    assertEquals(listOf(PostTreeEntry("content/posts/a.md", "sha-a")), result.entries)
  }

  @Test
  fun `getPostsTree maps a 401 the same way checkConnection does`() = runTest {
    server.enqueue(MockResponse.Builder().code(401).build())

    val result = client.getPostsTree("o/r", "main", token = "bad") as PostsTreeResult.Failed

    assertEquals(GitHubApiError.Unauthorized, result.error)
  }

  @Test
  fun `getPostsTree with no network maps to NoNetwork`() = runTest {
    server.close() // any request now fails as an IOException, not an HTTP response

    val result = client.getPostsTree("o/r", "main", token = "t") as PostsTreeResult.Failed

    assertEquals(GitHubApiError.NoNetwork, result.error)
  }

  // --- getImagesTree: the Media screen's counterpart to getPostsTree ---

  @Test
  fun `getImagesTree keeps only image blobs under the configured image path`() = runTest {
    server.enqueue(
      MockResponse.Builder().code(200).body(
        """
        {"sha":"root","truncated":false,"tree":[
          {"path":"static/images/2026/a.png","mode":"100644","type":"blob","sha":"sha-a"},
          {"path":"static/images/2026","mode":"040000","type":"tree","sha":"sha-dir"},
          {"path":"static/images/2026/notes.txt","mode":"100644","type":"blob","sha":"sha-notes"},
          {"path":"content/posts/a.md","mode":"100644","type":"blob","sha":"sha-post"}
        ]}
        """.trimIndent()
      ).build()
    )

    val result = client.getImagesTree("o/r", "main", "static/images/", token = "t") as ImagesTreeResult.Success

    assertEquals(listOf(ImageTreeEntry("static/images/2026/a.png", "sha-a")), result.entries)
  }

  @Test
  fun `getImagesTree falls back to a directory listing when truncated`() = runTest {
    server.enqueue(
      MockResponse.Builder().code(200).body("""{"sha":"root","truncated":true,"tree":[]}""").build()
    )
    server.enqueue(
      MockResponse.Builder().code(200).body(
        """[{"name":"a.png","path":"static/images/a.png","sha":"sha-a","type":"file"}]"""
      ).build()
    )

    val result = client.getImagesTree("o/r", "main", "static/images/", token = "t") as ImagesTreeResult.Success

    assertEquals(listOf(ImageTreeEntry("static/images/a.png", "sha-a")), result.entries)
  }

  @Test
  fun `getImagesTree maps a 401 the same way getPostsTree does`() = runTest {
    server.enqueue(MockResponse.Builder().code(401).build())

    val result = client.getImagesTree("o/r", "main", "static/images/", token = "bad") as ImagesTreeResult.Failed

    assertEquals(GitHubApiError.Unauthorized, result.error)
  }

  // --- getFileContent ---

  @Test
  fun `getFileContent returns the raw body and requests the raw media type`() = runTest {
    server.enqueue(MockResponse.Builder().code(200).body("---\ntitle: A post\n---\nBody.").build())

    val content = client.getFileContent("o/r", "content/posts/a.md", "main", token = "t")

    assertEquals("---\ntitle: A post\n---\nBody.", content)
    assertEquals("application/vnd.github.raw", server.takeRequest().headers["Accept"])
  }

  @Test
  fun `getFileContent returns null on a 404`() = runTest {
    server.enqueue(MockResponse.Builder().code(404).build())

    val content = client.getFileContent("o/r", "content/posts/missing.md", "main", token = "t")

    assertEquals(null, content)
  }

  // --- commitFiles: the Git Data API write path ---

  private fun branchResponse(headSha: String = "head-sha", baseTreeSha: String = "base-tree-sha") =
    MockResponse.Builder()
      .code(200)
      .body("""{"name":"b","commit":{"sha":"$headSha","commit":{"tree":{"sha":"$baseTreeSha"}}}}""")
      .build()

  private fun blobResponse(sha: String) = MockResponse.Builder().code(201).body("""{"sha":"$sha"}""").build()
  private fun treeResponse(sha: String) = MockResponse.Builder().code(201).body("""{"sha":"$sha"}""").build()
  private fun commitResponse(sha: String) = MockResponse.Builder().code(201).body("""{"sha":"$sha"}""").build()
  private fun refUpdateResponse() = MockResponse.Builder().code(200).body("{}").build()

  @Test
  fun `commitFiles walks the full blob-tree-commit-ref sequence and returns the new commit sha`() = runTest {
    server.enqueue(branchResponse())
    server.enqueue(blobResponse("blob-sha"))
    server.enqueue(treeResponse("new-tree-sha"))
    server.enqueue(commitResponse("new-commit-sha"))
    server.enqueue(refUpdateResponse())

    val result = client.commitFiles(
      "o/r", "main", token = "t", message = "Draft: a post",
      files = listOf(CommitFile("content/posts/a.md", FileContent.Text("---\ntitle: A post\n---\n"))),
    )

    assertEquals(CommitResult.Success("new-commit-sha"), result)
    assertEquals(5, server.requestCount)
  }

  @Test
  fun `commitFiles uses whatever branch is configured, never a hardcoded main`() = runTest {
    server.enqueue(branchResponse())
    server.enqueue(blobResponse("blob-sha"))
    server.enqueue(treeResponse("new-tree-sha"))
    server.enqueue(commitResponse("new-commit-sha"))
    server.enqueue(refUpdateResponse())

    client.commitFiles(
      "o/r", "content-v2", token = "t", message = "msg",
      files = listOf(CommitFile("content/posts/a.md", FileContent.Text("body"))),
    )

    val branchRequest = server.takeRequest()
    assertEquals("/repos/o/r/branches/content-v2", branchRequest.target)
    repeat(3) { server.takeRequest() } // blob, tree, commit
    val refRequest = server.takeRequest()
    assertEquals("/repos/o/r/git/refs/heads/content-v2", refRequest.target)
  }

  @Test
  fun `commitFiles creates one blob per file with the right encoding for text vs images`() = runTest {
    server.enqueue(branchResponse())
    server.enqueue(blobResponse("post-blob-sha"))
    server.enqueue(blobResponse("image-blob-sha"))
    server.enqueue(treeResponse("new-tree-sha"))
    server.enqueue(commitResponse("new-commit-sha"))
    server.enqueue(refUpdateResponse())

    val imageBytes = byteArrayOf(1, 2, 3, 4)
    val result = client.commitFiles(
      "o/r", "main", token = "t", message = "msg",
      files = listOf(
        CommitFile("content/posts/a.md", FileContent.Text("body text")),
        CommitFile("static/images/2026/a-1.png", FileContent.Base64(imageBytes)),
      ),
    )

    assertEquals(CommitResult.Success("new-commit-sha"), result)
    server.takeRequest() // getBranch
    val blobBodies = listOf(server.takeRequest().body!!.utf8(), server.takeRequest().body!!.utf8())
    assertTrue(blobBodies.any { it.contains("\"encoding\":\"utf-8\"") && it.contains("body text") })
    assertTrue(
      blobBodies.any {
        it.contains("\"encoding\":\"base64\"") && it.contains(java.util.Base64.getEncoder().encodeToString(imageBytes))
      }
    )
  }

  @Test
  fun `commitFiles maps a rejected ref update to Conflict, not force-pushed`() = runTest {
    server.enqueue(branchResponse())
    server.enqueue(blobResponse("blob-sha"))
    server.enqueue(treeResponse("new-tree-sha"))
    server.enqueue(commitResponse("new-commit-sha"))
    server.enqueue(MockResponse.Builder().code(422).build()) // branch moved since getBranch was read

    val result = client.commitFiles(
      "o/r", "main", token = "t", message = "msg",
      files = listOf(CommitFile("content/posts/a.md", FileContent.Text("body"))),
    ) as CommitResult.Failed

    assertEquals(GitHubApiError.Conflict, result.error)
    repeat(4) { server.takeRequest() } // branch, blob, tree, commit
    val refRequest = server.takeRequest()
    assertEquals(false, refRequest.body!!.utf8().contains("\"force\":true"))
  }

  @Test
  fun `commitFiles stops after a failed blob create and never calls createTree`() = runTest {
    server.enqueue(branchResponse())
    server.enqueue(MockResponse.Builder().code(401).build())

    val result = client.commitFiles(
      "o/r", "main", token = "bad", message = "msg",
      files = listOf(CommitFile("content/posts/a.md", FileContent.Text("body"))),
    ) as CommitResult.Failed

    assertEquals(GitHubApiError.Unauthorized, result.error)
    assertEquals(2, server.requestCount) // getBranch + the one failed blob create, nothing after
  }

  @Test
  fun `commitFiles with no network maps to NoNetwork`() = runTest {
    server.close()

    val result = client.commitFiles(
      "o/r", "main", token = "t", message = "msg",
      files = listOf(CommitFile("content/posts/a.md", FileContent.Text("body"))),
    ) as CommitResult.Failed

    assertEquals(GitHubApiError.NoNetwork, result.error)
  }

  // --- deleteFile: the Git Data API delete path (no blob, a null-sha tree entry) ---

  @Test
  fun `deleteFile walks branch-tree-commit-ref without creating a blob`() = runTest {
    server.enqueue(branchResponse())
    server.enqueue(treeResponse("new-tree-sha"))
    server.enqueue(commitResponse("new-commit-sha"))
    server.enqueue(refUpdateResponse())

    val result = client.deleteFile(
      "o/r", "main", token = "t", message = "Delete post: a post",
      path = "content/posts/a.md",
    )

    assertEquals(CommitResult.Success("new-commit-sha"), result)
    assertEquals(4, server.requestCount) // branch, tree, commit, ref — no blob create
  }

  @Test
  fun `deleteFile sends a null sha tree entry for the deleted path`() = runTest {
    server.enqueue(branchResponse())
    server.enqueue(treeResponse("new-tree-sha"))
    server.enqueue(commitResponse("new-commit-sha"))
    server.enqueue(refUpdateResponse())

    client.deleteFile(
      "o/r", "main", token = "t", message = "msg",
      path = "content/posts/a.md",
    )

    server.takeRequest() // getBranch
    val treeRequest = server.takeRequest().body!!.utf8()
    assertTrue(treeRequest.contains("\"path\":\"content/posts/a.md\""))
    assertTrue(treeRequest.contains("\"sha\":null"))
  }

  @Test
  fun `deleteFile maps a rejected ref update to Conflict, not force-pushed`() = runTest {
    server.enqueue(branchResponse())
    server.enqueue(treeResponse("new-tree-sha"))
    server.enqueue(commitResponse("new-commit-sha"))
    server.enqueue(MockResponse.Builder().code(422).build())

    val result = client.deleteFile(
      "o/r", "main", token = "t", message = "msg",
      path = "content/posts/a.md",
    ) as CommitResult.Failed

    assertEquals(GitHubApiError.Conflict, result.error)
  }

  @Test
  fun `deleteFile propagates a 404 from a missing repo or branch`() = runTest {
    server.enqueue(MockResponse.Builder().code(404).build())

    val result = client.deleteFile(
      "o/r", "main", token = "t", message = "msg",
      path = "content/posts/a.md",
    ) as CommitResult.Failed

    assertEquals(GitHubApiError.NotFound, result.error)
  }

  @Test
  fun `deleteFile with no network maps to NoNetwork`() = runTest {
    server.close()

    val result = client.deleteFile(
      "o/r", "main", token = "t", message = "msg",
      path = "content/posts/a.md",
    ) as CommitResult.Failed

    assertEquals(GitHubApiError.NoNetwork, result.error)
  }
}
