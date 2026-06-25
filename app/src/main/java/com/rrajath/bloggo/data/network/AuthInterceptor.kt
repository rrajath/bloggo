package com.rrajath.bloggo.data.network

import com.rrajath.bloggo.data.SecureStorage
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val secureStorage: SecureStorage,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val pat = secureStorage.getPat()
        val request = if (pat.isNotBlank()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $pat")
                .addHeader("Accept", "application/vnd.github+json")
                .addHeader("X-GitHub-Api-Version", "2022-11-28")
                .build()
        } else {
            chain.request().newBuilder()
                .addHeader("Accept", "application/vnd.github+json")
                .addHeader("X-GitHub-Api-Version", "2022-11-28")
                .build()
        }
        return chain.proceed(request)
    }
}
