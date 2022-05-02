package com.genius.srss.di.services.network

import com.einmalfel.earl.AtomFeed
import com.einmalfel.earl.EarlParser
import com.einmalfel.earl.Feed
import com.einmalfel.earl.RSSFeed
import com.ub.utils.download
import okhttp3.*
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

    private val wordPressPagination: WordPressPagination by lazy { WordPressPagination() }

    override suspend fun loadFeed(url: String): Feed? {
        return okHttpClient.download(url) { inputStream ->
            inputStream?.let {
                EarlParser.parseOrThrow(it, 0)
            }
        }
    }

    override suspend fun paginationLoad(link: String, feed: Feed): Feed? {
        val generator = when (feed) {
            is AtomFeed -> feed.generator?.value
            is RSSFeed -> feed.generator
            else -> null
        }
        val pagedLink = when {
            generator?.startsWith("https://wordpress.org") == true -> {
                wordPressPagination.nextPageLink(link)
            }
            else -> null
        }
        return pagedLink?.let { loadFeed(it) }
    }

    override fun isFeedSupportedPagination(feed: Feed): Boolean {
        val generator = when (feed) {
            is AtomFeed -> feed.generator?.value
            is RSSFeed -> feed.generator
            else -> null
        }
        return when {
            generator?.startsWith("https://wordpress.org") == true -> true
            else -> false
        }
    }

    override fun extractPageNumber(feed: Feed): Int? {
        val generator = when (feed) {
            is AtomFeed -> feed.generator?.value
            is RSSFeed -> feed.generator
            else -> null
        }
        return when {
            generator?.startsWith("https://wordpress.org") == true ->
                wordPressPagination.currentPage()
            else -> null
        }
    }
}