package com.example.giphytask

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // creating mock
    private val mockApi = mock<GiphyApi>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initial_state_should_have_empty_gif_list_and_Search_tab_selected() = runTest {

        whenever(mockApi.searchGifs(any(), any(), any(), any())).thenReturn(
            GiphyResponse(data = emptyList())
        )

        whenever(mockApi.getTrending(any(), any(), any())).thenReturn(
            GiphyResponse(data = emptyList())
        )

        val viewModel = MainViewModel(api = mockApi)


        assertTrue(viewModel.gifs.value.isEmpty()) // Gifs are not loaded after app initialization
        assertFalse(viewModel.isLoading.value) // Loading indicator is not shown after initialization
        assertEquals(GifTab.Search, viewModel.currentTab.value) // Initial tab is "Search" tab
    }

    @Test
    fun switching_tab_should_update_currentTab() = runTest {
        whenever(mockApi.getTrending(any(), any(), any())).thenReturn(GiphyResponse(data = emptyList()))

        val viewModel = MainViewModel(api = mockApi)

        viewModel.switchTab(GifTab.Trending)

        assertEquals(GifTab.Trending, viewModel.currentTab.value)
    }


    @Test
    fun loadNextPage_does_nothing_if_loading_or_empty_lastQuery_in_Search_tab() = runTest {
        val vm = MainViewModel(api = mockApi)
        advanceUntilIdle()

        // if loading true - should skip
        vm.loadNextPage()
        assertFalse(vm.isLoading.value) // no loading started

        // if set Search tab and lastQuery empty - should skip
        vm.searchGifs("")
        advanceTimeBy(300)
        advanceUntilIdle()

        vm.loadNextPage()
        assertFalse(vm.isLoading.value)
    }

}