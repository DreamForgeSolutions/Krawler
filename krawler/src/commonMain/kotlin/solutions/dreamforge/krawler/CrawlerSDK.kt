package solutions.dreamforge.krawler

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Clock
import solutions.dreamforge.krawler.domain.model.CrawlRequest
import solutions.dreamforge.krawler.domain.model.CrawlResult
import solutions.dreamforge.krawler.domain.repository.CrawlRepository
import solutions.dreamforge.krawler.domain.service.ExtractionEngine
import solutions.dreamforge.krawler.domain.service.RobotsService
import solutions.dreamforge.krawler.domain.usecase.BatchCrawlUseCase
import solutions.dreamforge.krawler.domain.usecase.CrawlWebPageUseCase
import solutions.dreamforge.krawler.dsl.CrawlerConfiguration
import solutions.dreamforge.krawler.engine.CrawlerEngine
import solutions.dreamforge.krawler.http.HttpClient
import solutions.dreamforge.krawler.infrastructure.extraction.DefaultPostProcessorService
import solutions.dreamforge.krawler.infrastructure.extraction.KsoupExtractionEngine
import solutions.dreamforge.krawler.infrastructure.repository.InMemoryCrawlRepository
import solutions.dreamforge.krawler.infrastructure.robots.RobotService
import co.touchlab.kermit.Logger
import kotlin.time.Duration.Companion.seconds

private val logger = Logger.withTag("CrawlerSDK")

/**
 * Main SDK facade providing a simple interface for web crawling
 */
class CrawlerSDK private constructor(
    private val httpClient: HttpClient,
    private val extractionEngine: ExtractionEngine,
    private val robotsService: RobotsService,
    private val repository: CrawlRepository,
    private val crawlerEngine: CrawlerEngine
) {

    /**
     * Execute a crawler configuration and return a flow of results
     */
    suspend fun crawl(configuration: CrawlerConfiguration): Flow<CrawlResult> {
        logger.i { "Starting crawl with configuration: ${configuration.name}" }

        val requests = configuration.build()
        logger.i { "Generated ${requests.size} initial crawl requests" }

        val resultFlow = crawlerEngine.start()

        // Submit all requests
        crawlerEngine.submitRequests(requests)

        return resultFlow.onEach { result ->
            logger.d { "Received result for ${result.request.url}: ${result.status}" }
        }
    }

    /**
     * Execute a single crawl request
     */
    suspend fun crawlSingle(request: CrawlRequest): CrawlResult {
        val crawlUseCase = CrawlWebPageUseCase(httpClient, extractionEngine, robotsService, repository)
        return crawlUseCase.execute(request)
    }

    /**
     * Execute a batch crawl with efficient scheduling and tracking
     */
    suspend fun batchCrawl(
        requests: List<CrawlRequest>,
        maxConcurrency: Int = 50,
        batchId: String = "batch_${Clock.System.now().toEpochMilliseconds()}"
    ): Flow<CrawlResult> {
        logger.i { "Starting batch crawl $batchId with ${requests.size} requests" }
        return crawlerEngine.batchCrawl(requests, maxConcurrency, batchId)
    }

    /**
     * Get crawler engine statistics
     */
    fun getStats() = crawlerEngine.getStats()

    /**
     * Get repository statistics
     */
    suspend fun getRepositoryStats(source: String) = repository.getCrawlStats(source)

    /**
     * Stop the crawler engine
     */
    suspend fun stop() {
        logger.i { "Stopping crawler SDK" }
        crawlerEngine.stop()
    }

    companion object {
        /**
         * Create a new CrawlerSDK instance with default configuration
         */
        fun create(config: SDKConfiguration = SDKConfiguration()): CrawlerSDK {
            val httpClient = HttpClient(
                userAgent = config.userAgent,
                connectTimeout = config.connectTimeoutSeconds.seconds,
                readTimeout = config.readTimeoutSeconds.seconds
            )
            val postProcessorService = DefaultPostProcessorService()
            val extractionEngine = KsoupExtractionEngine(postProcessorService)
            val robotsService = RobotService(httpClient)
            val repository = InMemoryCrawlRepository()

            val crawlWebPageUseCase = CrawlWebPageUseCase(httpClient, extractionEngine, robotsService, repository)
            val batchCrawlUseCase = BatchCrawlUseCase(crawlWebPageUseCase, robotsService)

            val crawlerEngine = CrawlerEngine(
                crawlWebPageUseCase = crawlWebPageUseCase,
                batchCrawlUseCase = batchCrawlUseCase,
                config = config.toCrawlerEngineConfig()
            )

            return CrawlerSDK(
                httpClient = httpClient,
                extractionEngine = extractionEngine,
                robotsService = robotsService,
                repository = repository,
                crawlerEngine = crawlerEngine
            )
        }

        /**
         * Create a CrawlerSDK with custom components
         */
        fun create(
            httpClient: HttpClient,
            extractionEngine: ExtractionEngine,
            robotsService: RobotsService,
            repository: CrawlRepository,
            config: SDKConfiguration = SDKConfiguration()
        ): CrawlerSDK {
            val crawlWebPageUseCase = CrawlWebPageUseCase(httpClient, extractionEngine, robotsService, repository)
            val batchCrawlUseCase = BatchCrawlUseCase(crawlWebPageUseCase, robotsService)

            val crawlerEngine = CrawlerEngine(
                crawlWebPageUseCase = crawlWebPageUseCase,
                batchCrawlUseCase = batchCrawlUseCase,
                config = config.toCrawlerEngineConfig()
            )

            return CrawlerSDK(
                httpClient = httpClient,
                extractionEngine = extractionEngine,
                robotsService = robotsService,
                repository = repository,
                crawlerEngine = crawlerEngine
            )
        }
    }
}
