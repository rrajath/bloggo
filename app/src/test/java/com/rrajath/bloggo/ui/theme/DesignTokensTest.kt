package com.rrajath.bloggo.ui.theme

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DesignTokensTest {

    // ── Shared surface palette (light) ──────────────────────────────────────

    @Test
    fun lightSurface_isFcfbfe() {
        assertThat(LightSurface).isEqualTo(Color(0xFFFCFBFE))
    }

    @Test
    fun lightSurfaceLow_isF6f3f9() {
        assertThat(LightSurfaceLow).isEqualTo(Color(0xFFF6F3F9))
    }

    @Test
    fun lightSurfaceContainer_isF0edf3() {
        assertThat(LightSurfaceContainer).isEqualTo(Color(0xFFF0EDF3))
    }

    @Test
    fun lightSurfaceHigh_isEae7ee() {
        assertThat(LightSurfaceHigh).isEqualTo(Color(0xFFEAE7EE))
    }

    @Test
    fun lightSurfaceHighest_isE4e1e9() {
        assertThat(LightSurfaceHighest).isEqualTo(Color(0xFFE4E1E9))
    }

    @Test
    fun lightOnSurface_is1b1b1f() {
        assertThat(LightOnSurface).isEqualTo(Color(0xFF1B1B1F))
    }

    @Test
    fun lightOnSurfaceVariant_is46464f() {
        assertThat(LightOnSurfaceVariant).isEqualTo(Color(0xFF46464F))
    }

    @Test
    fun lightOutline_is777680() {
        assertThat(LightOutline).isEqualTo(Color(0xFF777680))
    }

    @Test
    fun lightOutlineVariant_isC7c5d0() {
        assertThat(LightOutlineVariant).isEqualTo(Color(0xFFC7C5D0))
    }

    // ── Shared surface palette (dark) ───────────────────────────────────────

    @Test
    fun darkSurface_is131318() {
        assertThat(DarkSurface).isEqualTo(Color(0xFF131318))
    }

    @Test
    fun darkSurfaceLow_is1a1a20() {
        assertThat(DarkSurfaceLow).isEqualTo(Color(0xFF1A1A20))
    }

    @Test
    fun darkSurfaceContainer_is1e1e24() {
        assertThat(DarkSurfaceContainer).isEqualTo(Color(0xFF1E1E24))
    }

    @Test
    fun darkSurfaceHigh_is29292f() {
        assertThat(DarkSurfaceHigh).isEqualTo(Color(0xFF29292F))
    }

    @Test
    fun darkSurfaceHighest_is34343a() {
        assertThat(DarkSurfaceHighest).isEqualTo(Color(0xFF34343A))
    }

    @Test
    fun darkOnSurface_isE4e1e9() {
        assertThat(DarkOnSurface).isEqualTo(Color(0xFFE4E1E9))
    }

    @Test
    fun darkOnSurfaceVariant_isC7c5d0() {
        assertThat(DarkOnSurfaceVariant).isEqualTo(Color(0xFFC7C5D0))
    }

    @Test
    fun darkOutline_is918f9a() {
        assertThat(DarkOutline).isEqualTo(Color(0xFF918F9A))
    }

    @Test
    fun darkOutlineVariant_is46464f() {
        assertThat(DarkOutlineVariant).isEqualTo(Color(0xFF46464F))
    }

    // ── Indigo accent ───────────────────────────────────────────────────────

    @Test
    fun indigoLightPrimary_is4f5bd5() {
        assertThat(IndigoLightPrimary).isEqualTo(Color(0xFF4F5BD5))
    }

    @Test
    fun indigoDarkPrimary_isC0c1ff() {
        assertThat(IndigoDarkPrimary).isEqualTo(Color(0xFFC0C1FF))
    }

    @Test
    fun indigoLightPrimaryContainer_isE1e0ff() {
        assertThat(IndigoLightPrimaryContainer).isEqualTo(Color(0xFFE1E0FF))
    }

    @Test
    fun indigoDarkPrimaryContainer_is373b91() {
        assertThat(IndigoDarkPrimaryContainer).isEqualTo(Color(0xFF373B91))
    }

    // ── Green accent ────────────────────────────────────────────────────────

    @Test
    fun greenLightPrimary_is006b5b() {
        assertThat(GreenLightPrimary).isEqualTo(Color(0xFF006B5B))
    }

    @Test
    fun greenDarkPrimary_is53dbc3() {
        assertThat(GreenDarkPrimary).isEqualTo(Color(0xFF53DBC3))
    }

    @Test
    fun greenLightPrimaryContainer_is71f8e0() {
        assertThat(GreenLightPrimaryContainer).isEqualTo(Color(0xFF71F8E0))
    }

    @Test
    fun greenDarkPrimaryContainer_is005045() {
        assertThat(GreenDarkPrimaryContainer).isEqualTo(Color(0xFF005045))
    }

    // ── Amber accent ────────────────────────────────────────────────────────

    @Test
    fun amberLightPrimary_is8a5100() {
        assertThat(AmberLightPrimary).isEqualTo(Color(0xFF8A5100))
    }

    @Test
    fun amberDarkPrimary_isFfb868() {
        assertThat(AmberDarkPrimary).isEqualTo(Color(0xFFFFB868))
    }

    @Test
    fun amberLightPrimaryContainer_isFfdcbe() {
        assertThat(AmberLightPrimaryContainer).isEqualTo(Color(0xFFFFDCBE))
    }

    @Test
    fun amberDarkPrimaryContainer_is693c00() {
        assertThat(AmberDarkPrimaryContainer).isEqualTo(Color(0xFF693C00))
    }

    // ── Violet accent ───────────────────────────────────────────────────────

    @Test
    fun violetLightPrimary_is6b4ea8() {
        assertThat(VioletLightPrimary).isEqualTo(Color(0xFF6B4EA8))
    }

    @Test
    fun violetDarkPrimary_isD5bbff() {
        assertThat(VioletDarkPrimary).isEqualTo(Color(0xFFD5BBFF))
    }

    @Test
    fun violetLightPrimaryContainer_isEbddff() {
        assertThat(VioletLightPrimaryContainer).isEqualTo(Color(0xFFEBDDFF))
    }

    @Test
    fun violetDarkPrimaryContainer_is523885() {
        assertThat(VioletDarkPrimaryContainer).isEqualTo(Color(0xFF523885))
    }

    // ── Semantic colors ─────────────────────────────────────────────────────

    @Test
    fun lightSuccess_is3b6939() {
        assertThat(LightSuccess).isEqualTo(Color(0xFF3B6939))
    }

    @Test
    fun lightSuccessContainer_isBcf0b4() {
        assertThat(LightSuccessContainer).isEqualTo(Color(0xFFBCF0B4))
    }

    @Test
    fun darkSuccess_isA1d39a() {
        assertThat(DarkSuccess).isEqualTo(Color(0xFFA1D39A))
    }

    @Test
    fun darkSuccessContainer_is1f4620() {
        assertThat(DarkSuccessContainer).isEqualTo(Color(0xFF1F4620))
    }

    @Test
    fun lightWarn_is8f4c00() {
        assertThat(LightWarn).isEqualTo(Color(0xFF8F4C00))
    }

    @Test
    fun lightWarnContainer_isFfdcc2() {
        assertThat(LightWarnContainer).isEqualTo(Color(0xFFFFDCC2))
    }

    @Test
    fun darkWarn_isFfb783() {
        assertThat(DarkWarn).isEqualTo(Color(0xFFFFB783))
    }

    @Test
    fun darkWarnContainer_is5e3500() {
        assertThat(DarkWarnContainer).isEqualTo(Color(0xFF5E3500))
    }

    @Test
    fun lightError_isBa1a1a() {
        assertThat(LightError).isEqualTo(Color(0xFFBA1A1A))
    }

    @Test
    fun darkError_isFfb4ab() {
        assertThat(DarkError).isEqualTo(Color(0xFFFFB4AB))
    }
}
