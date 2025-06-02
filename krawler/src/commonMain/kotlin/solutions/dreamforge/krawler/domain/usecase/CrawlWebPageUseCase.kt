package solutions.dreamforge.krawler.domain.usecase

import kotlinx.datetime.Clock
import solutions.dreamforge.krawler.domain.model.CrawlMetrics
import solutions.dreamforge.krawler.domain.model.CrawlRequest
import solutions.dreamforge.krawler.domain.model.CrawlResult
import solutions.dreamforge.krawler.domain.model.CrawlStatus
import solutions.dreamforge.krawler.domain.model.ImageInfo
import solutions.dreamforge.krawler.domain.model.PageMetadata
import solutions.dreamforge.krawler.domain.model.WebPage
import solutions.dreamforge.krawler.domain.repository.CrawlRepository
import solutions.dreamforge.krawler.domain.service.ExtractionEngine
import solutions.dreamforge.krawler.domain.service.RobotsService
import solutions.dreamforge.krawler.http.HttpClient
import solutions.dreamforge.krawler.utils.Logger
import solutions.dreamforge.krawler.utils.currentTimeMillis
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

private val logger = Logger("CrawlWebPageUseCase")

/**
 * Use case for crawling a single web page
 */
class CrawlWebPageUseCase(
    private val httpClient: HttpClient,
    private val extractionEngine: ExtractionEngine,
    private val robotsService: RobotsService,
    private val repository: CrawlRepository
) {
    
    suspend fun execute(request: CrawlRequest): CrawlResult {
        val startTime = currentTimeMillis()
        val metrics = CrawlMetricsBuilder()
        
        try {
            logger.debug { "Starting crawl for ${request.url}" }
            
            // Check if recently crawled
            if (repository.wasRecentlyCrawled(request.url)) {
                logger.debug { "URL ${request.url} was recently crawled, skipping" }
                return CrawlResult(
                    request = request,
                    status = CrawlStatus.SKIPPED,
                    error = "Recently crawled",
                    timestamp = Clock.System.now(),
                    webPage = null,
                    metrics = metrics.build()
                )
            }
            
            // Check robots.txt
            if (request.crawlPolicy.respectRobotsTxt) {
                val allowed = robotsService.isAllowed(request.url, request.crawlPolicy.userAgent)
                if (!allowed) {
                    logger.info { "Robots.txt disallows crawling ${request.url}" }
                    return CrawlResult(
                        request = request,
                        status = CrawlStatus.ROBOTS_BLOCKED,
                        error = "Blocked by robots.txt",
                        timestamp = Clock.System.now(),
                        webPage = null,
                        metrics = metrics.build()
                    )
                }
            }
            //todo: fix warning about unused variable
            val networkTime = measureTime {
                val fetchResult = httpClient.fetch(request.url)
                metrics.downloadTimeMillis = currentTimeMillis() - startTime
                metrics.contentSizeBytes = fetchResult.body?.length?.toLong() ?: 0L
                
                if (!fetchResult.isSuccessful || fetchResult.body == null) {
                    logger.warn { "Failed to fetch ${request.url}: ${fetchResult.error}" }
                    return CrawlResult(
                        request = request,
                        status = CrawlStatus.FAILED,
                        error = fetchResult.error ?: "HTTP ${fetchResult.statusCode}",
                        timestamp = Clock.System.now(),
                        webPage = null,
                        metrics = metrics.build()
                    )
                }
                
                // Check content type - headers are now normalized to lowercase
                val contentType = fetchResult.headers["content-type"]?.firstOrNull() ?: ""
                
                // Log for debugging
                logger.debug { "Headers for ${request.url}: ${fetchResult.headers.keys.joinToString()}" }
                logger.debug { "Content-Type found: '$contentType'" }
                
                if (!isAllowedContentType(contentType, request.crawlPolicy.allowedContentTypes)) {
                    logger.info { "Unsupported content type '$contentType' for ${request.url}" }
                    return CrawlResult(
                        request = request,
                        status = CrawlStatus.UNSUPPORTED_CONTENT_TYPE,
                        error = "Unsupported content type: $contentType",
                        timestamp = Clock.System.now(),
                        webPage = null,
                        metrics = metrics.build()
                    )
                }
                
                // Check content length
                val contentLength = fetchResult.body.length.toLong()
                if (contentLength > request.crawlPolicy.maxContentLength) {
                    logger.info { "Content too large (${contentLength} bytes) for ${request.url}" }
                    return CrawlResult(
                        request = request,
                        status = CrawlStatus.CONTENT_TOO_LARGE,
                        error = "Content exceeds maximum size",
                        timestamp = Clock.System.now(),
                        webPage = null,
                        metrics = metrics.build()
                    )
                }
                var extractionTime =0.seconds

                    // Extract data
             extractionTime=  measureTime {
                    val extractedData = extractionEngine.extractData(
                        content = fetchResult.body,
                        contentType = contentType,
                        rules = request.extractionRules,
                        baseUrl = request.url
                    )
                    
                    val links = extractionEngine.extractLinks(fetchResult.body, request.url)
                    val images = extractionEngine.extractImages(fetchResult.body, request.url)
                    val metadata = extractionEngine.extractMetadata(fetchResult.body)
                    
                    metrics.extractionTimeMillis = extractionTime.inWholeMilliseconds
                    metrics.extractedFieldsCount = extractedData.size
                    
                    // Create web page
                    val webPage = WebPage(
                        url = request.url,
                        title = metadata["title"],
                        content = fetchResult.body,
                        html = fetchResult.body,
                        extractedData = extractedData,
                        links = links,
                        images = images.map { ImageInfo(url = it) },
                        metadata = PageMetadata(
                            statusCode = fetchResult.statusCode ?: 0,
                            contentType = contentType,
                            contentLength = contentLength,
                            headers = fetchResult.headers.mapValues { listOf(it.value.joinToString(", ")) },
                            charset = metadata["charset"],
                            language = metadata["language"]
                        ),
                        timestamp = Clock.System.now()
                    )
                    
                    // Generate new requests
                    val newRequests = generateNewCrawlRequests(request, links)
                    
                    // Create result
                    val result = CrawlResult(
                        request = request,
                        webPage = webPage,
                        status = CrawlStatus.SUCCESS,
                        newRequests = newRequests,
                        timestamp = Clock.System.now(),
                        metrics = metrics.build()
                    )
                    
                    // Save to repository
                    repository.saveCrawlResult(result)
                    repository.saveWebPage(webPage)
                    
                    logger.debug { "Successfully crawled ${request.url} in ${metrics.totalTimeMillis}ms" }
                    return result
                }
            }
            
            metrics.totalTimeMillis = currentTimeMillis() - startTime
            return CrawlResult(
                request = request,
                status = CrawlStatus.FAILED,
                error = "Unexpected error",
                timestamp = Clock.System.now(),
                webPage = null,
                metrics = metrics.build()
            )
            
        } catch (e: Exception) {
            logger.error(e) { "Error crawling ${request.url}" }
            return CrawlResult(
                request = request,
                status = CrawlStatus.FAILED,
                error = e.message ?: "Unknown error",
                timestamp = Clock.System.now(),
                webPage = null,
                metrics = metrics.build(totalTime = currentTimeMillis() - startTime)
            )
        }
    }
    
    private fun isAllowedContentType(contentType: String, allowedTypes: Set<String>): Boolean {
        // If content type is empty and we have HTML-like allowed types, accept it
        // Some servers don't send content-type headers
        if (contentType.isBlank()) {
            logger.warn { "Empty content type received, accepting by default" }
            return true
        }
        
        return allowedTypes.any { allowedType ->
            contentType.lowercase().contains(allowedType.lowercase())
        }
    }
    
    private fun generateNewCrawlRequests(parentRequest: CrawlRequest, links: Set<String>): List<CrawlRequest> {
        if (parentRequest.depth >= parentRequest.maxDepth) {
            return emptyList()
        }
        
        return links
            .filter { isValidUrl(it) }
            .filter { isSameDomain(parentRequest.url, it) }
            .take(100)
            .map { link ->
                parentRequest.copy(
                    id = generateRequestId(),
                    url = link,
                    depth = parentRequest.depth + 1,
                    timestamp = Clock.System.now()
                )
            }
    }
    
    private fun isValidUrl(url: String): Boolean {
        return try {
            url.startsWith("http://") || url.startsWith("https://")
        } catch (e: Exception) {
            false
        }
    }
    
    private fun isSameDomain(url1: String, url2: String): Boolean {
        return try {
            val domain1 = extractDomain(url1)
            val domain2 = extractDomain(url2)
            domain1 == domain2
        } catch (e: Exception) {
            false
        }
    }
    
    private fun extractDomain(url: String): String? {
        return try {
            val withoutProtocol = url.substringAfter("://")
            val host = withoutProtocol.substringBefore("/").substringBefore(":")
            host.lowercase()
        } catch (e: Exception) {
            null
        }
    }
    
    private fun generateRequestId(): String {
        return "req_${currentTimeMillis()}_${(1000..9999).random()}"
    }
}

private class CrawlMetricsBuilder {
    var downloadTimeMillis: Long = 0
    var parseTimeMillis: Long = 0
    var extractionTimeMillis: Long = 0
    var totalTimeMillis: Long = 0
    var contentSizeBytes: Long = 0
    var extractedFieldsCount: Int = 0
    
    fun build(totalTime: Long = totalTimeMillis) = CrawlMetrics(
        downloadTimeMillis = downloadTimeMillis,
        parseTimeMillis = parseTimeMillis,
        extractionTimeMillis = extractionTimeMillis,
        totalTimeMillis = totalTime,
        contentSizeBytes = contentSizeBytes,
        extractedFieldsCount = extractedFieldsCount
    )
}
