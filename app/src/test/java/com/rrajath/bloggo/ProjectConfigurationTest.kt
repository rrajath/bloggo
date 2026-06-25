package com.rrajath.bloggo

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProjectConfigurationTest {

    @Test
    fun applicationId_matchesExpectedPackage() {
        val expected = "com.rrajath.bloggo"
        assertThat(BuildConfig::class.java.`package`?.name).isEqualTo(expected)
    }

    @Test
    fun buildConfigExists() {
        assertThat(BuildConfig::class.java).isNotNull()
    }
}
