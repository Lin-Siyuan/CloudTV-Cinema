package com.tv.stream

import android.app.Application
import android.graphics.Bitmap
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache
import coil.request.DefaultRequestOptions

class App : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this).maxSizePercent(0.15).build()
            }
            .defaultRequestOptions(
                DefaultRequestOptions(
                    bitmapConfig = Bitmap.Config.RGB_565,
                    allowHardware = true
                )
            )
            .build()
    }
}
