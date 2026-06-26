package com.rrajath.bloggo.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.rrajath.bloggo.ui.theme.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

private class FakeSecureStorage : SecureStorage {
    private var pat: String = ""
    override fun getPat(): String = pat
    override fun setPat(value: String) { pat = value }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsRepositoryTest {

    private lateinit var repository: SettingsRepository
    private lateinit var context: android.content.Context
    private val fakeSecureStorage = FakeSecureStorage()
    private lateinit var tempDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        tempDir = File(context.cacheDir, "test_datastore_${System.nanoTime()}")
        tempDir.mkdirs()
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { File(tempDir, "settings.preferences_pb") },
        )
        repository = SettingsRepository(dataStore, fakeSecureStorage)
    }

    @Test
    fun defaults_areCorrect() = runTest {
        val settings = repository.settings.first()

        assertThat(settings.githubPat).isEmpty()
        assertThat(settings.repository).isEmpty()
        assertThat(settings.branch).isEqualTo("main")
        assertThat(settings.contentPath).isEqualTo("content/posts")
        assertThat(settings.theme).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    fun savePat_andReadBack() = runTest {
        repository.savePat("ghp_abc123secret")
        val settings = repository.settings.first()

        assertThat(settings.githubPat).isEqualTo("ghp_abc123secret")
    }

    @Test
    fun savePat_overwritesPreviousValue() = runTest {
        repository.savePat("first")
        repository.savePat("second")
        val settings = repository.settings.first()

        assertThat(settings.githubPat).isEqualTo("second")
    }

    @Test
    fun savePat_storedInSecureStorage_notInPlainPrefs() = runTest {
        repository.savePat("ghp_secret_token")

        val plainPrefs = context.getSharedPreferences("secure_settings", android.content.Context.MODE_PRIVATE)
        assertThat(plainPrefs.all.values).doesNotContain("ghp_secret_token")
        assertThat(fakeSecureStorage.getPat()).isEqualTo("ghp_secret_token")
    }

    @Test
    fun saveRepository_andReadBack() = runTest {
        repository.saveRepository("me/blog")
        val settings = repository.settings.first()

        assertThat(settings.repository).isEqualTo("me/blog")
    }

    @Test
    fun saveTheme_andReadBack() = runTest {
        repository.saveTheme(ThemeMode.DARK)
        val settings = repository.settings.first()

        assertThat(settings.theme).isEqualTo(ThemeMode.DARK)
    }

    @Test
    fun saveAll_persistsAllFields() = runTest {
        val settings = Settings(
            githubPat = "ghp_token",
            repository = "me/blog",
            branch = "develop",
            contentPath = "content/articles",
            imageRepoPath = "static/img",
            imageUrlBase = "/img",
            blogBaseUrl = "https://blog.me.dev",
            frontMatterTemplate = "date: {date}\ntags: []",
            theme = ThemeMode.LIGHT,
        )

        repository.saveAll(settings)
        val loaded = repository.settings.first()

        assertThat(loaded).isEqualTo(settings)
    }

    @Test
    fun saveAll_thenIndividualSave_overridesSpecificField() = runTest {
        repository.saveAll(Settings(githubPat = "pat", repository = "me/blog", theme = ThemeMode.DARK))
        repository.saveTheme(ThemeMode.LIGHT)

        val settings = repository.settings.first()
        assertThat(settings.theme).isEqualTo(ThemeMode.LIGHT)
        assertThat(settings.repository).isEqualTo("me/blog")
    }

    @Test
    fun saveContentPath_andReadBack() = runTest {
        repository.saveContentPath("content/articles")
        val settings = repository.settings.first()

        assertThat(settings.contentPath).isEqualTo("content/articles")
    }

    @Test
    fun saveFrontMatterTemplate_andReadBack() = runTest {
        val template = "date: {date}\ntags: [rust, memory]\nsummary: \"\""
        repository.saveFrontMatterTemplate(template)
        val settings = repository.settings.first()

        assertThat(settings.frontMatterTemplate).isEqualTo(template)
    }
}
