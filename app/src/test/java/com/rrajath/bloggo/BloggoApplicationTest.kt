package com.rrajath.bloggo

import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.HiltAndroidApp
import org.junit.Test

class BloggoApplicationTest {

    @Test
    fun applicationClass_extendsApplication() {
        val clazz = BloggoApplication::class.java
        assertThat(android.app.Application::class.java.isAssignableFrom(clazz)).isTrue()
    }

    @Test
    fun applicationClass_hasHiltAndroidAppAnnotation() {
        assertThat(BloggoApplication::class.java.isAnnotationPresent(HiltAndroidApp::class.java)).isTrue()
    }
}
