package sample.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sample.app.crawler.runMassiveCrawler
import sample.app.models.EngineStats
import sample.app.models.MassiveCrawlerUiState
import sample.app.ui.components.AppHeader
import sample.app.ui.sections.*
import solutions.dreamforge.krawler.CrawlerSDK

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    MaterialTheme {
        var uiState by remember { mutableStateOf(MassiveCrawlerUiState()) }
        val scope = rememberCoroutineScope()
        var crawler by remember { mutableStateOf<CrawlerSDK?>(null) }

        // Auto-update stats while running
        LaunchedEffect(uiState.isRunning) {
            while (uiState.isRunning) {
                delay(1000)
                crawler?.let { sdk ->
                    val stats = sdk.getStats()
                    uiState = uiState.copy(
                        engineStats = EngineStats(
                            queueSize = stats.queueSize,
                            successRate = stats.successRate,
                            averageRequestsPerSecond = stats.averageRequestsPerSecond,
                            activeConnections = stats.queueSize.toInt()
                        ),
                        currentRps = stats.averageRequestsPerSecond
                    )
                }
            }
        }

        Scaffold(
            topBar = {
                CrawlerTopBar(isRunning = uiState.isRunning)
            }
        ) { paddingValues ->
            CrawlerContent(
                uiState = uiState,
                paddingValues = paddingValues,
                onStateUpdate = { uiState = it },
                onStartCrawl = {
                    scope.launch {
                        runMassiveCrawler(
                            uiState = uiState,
                            onStateUpdate = { uiState = it },
                            onCrawlerCreated = { crawler = it }
                        )
                    }
                },
                onStopCrawl = {
                    scope.launch {
                        crawler?.stop()
                        uiState = uiState.copy(isRunning = false)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrawlerTopBar(isRunning: Boolean) {
    CenterAlignedTopAppBar(
        title = {
            AppHeader(
                title = "Massive Web Crawler",
                subtitle = "Enterprise-Scale Data Collection",
                emoji = "🌐",
                isLoading = isRunning
            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun CrawlerContent(
    uiState: MassiveCrawlerUiState,
    paddingValues: PaddingValues,
    onStateUpdate: (MassiveCrawlerUiState) -> Unit,
    onStartCrawl: () -> Unit,
    onStopCrawl: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdaptiveStatsDashboard(
                uiState,
                onStopCrawl = onStopCrawl,
                onStartCrawl = onStartCrawl
            )
        }


        item {
            CategorySelectionCard(
                selectedCategories = uiState.selectedCategories,
                onCategoryToggle = { category ->
                    onStateUpdate(
                        uiState.copy(
                            selectedCategories = if (category in uiState.selectedCategories) {
                                uiState.selectedCategories - category
                            } else {
                                uiState.selectedCategories + category
                            }
                        )
                    )
                },
                enabled = !uiState.isRunning,

            )
        }


        // Source Performance
        if (uiState.sourceStats.isNotEmpty()) {
            item {
                SourcePerformanceCard(uiState.sourceStats)
            }
        }

        // Recent Results
        if (uiState.recentResults.isNotEmpty()) {
            item {
                RecentResultsHeader()
            }

            items(
                items = uiState.recentResults,
                key = { it.id }
            ) { result ->
                RecentResultCard(
                    result = result,
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
private fun RecentResultsHeader() {
    Text(
        text = "Recent Results",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
