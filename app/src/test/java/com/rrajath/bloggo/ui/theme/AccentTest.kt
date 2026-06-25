package com.rrajath.bloggo.ui.theme

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AccentTest {

    @Test
    fun accent_hasFourOptions() {
        assertThat(Accent.entries).hasSize(4)
    }

    @Test
    fun accent_containsAllExpectedValues() {
        assertThat(Accent.entries).containsExactly(
            Accent.INDIGO,
            Accent.GREEN,
            Accent.AMBER,
            Accent.VIOLET,
        )
    }

    @Test
    fun accent_labelsAreCapitalized() {
        assertThat(Accent.INDIGO.label).isEqualTo("Indigo")
        assertThat(Accent.GREEN.label).isEqualTo("Green")
        assertThat(Accent.AMBER.label).isEqualTo("Amber")
        assertThat(Accent.VIOLET.label).isEqualTo("Violet")
    }

    @Test
    fun accent_defaultIsIndigo() {
        assertThat(Accent.entries.first()).isEqualTo(Accent.INDIGO)
    }
}

class ThemeModeTest {

    @Test
    fun themeMode_hasThreeOptions() {
        assertThat(ThemeMode.entries).hasSize(3)
    }

    @Test
    fun themeMode_containsAllExpectedValues() {
        assertThat(ThemeMode.entries).containsExactly(
            ThemeMode.LIGHT,
            ThemeMode.DARK,
            ThemeMode.SYSTEM,
        )
    }
}
