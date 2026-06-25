package com.rrajath.bloggo.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ImageSizeTest {

    @Test
    fun small_hasMaxWidth480() {
        assertThat(ImageSize.SMALL.maxWidth).isEqualTo(480)
        assertThat(ImageSize.SMALL.label).isEqualTo("Small")
    }

    @Test
    fun medium_hasMaxWidth800() {
        assertThat(ImageSize.MEDIUM.maxWidth).isEqualTo(800)
        assertThat(ImageSize.MEDIUM.label).isEqualTo("Medium")
    }

    @Test
    fun large_hasMaxWidth1600() {
        assertThat(ImageSize.LARGE.maxWidth).isEqualTo(1600)
        assertThat(ImageSize.LARGE.label).isEqualTo("Large")
    }

    @Test
    fun imageSize_hasThreeOptions() {
        assertThat(ImageSize.entries).hasSize(3)
    }
}
