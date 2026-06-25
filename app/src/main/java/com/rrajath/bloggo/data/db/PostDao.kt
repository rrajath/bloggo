package com.rrajath.bloggo.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<PostDraftEntity>>

    @Query("SELECT * FROM posts WHERE localId = :localId")
    suspend fun getById(localId: String): PostDraftEntity?

    @Query("SELECT * FROM posts WHERE localId = :localId")
    fun observeById(localId: String): Flow<PostDraftEntity?>

    @Upsert
    suspend fun upsert(entity: PostDraftEntity)

    @Delete
    suspend fun delete(entity: PostDraftEntity)

    @Query("DELETE FROM posts WHERE localId = :localId")
    suspend fun deleteById(localId: String)

    @Query("SELECT COUNT(*) FROM posts")
    suspend fun count(): Int
}
