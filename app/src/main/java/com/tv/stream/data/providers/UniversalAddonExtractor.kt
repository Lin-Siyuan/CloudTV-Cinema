// File: app/src/main/java/com/tv/stream/data/providers/UniversalAddonExtractor.kt
package com.tv.stream.data.providers

import com.tv.stream.domain.orchestrator.CodecType
import com.tv.stream.domain.orchestrator.LiveStream
import com.tv.stream.domain.orchestrator.MediaTarget
import com.tv.stream.domain.orchestrator.StreamExtractor
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Connects your Akai CloudTV518 directly to:
 *  1. Any AIOStreams / Stremio Addon URL (Zero TV scraping RAM)
 *  2. CinePro Core (with STREMIO_ADDON=true or OMSS bridge)
 *  3. Local Chaos Test Server (http://10.0.2.2:17000)
 */
class UniversalAddonExtractor(
    override val tag: String,
    override val basePriority: Int,
    private val baseUrl: String
) : StreamExtractor {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(3000, TimeUnit.MILLISECONDS)
        .readTimeout(3500, TimeUnit.MILLISECONDS)
        .followRedirects(true)
        .build()

    override suspend fun extract(target: MediaTarget): List<LiveStream> {
        val imdbId = target.imdbId ?: return emptyList()
        val endpoint = if (target.season != null && target.episode != null) {
            "${baseUrl.trimEnd('/')}/stream/series/$imdbId:${target.season}:${target.episode}.json"
        } else {
            "${baseUrl.trimEnd('/')}/stream/movie/$imdbId.json"
        }

        val request = Request.Builder()
            .url(endpoint)
            .header("User-Agent", "CloudTV518-LeanClient/1.0")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val bodyString = response.body?.string() ?: return emptyList()
            return parseStreamsJson(bodyString)
        }
    }

    private fun parseStreamsJson(rawJson: String): List<LiveStream> {
        val root = JSONObject(rawJson)
        val streamsArray = root.optJSONArray("streams") ?: return emptyList()
        val resolved = ArrayList<LiveStream>(streamsArray.length())

        for (i in 0 until streamsArray.length()) {
            val item = streamsArray.optJSONObject(i) ?: continue
            val url = item.optString("url", "")
            if (url.isBlank()) continue

            val name = item.optString("name", tag)
            val title = item.optString("title", "")
            val hints = item.optJSONObject("behaviorHints")

            // Hardware VPU Whitelist: H.264 & H.265 only (Drop AV1)
            val codecHint = (hints?.optString("videoCodec", "") ?: "$name $title").lowercase()
            val codec = when {
                codecHint.contains("av1") || codecHint.contains("vp9") -> CodecType.UNSUPPORTED
                codecHint.contains("265") || codecHint.contains("hevc") -> CodecType.H265
                else -> CodecType.H264
            }

            // Cap resolution at 1080p for Mali-G31 MP2 & 128MB Heap
            val resolution = hints?.optInt("resolution", 0)?.takeIf { it > 0 } ?: when {
                title.contains("2160p") || title.contains("4k", ignoreCase = true) -> 2160
                title.contains("1080p") -> 1080
                title.contains("720p") -> 720
                else -> 1080
            }
            if (resolution > 1080) continue

            val headersMap = mutableMapOf<String, String>()
            val reqHeaders = hints?.optJSONObject("proxyHeaders")?.optJSONObject("request")
            reqHeaders?.keys()?.forEach { key ->
                headersMap[key] = reqHeaders.optString(key, "")
            }

            resolved.add(
                LiveStream(
                    providerTag = "$tag • $name",
                    streamUrl = url,
                    headers = headersMap,
                    resolution = resolution,
                    codec = codec
                )
            )
        }
        return resolved
    }
}
