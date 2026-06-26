package com.rrajath.bloggo.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PostDraftEntity::class, AutosaveEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class BloggoDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun autosaveDao(): AutosaveDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE posts ADD COLUMN postDate TEXT")
            }
        }
    }
}
