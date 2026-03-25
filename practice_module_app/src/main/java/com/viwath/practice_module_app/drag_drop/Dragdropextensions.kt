package com.viwath.practice_module_app.drag_drop

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.zIndex

// ─────────────────────────────────────────────────────────────────────────────
// Factory
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Creates and remembers a [DragDropState].
 *
 * The list is **not** mutated while dragging — [onMove] is only called when
 * the user releases their finger. Other items animate smoothly out of the
 * way to give a live preview of where the item will land.
 *
 * @param lazyListState   Optionally supply your own [LazyListState].
 * @param edgeScrollZone  px from viewport edge that triggers auto-scroll (default 100).
 * @param edgeScrollSpeed px scrolled per tick during edge-scroll (default 14).
 * @param onMove          Called on drop. Mutate your list here.
 *
 * ### Example
 * ```kotlin
 * val items = remember { mutableStateListOf("A", "B", "C") }
 *
 * val dragState = rememberDragDropState { from, to ->
 *     items.add(to, items.removeAt(from))
 * }
 *
 * LazyColumn(state = dragState.lazyListState) {
 *     dragDropItemsIndexed(items = items, state = dragState) { index, item, isDragging, modifier ->
 *         MyCard(item, isDragging, modifier)
 *     }
 * }
 * ```
 */
@Composable
fun rememberDragDropState(
    lazyListState: LazyListState = rememberLazyListState(),
    edgeScrollZone: Float = 100f,
    edgeScrollSpeed: Float = 14f,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
): DragDropState {
    val scope = rememberCoroutineScope()
    return remember {
        DragDropState(
            lazyListState = lazyListState,
            onMove = onMove,
            scope = scope,
            edgeScrollZone = edgeScrollZone,
            edgeScrollSpeed = edgeScrollSpeed,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Modifier extension
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Attaches long-press drag-to-reorder gestures for an item at [index].
 *
 * Behaviour:
 * - The dragged item follows the finger in real time.
 * - Neighbouring items animate into their displaced positions with a spring.
 * - The list is **only mutated on release** — no swaps happen mid-drag.
 *
 * Apply this modifier to the root composable of each list item.
 * When using [dragDropItemsIndexed] the modifier is supplied automatically.
 *
 * ### Example (manual usage)
 * ```kotlin
 * Box(
 *     modifier = Modifier
 *         .dragToReorder(dragState, index)
 *         .fillMaxWidth()
 * ) { ... }
 * ```
 */
fun Modifier.dragToReorder(
    state: DragDropState,
    index: Int,
): Modifier = this
    .zIndex(if (state.draggingItemIndex == index) 1f else 0f)
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            val offsetY = when {
                // Dragged item: follow the finger directly
                state.draggingItemIndex == index ->
                    state.draggingItemOffset.toInt()
                // Other items: use their spring-animated displacement
                else ->
                    state.displacementFor(index).toInt()
            }
            placeable.placeRelative(0, offsetY)
        }
    }
    .pointerInput(index) {
        detectDragGesturesAfterLongPress(
            onDragStart = { offset ->
                val itemInfo = state.lazyListState.layoutInfo
                    .visibleItemsInfo
                    .firstOrNull { it.index == index }
                val touchOffset = itemInfo?.let {
                    offset.y - (it.offset + it.size / 2f)
                } ?: 0f
                state.onDragStart(index, touchOffset)
            },
            onDrag = { change, dragAmount ->
                change.consume()
                state.onDrag(dragAmount.y)
            },
            onDragEnd = state::onDragEnd,
            onDragCancel = state::onDragCancel,
        )
    }

// ─────────────────────────────────────────────────────────────────────────────
// LazyListScope DSL extension
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Drop-in replacement for [itemsIndexed] with smooth drag-to-reorder support.
 *
 * The [Modifier] passed to [itemContent] is pre-wired with gesture detection,
 * z-index, and animated Y-offset — just apply it to your item's root composable.
 *
 * Items shift out of the way with a spring animation as you drag. The swap
 * only happens when the user releases, keeping the data model stable during
 * the drag and allowing a settle animation on drop.
 *
 * ### Example
 * ```kotlin
 * LazyColumn(state = dragState.lazyListState) {
 *     dragDropItemsIndexed(
 *         items  = items,
 *         state  = dragState,
 *         key    = { _, item -> item.id },
 *     ) { index, item, isDragging, modifier ->
 *         MyCard(
 *             item      = item,
 *             modifier  = modifier,
 *             // Optionally style the dragged item differently:
 *             elevation = if (isDragging) 8.dp else 2.dp,
 *         )
 *     }
 * }
 * ```
 *
 * @param items       Your data list.
 * @param state       The [DragDropState] managing this list.
 * @param key         Stable key extractor (strongly recommended).
 * @param itemContent Composable lambda receiving: index, item, isDragging flag,
 *                    and a pre-configured [Modifier] to apply to the item root.
 */
inline fun <T : Any> LazyListScope.dragDropItemsIndexed(
    items: List<T>,
    state: DragDropState,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    crossinline itemContent: @Composable (
        index: Int,
        item: T,
        isDragging: Boolean,
        modifier: Modifier,
    ) -> Unit,
) {
    itemsIndexed(items, key = key) { index, item ->
        val isDragging = state.draggingItemIndex == index
        itemContent(
            index,
            item,
            isDragging,
            Modifier.dragToReorder(state, index),
        )
    }
}