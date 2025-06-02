package solutions.dreamforge.krawler.engine

/**
 * Configuration for the crawler engine
 */
data class CrawlerEngineConfig(
    val maxConcurrency: Int = 50,
    val queueCapacity: Int = 10000,
    val resultBufferSize: Int = 1000,
    val progressReportInterval: Long = 5000L,
    val respectRateLimits: Boolean = true,
    val defaultDelayMillis: Long = 1000L,
    val maxRetries: Int = 3,
    val retryDelayMillis: Long = 1000L
)
