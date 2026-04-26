package com.viwath.practice_module_app.drag_drop

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

// ─────────────────────────────────────────────────────────────────────────────
// Drag zones
// ─────────────────────────────────────────────────────────────────────────────

enum class DragZone { PINNED, MORE }

// ─────────────────────────────────────────────────────────────────────────────
// Drag info — what is being dragged and where
// ─────────────────────────────────────────────────────────────────────────────

data class DragInfo(
    val widget: ActionWidget,
    val sourceZone: DragZone,
    val sourceIndex: Int,
    val fingerRootOffset: Offset = Offset.Zero,
)

// ─────────────────────────────────────────────────────────────────────────────
// Shared drag state — holds the active drag and all slot bounds
// Reusable: any grid that needs long-press drag can hold one of these
// ─────────────────────────────────────────────────────────────────────────────

class QuickAccessDragState {
    var dragging by mutableStateOf<DragInfo?>(null)

    /** Maps (zone, flatIndex) → screen Rect, updated by onGloballyPositioned */
    val slotBounds = mutableStateMapOf<Pair<DragZone, Int>, Rect>()

    fun startDrag(widget: ActionWidget, zone: DragZone, index: Int, rootOffset: Offset) {
        dragging = DragInfo(
            widget            = widget,
            sourceZone        = zone,
            sourceIndex       = index,
            fingerRootOffset  = rootOffset,
        )
    }

    fun updateFinger(rootOffset: Offset) {
        dragging = dragging?.copy(fingerRootOffset = rootOffset)
    }

    fun updateSource(zone: DragZone, index: Int) {
        dragging = dragging?.copy(sourceZone = zone, sourceIndex = index)
    }

    fun endDrag() {
        dragging = null
    }

    /** Returns the slot (zone, index) whose bounds contain [point], or null. */
    fun hitTest(point: Offset): Pair<DragZone, Int>? =
        slotBounds.entries.firstOrNull { it.value.contains(point) }?.key

    /** Returns all slot keys matching the predicate. */
    fun slotsWhere(predicate: (Pair<DragZone, Int>) -> Boolean): List<Pair<DragZone, Int>> =
        slotBounds.keys.filter(predicate)
}

// ─────────────────────────────────────────────────────────────────────────────
// Offset ↔ AnimationVector2D converter
// Reusable for any Animatable<Offset, …> usage
// ─────────────────────────────────────────────────────────────────────────────

val OffsetVectorConverter = TwoWayConverter<Offset, AnimationVector2D>(
    convertToVector   = { AnimationVector2D(it.x, it.y) },
    convertFromVector = { Offset(it.v1, it.v2) }
)

// ─────────────────────────────────────────────────────────────────────────────
// rememberSlotAnimatable
// Reusable: give it a stable key (widget identity), and it animates the widget
// from its old slot position to its new one whenever currentIndex changes.
// ─────────────────────────────────────────────────────────────────────────────

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
    val previousIndex = remember(key) { mutableIntStateOf(currentIndex) }

    LaunchedEffect(key, currentIndex) {
        val oldIndex = previousIndex.intValue
        if (oldIndex != currentIndex) {
            val oldBounds = slotBounds[zone to oldIndex]
            val newBounds = slotBounds[zone to currentIndex]

            if (oldBounds != null && newBounds != null) {
                animatable.stop()
                animatable.snapTo(
                    Offset(
                        x = oldBounds.left - newBounds.left,
                        y = oldBounds.top  - newBounds.top,
                    )
                )
                animatable.animateTo(
                    targetValue   = Offset.Zero,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 1700f)
                )
            }
            previousIndex.intValue = currentIndex
        }
    }

    return animatable
}