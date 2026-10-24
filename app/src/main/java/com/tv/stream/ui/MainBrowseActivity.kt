// File: app/src/main/java/com/tv/stream/ui/MainBrowseActivity.kt
// Complete D-Pad TV Browse & Player Activity (Programmatic UI — Zero XML inflation overhead)
package com.tv.stream.ui

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.ui.PlayerView
import com.tv.stream.data.providers.UniversalAddonExtractor
import com.tv.stream.domain.orchestrator.LiveStream
import com.tv.stream.domain.orchestrator.MediaTarget
import com.tv.stream.domain.orchestrator.ResilientStreamOrchestrator
import com.tv.stream.playback.CloudTvPlayer
import com.tv.stream.ui.components.CinemaFocusCard
import com.tv.stream.ui.helper.TvKeyNormalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MainBrowseActivity : FragmentActivity() {

    // Default Addon Base URL (Can be changed in-app or pre-baked before ZIP download)
    private var addonBaseUrl: String = "https://aiostreams.elfhosted.com"

    private lateinit var rootFrame: FrameLayout
    private lateinit var browseContainer: LinearLayout
    private lateinit var heroTitleView: TextView
    private lateinit var heroMetaView: TextView
    private lateinit var carouselRowLayout: LinearLayout
    private lateinit var statusBannerView: TextView
    private lateinit var playerView: PlayerView
    private lateinit var cloudTvPlayer: CloudTvPlayer
    private lateinit var orchestrator: ResilientStreamOrchestrator

    private var activeStreams: List<LiveStream> = emptyList()
    private var activeStreamIdx: Int = 0
    private var isPlayerVisible: Boolean = false

    private val defaultCatalog = listOf(
        MediaTarget("101", "tt0816692", "Interstellar (1080p Hardware Test)"),
        MediaTarget("102", "tt15239678", "Dune: Part Two (Multi-Mirror)"),
        MediaTarget("103", "tt0133093", "The Matrix (H.264 Direct)"),
        MediaTarget("104", "tt4154796", "Avengers: Endgame (1080p)"),
        MediaTarget("105", "tt0903747", "Breaking Bad S1:E1", season = 1, episode = 1),
        MediaTarget("106", "tt0944947", "Game of Thrones S1:E1", season = 1, episode = 1)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("cloudtv_prefs", MODE_PRIVATE)
        addonBaseUrl = prefs.getString("addon_base_url", addonBaseUrl) ?: addonBaseUrl

        rebuildOrchestrator()
        buildProgrammaticTenFootUi()
        populateCatalogCards(defaultCatalog)
        fetchLiveCatalogFromAddon()
    }

    private fun rebuildOrchestrator() {
        orchestrator = ResilientStreamOrchestrator(
            listOf(
                UniversalAddonExtractor("Primary-Addon", 100, addonBaseUrl),
                UniversalAddonExtractor("Cinemeta-Fallback", 80, "https://v3-cinemeta.strem.io")
            )
        )
    }

    private fun buildProgrammaticTenFootUi() {
        rootFrame = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#06080D"))
        }

        // 1. 10-Foot Browse Container with 5% Overscan Safe Margin (42px)
        browseContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 36, 48, 36)
        }

        // Top Header Bar
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val logoTitle = TextView(this).apply {
            text = "CLOUDTV CINEMA • AKAI 518 (armeabi-v7a)"
            textSize = 20f
            setTextColor(Color.parseColor("#00E5FF"))
        }
        val configBtn = Button(this).apply {
            text = "⚙ Set Addon URL"
            setOnClickListener { promptAddonUrlDialog() }
        }
        headerRow.addView(logoTitle, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        headerRow.addView(configBtn)
        browseContainer.addView(headerRow)

        // Hero Billboard Section
        heroTitleView = TextView(this).apply {
            text = "SELECT ANY TITLE TO STREAM"
            textSize = 28f
            setTextColor(Color.WHITE)
            setPadding(0, 28, 0, 8)
        }
        heroMetaView = TextView(this).apply {
            text = "Hardware Decoder: Mali-G31 MP2 (H.264/HEVC) • Max RAM Budget: <120MB PSS"
            textSize = 14f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 0, 0, 24)
        }
        browseContainer.addView(heroTitleView)
        browseContainer.addView(heroMetaView)

        // Horizontal D-Pad Carousel Row
        val scrollRow = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        carouselRowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        scrollRow.addView(carouselRowLayout)
        browseContainer.addView(scrollRow)

        // Status Telemetry Footer
        statusBannerView = TextView(this).apply {
            text = "Ready. Use D-Pad Arrows + OK to launch stream. Press DOWN during playback to switch servers."
            textSize = 13f
            setTextColor(Color.parseColor("#34D399"))
            setPadding(0, 28, 0, 0)
        }
        browseContainer.addView(statusBannerView)

        // 2. Full-Screen Hardware ExoPlayer Overlay
        playerView = PlayerView(this).apply {
            useController = true
            visibility = View.GONE
            setBackgroundColor(Color.BLACK)
        }

        cloudTvPlayer = CloudTvPlayer(
            context = this,
            onSourceChanged = { stream, idx, total ->
                runOnUiThread {
                    statusBannerView.text = "Playing Mirror $idx/$total: ${stream.providerTag} (${stream.resolution}p ${stream.codec})"
                    Toast.makeText(
                        this,
                        "Mirror $idx/$total: ${stream.providerTag} (${stream.resolution}p)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onAllSourcesExhausted = {
                runOnUiThread {
                    statusBannerView.text = "No playable 1080p H.264/H.265 streams returned. Check your Addon URL."
                    Toast.makeText(this, "All mirrors exhausted. Check Addon URL.", Toast.LENGTH_LONG).show()
                    exitPlayerOverlay()
                }
            }
        ).also {
            it.setup()
            playerView.player = it.exoPlayer
        }

        rootFrame.addView(browseContainer, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        rootFrame.addView(playerView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(rootFrame)
    }

    private fun populateCatalogCards(items: List<MediaTarget>) {
        carouselRowLayout.removeAllViews()
        items.forEachIndexed { index, item ->
            val card = CinemaFocusCard(this).apply {
                setBackgroundColor(Color.parseColor("#111827"))
                setPadding(24, 24, 24, 24)
                val cardText = TextView(context).apply {
                    text = "${item.title}\n\n[IMDB: ${item.imdbId}]\n▶ Press OK to Stream"
                    textSize = 15f
                    setTextColor(Color.WHITE)
                }
                addView(cardText)
                setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        heroTitleView.text = item.title
                        heroMetaView.text = "IMDB ID: ${item.imdbId} • Querying: $addonBaseUrl"
                    }
                }
                setOnClickListener {
                    resolveAndPlayTarget(item)
                }
            }
            val lp = LinearLayout.LayoutParams(360, 220).apply {
                rightMargin = 24
            }
            carouselRowLayout.addView(card, lp)
            if (index == 0) card.requestFocus()
        }
    }

    private fun fetchLiveCatalogFromAddon() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder().connectTimeout(4, TimeUnit.SECONDS).build()
                val req = Request.Builder().url("https://v3-cinemeta.strem.io/catalog/movie/top.json").build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@launch
                    val body = resp.body?.string() ?: return@launch
                    val metas = JSONObject(body).optJSONArray("metas") ?: return@launch
                    val liveItems = mutableListOf<MediaTarget>()
                    for (i in 0 until minOf(16, metas.length())) {
                        val m = metas.optJSONObject(i) ?: continue
                        val imdb = m.optString("imdb_id", m.optString("id", ""))
                        val name = m.optString("name", "Movie #${i + 1}")
                        if (imdb.startsWith("tt")) {
                            liveItems.add(MediaTarget(imdb, imdb, name))
                        }
                    }
                    if (liveItems.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            populateCatalogCards(liveItems)
                            statusBannerView.text = "Loaded ${liveItems.size} live Trending Movies from Cinemeta + Addon Engine."
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun resolveAndPlayTarget(target: MediaTarget) {
        statusBannerView.text = "Resolving 1080p H.264/H.265 mirrors for ${target.title}..."
        lifecycleScope.launch {
            val resolved = orchestrator.resolveBestStreams(target)
            val fallbackDemo = listOf(
                LiveStream(
                    providerTag = "Fallback-Demo-1080p",
                    streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    resolution = 1080,
                    codec = com.tv.stream.domain.orchestrator.CodecType.H264
                )
            )
            activeStreams = if (resolved.isNotEmpty()) resolved else fallbackDemo
            activeStreamIdx = 0
            isPlayerVisible = true
            browseContainer.visibility = View.GONE
            playerView.visibility = View.VISIBLE
            cloudTvPlayer.load(activeStreams)
        }
    }

    private fun exitPlayerOverlay() {
        isPlayerVisible = false
        cloudTvPlayer.exoPlayer?.pause()
        playerView.visibility = View.GONE
        browseContainer.visibility = View.VISIBLE
    }

    private fun promptAddonUrlDialog() {
        val input = EditText(this).apply {
            setText(addonBaseUrl)
            setTextColor(Color.BLACK)
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("Configure Stremio / AIOStreams Base URL")
            .setMessage("Paste your AIOStreams or CinePro base URL (without /manifest.json):")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                addonBaseUrl = input.text.toString().trim().removeSuffix("/manifest.json").removeSuffix("/")
                getSharedPreferences("cloudtv_prefs", MODE_PRIVATE).edit().putString("addon_base_url", addonBaseUrl).apply()
                rebuildOrchestrator()
                statusBannerView.text = "Updated Addon Base URL: $addonBaseUrl"
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isPlayerVisible) {
            if (TvKeyNormalizer.isBackKey(keyCode)) {
                exitPlayerOverlay()
                return true
            }
            if (TvKeyNormalizer.isQuickSourceMenuKey(keyCode) && activeStreams.size > 1) {
                activeStreamIdx = (activeStreamIdx + 1) % activeStreams.size
                cloudTvPlayer.switchManualSource(activeStreamIdx)
                return true
            }
        } else if (TvKeyNormalizer.isBackKey(keyCode)) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        cloudTvPlayer.release()
    }
}
