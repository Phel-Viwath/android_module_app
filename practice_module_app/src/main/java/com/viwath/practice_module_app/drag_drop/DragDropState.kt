package com.viwath.practice_module_app.drag_drop

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Core state holder for drag-and-drop reordering in a LazyColumn.
 *
 * Changes from v1:
 * - Items animate smoothly to their displaced positions while dragging.
 * - The actual [onMove] swap is deferred until the finger is lifted (drop-on-release).
 * - A [pendingTargetIndex] field exposes the "would-land-here" index so the UI
 *   can show a visual hint without mutating the list prematurely.
 *
 * @param lazyListState   The [LazyListState] of the target LazyColumn.
 * @param onMove          Callback invoked on drop. Receives (fromIndex, toIndex);
 *                        caller is responsible for mutating their own list.
 * @param scope           A [CoroutineScope] used for edge-scroll automation and
 *                        spring animations.
 * @param edgeScrollZone  Height in px from viewport edge that triggers auto-scroll.
 * @param edgeScrollSpeed Pixels scrolled per auto-scroll tick.
 */
class DragDropState(
    val lazyListState: LazyListState,
    private val onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    internal val scope: CoroutineScope,
    val edgeScrollZone: Float = 100f,
    val edgeScrollSpeed: Float = 14f,
) {
    // ── Dragging metadata ────────────────────────────────────────────────────

    /** Index of the item currently being dragged, or null when idle. */
    var draggingItemIndex by mutableStateOf<Int?>(null)
        internal set

    /**
     * Raw Y-offset (px) applied to the dragged item, driven directly by
     * pointer movement. This controls where the dragged item is drawn.
     */
    var draggingItemOffset by mutableFloatStateOf(0f)
        internal set

    /**
     * The index the dragged item *would* land on if released right now.
     * Other items smoothly animate out of the way based on this value.
     * The list is NOT mutated until [onDragEnd].
     */
    var pendingTargetIndex by mutableStateOf<Int?>(null)
        internal set

    /**
     * Per-item animated displacement (px). Key = item index.
     * Non-dragged items animate towards their displaced positions here.
     */
    internal val itemDisplacements = mutableMapOf<Int, Animatable<Float, *>>()

    private var draggingItemInitialOffset by mutableFloatStateOf(0f)
    private var scrollJob: Job? = null

    // ── Public read-only properties ──────────────────────────────────────────

    /** True while a drag is in progress. */
    val isDragging: Boolean get() = draggingItemIndex != null

    /**
     * The absolute Y-center (in list coordinates) of the item being dragged,
     * or 0f if no drag is active.
     */
    val draggingItemCenter: Float
        get() {
            val index = draggingItemIndex ?: return 0f
            val info = lazyListState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == index } ?: return 0f
            return info.offset + info.size / 2f + draggingItemOffset
        }

    // ── Internal gesture callbacks ────────────────────────────────────────────

    internal fun onDragStart(index: Int, touchOffsetWithinItem: Float) {
        draggingItemIndex = index
        pendingTargetIndex = index
        draggingItemInitialOffset = touchOffsetWithinItem
        draggingItemOffset = 0f
    }

    internal fun onDrag(delta: Float) {
        draggingItemOffset += delta
        updatePendingTarget()
        handleEdgeScroll()
    }

    internal fun onDragEnd() {
        val from = draggingItemIndex
        val to = pendingTargetIndex
        if (from != null && to != null && from != to) {
            onMove(from, to)
        }
        cleanup()
    }

    internal fun onDragCancel() = cleanup()

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Finds the item the drag center is currently hovering over and updates
     * [pendingTargetIndex]. Other items are then animated to their displaced
     * positions via [animateDisplacements].
     */
    private fun updatePendingTarget() {
        val currentIndex = draggingItemIndex ?: return
        val center = draggingItemCenter
        val newTarget = lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item ->
                item.index != currentIndex &&
                        center > item.offset &&
                        center < item.offset + item.size
            }
            ?.index

        if (newTarget != null && newTarget != pendingTargetIndex) {
            pendingTargetIndex = newTarget
            animateDisplacements(currentIndex, newTarget)
        }
    }

    /**
     * Animates every visible item into its displaced position given that item
     * [dragging] is being moved towards slot [target].
     *
     * Items between [dragging] and [target] shift by ±dragged-item-height;
     * all others snap back to 0.
     */
    private fun animateDisplacements(dragging: Int, target: Int) {
        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
        val draggedItem = visibleItems.firstOrNull { it.index == dragging } ?: return
        val itemHeight = draggedItem.size.toFloat()

        val range = if (dragging < target) dragging + 1..target
        else target until dragging

        visibleItems.forEach { item ->
            if (item.index == dragging) return@forEach

            val displacement = when (item.index) {
                in range -> if (dragging < target) -itemHeight else itemHeight
                else -> 0f
            }

            val animatable = itemDisplacements.getOrPut(item.index) {
                Animatable(0f)
            }
            scope.launch {
                animatable.animateTo(
                    targetValue = displacement,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                )
            }
        }
    }

    /** Returns the current animated displacement for an item at [index]. */
    fun displacementFor(index: Int): Float =
        itemDisplacements[index]?.value ?: 0f

    private fun handleEdgeScroll() {
        val layoutInfo = lazyListState.layoutInfo
        val center = draggingItemCenter
        val viewportEnd = layoutInfo.viewportEndOffset.toFloat()
        val viewportStart = layoutInfo.viewportStartOffset.toFloat()

        scrollJob?.cancel()
        scrollJob = when {
            center > viewportEnd - edgeScrollZone -> scope.launch {
                lazyListState.scrollBy(edgeScrollSpeed)
            }
            center < viewportStart + edgeScrollZone -> scope.launch {
                lazyListState.scrollBy(-edgeScrollSpeed)
            }
            else -> null
        }
    }

    private fun cleanup() {
        scrollJob?.cancel()
        // Snap all displaced items back to 0 with a spring
        scope.launch {
            itemDisplacements.values.forEach { anim ->
                launch {
                    anim.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    )
                }
            }
        }
        draggingItemIndex = null
        draggingItemOffset = 0f
        draggingItemInitialOffset = 0f
        pendingTargetIndex = null
        itemDisplacements.clear()
    }
}