package com.rrajath.bloggo.ui.settings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.rrajath.bloggo.data.Settings
import com.rrajath.bloggo.data.SettingsRepository
import com.rrajath.bloggo.ui.theme.Accent
import com.rrajath.bloggo.ui.theme.ThemeMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    private val testSettings = Settings()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { settingsRepository.settings } returns MutableStateFlow(testSettings)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun settings_initiallyHasDefaults() = runTest {
        val vm = SettingsViewModel(settingsRepository)
        vm.settings.test {
            val settings = awaitItem()
            assertThat(settings.branch).isEqualTo("main")
            assertThat(settings.theme).isEqualTo(ThemeMode.SYSTEM)
            assertThat(settings.accent).isEqualTo(Accent.INDIGO)
            assertThat(settings.appLock).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun savePat_delegatesToRepository() = runTest {
        val vm = SettingsViewModel(settingsRepository)
        vm.savePat("ghp_token")
        advanceUntilIdle()
        coVerify { settingsRepository.savePat("ghp_token") }
    }

    @Test
    fun saveRepository_delegatesToRepository() = runTest {
        val vm = SettingsViewModel(settingsRepository)
        vm.saveRepository("me/blog")
        advanceUntilIdle()
        coVerify { settingsRepository.saveRepository("me/blog") }
    }

    @Test
    fun saveBranch_delegatesToRepository() = runTest {
        val vm = SettingsViewModel(settingsRepository)
        vm.saveBranch("develop")
        advanceUntilIdle()
        coVerify { settingsRepository.saveBranch("develop") }
    }

    @Test
    fun saveContentPath_delegatesToRepository() = runTest {
        val vm = SettingsViewModel(settingsRepository)
        vm.saveContentPath("content/articles")
        advanceUntilIdle()
        coVerify { settingsRepository.saveContentPath("content/articles") }
    }

    @Test
    fun saveImageRepoPath_delegatesToRepository() = runTest {
        val vm = SettingsViewModel(settingsRepository)
        vm.saveImageRepoPath("static/img")
        advanceUntilIdle()
        coVerify { settingsRepository.saveImageRepoPath("static/img") }
    }

    @Test
    fun saveImageUrlBase_delegatesToRepository() = runTest {
        val vm = SettingsViewModel(settingsRepository)
        vm.saveImageUrlBase("/img")
        advanceUntilIdle()
        coVerify { settingsRepository.saveImageUrlBase("/img") }
    }

    @Test
    fun saveBlogBaseUrl_delegatesToRepository() = runTest {
        val vm = SettingsViewModel(settingsRepository)
        vm.saveBlogBaseUrl("https://blog.me.dev")
        advanceUntilIdle()
        coVerify { settingsRepository.saveBlogBaseUrl("https://blog.me.dev") }
    }

    @Test
    fun saveFrontMatterTemplate_delegatesToRepository() = runTest {
        val vm = SettingsViewModel(settingsRepository)
        vm.saveFrontMatterTemplate("date: {date}\ntags: []")
        advanceUntilIdle()
        coVerify { settingsRepository.saveFrontMatterTemplate("date: {date}\ntags: []") }
    }

    @Test
    fun saveTheme_delegatesToRepository() = runTest {
        val vm = SettingsViewModel(settingsRepository)
        vm.saveTheme(ThemeMode.DARK)
        advanceUntilIdle()
        coVerify { settingsRepository.saveTheme(ThemeMode.DARK) }
    }

    @Test
    fun saveAccent_delegatesToRepository() = runTest {
        val vm = SettingsViewModel(settingsRepository)
        vm.saveAccent(Accent.GREEN)
        advanceUntilIdle()
        coVerify { settingsRepository.saveAccent(Accent.GREEN) }
    }

    @Test
    fun saveAppLock_delegatesToRepository() = runTest {
        val vm = SettingsViewModel(settingsRepository)
        vm.saveAppLock(true)
        advanceUntilIdle()
        coVerify { settingsRepository.saveAppLock(true) }
    }
}
