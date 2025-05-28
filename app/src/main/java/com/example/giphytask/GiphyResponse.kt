package com.example.giphytask

data class GiphyResponse(
    val data: List<GifObject>
)

data class GifObject(
    val id: String,
    val title: String,
    val images: Images
)

data class Images(
    val fixed_width: GifImage
)

data class GifImage(
    val url: String,
    val width: String,
    val height: String
)