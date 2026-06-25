package com.rrajath.bloggo.security

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BiometricHelperTest {

    @Test
    fun biometricHelper_canBeInstantiated() {
        val helper = BiometricHelper()
        assertThat(helper).isNotNull()
    }
}
