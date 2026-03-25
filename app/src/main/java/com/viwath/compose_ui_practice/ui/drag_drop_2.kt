package com.viwath.compose_ui_practice.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.viwath.practice_module_app.drag_drop.dragDropItemsIndexed
import com.viwath.practice_module_app.drag_drop.rememberDragDropState

sealed class DashboardWidget(val id: Int) {
    data class Weather(val city: Int, val temp: String) : DashboardWidget(city)
    data class TaskSummary(val taskId: Int, val count: Int) : DashboardWidget(taskId)
    data class QuickSetting(val settingId: Int, val label: String, val enabled: Boolean) : DashboardWidget(settingId)
}

@Composable
fun WidgetDashboardScreen() {
    // Initializing with different widget types
    val widgets = remember {
        mutableStateListOf(
            DashboardWidget.Weather(1, "28°C"),
            DashboardWidget.TaskSummary(2, 5),
            DashboardWidget.QuickSetting(3, "Wi-Fi", true),
            DashboardWidget.QuickSetting(4, "Bluetooth", false),
            DashboardWidget.Weather(5, "31°C")
        )
    }

    val dragState = rememberDragDropState { from, to ->
        // Your logic: items.add(to, items.removeAt(from))
        widgets.add(to, widgets.removeAt(from))
    }

    LazyColumn(
        state = dragState.lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        dragDropItemsIndexed(
            items = widgets,
            state = dragState,
            key = { _, item -> item.id } // Stable ID is mandatory for animations
        ) { index, item, isDragging, dragModifier ->

            // We pass the dragModifier to the root of our widget card
            WidgetRenderer(
                item = item,
                isDragging = isDragging,
                modifier = dragModifier
            )
        }
    }
}

@Composable
fun WidgetRenderer(
    item: DashboardWidget,
    isDragging: Boolean,
    modifier: Modifier
) {
    // Elevation and Scale animations for the "Lift" effect
    val elevation = animateDpAsState(if (isDragging) 12.dp else 2.dp, label = "elev")
    val scale = animateFloatAsState(if (isDragging) 1.04f else 1.0f, label = "scale")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = elevation.value,
        shadowElevation = elevation.value
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Unique icon per widget type
            val (icon, color) = when (item) {
                is DashboardWidget.Weather -> Icons.Default.Cloud to Color(0xFF4FC3F7)
                is DashboardWidget.TaskSummary -> Icons.Default.List to Color(0xFF81C784)
                is DashboardWidget.QuickSetting -> Icons.Default.Settings to Color(0xFFFFB74D)
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color)
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = when (item) {
                        is DashboardWidget.Weather -> "Weather: Phnom Penh"
                        is DashboardWidget.TaskSummary -> "Pending Tasks"
                        is DashboardWidget.QuickSetting -> item.label
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = when (item) {
                        is DashboardWidget.Weather -> item.temp
                        is DashboardWidget.TaskSummary -> "${item.count} items left"
                        is DashboardWidget.QuickSetting -> if (item.enabled) "On" else "Off"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Visual drag handle
            Icon(
                imageVector = Icons.Default.Reorder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}