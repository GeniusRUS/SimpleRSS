package com.genius.srss

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.util.CoilUtils
import com.genius.srss.di.DIManager
import com.genius.srss.di.components.DaggerAppComponent
import okhttp3.OkHttpClient
import javax.inject.Inject

class SRSSApplication : Application(), ImageLoaderFactory {

    @Inject
    lateinit var okHttpClient: OkHttpClient

    override fun onCreate() {
        super.onCreate()

        DIManager.appComponent = DaggerAppComponent.builder()
            .context(this)
            .build()

        DIManager.appComponent.inject(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(
                okHttpClient.newBuilder()
                    .cache(CoilUtils.createDefaultCache(this))
                    .build()
            )
            .build()
    }
}