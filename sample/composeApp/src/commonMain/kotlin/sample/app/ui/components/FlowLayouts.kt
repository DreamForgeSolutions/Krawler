package sample.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResponsiveFlowRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: Int = 8,
    verticalSpacing: Int = 8,
    maxItemsInEachRow: Int? = null,
    content: @Composable FlowRowScope.() -> Unit
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing.dp),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing.dp),
        maxItemsInEachRow = maxItemsInEachRow ?: Int.MAX_VALUE,
        content = content
    )
}

@Composable
fun FlowRowScope.FlowItem(
    fraction: Float = 0.5f,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .weight(1f)
            .fillMaxWidth(fraction),
        content = content
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdaptiveFlowRow(
    modifier: Modifier = Modifier,
    minItemWidth: Int = 150,
    horizontalSpacing: Int = 8,
    verticalSpacing: Int = 8,
    content: @Composable FlowRowScope.() -> Unit
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing.dp),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing.dp),
        content = content
    )
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRowScope.AdaptiveFlowItem(
    minWidth: Int = 150,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .weight(1f)
            .widthIn(min = minWidth.dp),
        content = content
    )
}
