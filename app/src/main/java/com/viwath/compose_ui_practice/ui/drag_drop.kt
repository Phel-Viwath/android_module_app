package com.viwath.compose_ui_practice.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import com.viwath.practice_module_app.drag_drop.dragDropGridItemsIndexed
import com.viwath.practice_module_app.drag_drop.rememberDragDropGridState
import com.viwath.practice_module_app.drag_drop.rememberDragDropState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viwath.practice_module_app.drag_drop.DRAG_ELEVATION_BOOST_DP
import com.viwath.practice_module_app.drag_drop.dragDropItemsIndexed

data class WidgetItem(
    val id: Int,
    val title: String,
    val subtitle: String,
    val accentColor: Color,
    val icon: ImageVector,
)

/**
 * Full-screen example demonstrating production-grade drag-to-reorder.
 *
 * Features shown:
 *  - Scale + shadow on the held item
 *  - Live item swapping: items immediately swap positions as the finger drags
 *  - Spring "hop" animation on each swapped neighbour
 *  - Haptic feedback on drag-start and on each live swap
 *  - Auto-scroll when dragging near top/bottom edges
 *  - Dimmed alpha on non-dragged items while a drag is active
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DragDropScreen(items: List<WidgetItem> = sampleWidgets) {

    // ── 1. Mutable list that backs the LazyColumn / LazyVerticalGrid ─────────
    val widgets = remember { mutableStateListOf(*items.toTypedArray()) }

    // ── 2. Haptic feedback ───────────────────────────────────────────────────
    val haptic = LocalHapticFeedback.current
    val view   = LocalView.current

    // Fires a crisp tick haptic — used on every live swap
    val fireSwapHaptic: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // ── 3. Drag state — List ─────────────────────────────────────────────────
    val lazyListState = rememberLazyListState()
    val dragState = rememberDragDropState(
        lazyListState   = lazyListState,
        edgeScrollZone  = 120f,
        edgeScrollSpeed = 18f,
        onMove = { from, to -> widgets.add(to, widgets.removeAt(from)) },
    )

    // ── 4. Drag state — Grid ─────────────────────────────────────────────────
    val gridState     = rememberLazyGridState()
    val dragGridState = rememberDragDropGridState(
        lazyGridState = gridState,
        onMove = { from, to -> widgets.add(to, widgets.removeAt(from)) },
    )

    // ── 5. Haptic: fire on every live swap (draggingItemIndex change) ─────────
    //
    // With the new live-swap architecture, each time `draggingItemIndex`
    // changes it means a real swap just happened in the backing list.
    // We skip the very first emission (drag-start) by only firing when a
    // drag is already in progress AND the index actually changes.
    //
    var prevListDragIndex by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(dragState.draggingItemIndex) {
        val current = dragState.draggingItemIndex
        if (current != null && prevListDragIndex != null && current != prevListDragIndex) {
            fireSwapHaptic()
        }
        prevListDragIndex = current
    }

    var prevGridDragIndex by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(dragGridState.draggingItemIndex) {
        val current = dragGridState.draggingItemIndex
        if (current != null && prevGridDragIndex != null && current != prevGridDragIndex) {
            fireSwapHaptic()
        }
        prevGridDragIndex = current
    }

    // ── 6. UI ─────────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text       = "Widget Order",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 20.sp,
                        )
                        Text(
                            text  = "Hold & drag to rearrange",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            var isGrid by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text("Grid View", fontSize = 14.sp)
                Switch(
                    checked         = isGrid,
                    onCheckedChange = { isGrid = it },
                    modifier        = Modifier.padding(start = 8.dp),
                )
            }

            if (isGrid) {
                LazyVerticalGrid(
                    columns              = GridCells.Fixed(2),
                    state                = gridState,
                    contentPadding       = PaddingValues(16.dp),
                    verticalArrangement  = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier             = Modifier.fillMaxSize(),
                ) {
                    dragDropGridItemsIndexed(
                        items       = widgets,
                        state       = dragGridState,
                        key         = { _, w -> w.id },
                    ) { _, widget, isDragging, modifier ->
                        WidgetCard(
                            widget     = widget,
                            isDragging = isDragging,
                            dimmed     = dragGridState.isDragging && !isDragging,
                            modifier   = modifier,
                        )
                    }
                }
            } else {
                LazyColumn(
                    state                = lazyListState,
                    contentPadding       = PaddingValues(16.dp),
                    verticalArrangement  = Arrangement.spacedBy(10.dp),
                    modifier             = Modifier.fillMaxSize(),
                ) {
                    dragDropItemsIndexed(
                        items = widgets,
                        state = dragState,
                        key   = { _, w -> w.id },
                    ) { _, widget, isDragging, modifier ->
                        WidgetCard(
                            widget     = widget,
                            isDragging = isDragging,
                            dimmed     = dragState.isDragging && !isDragging,
                            modifier   = modifier,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Item composable
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A single reorderable widget card.
 *
 * All gesture handling and position animation live inside [modifier] —
 * this composable is fully decoupled from drag mechanics.
 */
