// File: app/src/main/java/com/tv/stream/playback/CloudTvPlayer.kt
package com.tv.stream.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.tv.stream.domain.orchestrator.LiveStream

@OptIn(UnstableApi::class)
class CloudTvPlayer(
    private val context: Context,
    private val onSourceChanged: (LiveStream, Int, Int) -> Unit,
    private val onAllSourcesExhausted: () -> Unit
) {
    var exoPlayer: ExoPlayer? = null
        private set

    private var streamQueue: List<LiveStream> = emptyList()
    private var currentIndex = 0
    private var savedPlaybackPositionMs = 0L

    fun setup() {
        // Strict 10s-20s buffer bounds (~15MB RAM ceiling) for 1GB Akai CloudTV518
        val lowMemControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(10_000, 20_000, 1_500, 2_000)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        exoPlayer = ExoPlayer.Builder(context)
            .setLoadControl(lowMemControl)
            .build().apply {
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        recoverNextSource()
                    }
                })
            }
    }

    fun load(streams: List<LiveStream>, resumePositionMs: Long = 0L) {
        streamQueue = streams
        currentIndex = 0
        savedPlaybackPositionMs = resumePositionMs
        executeStream()
    }

    private fun executeStream() {
        if (currentIndex >= streamQueue.size) {
            onAllSourcesExhausted()
            return
        }

        val activeStream = streamQueue[currentIndex]
        onSourceChanged(activeStream, currentIndex + 1, streamQueue.size)

        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (SmartTV; CloudTV518; AOSP)")
            .setConnectTimeoutMs(6000)
            .setReadTimeoutMs(6000)
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(activeStream.headers)

        val mediaSource = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpFactory)
            .createMediaSource(MediaItem.fromUri(activeStream.streamUrl))

        exoPlayer?.apply {
            setMediaSource(mediaSource)
            prepare()
            if (savedPlaybackPositionMs > 0L) {
                seekTo(savedPlaybackPositionMs)
            }
        }
    }

    private fun recoverNextSource() {
        savedPlaybackPositionMs = exoPlayer?.currentPosition ?: 0L
        currentIndex++
        executeStream()
    }
}
