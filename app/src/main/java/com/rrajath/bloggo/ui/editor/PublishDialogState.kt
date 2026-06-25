package com.rrajath.bloggo.ui.editor

import com.rrajath.bloggo.domain.FrontMatter
import com.rrajath.bloggo.domain.PostDraft

data class DiffLine(
    val text: String,
    val type: DiffType,
)

enum class DiffType { ADDED, REMOVED, CONTEXT }

data class PublishDialogState(
    val showDraftFlip: Boolean = false,
    val showPushConfirm: Boolean = false,
    val isPushing: Boolean = false,
    val error: String? = null,
    val post: PostDraft? = null,
)

data class PushConfirmData(
    val title: String,
    val targetPath: String,
    val commitMessage: String,
    val diffLines: List<DiffLine>,
    val isNew: Boolean,
)

fun buildPushConfirmData(post: PostDraft, contentPath: String): PushConfirmData {
    val isNew = post.syncState == com.rrajath.bloggo.domain.SyncState.LOCAL_ONLY
    val targetPath = post.targetPath(contentPath)
    val commitMessage = "New post: ${post.title.ifBlank { "Untitled" }}"
    val diffLines = if (isNew) {
        buildNewFileDiff(post)
    } else {
        buildEditDiff(post)
    }

    return PushConfirmData(
        title = if (isNew) "Confirm new post" else "Confirm push",
        targetPath = targetPath,
        commitMessage = commitMessage,
        diffLines = diffLines,
        isNew = isNew,
    )
}

private fun buildNewFileDiff(post: PostDraft): List<DiffLine> {
    val assembled = FrontMatter.assemble(post)
    val lines = assembled.content.lines()
    return lines.map { DiffLine("+ $it", DiffType.ADDED) }
}

private fun buildEditDiff(post: PostDraft): List<DiffLine> {
    val assembled = FrontMatter.assemble(post)
    val lines = assembled.content.lines().take(10)
    return lines.mapIndexed { index, line ->
        when {
            line.contains("draft: false") && post.draft -> DiffLine("- $line", DiffType.REMOVED)
            line.contains("draft: true") && !post.draft -> DiffLine("+ $line", DiffType.ADDED)
            else -> DiffLine("  $line", DiffType.CONTEXT)
        }
    }
}
