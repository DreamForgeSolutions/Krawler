package solutions.dreamforge.krawler.domain.repository

import solutions.dreamforge.krawler.domain.model.CrawlRequest
import solutions.dreamforge.krawler.domain.model.CrawlResult
import solutions.dreamforge.krawler.domain.model.WebPage

/**
 * Repository interface for managing crawl data
 */
interface CrawlRepository {
    
    /**
     * Save a crawl result
     */
    suspend fun saveCrawlResult(result: CrawlResult)
    
    /**
     * Save a web page
     */
    suspend fun saveWebPage(webPage: WebPage)
    
    /**
     * Find web pages by URL pattern
     */
    suspend fun findWebPagesByUrl(urlPattern: String): List<WebPage>
    
    /**
     * Find web pages by source
     */
    suspend fun findWebPagesBySource(source: String): List<WebPage>
    
    /**
     * Get crawl statistics for a source
     */
    suspend fun getCrawlStats(source: String): CrawlStats
    
    /**
     * Check if URL was recently crawled
     */
    suspend fun wasRecentlyCrawled(url: String, withinMinutes: Int = 60): Boolean
    
    /**
     * Get failed crawl requests for retry
     */
    suspend fun getFailedCrawlRequests(maxRetries: Int = 3): List<CrawlRequest>
}

data class CrawlStats(
    val totalRequests: Long,
    val successfulCrawls: Long,
    val failedCrawls: Long,
    val avgResponseTime: Double,
    val totalBytesDownloaded: Long,
    val lastCrawlTime: String?
)