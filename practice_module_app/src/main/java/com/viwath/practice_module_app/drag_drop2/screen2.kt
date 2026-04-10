package com.viwath.practice_module_app.drag_drop2

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt

// ── Data model for flattened list items ──────────────────────────────────────

private sealed class FlatItem {
    object PinnedHeader : FlatItem()
    // One row of the 3-col grid (may have 1, 2 or 3 cells)
    data class PinnedRow(
        val items: List<Pair<Int, MenuItem>>, // (pinnedIndex, menuItem)
        val rowIndex: Int
    ) : FlatItem()
    object MoreHeader : FlatItem()
    data class MoreItem(val moreIndex: Int, val item: MenuItem) : FlatItem()
}

private fun buildFlatList(
    pinnedIndices: List<Int>,
    moreIndices: List<Int>,
    allItems: List<MenuItem>
): List<FlatItem> {
    val result = mutableListOf<FlatItem>()
    result += FlatItem.PinnedHeader

    val pinnedItems = pinnedIndices.mapIndexed { i, idx -> i to allItems[idx] }
    pinnedItems.chunked(3).forEachIndexed { rowIdx, chunk ->
        result += FlatItem.PinnedRow(chunk, rowIdx)
    }

    result += FlatItem.MoreHeader

    moreIndices.forEachIndexed { i, idx ->
        result += FlatItem.MoreItem(i, allItems[idx])
    }

    return result
}

// ── Drag state ────────────────────────────────────────────────────────────────

private enum class Section { PINNED, MORE }

private data class DragTarget(
    val section: Section,
    val index: Int          // pinnedIndex or moreIndex
)

// ── Main screen ───────────────────────────────────────────────────────────────

