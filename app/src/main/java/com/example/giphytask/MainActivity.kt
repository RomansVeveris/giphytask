package com.example.giphytask

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

import coil.ImageLoader
import coil.request.ImageRequest
import coil.decode.GifDecoder

// had a hard time making use of DI (it is not made in project)
// because have never done it before
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainScreen(viewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val gifs by viewModel.gifs.collectAsState()
    var searchText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val columns = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 3 else 2

    val listState = rememberLazyGridState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()

    val isConnected by viewModel.isConnected.collectAsState()
    var showDialog by remember { mutableStateOf(true) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    // server response listener
    LaunchedEffect(Unit) {
        viewModel.errorMessage.collect { message ->
            errorMessage = message
            kotlinx.coroutines.delay(3000)
            errorMessage = null
        }
    }

    // internet connection listener
    LaunchedEffect(isConnected) {
        showDialog = !isConnected
    }

    // error display, for example error is shown when invalid api is provided
    if (errorMessage != null) {
        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        errorMessage = null
    }

    // alert when user is not connected to the internet
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("No Internet") },
            text = { Text("You're not connected to the internet. Please check your connection.") },
            confirmButton = {
                Button(onClick = {
                    showDialog = false
                }) {
                    Text("OK")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {

        // tab selector
        TabRow(
            selectedTabIndex = if (currentTab is GifTab.Search) 0 else 1
        ) {
            Tab(
                selected = currentTab is GifTab.Search,
                onClick = {
                    viewModel.switchTab(GifTab.Search)
                },
                text = { Text("Search") }
            )
            Tab(
                selected = currentTab is GifTab.Trending,
                onClick = {
                    viewModel.switchTab(GifTab.Trending)
                },
                text = { Text("Trending") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // show search only in Search tab
        if (currentTab is GifTab.Search) {
            TextField(
                value = searchText,
                onValueChange = { newText ->
                    searchText = newText
                    viewModel.searchGifs(newText)
                },
                label = { Text("Search Gifs") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // gifs are displayed in grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(4.dp)
        ) {
            items(gifs) { gif ->
                GifImage(
                    url = gif.images.fixed_width.url,
                    contentDescription = gif.title,
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxWidth()
                        .height(150.dp)
                        .clickable { // if clicked then detailed information appears in new view
                            val intent = Intent(context, GifDetailActivity::class.java).apply {
                                putExtra("gif_url", gif.images.fixed_width.url)
                                putExtra("gif_title", gif.title)
                            }
                            context.startActivity(intent)
                        }
                )
            }
            // circular progress indicator is shown while loading
            if (isLoading) {
                item(span = { GridItemSpan(columns) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    // pagination listener
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        }.collect { lastVisibleItemIndex ->
            if (lastVisibleItemIndex >= gifs.size - 5) {
                viewModel.loadNextPage()
            }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.networkMonitoring(context)
    }
}

@Composable
fun GifImage(url: String, contentDescription: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .components {
            add(GifDecoder.Factory())
        }
        .build()

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .build(),
        imageLoader = imageLoader,
        contentDescription = contentDescription,
        modifier = modifier
    )
}


