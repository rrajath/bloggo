package com.rrajath.bloggo.domain

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
    .withZone(ZoneOffset.UTC)

fun currentIsoTimestamp(): String =
    ISO_FORMATTER.format(Instant.now())

fun seedFrontMatter(template: String): String {
    return template.replace("{date}", currentIsoTimestamp())
}

fun ParseResult.toPostDraft(
    localId: String,
    syncState: SyncState = SyncState.SYNCED,
): PostDraft {
    return PostDraft(
        localId = localId,
        syncState = syncState,
        title = title ?: "",
        slug = slug ?: "",
        draft = draft,
        slugAutoDerive = false,
        rawFrontMatter = rawFrontMatter,
        postDate = date ?: lastmod,
        body = body,
    )
}

fun PostDraft.withSyncedState(): PostDraft =
    copy(
        syncState = SyncState.SYNCED,
        slugAutoDerive = false,
    )

fun PostDraft.markAsModified(): PostDraft =
    copy(syncState = SyncState.SYNCED_MODIFIED)
