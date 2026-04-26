package com.viwath.practice_module_app.drag_drop

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

// ─────────────────────────────────────────────────────────────────────────────
// Drag zones
// ─────────────────────────────────────────────────────────────────────────────

enum class DragZone { PINNED, MORE }

// ─────────────────────────────────────────────────────────────────────────────
// Drag info
// ─────────────────────────────────────────────────────────────────────────────

data class DragInfo(
    val widget: ActionWidget,
    val sourceZone: DragZone,
    val sourceIndex: Int,
    val fingerRootOffset: Offset = Offset.Zero,
)



class QuickAccessDragState {
    var dragging by mutableStateOf<DragInfo?>(null)

    /** Current (zone, index) → screen rect. Refreshed every frame by onGloballyPositioned. */
    val slotBounds = mutableStateMapOf<Pair<DragZone, Int>, Rect>()

    /**
     * widgetKey → rect captured RIGHT BEFORE a reorder fires.
     * Every displaced widget uses this as its animation start point.
     */
    val widgetPreMoveRect = HashMap<String, Rect>()

    // ── Drag lifecycle ────────────────────────────────────────────────────────

    fun startDrag(widget: ActionWidget, zone: DragZone, index: Int, rootOffset: Offset) {
        dragging = DragInfo(
            widget           = widget,
            sourceZone       = zone,
            sourceIndex      = index,
            fingerRootOffset = rootOffset,
        )
    }

    fun updateFinger(rootOffset: Offset) {
        dragging = dragging?.copy(fingerRootOffset = rootOffset)
    }

    fun updateSource(zone: DragZone, index: Int) {
        dragging = dragging?.copy(sourceZone = zone, sourceIndex = index)
    }

    fun endDrag() { dragging = null }

    // ── Hit testing ───────────────────────────────────────────────────────────

    fun hitTest(point: Offset): Pair<DragZone, Int>? =
        slotBounds.entries.firstOrNull { it.value.contains(point) }?.key

    fun slotsWhere(predicate: (Pair<DragZone, Int>) -> Boolean): List<Pair<DragZone, Int>> =
        slotBounds.keys.filter(predicate)

    // ── Pre-move snapshot ─────────────────────────────────────────────────────

    fun snapshotForReorder(widgetAtSlot: Map<Pair<DragZone, Int>, String>) {
        widgetPreMoveRect.clear()
        for ((slotKey, widgetKey) in widgetAtSlot) {
            val rect = slotBounds[slotKey] ?: continue
            widgetPreMoveRect[widgetKey] = rect
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Offset ↔ AnimationVector2D
// ─────────────────────────────────────────────────────────────────────────────

val OffsetVectorConverter = TwoWayConverter<Offset, AnimationVector2D>(
    convertToVector   = { AnimationVector2D(it.x, it.y) },
    convertFromVector = { Offset(it.v1, it.v2) }
)


@Composable
fun rememberSlotAnimatable(
    key: String,
    slotBounds: Map<Pair<DragZone, Int>, Rect>,
    zone: DragZone,
    currentIndex: Int,
): Animatable<Offset, AnimationVector2D> {
    val animatable = remember(key) {
        Animatable(initialValue = Offset.Zero, typeConverter = OffsetVectorConverter)
    }

    // Crucial: Track the index this specific widget held in the last frame
    val previousIndex = remember(key) { mutableIntStateOf(currentIndex) }

    LaunchedEffect(currentIndex) {
        val oldIdx = previousIndex.intValue

        // If the index changed, it means another item was inserted before/after it
        if (oldIdx != currentIndex) {
            val oldBounds = slotBounds[zone to oldIdx]
            val newBounds = slotBounds[zone to currentIndex]

            if (oldBounds != null && newBounds != null) {
                // Calculate the distance between the old slot and the new slot
                val deltaX = oldBounds.left - newBounds.left
                val deltaY = oldBounds.top - newBounds.top

                animatable.stop()
                // Snap to the physical location of the OLD slot
                animatable.snapTo(Offset(deltaX, deltaY))
                // Animate "back" to the NEW slot (which is Offset.Zero)
                animatable.animateTo(
                    targetValue = Offset.Zero,
                    animationSpec = spring(
                        dampingRatio = 0.85f,
                        stiffness = 600f // Adjust for "snappiness"
                    )
                )
            }
            previousIndex.intValue = currentIndex
        }
    }
    return animatable
}
