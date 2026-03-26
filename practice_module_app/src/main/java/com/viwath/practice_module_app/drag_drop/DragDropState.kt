package com.viwath.practice_module_app.drag_drop

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Drag & Drop Abstractions
// ─────────────────────────────────────────────────────────────────────────────

interface DragDropLazyLayoutState {
    val layoutInfo: DragDropLayoutInfo
    suspend fun scrollBy(value: Float): Float
}

interface DragDropLayoutInfo {
    val visibleItemsInfo: List<DragDropItemInfo>
    val viewportStartOffset: Int
    val viewportEndOffset: Int
}

interface DragDropItemInfo {
    val index: Int
    val offset: IntOffset
    val size: IntSize
}

// ── LazyList implementation ──────────────────────────────────────────────────

class DragDropLazyListState(private val state: LazyListState) : DragDropLazyLayoutState {
    override val layoutInfo: DragDropLayoutInfo
        get() = DragDropLazyListLayoutInfo(state.layoutInfo)

    override suspend fun scrollBy(value: Float): Float = state.scrollBy(value)
}

class DragDropLazyListLayoutInfo(private val info: LazyListLayoutInfo) : DragDropLayoutInfo {
    override val visibleItemsInfo: List<DragDropItemInfo>
        get() = info.visibleItemsInfo.map { DragDropLazyListItemInfo(it) }
    override val viewportStartOffset: Int get() = info.viewportStartOffset
    override val viewportEndOffset: Int get() = info.viewportEndOffset
}

class DragDropLazyListItemInfo(private val info: LazyListItemInfo) : DragDropItemInfo {
    override val index: Int get() = info.index
    override val offset: IntOffset get() = IntOffset(0, info.offset)
    override val size: IntSize get() = IntSize(0, info.size)
}

// ── LazyGrid implementation ──────────────────────────────────────────────────

class DragDropLazyGridState(private val state: LazyGridState) : DragDropLazyLayoutState {
    override val layoutInfo: DragDropLayoutInfo
        get() = DragDropLazyGridLayoutInfo(state.layoutInfo)

    override suspend fun scrollBy(value: Float): Float = state.scrollBy(value)
}

class DragDropLazyGridLayoutInfo(private val info: LazyGridLayoutInfo) : DragDropLayoutInfo {
    override val visibleItemsInfo: List<DragDropItemInfo>
        get() = info.visibleItemsInfo.map { DragDropLazyGridItemInfo(it) }
    override val viewportStartOffset: Int get() = info.viewportStartOffset
    override val viewportEndOffset: Int get() = info.viewportEndOffset
}

class DragDropLazyGridItemInfo(private val info: LazyGridItemInfo) : DragDropItemInfo {
    override val index: Int get() = info.index
    override val offset: IntOffset get() = info.offset
    override val size: IntSize get() = info.size
}

// ─────────────────────────────────────────────────────────────────────────────
// Animation constants
// ─────────────────────────────────────────────────────────────────────────────

const val DRAG_SCALE = 1.05f
const val DRAG_ELEVATION_BOOST_DP = 10

/**
 * Spring spec for items shuffling out of the way during a live swap.
 * Slightly stiffer / less bouncy than the original so rapid swaps feel crisp.
 */
private val ShiftSpring = spring<Offset>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness    = Spring.StiffnessMediumLow,
)

private val SettleSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness    = Spring.StiffnessMedium,
)

private val SettleSpringOffset = spring<Offset>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness    = Spring.StiffnessMediumLow,
)

// ─────────────────────────────────────────────────────────────────────────────
// DragDropState
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Production-ready state holder for drag-and-drop reordering.
 *
 * ### Live-swap behaviour (key change vs original)
 * Items swap their **data positions** the moment the dragged item's centre
 * crosses the halfway point of a neighbour — exactly like iOS/Android launcher
 * reordering.  [onMove] fires immediately on each swap, so the backing list
 * stays in sync with what the user sees.  [draggingItemIndex] tracks the
 * dragged item through every intermediate swap, keeping it glued to the
 * finger at all times.
 *
 * The old "pending target on drop" approach is removed; drop now only triggers
 * cleanup + settle animations.
 */
