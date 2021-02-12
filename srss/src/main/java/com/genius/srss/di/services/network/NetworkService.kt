package com.genius.srss.di.services.network

import com.einmalfel.earl.EarlParser
import com.einmalfel.earl.Feed
import com.ub.utils.download
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class NetworkService @Inject constructor(
    @Named(value = "logging_interceptor") loggingInterceptor: Interceptor
) : INetworkSource {
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    override suspend fun loadFeed(url: String): Feed? {
        return okHttpClient.download(url) { inputStream ->
            inputStream?.let {
                EarlParser.parseOrThrow(it, 0)
            }
        }
    }
}