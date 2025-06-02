package solutions.dreamforge.krawler

import solutions.dreamforge.krawler.engine.CrawlerEngineConfig

/**
 * Configuration for the multiplatform SDK
 */
data class SDKConfiguration(
    val userAgent: String = "DreamForge-Crawler/1.0",
    val connectTimeoutSeconds: Long = 10,
    val readTimeoutSeconds: Long = 30,
    val followRedirects: Boolean = true,
    val maxConcurrency: Int = 50,
    val queueCapacity: Int = 10000,
    val resultBufferSize: Int = 1000,
    val progressReportInterval: Long = 5000L
) {
    internal fun toCrawlerEngineConfig() = CrawlerEngineConfig(
        maxConcurrency = maxConcurrency,
        queueCapacity = queueCapacity,
        resultBufferSize = resultBufferSize,
        progressReportInterval = progressReportInterval
    )
}