class DragDropState(
    private val layoutState: DragDropLazyLayoutState,
    private val onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    internal val scope: CoroutineScope,
    val edgeScrollZone: Float = 100f,
    val edgeScrollSpeed: Float = 14f,
) {

    // ── Core drag state ──────────────────────────────────────────────────────

    /** Index of the item currently held by the user, or null when idle. */
    var draggingItemIndex by mutableStateOf<Int?>(null)
        internal set

    /** Raw accumulated pointer delta (px) — drives the floating item. */
    var draggingItemOffset by mutableStateOf(Offset.Zero)
        internal set

    // ── Visual effect animatables ────────────────────────────────────────────

    internal val dragScale    = Animatable(1f)
    internal val dragRotation = Animatable(0f)
    internal val floatingOffset = Animatable(Offset.Zero, Offset.VectorConverter)

    /** Per-item spring displacement used for the brief "hop" on live swap. */
    internal val itemDisplacements =
        mutableMapOf<Int, Animatable<Offset, AnimationVector2D>>()

    // ── Private bookkeeping ──────────────────────────────────────────────────

    private var scrollJob: Job? = null

    /**
     * The last [DragDropItemInfo] snapshot of the dragged item, captured at
     * drag-start and updated after every live swap so we always know the item's
     * original viewport rectangle even when the list recomposes.
     */
    private var draggedItemSnapshot: DragDropItemInfo? = null

    // ── Public helpers ───────────────────────────────────────────────────────

    val isDragging: Boolean get() = draggingItemIndex != null

    /**
     * Absolute centre of the dragged item in viewport coordinates.
     * Combines the item's layout position with the accumulated drag offset.
     */
    val draggingItemCenter: Offset
        get() {
            val snapshot = draggedItemSnapshot ?: return Offset.Zero
            return Offset(
                x = snapshot.offset.x + snapshot.size.width  / 2f + draggingItemOffset.x,
                y = snapshot.offset.y + snapshot.size.height / 2f + draggingItemOffset.y,
            )
        }

    fun displacementFor(index: Int): Offset =
        itemDisplacements[index]?.value ?: Offset.Zero

    // ── Gesture entry points ─────────────────────────────────────────────────

    internal fun onDragStart(index: Int) {
        draggingItemIndex  = index
        draggingItemOffset = Offset.Zero

        // Capture layout snapshot so draggingItemCenter works before first recompose
        draggedItemSnapshot = layoutState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == index }

        scope.launch {
            launch {
                dragScale.animateTo(
                    targetValue    = DRAG_SCALE,
                    animationSpec  = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness    = Spring.StiffnessMedium,
                    ),
                )
            }
            launch {
                dragRotation.animateTo(
                    targetValue   = 2f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioHighBouncy,
                        stiffness    = Spring.StiffnessLow,
                    ),
                )
            }
            launch {
                floatingOffset.animateTo(
                    targetValue   = Offset(0f, -10f),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessLow,
                    ),
                )
            }
        }
    }

    internal fun onDrag(delta: Offset) {
        draggingItemOffset += delta
        checkAndSwap()
        handleEdgeScroll()
    }

    internal fun onDragEnd()    = cleanup()
    internal fun onDragCancel() = cleanup()

    // ── Live-swap logic ───────────────────────────────────────────────────────

    /**
     * Called on every drag delta.
     *
     * Finds the item whose **centre** is closest to [draggingItemCenter]
     * (excluding the dragged item itself).  When that neighbour's index
     * differs from [draggingItemIndex], we:
     *
     * 1. Trigger a brief spring "hop" on the neighbour (visual feedback).
     * 2. Call [onMove] so the backing list swaps immediately.
     * 3. Update [draggingItemIndex] to the new logical position so subsequent
     *    centre calculations stay correct.
     * 4. Refresh [draggedItemSnapshot] from the post-recompose layout, keeping
     *    the floating item anchored to the finger.
     */
    private fun checkAndSwap() {
        val currentIndex = draggingItemIndex ?: return
        val center       = draggingItemCenter
        val visibleItems = layoutState.layoutInfo.visibleItemsInfo

        // Find the neighbour whose centre is closest to the drag centre.
        val closest = visibleItems
            .filter { it.index != currentIndex }
            .minByOrNull { item ->
                val cx = item.offset.x + item.size.width  / 2f
                val cy = item.offset.y + item.size.height / 2f
                val dx = cx - center.x
                val dy = cy - center.y
                dx * dx + dy * dy
            } ?: return

        // Only swap once the drag centre has crossed the neighbour's mid-point.
        val neighbourCx = closest.offset.x + closest.size.width  / 2f
        val neighbourCy = closest.offset.y + closest.size.height / 2f

        val dragCrossedX = when {
            currentIndex < closest.index -> center.x >= neighbourCx
            currentIndex > closest.index -> center.x <= neighbourCx
            else                         -> true   // same column (list)
        }
        val dragCrossedY = when {
            currentIndex < closest.index -> center.y >= neighbourCy
            currentIndex > closest.index -> center.y <= neighbourCy
            else                         -> true
        }

        // For a vertical list only the Y axis matters; for a grid both may apply.
        // We treat them independently: swap when either axis is crossed.
        val shouldSwap = dragCrossedY || dragCrossedX

        if (!shouldSwap || closest.index == currentIndex) return

        // ── 1. Brief hop animation on the neighbour ──────────────────────────
        val hopOffset = Offset(
            x = (draggedItemSnapshot?.let { (it.offset.x - closest.offset.x).toFloat() } ?: 0f) * 0.18f,
            y = (draggedItemSnapshot?.let { (it.offset.y - closest.offset.y).toFloat() } ?: 0f) * 0.18f,
        )
        val anim = itemDisplacements.getOrPut(closest.index) {
            Animatable(Offset.Zero, Offset.VectorConverter)
        }
        scope.launch {
            // Quick nudge toward where the dragged item was, then spring back.
            anim.snapTo(hopOffset)
            anim.animateTo(Offset.Zero, ShiftSpring)
        }

        // ── 2. Commit the swap in the backing list ───────────────────────────
        onMove(currentIndex, closest.index)

        // ── 3. Follow the item to its new index ──────────────────────────────
        draggingItemIndex = closest.index

        // ── 4. Recapture the snapshot at the new index ───────────────────────
        //    Layout may not have recomposed yet, so we update after a frame.
        scope.launch {
            // Wait one frame for the list to recompose with the new order.
            kotlinx.coroutines.yield()
            draggedItemSnapshot = layoutState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == closest.index }
                ?: draggedItemSnapshot
        }
    }

    // ── Edge-scroll ───────────────────────────────────────────────────────────

    private fun handleEdgeScroll() {
        val info         = layoutState.layoutInfo
        val center       = draggingItemCenter
        val viewportEnd  = info.viewportEndOffset.toFloat()
        val viewportStart = info.viewportStartOffset.toFloat()

        scrollJob?.cancel()

        val scrollAmount = when {
            center.y > viewportEnd   - edgeScrollZone ->  edgeScrollSpeed
            center.y < viewportStart + edgeScrollZone -> -edgeScrollSpeed
            else                                      ->  0f
        }

        if (scrollAmount != 0f) {
            scrollJob = scope.launch {
                layoutState.scrollBy(scrollAmount)
                // Re-evaluate swaps after scroll shifts viewport
                checkAndSwap()
            }
        }
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    private fun cleanup() {
        scrollJob?.cancel()

        draggingItemIndex   = null
        draggingItemOffset  = Offset.Zero
        draggedItemSnapshot = null

        val snapshot = itemDisplacements.toMap()
        scope.launch {
            val jobs = buildList {
                add(launch { dragScale.animateTo(1f, SettleSpring) })
                add(launch { dragRotation.animateTo(0f, SettleSpring) })
                add(launch { floatingOffset.animateTo(Offset.Zero, SettleSpringOffset) })
                snapshot.values.forEach { anim ->
                    add(launch { anim.animateTo(Offset.Zero, SettleSpringOffset) })
                }
            }
            jobs.joinAll()
            itemDisplacements.clear()
        }
    }
}