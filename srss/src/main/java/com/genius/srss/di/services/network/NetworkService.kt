package com.genius.srss.di.services.network

import com.einmalfel.earl.EarlParser
import com.einmalfel.earl.Feed
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

    private suspend inline fun <T> OkHttpClient.download(url: String, crossinline objectMapper: (byteStream: InputStream?) -> T?) =
        suspendCancellableCoroutine<T?> { continuation ->
            val request = Request.Builder().url(url).build()
            val call = newCall(request)
            call.enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (!continuation.isCompleted) {
                        try {
                            val result = objectMapper.invoke(response.body?.byteStream())
                            continuation.resume(result)
                        } catch (e: Exception) {
                            continuation.resumeWithException(e)
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    if (!continuation.isCompleted) {
                        continuation.resumeWithException(e)
                    }
                }
            })

            continuation.invokeOnCancellation {
                call.cancel()
            }
        }
}