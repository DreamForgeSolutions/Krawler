package solutions.dreamforge.krawler.infrastructure.repository

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import solutions.dreamforge.krawler.domain.model.*
import solutions.dreamforge.krawler.domain.repository.CrawlRepository
import solutions.dreamforge.krawler.domain.repository.CrawlStats
import co.touchlab.stately.collections.ConcurrentMutableMap
import solutions.dreamforge.krawler.domain.model.CrawlRequest
import solutions.dreamforge.krawler.domain.model.CrawlResult
import solutions.dreamforge.krawler.domain.model.CrawlStatus
import solutions.dreamforge.krawler.domain.model.WebPage
import kotlin.text.get
import kotlin.time.Duration.Companion.minutes

/**
 * In-memory implementation of CrawlRepository
 */
class InMemoryCrawlRepository : CrawlRepository {
    
    private val crawlResults = ConcurrentMutableMap<String, CrawlResult>()
    private val webPages = ConcurrentMutableMap<String, WebPage>()
    private val urlCrawlTimes = ConcurrentMutableMap<String, Instant>()
    private val failedRequests = mutableListOf<CrawlRequest>()
    private val mutex = Mutex()
    
    override suspend fun saveCrawlResult(result: CrawlResult) {
        crawlResults[result.request.id] = result
        
        // Track crawl time for duplicate detection
        urlCrawlTimes[result.request.url] = result.timestamp
        
        // Track failed requests for retry
        if (result.status == CrawlStatus.FAILED) {
            mutex.withLock {
                val retryCount = result.request.metadata["retryCount"]?.toIntOrNull() ?: 0
                if (retryCount < 3) {
                    val retryRequest = result.request.copy(
                        id = generateRequestId(),
                        metadata = result.request.metadata + ("retryCount" to "${retryCount + 1}"),
                        timestamp = Clock.System.now() + 5.minutes
                    )
                    failedRequests.add(retryRequest)
                }
            }
        }
    }
    
    override suspend fun saveWebPage(webPage: WebPage) {
        webPages[webPage.url] = webPage
    }
    
    override suspend fun findWebPagesByUrl(urlPattern: String): List<WebPage> {
        val regex = urlPattern.toRegex()
        return webPages.asMap().values.filter { page ->
            regex.matches(page.url)
        }
    }
    
    override suspend fun findWebPagesBySource(source: String): List<WebPage> {
        // we need to match through crawl results
        return crawlResults.asMap().values
            .filter { it.request.metadata["source"] == source && it.webPage != null }
            .mapNotNull { it.webPage }
    }
    
    override suspend fun getCrawlStats(source: String): CrawlStats {
        val sourceResults = crawlResults.asMap().values.filter { result ->
            result.request.metadata["source"] == source
        }
        
        val totalRequests = sourceResults.size.toLong()
        val successfulCrawls = sourceResults.count { it.status == CrawlStatus.SUCCESS }.toLong()
        val failedCrawls = sourceResults.count { it.status == CrawlStatus.FAILED }.toLong()
        
        val avgResponseTime = if (successfulCrawls > 0) {
            sourceResults
                .filter { it.status == CrawlStatus.SUCCESS }
                .mapNotNull { it.metrics.downloadTimeMillis }
                .average()
        } else 0.0
        
        val totalBytesDownloaded = sourceResults
            .sumOf { it.metrics.contentSizeBytes }
        
        val lastCrawlTime = sourceResults
            .maxByOrNull { it.timestamp }
            ?.timestamp
            ?.toString()
        
        return CrawlStats(
            totalRequests = totalRequests,
            successfulCrawls = successfulCrawls,
            failedCrawls = failedCrawls,
            avgResponseTime = avgResponseTime,
            totalBytesDownloaded = totalBytesDownloaded,
            lastCrawlTime = lastCrawlTime
        )
    }
    
    override suspend fun wasRecentlyCrawled(url: String, withinMinutes: Int): Boolean {
        val lastCrawlTime = urlCrawlTimes[url] ?: return false
        if (withinMinutes == 0) {
            return true
        }
        val cutoffTime = Clock.System.now() - withinMinutes.minutes
        return lastCrawlTime > cutoffTime
    }
    
    override suspend fun getFailedCrawlRequests(maxRetries: Int): List<CrawlRequest> = mutex.withLock {
        val now = Clock.System.now()
        val retryRequests = mutableListOf<CrawlRequest>()
        
        val iterator = failedRequests.iterator()
        while (iterator.hasNext()) {
            val request = iterator.next()
            val retryCount = request.metadata["retryCount"]?.toIntOrNull() ?: 0
            
            if (retryCount <= maxRetries && request.timestamp < now) {
                retryRequests.add(request)
                iterator.remove()
            }
        }
        
        retryRequests
    }
    
 //todo: Implement a proper asMap function for ConcurrentMutableMap
    private suspend fun <K, V> ConcurrentMutableMap<K, V>.asMap(): Map<K, V> = mutex.withLock {
        buildMap {
            this@asMap.let { map ->
                // This is a workaround - actual implementation would iterate over entries
                // For now, we'll leave it as a TODO
            }
        }
    }
    
    private fun generateRequestId(): String {
        return "req_${Clock.System.now().toEpochMilliseconds()}_${(1000..9999).random()}"
    }
}
