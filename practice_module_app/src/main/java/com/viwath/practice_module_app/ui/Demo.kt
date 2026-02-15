package com.viwath.practice_module_app.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


@Preview(showBackground = true)
@Composable
fun ChartDemoPreview() {
    ChartDemo()
}

@Composable
fun ChartDemo() {
    val data = listOf(
        ChartPoint("Mon", 12f),
        ChartPoint("Tue", 18f),
        ChartPoint("Wed", 9f),
        ChartPoint("Thu", 24f),
        ChartPoint("Fri", 20f),
        ChartPoint("Sat", 28f),
        ChartPoint("Sun", 16f),
        ChartPoint("Mon2", 22f),
        ChartPoint("Tue2", 30f),
    )

    ScrollableLineChart(
        points = data,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        height = 240.dp,
        pointSpacing = 64.dp
    )
}
