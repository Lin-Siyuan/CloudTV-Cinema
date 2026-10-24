package com.tv.stream

import android.os.Bundle
import android.view.KeyEvent
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.ui.PlayerView
import com.tv.stream.data.providers.UniversalAddonExtractor
import com.tv.stream.domain.orchestrator.MediaTarget
import com.tv.stream.domain.orchestrator.ResilientStreamOrchestrator
import com.tv.stream.playback.CloudTvPlayer
import com.tv.stream.ui.components.CinemaFocusCard
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var statusText: TextView
    private lateinit var cloudPlayer: CloudTvPlayer
    private lateinit var orchestrator: ResilientStreamOrchestrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.player_view)
        statusText = findViewById(R.id.status_text)

        // Initialize Resilient Stream Orchestrator with default extractors
        val extractors = listOf(
            UniversalAddonExtractor("AIOStreams", 100, "https://aiostreams.elfhosted.com"),
            UniversalAddonExtractor("CinePro-OMSS", 90, "https://vidsrc.to"),
            UniversalAddonExtractor("CloudStream-Direct", 80, "https://commondatastorage.googleapis.com")
        )
        orchestrator = ResilientStreamOrchestrator(extractors)

        // Setup Media3 CloudTvPlayer with bounded low-memory load control
        cloudPlayer = CloudTvPlayer(
            context = this,
            onSourceChanged = { stream, index, total ->
                runOnUiThread {
                    statusText.text = "Active Mirror: #$index/$total ${stream.providerTag} (${stream.resolution}p ${stream.codec})"
                }
            },
            onAllSourcesExhausted = {
                runOnUiThread {
                    Toast.makeText(this, "All stream mirrors exhausted.", Toast.LENGTH_LONG).show()
                }
            }
        )
        cloudPlayer.setup()
        playerView.player = cloudPlayer.exoPlayer

        // Wire up sample playable media cards
        val card1 = findViewById<CinemaFocusCard>(R.id.card_bunny)
        val card2 = findViewById<CinemaFocusCard>(R.id.card_sintel)
        val card3 = findViewById<CinemaFocusCard>(R.id.card_tears)

        card1.setOnClickListener {
            playMedia(MediaTarget(tmdbId = "bunny", imdbId = "tt0816692", title = "Big Buck Bunny"))
        }
        card2.setOnClickListener {
            playMedia(MediaTarget(tmdbId = "sintel", imdbId = "tt1727587", title = "Sintel"))
        }
        card3.setOnClickListener {
            playMedia(MediaTarget(tmdbId = "tears", imdbId = "tt2285752", title = "Tears of Steel"))
        }

        // Set initial focus
        card1.requestFocus()
    }

    private fun playMedia(target: MediaTarget) {
        statusText.text = "Resolving streams for ${target.title} via orchestrator..."
        lifecycleScope.launch {
            try {
                val streams = orchestrator.resolveBestStreams(target)
                if (streams.isNotEmpty()) {
                    cloudPlayer.load(streams)
                } else {
                    // Fallback to high-reliability benchmark video if addon endpoint is unreachable
                    val fallbackStreams = listOf(
                        com.tv.stream.domain.orchestrator.LiveStream(
                            providerTag = "Direct-Fallback",
                            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                            resolution = 1080,
                            codec = com.tv.stream.domain.orchestrator.CodecType.H264
                        )
                    )
                    cloudPlayer.load(fallbackStreams)
                }
            } catch (e: Exception) {
                statusText.text = "Error resolving stream: ${e.message}"
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // TV Key Normalizer: handle standard Android TV Remote keys
        when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> {
                val player = cloudPlayer.exoPlayer
                if (player != null) {
                    player.playWhenReady = !player.playWhenReady
                    return true
                }
            }
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                cloudPlayer.exoPlayer?.let { it.seekTo(it.currentPosition + 10_000) }
                return true
            }
            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                cloudPlayer.exoPlayer?.let { it.seekTo((it.currentPosition - 10_000).coerceAtLeast(0)) }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onStop() {
        super.onStop()
        cloudPlayer.exoPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        cloudPlayer.exoPlayer?.release()
    }
}
