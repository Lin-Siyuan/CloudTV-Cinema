// File: app/src/main/java/com/tv/stream/domain/orchestrator/ResilientStreamOrchestrator.kt
package com.tv.stream.domain.orchestrator

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

enum class CodecType { H264, H265, UNSUPPORTED }

data class MediaTarget(
    val tmdbId: String,
    val imdbId: String?,
    val title: String,
    val season: Int? = null,
    val episode: Int? = null
)

data class LiveStream(
    val providerTag: String,
    val streamUrl: String,
    val headers: Map<String, String> = emptyMap(),
    val resolution: Int,
    val codec: CodecType,
    val pingMs: Long = 0L
)

interface StreamExtractor {
    val tag: String
    val basePriority: Int
    suspend fun extract(target: MediaTarget): List<LiveStream>
}

class ResilientStreamOrchestrator(
    private val extractors: List<StreamExtractor>
) {
    // Strictly isolate CPU execution to 2 threads to protect 1GB RAM
    private val resolverContext = Dispatchers.IO.limitedParallelism(2)
    private val circuitBreaker = ConcurrentHashMap<String, Int>()

    suspend fun resolveBestStreams(target: MediaTarget): List<LiveStream> = withContext(resolverContext) {
        val operational = extractors
            .filter { (circuitBreaker[it.tag] ?: 0) < 3 }
            .sortedByDescending { it.basePriority }

        val candidates = mutableListOf<LiveStream>()

        val tasks = operational.take(2).map { extractor ->
            async {
                val start = System.currentTimeMillis()
                try {
                    withTimeout(3500L) {
                        val results = extractor.extract(target)
                        val elapsed = System.currentTimeMillis() - start
                        circuitBreaker[extractor.tag] = 0
                        results.map { it.copy(pingMs = elapsed) }
                    }
                } catch (e: Exception) {
                    circuitBreaker[extractor.tag] = (circuitBreaker[extractor.tag] ?: 0) + 1
                    emptyList()
                }
            }
        }

        tasks.awaitAll().forEach { candidates.addAll(it) }

        return@withContext candidates
            .filter { it.codec != CodecType.UNSUPPORTED && it.resolution <= 1080 }
            .sortedWith(
                compareByDescending<LiveStream> { it.resolution }
                    .thenBy { it.pingMs }
            )
    }

    fun penalizeProvider(tag: String) {
        circuitBreaker[tag] = (circuitBreaker[tag] ?: 0) + 1
    }
}
