package com.rrajath.bloggo.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SyncStateTest {

    @Test
    fun localOnly_tagLabelIsLocal() {
        assertThat(SyncState.LOCAL_ONLY.tagLabel()).isEqualTo("Local")
    }

    @Test
    fun synced_tagLabelIsSynced() {
        assertThat(SyncState.SYNCED.tagLabel()).isEqualTo("Synced")
    }

    @Test
    fun syncedModified_tagLabelIsSynced() {
        assertThat(SyncState.SYNCED_MODIFIED.tagLabel()).isEqualTo("Synced")
    }

    @Test
    fun localOnly_doesNotShowEditedDot() {
        assertThat(SyncState.LOCAL_ONLY.showsEditedDot).isFalse()
    }

    @Test
    fun synced_doesNotShowEditedDot() {
        assertThat(SyncState.SYNCED.showsEditedDot).isFalse()
    }

    @Test
    fun syncedModified_showsEditedDot() {
        assertThat(SyncState.SYNCED_MODIFIED.showsEditedDot).isTrue()
    }
}
