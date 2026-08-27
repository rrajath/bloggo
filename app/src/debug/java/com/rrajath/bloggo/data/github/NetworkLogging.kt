package com.rrajath.bloggo.data.github

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

/** Debug builds only. ANDROID_TDD.md §7.3: logs are debug-only and never the token. */
internal fun OkHttpClient.Builder.withDebugLogging(): OkHttpClient.Builder {
  val logging = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BASIC
    redactHeader("Authorization")
  }
  return addInterceptor(logging)
}
