package solutions.dreamforge.krawler.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CrawlRequest(
    val id: String,
    val url: String,
    val depth: Int = 0,
    val maxDepth: Int = 3,
    val extractionRules: List<ExtractionRule> = emptyList(),
    val crawlPolicy: CrawlPolicy = CrawlPolicy(),
    val priority: Priority = Priority.NORMAL,
    val metadata: Map<String, String> = emptyMap(),
    val parentRequestId: String? = null,
    val timestamp: Instant
) {
    @Serializable
    enum class Priority {
        LOW, NORMAL, HIGH, URGENT
    }
    
    fun createChildRequest(childUrl: String, additionalRules: List<ExtractionRule> = emptyList()): CrawlRequest {
        return copy(
            id = "${id}_${childUrl.hashCode()}",
            url = childUrl,
            depth = depth + 1,
            extractionRules = extractionRules + additionalRules,
            parentRequestId = id
        )
    }
}

@Serializable
data class CrawlPolicy(
    val respectRobotsTxt: Boolean = true,
    val delayMillis: Long = 1000,
    val maxRetries: Int = 3,
    val timeoutMillis: Long = 30000,
    val userAgent: String = "DreamForge-Crawler/1.0",
    val maxContentLength: Long = 10 * 1024 * 1024, // 10MB
    val allowedContentTypes: Set<String> = setOf("text/html", "application/xhtml+xml"),
    val headers: Map<String, String> = emptyMap(),
    val followRedirects: Boolean = true,
    val maxRedirects: Int = 5
)

@Serializable
data class CrawlResult(
    val request: CrawlRequest,
    val webPage: WebPage?,
    val status: CrawlStatus,
    val error: String? = null,
    val newRequests: List<CrawlRequest> = emptyList(),
    val timestamp: Instant,
    val metrics: CrawlMetrics
)

//todo
@Serializable
enum class CrawlStatus {
    SUCCESS,
    FAILED,
    SKIPPED,
    ROBOTS_BLOCKED,
    TIMEOUT,
    TOO_MANY_RETRIES,
    CONTENT_TOO_LARGE,
    UNSUPPORTED_CONTENT_TYPE,
    NETWORK_ERROR,
    PARSE_ERROR
}

@Serializable
data class CrawlMetrics(
    val downloadTimeMillis: Long,
    val parseTimeMillis: Long,
    val extractionTimeMillis: Long,
    val totalTimeMillis: Long,
    val contentSizeBytes: Long,
    val extractedFieldsCount: Int
)

@Serializable
data class WebPage(
    val url: String,
    val title: String? = null,
    val content: String? = null,
    val html: String,
    val extractedData: Map<String, ExtractedValue> = emptyMap(),
    val links: Set<String> = emptySet(),
    val images: List<ImageInfo> = emptyList(),
    val metadata: PageMetadata,
    val timestamp: Instant
)

@Serializable
data class PageMetadata(
    val statusCode: Int,
    val contentType: String? = null,
    val contentLength: Long? = null,
    val headers: Map<String, List<String>> = emptyMap(),
    val language: String? = null,
    val charset: String? = null,
    val lastModified: Instant? = null,
    val etag: String? = null
)

@Serializable
data class ImageInfo(
    val url: String,
    val alt: String? = null,
    val width: Int? = null,
    val height: Int? = null
)

/**
 * Repository statistics for tracking crawl performance
 */
@Serializable
data class CrawlStats(
    val source: String,
    val totalRequests: Long,
    val successfulRequests: Long,
    val failedRequests: Long,
    val averageResponseTimeMillis: Double,
    val totalContentSizeBytes: Long,
    val lastCrawlTime: Instant?
)
