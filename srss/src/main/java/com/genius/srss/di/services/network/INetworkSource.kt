package com.genius.srss.di.services.network

import com.einmalfel.earl.Feed

interface INetworkSource {
    suspend fun loadFeed(url: String): Feed?
    suspend fun paginationLoad(link: String, feed: Feed): Feed?
    fun isFeedSupportedPagination(feed: Feed): Boolean
    fun extractPageNumber(feed: Feed): Int?
}