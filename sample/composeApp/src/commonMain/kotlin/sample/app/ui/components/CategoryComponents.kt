package sample.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sample.app.models.CrawlCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryCard(
    category: CrawlCategory,
    isSelected: Boolean,
    onToggle: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onToggle,
        enabled = enabled,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                category.color.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = 2.dp,
            color = if (isSelected) category.color else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CategoryEmoji(emoji = category.emoji, isSelected = isSelected, color = category.color)
            CategoryTitle(title = category.displayName, isSelected = isSelected)
            CategorySourceCount(count = category.sources.size)
        }
    }
}

@Composable
private fun CategoryEmoji(
    emoji: String,
    isSelected: Boolean,
    color: Color
) {
    Text(
        text = emoji,
        fontSize = 24.sp,
        color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun CategoryTitle(
    title: String,
    isSelected: Boolean
) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun CategorySourceCount(count: Int) {
    Text(
        text = "$count sources",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