@Composable
fun WidgetCard(
    widget: WidgetItem,
    isDragging: Boolean,
    dimmed: Boolean,
    modifier: Modifier = Modifier,
) {
    val elevation: Dp by animateDpAsState(
        targetValue   = if (isDragging) (2 + DRAG_ELEVATION_BOOST_DP).dp else 2.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium,
        ),
        label = "card_elevation",
    )

    val alpha: Float by animateFloatAsState(
        targetValue   = if (dimmed) 0.55f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label         = "card_alpha",
    )

    Card(
        modifier  = modifier
            .fillMaxWidth()
            .alpha(alpha),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors    = CardDefaults.cardColors(
            containerColor = if (isDragging)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Accent icon badge
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(widget.accentColor.copy(alpha = 0.15f)),
            ) {
                Icon(
                    imageVector        = widget.icon,
                    contentDescription = null,
                    tint               = widget.accentColor,
                    modifier           = Modifier.size(22.dp),
                )
            }

            // Title + subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = widget.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = widget.subtitle,
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }

            // Drag handle — purely decorative
            Icon(
                imageVector        = Icons.Default.DragHandle,
                contentDescription = "Drag to reorder",
                tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sample data
// ─────────────────────────────────────────────────────────────────────────────

private val sampleWidgets = listOf(
    WidgetItem(1,  "Weather",      "Phnom Penh · 32°C",        Color(0xFF4FC3F7), Icons.Default.DragHandle),
    WidgetItem(2,  "Calendar",     "3 events today",            Color(0xFF81C784), Icons.Default.DragHandle),
    WidgetItem(3,  "Music",        "Now playing: Lo-fi Beats",  Color(0xFFBA68C8), Icons.Default.DragHandle),
    WidgetItem(4,  "Fitness",      "6,240 steps · 72% goal",    Color(0xFFFF8A65), Icons.Default.DragHandle),
    WidgetItem(5,  "News",         "5 unread headlines",        Color(0xFF4DB6AC), Icons.Default.DragHandle),
    WidgetItem(6,  "Battery",      "87% · Est. 9h remaining",   Color(0xFFFFD54F), Icons.Default.DragHandle),
    WidgetItem(7,  "Notes",        "Last edited 2 hours ago",   Color(0xFFE57373), Icons.Default.DragHandle),
    WidgetItem(8,  "Clock",        "World clocks · 4 zones",    Color(0xFF90CAF9), Icons.Default.DragHandle),
    WidgetItem(9,  "Reminders",    "2 due today",               Color(0xFFA5D6A7), Icons.Default.DragHandle),
    WidgetItem(10, "Screen Time",  "3h 12m today",              Color(0xFFCE93D8), Icons.Default.DragHandle),
)

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