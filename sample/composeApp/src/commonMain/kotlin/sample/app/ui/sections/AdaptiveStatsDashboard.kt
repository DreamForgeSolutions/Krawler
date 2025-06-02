package sample.app.ui.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import sample.app.models.MassiveCrawlerUiState
import sample.app.models.EngineStats
import sample.app.ui.components.*
import sample.app.utils.*

/**
 * Alternative version of LiveStatsDashboard that adapts to screen width
 * Shows 1, 2, 3, or 4 columns based on available space
 */
@Composable
fun AdaptiveStatsDashboard(
    uiState: MassiveCrawlerUiState,
    modifier: Modifier = Modifier,
    onStopCrawl: () -> Unit,
    onStartCrawl: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier= Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {

                DashboardHeader(isRunning = uiState.isRunning)
                ControlButton(
                    isRunning = uiState.isRunning,
                    onClick = if (uiState.isRunning) onStopCrawl else onStartCrawl
                )

            }
            AdaptiveStatsGrid(uiState)
            uiState.engineStats?.let { stats ->
                HorizontalDivider()
                AdaptiveEngineStats(stats)
            }
        }
    }
}

@Composable
private fun DashboardHeader(isRunning: Boolean) {
    Row(
        modifier = Modifier.wrapContentWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionHeader(
            title = "Live Performance",
            subtitle = if (isRunning) "Actively crawling" else "Ready to start"
        )
        
        if (isRunning) {
            PulsingDot()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdaptiveStatsGrid(uiState: MassiveCrawlerUiState) {
    AdaptiveFlowRow(
        minItemWidth = 140
    ) {
        AdaptiveFlowItem(minWidth = 140) {
            StatCard(
                emoji = "📊",
                label = "Current RPS",
                value = formatNumber(uiState.currentRps),
                trend = if (uiState.currentRps > uiState.averageRps) TrendDirection.UP else TrendDirection.DOWN,
                color = MaterialTheme.colorScheme.primary
            )
        }

        AdaptiveFlowItem(minWidth = 140) {
            StatCard(
                emoji = "✅",
                label = "Success Rate",
                value = if (uiState.totalProcessed > 0) {
                    "${formatPercentage(uiState.successCount.toDouble() / uiState.totalProcessed)}%"
                } else "0%",
                color = Color(0xFF4CAF50)
            )
        }

        AdaptiveFlowItem(minWidth = 140) {
            StatCard(
                emoji = "📄",
                label = "Total Processed",
                value = formatLargeNumber(uiState.totalProcessed),
                color = MaterialTheme.colorScheme.tertiary
            )
        }

        AdaptiveFlowItem(minWidth = 140) {
            StatCard(
                emoji = "⏱️",
                label = "Runtime",
                value = formatDuration(uiState.crawlStartTime),
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdaptiveEngineStats(stats: EngineStats) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MiniStat(
            emoji = "📚",
            label = "Queue",
            value = stats.queueSize.toString()
        )
        MiniStat(
            emoji = "⚡",
            label = "Avg RPS",
            value = formatNumber(stats.averageRequestsPerSecond)
        )
        MiniStat(
            emoji = "🌐",
            label = "Active",
            value = stats.activeConnections.toString()
        )
    }
}
