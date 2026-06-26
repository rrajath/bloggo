package com.rrajath.bloggo.data

import com.rrajath.bloggo.data.db.AutosaveEntity
import com.rrajath.bloggo.data.db.PostDraftEntity
import com.rrajath.bloggo.domain.PostDraft
import com.rrajath.bloggo.domain.SyncState

fun PostDraftEntity.toDomain(): PostDraft = PostDraft(
    localId = localId,
    repoPath = repoPath,
    blobSha = blobSha,
    syncState = SyncState.valueOf(syncState),
    title = title,
    slug = slug,
    draft = draft,
    slugAutoDerive = slugAutoDerive,
    rawFrontMatter = rawFrontMatter,
    postDate = postDate,
    body = body,
    updatedAt = updatedAt,
)

fun PostDraft.toEntity(): PostDraftEntity = PostDraftEntity(
    localId = localId,
    repoPath = repoPath,
    blobSha = blobSha,
    syncState = syncState.name,
    title = title,
    slug = slug,
    draft = draft,
    slugAutoDerive = slugAutoDerive,
    rawFrontMatter = rawFrontMatter,
    postDate = postDate,
    body = body,
    updatedAt = updatedAt,
)

fun PostDraft.toAutosaveEntity(): AutosaveEntity = AutosaveEntity(
    localId = localId,
    title = title,
    slug = slug,
    draft = draft,
    slugAutoDerive = slugAutoDerive,
    rawFrontMatter = rawFrontMatter,
    body = body,
    updatedAt = updatedAt,
)

fun AutosaveEntity.toDomain(localId: String): PostDraft = PostDraft(
    localId = localId,
    title = title,
    slug = slug,
    draft = draft,
    slugAutoDerive = slugAutoDerive,
    rawFrontMatter = rawFrontMatter,
    body = body,
    updatedAt = updatedAt,
)
