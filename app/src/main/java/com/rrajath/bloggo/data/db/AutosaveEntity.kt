package com.rrajath.bloggo.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "autosaves")
data class AutosaveEntity(
    @PrimaryKey val localId: String,
    val title: String,
    val slug: String,
    val draft: Boolean,
    val slugAutoDerive: Boolean,
    val rawFrontMatter: String,
    val body: String,
    val updatedAt: Long,
)
