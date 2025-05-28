package com.example.giphytask

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class GifTab {
    object Search : GifTab()
    object Trending : GifTab()
}

class MainViewModel(
    private val api: GiphyApi = RetrofitInstance.api
) : ViewModel() {
    private val _gifs = MutableStateFlow<List<GifObject>>(emptyList())
    val gifs: StateFlow<List<GifObject>> = _gifs

    private val searchQuery = MutableStateFlow("")
    private var currentOffset = 0
    private var lastQuery = ""

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _currentTab = MutableStateFlow<GifTab>(GifTab.Search)
    val currentTab: StateFlow<GifTab> = _currentTab

    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage = _errorMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            searchQuery
                .debounce(300) // minor delay after user input
                .filter { it.isNotBlank() }
                .distinctUntilChanged()
                .collect { query ->
                    if (_currentTab.value == GifTab.Search) {
                        lastQuery = query
                        currentOffset = 0
                        loadGifs(reset = true)
                    }
                }
        }

        loadGifs(reset = true)

    }
    fun networkMonitoring(context: Context) {
        viewModelScope.launch {
            NetworkMonitor.observe(context).collect {
                _isConnected.value = it
            }
        }
    }

    fun switchTab(tab: GifTab) {
        if (_currentTab.value == tab) return
        _currentTab.value = tab
        currentOffset = 0
        lastQuery = ""
        searchQuery.value = ""
        loadGifs(reset = true)
    }

    fun searchGifs(query: String) {
        _currentTab.value = GifTab.Search
        searchQuery.value = query
    }

    fun loadNextPage() {
        if (_isLoading.value) return
        if (_currentTab.value == GifTab.Search && lastQuery.isBlank()) return
        loadGifs(reset = false)
    }

    private fun loadGifs(reset: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = when (_currentTab.value) {
                    GifTab.Search -> api.searchGifs(
                        apiKey = "hNJLlmlj0xM4B7cRIORYCL6wgKFqZ9HV",
                        query = lastQuery,
                        offset = currentOffset
                    )
                    GifTab.Trending -> api.getTrending(
                        apiKey = "hNJLlmlj0xM4B7cRIORYCL6wgKFqZ9HV",
                        offset = currentOffset
                    )
                }

                if (reset) {
                    _gifs.value = response.data
                } else {
                    _gifs.value += response.data
                }

                currentOffset += response.data.size
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading gifs", e)
                _errorMessage.emit("Error loading gifs: ${e.message}")
                if (reset) _gifs.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
