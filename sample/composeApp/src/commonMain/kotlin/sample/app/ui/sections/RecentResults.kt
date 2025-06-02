package sample.app.ui.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sample.app.models.CrawlResultUi
import sample.app.ui.components.StatusChip
import solutions.dreamforge.krawler.domain.model.CrawlStatus

@Composable
fun RecentResultCard(
    result: CrawlResultUi,
    modifier: Modifier = Modifier
) {
    val statusColor = getStatusColor(result.status)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ResultHeader(result, statusColor)
            
            if (result.content != null && result.status == CrawlStatus.SUCCESS) {
                ResultContentPreview(result.content)
            }
        }
    }
}

@Composable
private fun ResultHeader(
    result: CrawlResultUi,
    statusColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        ResultInfo(
            source = result.source,
            title = result.title,
            url = result.url,
            modifier = Modifier.weight(1f)
        )

        StatusChip(
            status = result.status,
            color = statusColor,
            responseTime = result.responseTime
        )
    }
}

@Composable
private fun ResultInfo(
    source: String,
    title: String?,
    url: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SourceLabel(source)
        
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = url,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SourceLabel(source: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "📄",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = source,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ResultContentPreview(content: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = content.take(150) + "...",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(8.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun getStatusColor(status: CrawlStatus): Color {
    return when (status) {
        CrawlStatus.SUCCESS -> Color(0xFF4CAF50)
        CrawlStatus.ROBOTS_BLOCKED -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}
