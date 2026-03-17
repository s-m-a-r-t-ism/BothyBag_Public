package com.bothybag.app.data

data class NewsItem(
    val title: String,
    val link: String,
    val pubDate: String,
    val description: String,
    val matchedBothyId: String? = null,
    val matchedBothyName: String? = null,
)
