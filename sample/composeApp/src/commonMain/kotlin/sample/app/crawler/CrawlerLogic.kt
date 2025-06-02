package sample.app.crawler

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import sample.app.models.CrawlCategory
import sample.app.models.CrawlResultUi
import sample.app.models.MassiveCrawlerUiState
import solutions.dreamforge.krawler.CrawlerSDK
import solutions.dreamforge.krawler.SDKConfiguration
import solutions.dreamforge.krawler.domain.model.*
import solutions.dreamforge.krawler.dsl.crawler

suspend fun runMassiveCrawler(
    uiState: MassiveCrawlerUiState,
    onStateUpdate: (MassiveCrawlerUiState) -> Unit,
    onCrawlerCreated: (CrawlerSDK) -> Unit
) {
    if (uiState.selectedCategories.isEmpty()) return

    val startTime = Clock.System.now().toEpochMilliseconds()
    onStateUpdate(
        uiState.copy(
            isRunning = true,
            totalProcessed = 0,
            successCount = 0,
            failureCount = 0,
            sourceStats = emptyMap(),
            recentResults = emptyList(),
            crawlStartTime = startTime
        )
    )

    val crawler = CrawlerSDK.create(
        SDKConfiguration(
            userAgent = "DreamForge-Crawler/1.0 (Compose Demo)",
            maxConcurrency = 20,
            queueCapacity = 5000,
            resultBufferSize = 1000,
            progressReportInterval = 500L
        )
    )

    onCrawlerCreated(crawler)

    try {
        val configuration = generateCrawlConfiguration(uiState.selectedCategories)

        // Thread-safe counters using Mutex
        val statsMutex = Mutex()
        val statsManager = CrawlStatsManager()

        crawler.crawl(configuration).collect { result ->
            statsMutex.withLock {
                val updatedStats = statsManager.processResult(result, startTime)
                onStateUpdate(uiState.copy(
                    totalProcessed = updatedStats.totalProcessed,
                    successCount = updatedStats.successCount,
                    failureCount = updatedStats.failureCount,
                    currentRps = updatedStats.currentRps,
                    averageRps = updatedStats.currentRps,
                    sourceStats = updatedStats.sourceStats,
                    recentResults = updatedStats.recentResults
                ))
            }
        }
    } catch (e: Exception) {
        // Handle error
    } finally {
        crawler.stop()
        onStateUpdate(uiState.copy(isRunning = false))
    }
}

private class CrawlStatsManager {
    private var totalProcessed = 0L
    private var successCount = 0L
    private var failureCount = 0L
    private val sourceStats = mutableMapOf<String, Int>()
    private val recentResults = mutableListOf<CrawlResultUi>()

    data class Stats(
        val totalProcessed: Long,
        val successCount: Long,
        val failureCount: Long,
        val currentRps: Double,
        val sourceStats: Map<String, Int>,
        val recentResults: List<CrawlResultUi>
    )

    fun processResult(result: CrawlResult, startTime: Long): Stats {
        totalProcessed++

        when (result.status) {
            CrawlStatus.SUCCESS -> handleSuccess(result)
            CrawlStatus.SKIPPED -> {} // Ignore skipped
            else -> handleFailure(result)
        }

        val elapsed = Clock.System.now().toEpochMilliseconds() - startTime
        val currentRps = if (elapsed > 0) {
            totalProcessed / (elapsed / 1000.0)
        } else 0.0

        return Stats(
            totalProcessed = totalProcessed,
            successCount = successCount,
            failureCount = failureCount,
            currentRps = currentRps,
            sourceStats = sourceStats.toMap(),
            recentResults = recentResults.toList()
        )
    }

    private fun handleSuccess(result: CrawlResult) {
        successCount++
        val source = result.request.source?: "Unknown"
        sourceStats[source] = (sourceStats[source] ?: 0) + 1

        val webPage = result.webPage!!
        val crawlResultUi = CrawlResultUi(
            url = webPage.url,
            source = source,
            status = CrawlStatus.SUCCESS,
            title = (webPage.extractedData["title"] as? ExtractedValue.Text)?.value,
            content = (webPage.extractedData["content"] as? ExtractedValue.Text)?.value
                ?: (webPage.extractedData["description"] as? ExtractedValue.Text)?.value,
            error = null,
            responseTime = webPage.metadata.responseTime ?: 0L
        )

        addRecentResult(crawlResultUi)
    }

    private fun handleFailure(result: CrawlResult) {
        failureCount++

        val crawlResultUi = CrawlResultUi(
            url = result.request.url,
            source = result.request.source ?: "Unknown",
            status = result.status,
            title = null,
            content = null,
            error = result.error
        )

        addRecentResult(crawlResultUi)
    }

    private fun addRecentResult(result: CrawlResultUi) {
        recentResults.add(0, result)
        if (recentResults.size > 20) {
            recentResults.removeAt(recentResults.lastIndex)
        }
    }
}

fun generateCrawlConfiguration(selectedCategories: Set<CrawlCategory>) = crawler {
    name = "Massive Multi-Category Crawler"
    maxConcurrency = 20

    // Global extraction rules
    extract {
        html("content", "html") {
            required()
        }

        text("title", "h1, title, .title, .headline") {
            process {
                trim()
            }
        }

        text("description", "meta[name=description], meta[property='og:description']") {
            process {
                trim()
                substring(0, 500)
            }
        }

        links("page_links", "a[href]") {
            multiple()
        }
    }

    // Global policy
    policy {
        respectRobotsTxt = true
        delay(2000)
        maxRetries = 2
        timeout = 15000
        maxContentLength = 5 * 1024 * 1024
        allowContentTypes("text/html", "application/xhtml+xml")
    }

    // Add sources from selected categories
    selectedCategories.forEach { category ->
        category.sources.forEach { crawlSource ->
            source(crawlSource.id) {
                urls(crawlSource.url)
                depth(1)
                priority(CrawlRequest.Priority.NORMAL)
            }
        }
    }
}