@Composable
fun MenuScreen2(viewModel: MenuViewModel2 = viewModel()) {

    val pinnedIndices by viewModel.pinnedIndices.collectAsState()
    val moreIndices   by viewModel.moreIndices.collectAsState()

    val flatList = remember(pinnedIndices, moreIndices) {
        buildFlatList(pinnedIndices, moreIndices, allMenuItems)
    }

    // ── Drag state ────────────────────────────────────────────────────────────
    var dragOffset     by remember { mutableStateOf(Offset.Zero) }
    var draggingTarget by remember { mutableStateOf<DragTarget?>(null) }
    var dragItemLabel  by remember { mutableStateOf("") }

    // Per-item bounds stored by key so we can hit-test on drop
    // Key = "pinned_N" or "more_N"
    val itemBounds = remember { mutableStateMapOf<String, androidx.compose.ui.geometry.Rect>() }

    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(horizontal = 16.dp)
        ) {
            itemsIndexed(flatList, key = { _, item ->
                when (item) {
                    FlatItem.PinnedHeader -> "header_pinned"
                    is FlatItem.PinnedRow -> "pinned_row_${item.rowIndex}"
                    FlatItem.MoreHeader   -> "header_more"
                    is FlatItem.MoreItem  -> "more_${item.moreIndex}_${item.item.action}"
                }
            }) { _, flatItem ->

                when (flatItem) {

                    FlatItem.PinnedHeader -> {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Pinned",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            color = Color(0xFF1C1C1E)
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    is FlatItem.PinnedRow -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 3 slots always rendered (empty spacer for short last row)
                            for (slot in 0 until 3) {
                                val entry = flatItem.items.getOrNull(slot)
                                if (entry != null) {
                                    val (pinnedIdx, menuItem) = entry
                                    val isDragging = draggingTarget?.section == Section.PINNED &&
                                            draggingTarget?.index == pinnedIdx
                                    val key = "pinned_$pinnedIdx"

                                    DraggableCell(
                                        label      = menuItem.name,
                                        isDragging = isDragging,
                                        modifier   = Modifier
                                            .weight(1f)
                                            .aspectRatio(1.15f)
                                            .onGloballyPositioned { coords ->
                                                itemBounds[key] = coords.boundsInWindow()
                                            },
                                        onDragStart = {
                                            draggingTarget = DragTarget(Section.PINNED, pinnedIdx)
                                            dragItemLabel  = menuItem.name
                                            dragOffset     = it
                                        },
                                        onDrag      = { delta -> dragOffset += delta },
                                        onDragEnd   = {
                                            // Hit-test: find which more_N item we're over
                                            val hit = findHit(dragOffset, itemBounds, "more_")
                                            if (hit != null) {
                                                val moreIdx = hit.removePrefix("more_").toIntOrNull()
                                                viewModel.movePinnedToMore(pinnedIdx, moreIdx ?: 0)
                                            }
                                            draggingTarget = null
                                        },
                                        onDragCancel = { draggingTarget = null }
                                    )
                                } else {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    FlatItem.MoreHeader -> {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "More",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            color = Color(0xFF1C1C1E)
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    is FlatItem.MoreItem -> {
                        val isDragging = draggingTarget?.section == Section.MORE &&
                                draggingTarget?.index == flatItem.moreIndex
                        val key = "more_${flatItem.moreIndex}"

                        DraggableMoreRow(
                            label      = flatItem.item.name,
                            isDragging = isDragging,
                            modifier   = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .onGloballyPositioned { coords ->
                                    itemBounds[key] = coords.boundsInWindow()
                                },
                            onDragStart = { startOffset ->
                                draggingTarget = DragTarget(Section.MORE, flatItem.moreIndex)
                                dragItemLabel  = flatItem.item.name
                                dragOffset     = startOffset
                            },
                            onDrag      = { delta -> dragOffset += delta },
                            onDragEnd   = {
                                // Find pinned cell we're over
                                val hit = findHit(dragOffset, itemBounds, "pinned_")
                                if (hit != null) {
                                    val pinnedIdx = hit.removePrefix("pinned_").toIntOrNull()
                                    if (pinnedIdx != null) {
                                        // Find position of that pinnedIdx in current pinnedIndices list
                                        val insertPos = pinnedIndices.indexOf(
                                            allMenuItems[pinnedIndices[pinnedIdx]].index
                                        ).takeIf { it >= 0 } ?: pinnedIndices.size
                                        viewModel.moveMoreToPinned(flatItem.moreIndex, pinnedIdx)
                                    } else {
                                        viewModel.moveMoreToPinned(flatItem.moreIndex)
                                    }
                                } else {
                                    // Check if dropped on another more item for reorder
                                    val hitMore = findHit(dragOffset, itemBounds, "more_")
                                    if (hitMore != null) {
                                        val toIdx = hitMore.removePrefix("more_").toIntOrNull()
                                        if (toIdx != null && toIdx != flatItem.moreIndex) {
                                            viewModel.moveMoreItem(flatItem.moreIndex, toIdx)
                                        }
                                    }
                                }
                                draggingTarget = null
                            },
                            onDragCancel = { draggingTarget = null }
                        )
                    }
                }
            }
        }

        // ── Floating drag ghost ───────────────────────────────────────────────
        if (draggingTarget != null) {
            val isFromPinned = draggingTarget!!.section == Section.PINNED
            Box(
                modifier = Modifier
                    .offset { IntOffset(dragOffset.x.roundToInt() - 60, dragOffset.y.roundToInt() - 40) }
                    .zIndex(10f)
                    .shadow(12.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF4A90E2))
                    .then(
                        if (isFromPinned)
                            Modifier.size(width = 100.dp, height = 86.dp)
                        else
                            Modifier
                                .width(220.dp)
                                .height(44.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dragItemLabel,
                    color = Color.White,
                    fontSize = if (isFromPinned) 11.sp else 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

// ── Hit test helper ───────────────────────────────────────────────────────────

private fun findHit(
    point: Offset,
    bounds: Map<String, androidx.compose.ui.geometry.Rect>,
    prefix: String
): String? {
    return bounds.entries
        .filter { it.key.startsWith(prefix) }
        .firstOrNull { it.value.contains(point) }
        ?.key
}

// ── Draggable pinned grid cell ────────────────────────────────────────────────

@Composable
private fun DraggableCell(
    label: String,
    isDragging: Boolean,
    modifier: Modifier = Modifier,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    Box(
        modifier = modifier
            .graphicsLayer { alpha = if (isDragging) 0.35f else 1f }
            .shadow(if (isDragging) 6.dp else 0.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF2F2F7))
            .border(
                width = if (isDragging) 2.dp else 0.dp,
                color = if (isDragging) Color(0xFF4A90E2) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = onDragStart,
                    onDrag      = { _, delta -> onDrag(delta) },
                    onDragEnd   = { onDragEnd() },
                    onDragCancel = { onDragCancel() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text      = label,
            fontSize  = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color     = Color(0xFF1C1C1E),
            maxLines  = 2,
            overflow  = TextOverflow.Ellipsis,
            modifier  = Modifier.padding(horizontal = 6.dp)
        )
    }
}

// ── Draggable more row ────────────────────────────────────────────────────────

@Composable
private fun DraggableMoreRow(
    label: String,
    isDragging: Boolean,
    modifier: Modifier = Modifier,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .graphicsLayer { alpha = if (isDragging) 0.35f else 1f }
            .shadow(if (isDragging) 6.dp else 0.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF2F2F7))
            .border(
                width = if (isDragging) 2.dp else 0.dp,
                color = if (isDragging) Color(0xFF4A90E2) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart  = onDragStart,
                    onDrag       = { _, delta -> onDrag(delta) },
                    onDragEnd    = { onDragEnd() },
                    onDragCancel = { onDragCancel() }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text      = label,
            fontSize  = 14.sp,
            fontWeight = FontWeight.Normal,
            color     = Color(0xFF1C1C1E),
            modifier  = Modifier.padding(horizontal = 16.dp),
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis
        )
    }
}