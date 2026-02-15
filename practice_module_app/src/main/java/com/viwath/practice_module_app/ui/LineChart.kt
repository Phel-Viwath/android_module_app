package com.viwath.practice_module_app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

data class ChartPoint(
    val x: String,
    val y: Float
)

@Composable
fun ScrollableLineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    height: Dp = 220.dp,
    pointSpacing: Dp = 56.dp,
    leftAxisWidth: Dp = 56.dp,
    topPadding: Dp = 12.dp,
    bottomPadding: Dp = 28.dp,
    gridLines: Int = 4,
    showDots: Boolean = true,
    showGrid: Boolean = true,
) {
    if (points.isEmpty()) return

    // ✅ Read theme colors OUTSIDE Canvas
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val tickColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val dotInnerColor = MaterialTheme.colorScheme.background
    val xLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val yLabelColor = MaterialTheme.colorScheme.onSurfaceVariant

    val density = LocalDensity.current
    val scrollState = rememberScrollState()

    val minY = points.minOf { it.y }
    val maxY = points.maxOf { it.y }
    val rangeY = (maxY - minY).let { if (it == 0f) 1f else it }

    val contentWidth = leftAxisWidth + (pointSpacing * (points.size - 1).coerceAtLeast(1)) + 16.dp

    Row(
        modifier = modifier
            .height(height)
            .fillMaxWidth()
    ) {
        // Y axis labels
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(leftAxisWidth),
            contentAlignment = Alignment.Center
        ) {
            YAxisLabels(
                minY = minY,
                maxY = maxY,
                steps = gridLines,
                color = yLabelColor,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Scrollable chart
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .horizontalScroll(scrollState)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(contentWidth)
            ) {
                val top = with(density) { topPadding.toPx() }
                val bottom = size.height - with(density) { bottomPadding.toPx() }
                val right = size.width
                val left = 0f

                // Grid lines
                if (showGrid) {
                    val steps = gridLines.coerceAtLeast(1)
                    for (i in 0..steps) {
                        val t = i / steps.toFloat()
                        val y = top + (bottom - top) * t
                        drawLine(
                            color = gridColor,
                            start = Offset(left, y),
                            end = Offset(right, y),
                            strokeWidth = 1f
                        )
                    }
                }

                val spacingPx = with(density) { pointSpacing.toPx() }

                fun mapY(value: Float): Float {
                    val norm = (value - minY) / rangeY
                    return bottom - norm * (bottom - top)
                }

                val path = Path()
                val offsets = ArrayList<Offset>(points.size)

                points.forEachIndexed { index, p ->
                    val x = index * spacingPx
                    val y = mapY(p.y)
                    val o = Offset(x, y)
                    offsets.add(o)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                // Line
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 4f)
                )

                // Dots
                if (showDots) {
                    offsets.forEach { o ->
                        drawCircle(color = lineColor, radius = 6f, center = o)
                        drawCircle(color = dotInnerColor, radius = 3f, center = o)
                    }
                }

                // Bottom ticks
                offsets.forEach { o ->
                    drawLine(
                        color = tickColor,
                        start = Offset(o.x, bottom),
                        end = Offset(o.x, bottom + 8f),
                        strokeWidth = 1f
                    )
                }
            }

            // X labels overlay (composable, safe)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(end = 16.dp)
            ) {
                points.forEach { p ->
                    Box(
                        modifier = Modifier.width(pointSpacing),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = p.x,
                            style = MaterialTheme.typography.labelSmall,
                            color = xLabelColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YAxisLabels(
    minY: Float,
    maxY: Float,
    steps: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val s = steps.coerceAtLeast(1)
    Column(
        modifier = modifier.padding(vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        for (i in 0..s) {
            val t = i / s.toFloat()
            val value = maxY - (maxY - minY) * t
            Text(
                text = formatAxis(value),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun formatAxis(v: Float): String {
    val rounded = (v * 10).roundToInt() / 10f
    val isInt = abs(rounded - rounded.toInt()) < 0.0001f
    return if (isInt) "%,d".format(rounded.toInt()) else "%,.1f".format(rounded)
}
