package com.rrajath.bloggo.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [PostDraftEntity::class, AutosaveEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class BloggoDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun autosaveDao(): AutosaveDao
}
