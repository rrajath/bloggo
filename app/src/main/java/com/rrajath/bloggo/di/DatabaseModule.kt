package com.rrajath.bloggo.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.rrajath.bloggo.data.db.AutosaveDao
import com.rrajath.bloggo.data.db.BloggoDatabase
import com.rrajath.bloggo.data.db.PostDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BloggoDatabase =
        Room.databaseBuilder(context, BloggoDatabase::class.java, "bloggo.db")
            .build()

    @Provides
    fun providePostDao(database: BloggoDatabase): PostDao = database.postDao()

    @Provides
    fun provideAutosaveDao(database: BloggoDatabase): AutosaveDao = database.autosaveDao()

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.settingsDataStore
}
