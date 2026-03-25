package com.viwath.compose_ui_practice.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viwath.practice_module_app.drag_drop.DragDropState
import com.viwath.practice_module_app.drag_drop.dragDropItemsIndexed
import com.viwath.practice_module_app.drag_drop.rememberDragDropState

data class TaskItem(
    val id: Int,
    val title: String,
    val subtitle: String,
    val color: Color,
)

private val sampleTasks = listOf(
    TaskItem(1, "Design system tokens", "Colors, spacing, typography", Color(0xFF6C63FF)),
    TaskItem(2, "Implement auth flow", "Login, register, forgot password", Color(0xFF43C6AC)),
    TaskItem(3, "Write unit tests", "ViewModels and repositories", Color(0xFFFF6584)),
    TaskItem(4, "Code review PRs", "3 pull requests pending", Color(0xFFFFA07A)),
    TaskItem(5, "Update dependencies", "Gradle, libs.versions.toml", Color(0xFF4FC3F7)),
    TaskItem(6, "Fix crash on startup", "NullPointerException in MainActivity", Color(0xFFFFD54F)),
    TaskItem(7, "Release v2.1.0", "Changelog and Play Store notes", Color(0xFF81C784)),
    TaskItem(8, "Accessibility audit", "TalkBack and content descriptions", Color(0xFFBA68C8)),
)

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen example showing drag-to-reorder with [rememberDragDropState].
 *
 * Key points:
 *  1. [rememberDragDropState] is called once; [onMove] mutates [tasks].
 *  2. [LazyColumn] uses `state = dragState.lazyListState` — required.
 *  3. [dragDropItemsIndexed] replaces the normal `itemsIndexed` call.
 *  4. The pre-built [modifier] from [dragDropItemsIndexed] is applied to the
 *     item's root composable — that's all that's needed for gestures + animation.
 *  5. [isDragging] is used purely for visual styling (elevation, scale, tint).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DragDropScreen() {
    // ── 1. Mutable list — this is what the drag state will reorder ───────────
    val tasks = remember { mutableStateListOf(*sampleTasks.toTypedArray()) }

    // ── 2. Create drag state — swap items when user releases finger ──────────
    val dragState: DragDropState = rememberDragDropState(
        onMove = { from, to ->
            tasks.add(to, tasks.removeAt(from))
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "My Tasks",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                        )
                        Text(
                            text = "Hold & drag to reorder",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    }
                },
            )
        }
    ) { paddingValues ->

        // ── 3. LazyColumn MUST use dragState.lazyListState ───────────────────
        LazyColumn(
            state = dragState.lazyListState,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = paddingValues.calculateTopPadding() + 8.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {

            // ── 4. dragDropItemsIndexed replaces itemsIndexed ─────────────────
            dragDropItemsIndexed(
                items = tasks,
                state = dragState,
                key   = { _, task -> task.id },   // stable key keeps animations correct
            ) { index, task, isDragging, modifier ->

                // ── 5. Pass modifier to the item root — that's it! ────────────
                TaskCard(
                    task       = task,
                    isDragging = isDragging,
                    modifier   = modifier,         // ← gesture + animation lives here
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Item composable
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A single task card.
 *
 * The only drag-awareness needed here is [isDragging]:
 * - Elevation increases while dragging (gives a "lifted" feel).
 * - Background lightens slightly.
 *
 * Everything else (offset, z-index, swap) is handled by the [modifier].
 */
@Composable
fun TaskCard(
    task: TaskItem,
    isDragging: Boolean,
    modifier: Modifier = Modifier,
) {
    val iconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    // Animate elevation up when lifted
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 12.dp else 2.dp,
        label = "card_elevation",
    )

    val background = if (isDragging)
        MaterialTheme.colorScheme.surface
    else
        MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Color dot
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(50))
                .background(task.color)
        )

        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
            Text(
                text = task.subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }

        // Drag handle — purely decorative, gesture is on the whole row

        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "Drag to reorder",
            tint = iconColor,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DragDropScreenPreview() {
    MaterialTheme {
        DragDropScreen()
    }
}