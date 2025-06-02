package sample.app.ui.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sample.app.ui.components.AnimatedScale



@Composable
fun ControlButton(
    isRunning: Boolean,
    onClick: () -> Unit
) {
    AnimatedScale(isAnimating = isRunning) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(24.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isRunning) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        ) {
            Text(
                text = if (isRunning) "⏹️" else "▶️",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun ControlText(isRunning: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = if (isRunning) "Stop Crawling" else "Start Massive Crawl",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        if (!isRunning) {
            Text(
                text = "Ready to crawl selected categories",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
