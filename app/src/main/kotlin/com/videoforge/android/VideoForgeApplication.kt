package com.videoforge.android

import android.app.Application
import android.os.StrictMode
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.memory.MemoryCache
import com.videoforge.core.adaptive.AdaptiveManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class VideoForgeApplication : Application(), ImageLoaderFactory {

    @Inject
    lateinit var adaptiveManager: AdaptiveManager

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )

            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .detectActivityLeaks()
                    .penaltyLog()
                    .build()
            )
        }
    }

    override fun newImageLoader(): ImageLoader {
        val policy = adaptiveManager.currentPolicy()

        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache(
                MemoryCache.Builder(this)
                    .maxSizeBytes(policy.imageCacheBytes)
                    .build()
            )
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }
}