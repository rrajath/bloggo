package com.rrajath.bloggo.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedSecureStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) : SecureStorage {

    companion object {
        private const val PREFS_NAME = "secure_settings"
        private const val PAT_KEY = "github_pat"
    }

    private val securePrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun getPat(): String = securePrefs.getString(PAT_KEY, "") ?: ""

    override fun setPat(value: String) {
        securePrefs.edit().putString(PAT_KEY, value).apply()
    }
}
