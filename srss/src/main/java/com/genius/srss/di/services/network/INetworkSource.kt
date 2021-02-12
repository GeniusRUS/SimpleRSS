package com.genius.srss.di.services.network

import com.einmalfel.earl.Feed

interface INetworkSource {
    suspend fun loadFeed(url: String): Feed?
}