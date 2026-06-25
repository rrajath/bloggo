package com.rrajath.bloggo.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface AutosaveDao {
    @Query("SELECT * FROM autosaves WHERE localId = :localId")
    suspend fun get(localId: String): AutosaveEntity?

    @Upsert
    suspend fun save(entity: AutosaveEntity)

    @Query("DELETE FROM autosaves WHERE localId = :localId")
    suspend fun delete(localId: String)
}
