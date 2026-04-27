package com.viwath.practice_module_app.drag_drop

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

    val slotBounds = mutableStateMapOf<Pair<DragZone, Int>, Rect>()

    val widgetLastRect = HashMap<String, Rect>()

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
    dragState: QuickAccessDragState,
    zone: DragZone,
    currentIndex: Int,
): Animatable<Offset, AnimationVector2D> {
    val animatable = remember(key) {
        Animatable(initialValue = Offset.Zero, typeConverter = OffsetVectorConverter)
    }

    val currentBounds = dragState.slotBounds[zone to currentIndex]

    LaunchedEffect(key, currentIndex, currentBounds) {
        if (currentBounds == null) return@LaunchedEffect

        val previousRect = dragState.widgetLastRect[key]

        // First-ever record for this widget — no animation, just remember it.
        if (previousRect == null) {
            dragState.widgetLastRect[key] = currentBounds
            return@LaunchedEffect
        }

        // Same physical position — nothing to animate.
        if (previousRect == currentBounds) return@LaunchedEffect

        // Compute the jump.
        val deltaX = previousRect.left - currentBounds.left
        val deltaY = previousRect.top  - currentBounds.top

        // Persist new resting rect BEFORE animating so a follow-up reorder
        // mid-animation reads the correct base.
        dragState.widgetLastRect[key] = currentBounds

        // If a previous animation hasn't finished, fold its current visual
        // offset into the start so we keep moving smoothly instead of jumping.
        val startOffset = Offset(deltaX, deltaY) + animatable.value

        if (startOffset.getDistance() > 0.5f) {
            animatable.snapTo(startOffset)
            animatable.animateTo(
                targetValue = Offset.Zero,
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness    = 350f,
                )
            )
        }
    }

    return animatable
}