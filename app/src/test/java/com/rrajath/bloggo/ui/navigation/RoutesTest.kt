package com.rrajath.bloggo.ui.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RoutesTest {

    @Test
    fun homeRoute_isHome() {
        assertThat(Routes.HOME).isEqualTo("home")
    }

    @Test
    fun editorRoute_isEditor() {
        assertThat(Routes.EDITOR).isEqualTo("editor")
    }

    @Test
    fun editorWithPostRoute_hasPostIdArg() {
        assertThat(Routes.EDITOR_WITH_POST).isEqualTo("editor?postId={postId}")
    }

    @Test
    fun settingsRoute_isSettings() {
        assertThat(Routes.SETTINGS).isEqualTo("settings")
    }

    @Test
    fun editor_withoutPostId_returnsEditorRoute() {
        assertThat(Routes.editor()).isEqualTo("editor")
    }

    @Test
    fun editor_withPostId_returnsEditorWithPostId() {
        assertThat(Routes.editor("abc-123")).isEqualTo("editor?postId=abc-123")
    }

    @Test
    fun editor_withNullPostId_returnsEditorRoute() {
        assertThat(Routes.editor(null)).isEqualTo("editor")
    }
}
