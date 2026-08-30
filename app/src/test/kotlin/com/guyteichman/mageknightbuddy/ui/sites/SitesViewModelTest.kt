package com.guyteichman.mageknightbuddy.ui.sites

import com.guyteichman.mageknightbuddy.data.FavoriteSitesRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Behavior of [SitesViewModel] - the Sites tab's favorites state + toggle action (issue #236) -
 * backed by a real [FavoriteSitesRepository] over an in-memory [FakeFavoriteSiteDao].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SitesViewModelTest {

    // setFavorite launches on viewModelScope (Dispatchers.Main), so Main needs a test dispatcher for
    // plain JVM unit tests, same setup as the Dummy Player ViewModel tests.
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `favorites starts empty`() = runTest {
        val viewModel = SitesViewModel(FavoriteSitesRepository(FakeFavoriteSiteDao()))

        assertEquals(emptySet(), viewModel.favorites.first())
    }

    @Test
    fun `setFavorite true makes the site appear in favorites`() = runTest {
        val viewModel = SitesViewModel(FavoriteSitesRepository(FakeFavoriteSiteDao()))

        viewModel.setFavorite("keep", favorite = true)
        advanceUntilIdle()

        assertEquals(setOf("keep"), viewModel.favorites.first())
    }

    @Test
    fun `setFavorite false removes just that site from favorites`() = runTest {
        val viewModel = SitesViewModel(FavoriteSitesRepository(FakeFavoriteSiteDao()))
        viewModel.setFavorite("keep", favorite = true)
        viewModel.setFavorite("village", favorite = true)
        advanceUntilIdle()

        viewModel.setFavorite("keep", favorite = false)
        advanceUntilIdle()

        assertEquals(setOf("village"), viewModel.favorites.first())
    }
}
