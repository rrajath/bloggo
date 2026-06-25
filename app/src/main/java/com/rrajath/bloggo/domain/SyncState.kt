package com.rrajath.bloggo.domain

enum class SyncState {
    LOCAL_ONLY,
    SYNCED,
    SYNCED_MODIFIED,
}

fun SyncState.tagLabel(): String = when (this) {
    SyncState.LOCAL_ONLY -> "Local"
    SyncState.SYNCED -> "Synced"
    SyncState.SYNCED_MODIFIED -> "Synced"
}

val SyncState.showsEditedDot: Boolean
    get() = this == SyncState.SYNCED_MODIFIED
