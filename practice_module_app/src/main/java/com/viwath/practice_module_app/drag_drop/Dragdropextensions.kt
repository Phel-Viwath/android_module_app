package com.viwath.practice_module_app.drag_drop

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.zIndex

// ─────────────────────────────────────────────────────────────────────────────
// Factory
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Creates and remembers a [DragDropState] for the given [LazyListState].
 *
 * The data list is **never mutated during a drag** — [onMove] fires only when
 * the finger lifts. While dragging, items animate smoothly out of the way and
 * the held item scales up for a tactile "lifted" feel.
 *
 * ### Minimal usage
 * ```kotlin
 * val items    = remember { mutableStateListOf("A", "B", "C") }
 * val dragState = rememberDragDropState { from, to ->
 *     items.add(to, items.removeAt(from))
 * }
 *
 * LazyColumn(state = dragState.lazyListState) {
 *     dragDropItemsIndexed(items = items, state = dragState) { _, item, isDragging, modifier ->
 *         MyCard(item, isDragging, modifier)
 *     }
 * }
 * ```
 *
 * @param lazyListState   Supply your own [LazyListState] when you need to
 *                        control scroll position externally.
 * @param edgeScrollZone  px from viewport edge that triggers auto-scroll.
 * @param edgeScrollSpeed px scrolled per auto-scroll tick.
 * @param onMove          Invoked on drop. Mutate your list here.
 */
@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    edgeScrollZone: Float = 100f,
    edgeScrollSpeed: Float = 14f,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
): DragDropState {
    val scope = rememberCoroutineScope()
    val state = remember(lazyListState) { DragDropLazyListState(lazyListState) }
    return remember {
        DragDropState(
            layoutState = state,
            onMove = onMove,
            scope = scope,
            edgeScrollZone = edgeScrollZone,
            edgeScrollSpeed = edgeScrollSpeed,
        )
    }
}

/**
 * Creates and remembers a [DragDropState] for the given [LazyGridState].
 */
@Composable
fun rememberDragDropGridState(
    lazyGridState: LazyGridState,
    edgeScrollZone: Float = 100f,
    edgeScrollSpeed: Float = 14f,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
): DragDropState {
    val scope = rememberCoroutineScope()
    val state = remember(lazyGridState) { DragDropLazyGridState(lazyGridState) }
    return remember {
        DragDropState(
            layoutState = state,
            onMove = onMove,
            scope = scope,
            edgeScrollZone = edgeScrollZone,
            edgeScrollSpeed = edgeScrollSpeed,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Core modifier
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Wires drag-to-reorder gestures, position offset, z-elevation, and scale
 * animations onto an item at [index].
 *
 * Apply this to the **root composable** of every list item.
 * [dragDropItemsIndexed] supplies it automatically via its lambda parameter.
 *
 * ### What this modifier does
 * | Layer          | Dragged item                     | Other items                   |
 * |----------------|----------------------------------|-------------------------------|
 * | `zIndex`       | Raised to 1f                     | 0f                            |
 * | `graphicsLayer`| Scale → [DRAG_SCALE], shadow up  | Scale stays 1f                |
 * | `layout`       | Y = raw finger offset            | Y = spring displacement       |
 * | `pointerInput` | Long-press activates drag        | —                             |
 *
 * ### Manual usage
 * ```kotlin
 * Box(modifier = Modifier.dragToReorder(dragState, index)) { ... }
 * ```
 */
fun Modifier.dragToReorder(
    state: DragDropState,
    index: Int,
): Modifier = this
    .zIndex(if (state.draggingItemIndex == index) 1f else 0f)
    .graphicsLayer {
        val isDragging = state.draggingItemIndex == index
        if (isDragging) {
            val s = state.dragScale.value
            scaleX = s
            scaleY = s
            rotationZ = state.dragRotation.value
            shadowElevation = DRAG_ELEVATION_BOOST_DP * density
            ambientShadowColor = Color(0x3C000000)
            spotShadowColor = Color(0x50000000)
        }
    }
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            val offset = when {
                state.draggingItemIndex == index -> state.draggingItemOffset + state.floatingOffset.value
                else -> state.displacementFor(index)
            }
            placeable.placeRelative(offset.x.toInt(), offset.y.toInt())
        }
    }
    .pointerInput(index) {
        detectDragGesturesAfterLongPress(
            onDragStart = { _ ->
                state.onDragStart(index)
            },
            onDrag = { change, dragAmount ->
                change.consume()
                state.onDrag(dragAmount)
            },
            onDragEnd = state::onDragEnd,
            onDragCancel = state::onDragCancel,
        )
    }

// ─────────────────────────────────────────────────────────────────────────────
// LazyListScope DSL
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Drop-in replacement for [itemsIndexed] with production-grade drag-to-reorder.
 *
 * The [Modifier] passed to [itemContent] is fully pre-wired — just apply it
 * to your item's root composable. No other setup is needed inside the lambda.
 *
 * ### What's automatic
 * - Long-press gesture detection
 * - Scale up / shadow boost on the held item
 * - Spring animations on displaced neighbours
 * - Drop-on-release (list is not mutated until the finger lifts)
 * - Settle animations after drop
 *
 * ### Example
 * ```kotlin
 * LazyColumn(state = dragState.lazyListState) {
 *     dragDropItemsIndexed(
 *         items = items,
 *         state = dragState,
 *         key   = { _, item -> item.id },
 *     ) { index, item, isDragging, modifier ->
 *         TaskCard(
 *             task      = item,
 *             modifier  = modifier,          // ← apply here
 *             elevation = if (isDragging) (2 + DRAG_ELEVATION_BOOST_DP).dp else 2.dp,
 *         )
 *     }
 * }
 * ```
 *
 * @param items       Your data list.
 * @param state       [DragDropState] from [rememberDragDropState].
 * @param key         Stable key extractor — **strongly recommended** to keep
 *                    item identity correct during recomposition.
 * @param itemContent Lambda receiving (index, item, isDragging, modifier).
 *                    Apply [modifier] to the item root.
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
        itemContent(
            index,
            item,
            state.draggingItemIndex == index,
            Modifier.dragToReorder(state, index),
        )
    }
}

inline fun <T : Any> LazyGridScope.dragDropGridItemsIndexed(
    items: List<T>,
    state: DragDropState,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    crossinline itemContent: @Composable LazyGridItemScope.(
        index: Int,
        item: T,
        isDragging: Boolean,
        modifier: Modifier,
    ) -> Unit,
) {
    itemsIndexed(items, key = key) { index, item ->
        itemContent(
            index,
            item,
            state.draggingItemIndex == index,
            Modifier.dragToReorder(state, index),
        )
    }
}