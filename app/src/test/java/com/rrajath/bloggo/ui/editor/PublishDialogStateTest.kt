package com.rrajath.bloggo.ui.editor

import com.google.common.truth.Truth.assertThat
import com.rrajath.bloggo.domain.PostDraft
import com.rrajath.bloggo.domain.SyncState
import org.junit.Test

class PublishDialogStateTest {

    private val newPost = PostDraft(
        localId = "1",
        title = "New Post",
        slug = "new-post",
        draft = true,
        syncState = SyncState.LOCAL_ONLY,
        rawFrontMatter = "date: 2026-06-21",
        body = "Body text.",
    )

    private val syncedPost = PostDraft(
        localId = "2",
        title = "Synced Post",
        slug = "synced-post",
        draft = false,
        syncState = SyncState.SYNCED,
        rawFrontMatter = "date: 2026-06-15",
        body = "Published body.",
    )

    @Test
    fun buildPushConfirmData_newPost_titleIsConfirmNewPost() {
        val data = buildPushConfirmData(newPost, "content/posts")
        assertThat(data.title).isEqualTo("Confirm new post")
        assertThat(data.isNew).isTrue()
    }

    @Test
    fun buildPushConfirmData_syncedPost_titleIsConfirmPush() {
        val data = buildPushConfirmData(syncedPost, "content/posts")
        assertThat(data.title).isEqualTo("Confirm push")
        assertThat(data.isNew).isFalse()
    }

    @Test
    fun buildPushConfirmData_targetPath_includesContentPath() {
        val data = buildPushConfirmData(newPost, "content/posts")
        assertThat(data.targetPath).isEqualTo("content/posts/new-post.md")
    }

    @Test
    fun buildPushConfirmData_commitMessage_isNewPostTitle() {
        val data = buildPushConfirmData(newPost, "content/posts")
        assertThat(data.commitMessage).isEqualTo("New post: New Post")
    }

    @Test
    fun buildPushConfirmData_emptyTitle_usesUntitled() {
        val post = newPost.copy(title = "")
        val data = buildPushConfirmData(post, "content/posts")
        assertThat(data.commitMessage).isEqualTo("New post: Untitled")
    }

    @Test
    fun buildPushConfirmData_newPost_allDiffLinesAdded() {
        val data = buildPushConfirmData(newPost, "content/posts")
        assertThat(data.diffLines).isNotEmpty()
        assertThat(data.diffLines.all { it.type == DiffType.ADDED }).isTrue()
    }

    @Test
    fun buildPushConfirmData_newPost_diffContainsFrontMatter() {
        val data = buildPushConfirmData(newPost, "content/posts")
        val fmText = data.diffLines.joinToString("\n") { it.text }
        assertThat(fmText).contains("title:")
        assertThat(fmText).contains("slug:")
        assertThat(fmText).contains("draft:")
    }

    @Test
    fun buildPushConfirmData_newPost_diffContainsBody() {
        val data = buildPushConfirmData(newPost, "content/posts")
        val bodyText = data.diffLines.joinToString("\n") { it.text }
        assertThat(bodyText).contains("Body text.")
    }

    @Test
    fun buildPushConfirmData_editPost_hasContextLines() {
        val data = buildPushConfirmData(syncedPost, "content/posts")
        assertThat(data.diffLines.any { it.type == DiffType.CONTEXT }).isTrue()
    }

    @Test
    fun buildPushConfirmData_editPostWithDraftFlip_hasContextLines() {
        val post = syncedPost.copy(draft = false)
        val data = buildPushConfirmData(post, "content/posts")
        assertThat(data.diffLines.any { it.type == DiffType.CONTEXT }).isTrue()
    }

    @Test
    fun buildPushConfirmData_differentContentPath_changesTargetPath() {
        val data = buildPushConfirmData(newPost, "content/articles")
        assertThat(data.targetPath).isEqualTo("content/articles/new-post.md")
    }

    @Test
    fun publishDialogState_defaultsAllFalse() {
        val state = PublishDialogState()
        assertThat(state.showDraftFlip).isFalse()
        assertThat(state.showPushConfirm).isFalse()
        assertThat(state.isPushing).isFalse()
        assertThat(state.error).isNull()
        assertThat(state.post).isNull()
    }
}
