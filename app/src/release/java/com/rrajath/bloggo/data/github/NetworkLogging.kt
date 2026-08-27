package com.rrajath.bloggo.data.github

import okhttp3.OkHttpClient

/** Release builds: no request logging at all. */
internal fun OkHttpClient.Builder.withDebugLogging(): OkHttpClient.Builder = this
