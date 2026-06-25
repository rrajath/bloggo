package com.rrajath.bloggo.di

import com.rrajath.bloggo.data.EncryptedSecureStorage
import com.rrajath.bloggo.data.SecureStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {

    @Binds
    @Singleton
    abstract fun bindSecureStorage(impl: EncryptedSecureStorage): SecureStorage
}
