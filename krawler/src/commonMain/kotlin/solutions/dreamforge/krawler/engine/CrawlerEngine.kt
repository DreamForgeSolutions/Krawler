package solutions.dreamforge.krawler.engine

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import solutions.dreamforge.krawler.domain.usecase.BatchCrawlUseCase
import solutions.dreamforge.krawler.domain.usecase.CrawlWebPageUseCase
import solutions.dreamforge.krawler.utils.StringFormat
import kotlin.time.Duration.Companion.milliseconds
import kotlin.collections.mutableListOf
import co.touchlab.kermit.Logger
import solutions.dreamforge.krawler.domain.model.CrawlMetrics
import solutions.dreamforge.krawler.domain.model.CrawlRequest
import solutions.dreamforge.krawler.domain.model.CrawlResult
import solutions.dreamforge.krawler.domain.model.CrawlStatus
import kotlin.concurrent.Volatile

private val logger = Logger.withTag("CrawlerEngine")

/**
 *  crawler engine with advanced scheduling and monitoring
 */
class CrawlerEngine(
    private val crawlWebPageUseCase: CrawlWebPageUseCase,
    private val batchCrawlUseCase: BatchCrawlUseCase,
    private val config: CrawlerEngineConfig = CrawlerEngineConfig()
) {
    
    private val requestQueue = Channel<CrawlRequest>(capacity = config.queueCapacity)
    private val resultChannel = Channel<CrawlResult>(capacity = config.resultBufferSize)
    
    @Volatile
    private var isRunning = false
    
    @Volatile
    private var totalRequests = 0L
    
    @Volatile
    private var processedRequests = 0L
    
    @Volatile
    private var successfulCrawls = 0L
    
    @Volatile
    private var failedCrawls = 0L
    
    @Volatile
    private var activeRequests = 0L
    
    private var engineJob: Job? = null
    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    /**
     * Start the crawler engine
     */
    fun start(): Flow<CrawlResult> {
        if (!isRunning) {
            isRunning = true
            logger.i { "Starting crawler engine with max concurrency ${config.maxConcurrency}" }
            
            engineJob = engineScope.launch {
                launchWorkers()
            }
            
            return resultChannel.receiveAsFlow()
        } else {
            throw IllegalStateException("Crawler engine is already running")
        }
    }

    /**
     * Execute a batch crawl with tracking
     */
    fun batchCrawl(
        requests: List<CrawlRequest>,
        maxConcurrency: Int = 50,
        batchId: String = "batch_${Clock.System.now().toEpochMilliseconds()}"
    ): Flow<CrawlResult> {
        logger.i { "Starting batch crawl $batchId with ${requests.size} requests" }
        return batchCrawlUseCase.execute(requests, maxConcurrency, batchId)
    }
    
    /**
     * Stop the crawler engine
     */
    suspend fun stop() {
        if (isRunning) {
            isRunning = false
            logger.i { "Stopping crawler engine..." }
            
            requestQueue.close()
            engineJob?.cancelAndJoin()
            resultChannel.close()
            engineScope.cancel()
            
            logger.i { "Crawler engine stopped" }
        }
    }
    
    /**
     * Submit a single crawl request
     */
    suspend fun submitRequest(request: CrawlRequest) {
        if (!isRunning) {
            throw IllegalStateException("Crawler engine is not running")
        }
        
        totalRequests++
        activeRequests++
        requestQueue.send(request)
        logger.d { "Submitted crawl request for ${request.url}" }
    }
    
    /**
     * Submit multiple crawl requests
     */
    suspend fun submitRequests(requests: List<CrawlRequest>) {
        if (!isRunning) {
            throw IllegalStateException("Crawler engine is not running")
        }
        
        val requestCount = requests.size.toLong()
        totalRequests += requestCount
        activeRequests += requestCount
        
        requests.forEach { request ->
            requestQueue.send(request)
        }
        
        logger.i { "Submitted ${requests.size} crawl requests" }
    }
    
    /**
     * Get current engine statistics
     */
    fun getStats(): CrawlerEngineStats {
        val successRate = if (processedRequests > 0) {
            (successfulCrawls.toDouble() / processedRequests) * 100
        } else 0.0
        
        return CrawlerEngineStats(
            isRunning = isRunning,
            totalRequests = totalRequests,
            processedRequests = processedRequests,
            successfulCrawls = successfulCrawls,
            failedCrawls = failedCrawls,
            queueSize = activeRequests,
            averageRequestsPerSecond = calculateRequestsPerSecond(),
            successRate = successRate
        )
    }
    
    private suspend fun launchWorkers() {
        coroutineScope {
            val workers = (1..config.maxConcurrency).map { workerId ->
                launch {
                    processRequests(workerId)
                }
            }
            launch {
                monitorProgress()
            }
            
            workers.joinAll()
        }
    }
    
    private suspend fun processRequests(workerId: Int) {
        logger.d { "Worker $workerId started" }
        
        try {
            for (request in requestQueue) {
                try {
                    val result = crawlWebPageUseCase.execute(request)
                    
                    processedRequests++
                    activeRequests--
                    
                    when (result.status) {
                        CrawlStatus.SUCCESS -> successfulCrawls++
                        else -> failedCrawls++
                    }
                    

                    recordRequestCompletion()
                    
                    resultChannel.send(result)

                    if (result.status == CrawlStatus.SUCCESS && result.newRequests.isNotEmpty()) {
                        result.newRequests.forEach { newRequest ->
                            if (requestQueue.trySend(newRequest).isSuccess) {
                                totalRequests++
                            }
                        }
                    }
                    
                } catch (e: Exception) {
                    logger.e(e) { "Worker $workerId error processing ${request.url}" }
                    
                    val errorResult = CrawlResult(
                        request = request,
                        webPage = null,
                        status = CrawlStatus.FAILED,
                        error = e.message ?: "Worker error",
                        timestamp = Clock.System.now(),
                        metrics = CrawlMetrics(
                            downloadTimeMillis = 0,
                            parseTimeMillis = 0,
                            extractionTimeMillis = 0,
                            totalTimeMillis = 0,
                            contentSizeBytes = 0,
                            extractedFieldsCount = 0
                        )
                    )
                    
                    failedCrawls++
                    processedRequests++
                    activeRequests--
                    
                    resultChannel.send(errorResult)
                }
            }
        } finally {
            logger.d { "Worker $workerId finished" }
        }
    }
    
    private suspend fun monitorProgress() {
        while (isRunning) {
            delay(config.progressReportInterval.milliseconds)
            
            val stats = getStats()
            if (stats.processedRequests > 0 && stats.processedRequests % 1000 == 0L) {
                logger.i { 
                    "Progress: ${stats.processedRequests}/${stats.totalRequests} " +
                    "(${StringFormat.format("%.1f", stats.successRate)}% success, " +
                    "${StringFormat.format("%.2f", stats.averageRequestsPerSecond)} req/s)"
                }
            }
        }
    }

    private val requestTimes = mutableListOf<Long>()
    private val slidingWindowSize = 1000 // Last 1000 requests
    private val windowDuration = 60_000L // 1 minute window in milliseconds
    
    private fun calculateRequestsPerSecond(): Double {
        val now = Clock.System.now().toEpochMilliseconds()
        // Remove timestamps outside the sliding window
        requestTimes.removeAll { it < now - windowDuration }
        
        val requestsInWindow = requestTimes.size
        return requestsInWindow / (windowDuration / 1000.0)
    }
    
    private fun recordRequestCompletion() {
        val now = Clock.System.now().toEpochMilliseconds()
        requestTimes.add(now)
        
        // Keep the queue bounded to prevent memory leaks
        while (requestTimes.size > slidingWindowSize) {
            requestTimes.removeAt(0)
        }
    }
}

/**
 * Statistics for the crawler engine
 */
data class CrawlerEngineStats(
    val isRunning: Boolean,
    val totalRequests: Long,
    val processedRequests: Long,
    val successfulCrawls: Long,
    val failedCrawls: Long,
    val queueSize: Long,
    val averageRequestsPerSecond: Double,
    val successRate: Double
) {
    val pendingRequests: Long
        get() = totalRequests - processedRequests
}
