package com.rrajath.bloggo.data.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class GitHubRepoDto(
  @SerialName("full_name") val fullName: String,
  @SerialName("default_branch") val defaultBranch: String,
  @SerialName("private") val private: Boolean,
  @SerialName("description") val description: String? = null,
)

@Serializable
data class GitTreeEntryDto(
  val path: String,
  val mode: String,
  val type: String,
  val sha: String,
)

@Serializable
data class GitTreeResponseDto(
  val sha: String,
  val tree: List<GitTreeEntryDto> = emptyList(),
  val truncated: Boolean = false,
)

@Serializable
data class GitHubContentEntryDto(
  val name: String,
  val path: String,
  val sha: String,
  val type: String,
)

@Serializable
data class TreeShaDto(val sha: String)

@Serializable
data class CommitTreeRefDto(val tree: TreeShaDto)

@Serializable
data class BranchCommitDto(val sha: String, val commit: CommitTreeRefDto)

/** `GET .../branches/{branch}`: the branch's head commit sha and that commit's
 * tree sha in one call, rather than a separate ref lookup plus a commit fetch. */
@Serializable
data class BranchDto(val commit: BranchCommitDto)

@Serializable
data class CreateBlobRequestDto(val content: String, val encoding: String)

@Serializable
data class CreateBlobResponseDto(val sha: String)

/** [sha] is nullable only for [GitHubClient.deleteFile]'s use of this same DTO:
 * a `null` sha on an existing path is the Git Trees API's own documented way
 * to remove that path from the tree it's layered on, rather than adding or
 * changing a blob at it. Every other caller (a real commit) always supplies a
 * real blob sha. */
@Serializable
data class TreeEntryInputDto(
  val path: String,
  val mode: String = "100644",
  val type: String = "blob",
  val sha: String? = null,
)

@Serializable
data class CreateTreeRequestDto(
  @SerialName("base_tree") val baseTree: String,
  val tree: List<TreeEntryInputDto>,
)

@Serializable
data class CreateCommitRequestDto(
  val message: String,
  val tree: String,
  val parents: List<String>,
)

@Serializable
data class CreateCommitResponseDto(val sha: String)

@Serializable
data class UpdateRefRequestDto(val sha: String, val force: Boolean = false)

interface GitHubApi {
  /** ANDROID_TDD.md §7.1: validates the token and reports what it found. */
  @GET("repos/{owner}/{repo}")
  suspend fun getRepo(
    @Header("Authorization") authorization: String?,
    @Path("owner") owner: String,
    @Path("repo") repo: String,
  ): Response<GitHubRepoDto>

  /** ANDROID_TDD.md §5.2: one recursive tree call lists every path and blob SHA.
   * [ifNoneMatch] carries the previous response's ETag, so an unchanged repo
   * answers 304 without re-sending the whole tree or spending a rate-limit unit. */
  @GET("repos/{owner}/{repo}/git/trees/{branch}")
  suspend fun getTree(
    @Header("Authorization") authorization: String?,
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path("branch") branch: String,
    @Query("recursive") recursive: Int = 1,
    @Header("If-None-Match") ifNoneMatch: String? = null,
  ): Response<GitTreeResponseDto>

  /** Fallback for a `truncated` tree (§5.2): a plain, non-recursive directory listing. */
  @GET("repos/{owner}/{repo}/contents/{path}")
  suspend fun listDirectory(
    @Header("Authorization") authorization: String?,
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path("path") path: String,
    @Query("ref") ref: String?,
  ): Response<List<GitHubContentEntryDto>>

  /** A post's raw markdown, frontmatter and all. */
  @Headers("Accept: application/vnd.github.raw")
  @GET("repos/{owner}/{repo}/contents/{path}")
  suspend fun getRawFile(
    @Header("Authorization") authorization: String?,
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path("path") path: String,
    @Query("ref") ref: String?,
  ): Response<ResponseBody>

  /** The branch's current head commit and that commit's tree, in one call —
   * what a commit needs as its parent and base tree. */
  @GET("repos/{owner}/{repo}/branches/{branch}")
  suspend fun getBranch(
    @Header("Authorization") authorization: String?,
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path("branch") branch: String,
  ): Response<BranchDto>

  /** Git Data API, step 1 of a commit: one blob per file, text or base64. */
  @POST("repos/{owner}/{repo}/git/blobs")
  suspend fun createBlob(
    @Header("Authorization") authorization: String?,
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Body body: CreateBlobRequestDto,
  ): Response<CreateBlobResponseDto>

  /** Git Data API, step 2: a new tree, layered on the branch's base tree so
   * every file the commit doesn't touch is carried forward unchanged. */
  @POST("repos/{owner}/{repo}/git/trees")
  suspend fun createTree(
    @Header("Authorization") authorization: String?,
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Body body: CreateTreeRequestDto,
  ): Response<GitTreeResponseDto>

  /** Git Data API, step 3: a commit object pointing at the new tree, parented
   * on the branch's current head. */
  @POST("repos/{owner}/{repo}/git/commits")
  suspend fun createCommit(
    @Header("Authorization") authorization: String?,
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Body body: CreateCommitRequestDto,
  ): Response<CreateCommitResponseDto>

  /** Git Data API, step 4: moves the branch to the new commit. `force = false`
   * always — a rejected non-fast-forward means the branch moved since [getBranch]
   * was read, which must surface as a real conflict, never a silent overwrite. */
  @PATCH("repos/{owner}/{repo}/git/refs/heads/{branch}")
  suspend fun updateRef(
    @Header("Authorization") authorization: String?,
    @Path("owner") owner: String,
    @Path("repo") repo: String,
    @Path("branch") branch: String,
    @Body body: UpdateRefRequestDto,
  ): Response<ResponseBody>
}
