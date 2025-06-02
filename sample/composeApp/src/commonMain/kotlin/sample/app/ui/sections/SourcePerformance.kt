package sample.app.ui.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sample.app.ui.components.SectionHeader
import sample.app.ui.components.SourceStatRow

@Composable
fun SourcePerformanceCard(
    sourceStats: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(
                title = "Top Performing Sources",
                emoji = "📈"
            )

            SourceStatsList(sourceStats)
        }
    }
}

@Composable
private fun SourceStatsList(sourceStats: Map<String, Int>) {
    val maxCount = sourceStats.values.maxOrNull() ?: 1
    
    sourceStats.entries
        .sortedByDescending { it.value }
        .take(5)
        .forEach { (source, count) ->
            SourceStatRow(
                source = source, 
                count = count, 
                maxCount = maxCount
            )
        }
}
