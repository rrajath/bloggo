package com.rrajath.bloggo.data.db

import androidx.room.TypeConverter
import com.rrajath.bloggo.domain.SyncState

class Converters {
    @TypeConverter
    fun fromSyncState(state: SyncState): String = state.name

    @TypeConverter
    fun toSyncState(value: String): SyncState = SyncState.valueOf(value)
}
