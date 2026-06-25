package com.rrajath.bloggo.ui.navigation

object Routes {
    const val HOME = "home"
    const val EDITOR = "editor"
    const val EDITOR_WITH_POST = "editor?postId={postId}"
    const val SETTINGS = "settings"

    fun editor(postId: String? = null): String {
        return if (postId != null) "editor?postId=$postId" else "editor"
    }
}
