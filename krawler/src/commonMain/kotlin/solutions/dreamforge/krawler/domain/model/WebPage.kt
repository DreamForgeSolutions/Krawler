package solutions.dreamforge.krawler.domain.model

import kotlinx.datetime.Instant


val CrawlRequest.Priority.value: Int
    get() = when (this) {
        CrawlRequest.Priority.LOW -> 1
        CrawlRequest.Priority.NORMAL -> 5
        CrawlRequest.Priority.HIGH -> 10
        CrawlRequest.Priority.URGENT -> 20
    }

val CrawlRequest.scheduledAt: Instant
    get() = timestamp

val CrawlRequest.source: String?
    get() = metadata["source"]


val CrawlPolicy.delayBetweenRequests: Long
    get() = delayMillis


val CrawlResult.completedAt: Instant
    get() = timestamp

//todo calculate this from metrics
val PageMetadata.responseTime: Long?
    get() = null // This would need to be calculated from metrics


val WebPage.finalUrl: String
    get() = url

//todo: this is not stored in multiplatform version, but should be added
val WebPage.crawlRequestId: String?
    get() = null
//todo: this is not stored in multiplatform version, but should be added
val WebPage.depth: Int
    get() = 0

//todo: this is not stored in multiplatform version, but should be added
val WebPage.source: String?
    get() = null
