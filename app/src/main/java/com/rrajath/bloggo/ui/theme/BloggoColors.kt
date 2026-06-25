package com.rrajath.bloggo.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class BloggoColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warn: Color,
    val onWarn: Color,
    val warnContainer: Color,
    val onWarnContainer: Color,
)

val LocalBloggoColors = staticCompositionLocalOf {
    BloggoColors(
        success = Color.Unspecified,
        onSuccess = Color.Unspecified,
        successContainer = Color.Unspecified,
        onSuccessContainer = Color.Unspecified,
        warn = Color.Unspecified,
        onWarn = Color.Unspecified,
        warnContainer = Color.Unspecified,
        onWarnContainer = Color.Unspecified,
    )
}

val MaterialTheme.bloggoColors: BloggoColors
    @Composable
    @ReadOnlyComposable
    get() = LocalBloggoColors.current
