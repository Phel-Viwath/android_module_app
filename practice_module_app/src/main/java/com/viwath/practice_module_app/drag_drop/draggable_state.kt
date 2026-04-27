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

val OffsetVectorConverter = TwoWayConverter<Offset, AnimationVector2D>(
    convertToVector   = { AnimationVector2D(it.x, it.y) },
    convertFromVector = { Offset(it.v1, it.v2) }
)

class SlotAnim(
    val animatable: Animatable<Offset, AnimationVector2D>,
) {
    var pendingSnap by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberSlotAnimatable(
    key: String,
    dragState: QuickAccessDragState,
    zone: DragZone,
    currentIndex: Int,
): SlotAnim {
    val slotAnim = remember(key) {
        SlotAnim(Animatable(Offset.Zero, OffsetVectorConverter))
    }
    val currentBounds = dragState.slotBounds[zone to currentIndex]

    if (currentBounds != null) {
        val previousRect = dragState.widgetLastRect[key]
        if (previousRect == null) {

            dragState.widgetLastRect[key] = currentBounds
        } else if (previousRect != currentBounds) {

            val deltaX = previousRect.left - currentBounds.left
            val deltaY = previousRect.top  - currentBounds.top


            val carry = slotAnim.animatable.value
            slotAnim.pendingSnap = Offset(deltaX + carry.x, deltaY + carry.y)

            dragState.widgetLastRect[key] = currentBounds
        }
    }

    LaunchedEffect(key, currentIndex, currentBounds) {
        val snap = slotAnim.pendingSnap
        if (snap == Offset.Zero) return@LaunchedEffect

        slotAnim.animatable.snapTo(snap)
        slotAnim.pendingSnap = Offset.Zero

        slotAnim.animatable.animateTo(
            targetValue = Offset.Zero,
            animationSpec = spring(
                dampingRatio = 0.85f,
                stiffness    = 600f,
            )
        )
    }

    return slotAnim
}