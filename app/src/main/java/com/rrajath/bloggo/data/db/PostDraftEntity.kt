package com.rrajath.bloggo.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostDraftEntity(
    @PrimaryKey val localId: String,
    val repoPath: String?,
    val blobSha: String?,
    val syncState: String,
    val title: String,
    val slug: String,
    val draft: Boolean,
    val slugAutoDerive: Boolean,
    val rawFrontMatter: String,
    val postDate: String?,
    val body: String,
    val updatedAt: Long,
)
