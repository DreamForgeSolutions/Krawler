package solutions.dreamforge.krawler.domain.usecase

import co.touchlab.stately.collections.ConcurrentMutableMap
import kotlinx.atomicfu.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import solutions.dreamforge.krawler.domain.model.CrawlRequest
import solutions.dreamforge.krawler.domain.model.CrawlResult
import solutions.dreamforge.krawler.domain.model.CrawlStatus
import solutions.dreamforge.krawler.domain.model.CrawlMetrics
import solutions.dreamforge.krawler.domain.service.RobotsService
import solutions.dreamforge.krawler.utils.Logger
import solutions.dreamforge.krawler.utils.currentTimeMillis
import kotlin.text.get

private val logger = Logger("BatchCrawlUseCase")

/**
 * Use case for batch crawling multiple sources with high performance
 */
class BatchCrawlUseCase(
    private val crawlWebPageUseCase: CrawlWebPageUseCase,
    private val robotsService: RobotsService
) {
    
    private val domainDelays = ConcurrentMutableMap<String, Long>()
    private val lastCrawlTimes = ConcurrentMutableMap<String, Long>()
    
    /**
     * Crawl multiple requests with intelligent scheduling and rate limiting
     */
    fun execute(
        requests: List<CrawlRequest>, 
        maxConcurrency: Int = 50,
        batchId: String = "batch_${currentTimeMillis()}"
    ): Flow<CrawlResult> {
        logger.info { "Starting batch crawl $batchId of ${requests.size} requests with max concurrency $maxConcurrency" }
        
        return channelFlow {
            val requestChannel = Channel<CrawlRequest>(capacity = Channel.UNLIMITED)
            val resultChannel = Channel<CrawlResult>(capacity = 1000)
            
            // Submit all requests to the channel
            launch {
                requests.forEach { request ->
                    requestChannel.send(request)
                }
                requestChannel.close()
            }
            
            // Launch worker coroutines
            val workers = (1..maxConcurrency).map { workerId ->
                launch {
                    processRequests(workerId, requestChannel, resultChannel, batchId)
                }
            }
            

            launch {
                var completed = 0
                val total = requests.size
                
                for (result in resultChannel) {
                    send(result)
                    completed++
                    
                    if (completed % 100 == 0) {
                        logger.info { "Batch $batchId: Completed $completed/$total crawl requests" }
                    }
                    
                    if (completed >= total) {
                        break
                    }
                }
                
                workers.forEach { it.cancel() }
            }

            workers.joinAll()
            resultChannel.close()
        }.flowOn(Dispatchers.IO)
         .buffer(1000)
    }
    
    private suspend fun processRequests(
        workerId: Int,
        requestChannel: Channel<CrawlRequest>,
        resultChannel: Channel<CrawlResult>,
        batchId: String
    ) {
        logger.debug { "Worker $workerId starting for batch $batchId" }
        
        try {
            for (request in requestChannel) {
                try {
                    val delay = getDomainDelay(request.url, request.crawlPolicy.userAgent)
                    if (delay > 0) {
                        delay(delay)
                    }

                    updateLastCrawlTime(request.url)

                    val result = crawlWebPageUseCase.execute(request)
                    

                    resultChannel.send(result)

                    if (result.status == CrawlStatus.SUCCESS && result.newRequests.isNotEmpty()) {
                        result.newRequests.forEach { newRequest ->
                            requestChannel.trySend(newRequest)
                        }
                    }
                    
                } catch (e: Exception) {
                    logger.error(e) { "Worker $workerId error processing ${request.url} in batch $batchId" }
                    
                    val errorResult = CrawlResult(
                        request = request,
                        status = CrawlStatus.FAILED,
                        error = e.message ?: "Worker error",
                        timestamp = Clock.System.now(),
                        webPage = null,
                        metrics = CrawlMetrics(
                            downloadTimeMillis = 0,
                            parseTimeMillis = 0,
                            extractionTimeMillis = 0,
                            totalTimeMillis = 0,
                            contentSizeBytes = 0,
                            extractedFieldsCount = 0
                        )
                    )
                    
                    resultChannel.send(errorResult)
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Worker $workerId terminated with error in batch $batchId" }
        } finally {
            logger.debug { "Worker $workerId finished for batch $batchId" }
        }
    }
    
    private suspend fun getDomainDelay(url: String, userAgent: String): Long {
        val domain = extractDomain(url) ?: return 1000L
        

        val cachedDelay = domainDelays[domain]
        if (cachedDelay != null) {
            return calculateActualDelay(domain, cachedDelay)
        }
        

        val robotsDelay = try {
            robotsService.getCrawlDelay(domain, userAgent) ?: 1000L
        } catch (e: Exception) {
            logger.warn(e) { "Failed to get robots delay for $domain" }
            1000L
        }
        
        // Cache the delay
        domainDelays[domain] = robotsDelay
        
        return calculateActualDelay(domain, robotsDelay)
    }
    
    private fun calculateActualDelay(domain: String, baseDelay: Long): Long {
        val lastCrawlTime = lastCrawlTimes[domain] ?: 0L
        val currentTime = currentTimeMillis()
        val timeSinceLastCrawl = currentTime - lastCrawlTime
        
        return maxOf(0L, baseDelay - timeSinceLastCrawl)
    }
    
    private fun updateLastCrawlTime(url: String) {
        val domain = extractDomain(url)
        if (domain != null) {
            lastCrawlTimes[domain] = currentTimeMillis()
        }
    }

    //todo make this more robust
    private fun extractDomain(url: String): String? {
        return try {
            val withoutProtocol = url.substringAfter("://")
            val host = withoutProtocol.substringBefore("/").substringBefore(":")
            host.lowercase()
        } catch (e: Exception) {
            null
        }
    }
}
