package com.rrajath.bloggo.ui.theme

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BloggoColorsTest {

    @Test
    fun bloggoColors_dataClassHoldsAllSemanticFields() {
        val colors = BloggoColors(
            success = Color(0xFF3B6939),
            onSuccess = Color.White,
            successContainer = Color(0xFFBCF0B4),
            onSuccessContainer = Color(0xFF1F4620),
            warn = Color(0xFF8F4C00),
            onWarn = Color.White,
            warnContainer = Color(0xFFFFDCC2),
            onWarnContainer = Color(0xFF5E3500),
        )
        assertThat(colors.success).isEqualTo(Color(0xFF3B6939))
        assertThat(colors.onSuccess).isEqualTo(Color.White)
        assertThat(colors.successContainer).isEqualTo(Color(0xFFBCF0B4))
        assertThat(colors.onSuccessContainer).isEqualTo(Color(0xFF1F4620))
        assertThat(colors.warn).isEqualTo(Color(0xFF8F4C00))
        assertThat(colors.onWarn).isEqualTo(Color.White)
        assertThat(colors.warnContainer).isEqualTo(Color(0xFFFFDCC2))
        assertThat(colors.onWarnContainer).isEqualTo(Color(0xFF5E3500))
    }
}
