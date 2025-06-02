package sample.app.ui.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import sample.app.models.CrawlCategory
import sample.app.models.MassiveCrawlerUiState
import sample.app.ui.components.CategoryCard
import sample.app.ui.components.SectionHeader
import sample.app.ui.components.ResponsiveFlowRow
import sample.app.ui.components.FlowItem

@Composable
fun CategorySelectionCard(
    selectedCategories: Set<CrawlCategory>,
    onCategoryToggle: (CrawlCategory) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CategorySelectionHeader(selectedCategories)
            CategoryGrid(
                selectedCategories = selectedCategories,
                onCategoryToggle = onCategoryToggle,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun CategorySelectionHeader(selectedCategories: Set<CrawlCategory>) {
    val totalSources = selectedCategories.sumOf { it.sources.size }

    SectionHeader(
        title = "Select Categories",
        subtitle = "$totalSources sources selected"
    )
}

@Composable
private fun CategoryGrid(
    selectedCategories: Set<CrawlCategory>,
    onCategoryToggle: (CrawlCategory) -> Unit,
    enabled: Boolean
) {
    ResponsiveFlowRow(
        maxItemsInEachRow = 4
    ) {
        CrawlCategory.entries.forEach { category ->
            FlowItem {
                CategoryCard(
                    category = category,
                    isSelected = category in selectedCategories,
                    onToggle = { onCategoryToggle(category) },
                    enabled = enabled
                )
            }
        }
    }
}
