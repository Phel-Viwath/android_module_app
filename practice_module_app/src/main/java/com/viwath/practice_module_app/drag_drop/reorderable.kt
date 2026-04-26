package com.viwath.practice_module_app.drag_drop

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.viwath.practice_module_app.drag_drop.GridPops.DUMMY
import com.viwath.practice_module_app.drag_drop.GridPops.MORE_COLS
import com.viwath.practice_module_app.drag_drop.GridPops.MORE_SIZE
import com.viwath.practice_module_app.drag_drop.GridPops.PINNED_COLS
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


// ─────────────────────────────────────────────────────────────────────────────
// Offset Animatable helper — tracks each widget's animated displacement
// ─────────────────────────────────────────────────────────────────────────────

private val OffsetVectorConverter =
    TwoWayConverter<Offset, AnimationVector2D>(
        convertToVector = {
            AnimationVector2D(it.x, it.y)
        },
        convertFromVector = {
            Offset(it.v1, it.v2)
        }
    )

@Composable
private fun rememberSlotAnimatable(
    key: String,
    slotBounds: Map<Pair<DragZone, Int>, Rect>,
    zone: DragZone,
    currentIndex: Int
): Animatable<Offset, AnimationVector2D> {

    val animatable = remember(key) {
        Animatable(
            initialValue = Offset.Zero,
            typeConverter = OffsetVectorConverter
        )
    }

    val previousIndex = remember(key) {
        mutableIntStateOf(currentIndex)
    }

    LaunchedEffect(key, currentIndex) {
        val oldIndex = previousIndex.intValue

        if (oldIndex != currentIndex) {
            val oldBounds = slotBounds[zone to oldIndex]
            val newBounds = slotBounds[zone to currentIndex]

            if (oldBounds != null && newBounds != null) {
                val startOffset = Offset(
                    x = oldBounds.left - newBounds.left,
                    y = oldBounds.top - newBounds.top
                )

                // stop old animation first
                animatable.stop()

                // instantly keep old visual position
                animatable.snapTo(startOffset)

                // animate into new slot
                animatable.animateTo(
                    targetValue = Offset.Zero,
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = 1700f
                    )
                )
            }

            previousIndex.intValue = currentIndex
        }
    }

    return animatable
}


@Suppress("DEPRECATION")
@Composable
fun QuickAccessMenuTestScreen(vm: QuickAccessTestViewModel = viewModel()) {
    val state by vm.state

    Scaffold(
        containerColor = Color(0xFF0A0E1A),
        bottomBar = {
            if (state.isEditMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF111827))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick  = { vm.exitEditMode() },
                        modifier = Modifier.weight(1f),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border   = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = SolidColor(Color.White.copy(0.3f))
                        )
                    ) { Text("Cancel") }

                    Button(
                        onClick  = { vm.saveLayout() },
                        modifier = Modifier.weight(1f),
                        enabled  = state.enableSave,
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = Color(0xFFE84C1E),
                            disabledContainerColor = Color(0xFFE84C1E).copy(alpha = 0.4f)
                        )
                    ) { Text("Save") }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0A0E1A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A2235))
                    .padding(vertical = 20.dp)
            ) {
                QuickAccessMenuDragGrid(
                    pinnedWidgets      = state.pinnedWidgets,
                    moreWidgets        = state.moreWidgets,
                    isEditMode         = state.isEditMode,
                    canAddToPinned     = state.canAddToPinned,
                    enableReset        = state.enableReset,
                    onEnterEditMode    = { vm.enterEditMode() },
                    onReset            = { vm.reset() },
                    onRemoveFromPinned = { vm.removeFromPinned(it) },
                    onAddToPinned      = { vm.addToPinned(it) },
                    onMovePinned       = { from, to -> vm.movePinned(from, to) },
                    onMoveMore         = { from, to -> vm.moveMore(from, to) },
                    onMovePinnedToMore = { vm.movePinnedToMore(it) },
                    onSwapPinnedToMore = { pinned, more -> vm.swapPinnedToMore(pinned, more) },
                    onMoveMoreToPinned = { vm.moveMoreToPinned(it) },
                    onSwapMoreToPinned = { more, pinned -> vm.swapMoreToPinned(more, pinned) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main drag grid
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickAccessMenuDragGrid(
    pinnedWidgets: List<ActionWidget>,
    moreWidgets: List<List<ActionWidget>>,
    isEditMode: Boolean,
    canAddToPinned: Boolean,
    enableReset: Boolean,
    onEnterEditMode: () -> Unit,
    onReset: () -> Unit,
    onRemoveFromPinned: (Int) -> Unit,
    onAddToPinned: (ActionWidget) -> Unit,
    onMovePinned: (Int, Int) -> Unit,
    onMoveMore: (Int, Int) -> Unit,
    onMovePinnedToMore: (Int) -> Unit,
    onSwapPinnedToMore: (Int, Int) -> Unit,
    onMoveMoreToPinned: (Int) -> Unit,
    onSwapMoreToPinned: (Int, Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val dragState = remember { QuickAccessDragState() }

    val pagerState = rememberPagerState(pageCount = { moreWidgets.size.coerceAtLeast(1) })
    val morePage by remember { derivedStateOf { pagerState.currentPage } }
    var pagerRootBounds by remember { mutableStateOf<Rect?>(null) }
    var containerRootOffset by remember { mutableStateOf(Offset.Zero) }

    var crossZoneHoverJob by remember {
        mutableStateOf<Job?>(null)
    }

    LaunchedEffect(moreWidgets.size) {
        if (pagerState.currentPage >= moreWidgets.size)
            pagerState.animateScrollToPage((moreWidgets.size - 1).coerceAtLeast(0))
    }

    val onDragUpdate: (Offset) -> Unit = { rootOffset ->
        dragState.dragging = dragState.dragging?.copy(fingerRootOffset = rootOffset)

        dragState.dragging?.let { info ->
            if (info.sourceZone == DragZone.PINNED) {
                val hovered = dragState.slotBounds.entries
                    .firstOrNull { (key, rect) ->
                        key.first == DragZone.PINNED &&
                                key.second != info.sourceIndex &&
                                rect.contains(rootOffset)
                    }
                if (hovered != null) {
                    val toIdx = hovered.key.second
                    onMovePinned(info.sourceIndex, toIdx)
                    dragState.dragging = info.copy(
                        sourceIndex = toIdx,
                        fingerRootOffset = rootOffset
                    )
                }
            }


            if (info.sourceZone == DragZone.MORE) {
                // Determine if we are hovering over the CURRENT page's slots
                val hovered = dragState.slotBounds.entries
                    .firstOrNull { (key, rect) ->
                        key.first == DragZone.MORE &&
                                rect.contains(rootOffset) &&
                                (key.second / MORE_SIZE == morePage) // ONLY allow reorder on current page
                    }

                if (hovered != null) {
                    val toIdx = hovered.key.second

                    if (toIdx != info.sourceIndex) {
                        onMoveMore(info.sourceIndex, toIdx)

                        dragState.dragging = info.copy(
                            sourceIndex = toIdx,
                            fingerRootOffset = rootOffset
                        )
                    }
                }
            }
        }

        // drag cross
        dragState.dragging?.let { info ->

            val hoveredCrossZone = dragState.slotBounds.entries
                .firstOrNull { (key, rect) ->
                    key.first != info.sourceZone &&
                            rect.contains(rootOffset) &&
                            (key.first != DragZone.MORE || key.second / MORE_SIZE == morePage)
                }

            if (hoveredCrossZone != null) {
                val (targetZone, targetIdx) = hoveredCrossZone.key

                crossZoneHoverJob?.cancel()

                crossZoneHoverJob = scope.launch {
                    delay(300L) // ABA feeling delay

                    val latest = dragState.dragging ?: return@launch

                    when (latest.sourceZone) {
                        DragZone.PINNED -> {
                            if (targetZone == DragZone.MORE) {
                                if (moreWidgets.getOrElse(morePage) { emptyList() }.size >= MORE_SIZE) {
                                    onSwapPinnedToMore(latest.sourceIndex, targetIdx)
                                } else {
                                    onMovePinnedToMore(latest.sourceIndex)
                                }

                                dragState.dragging = latest.copy(
                                    sourceZone = DragZone.MORE,
                                    sourceIndex = targetIdx
                                )
                            }
                        }

                        DragZone.MORE -> {
                            if (targetZone == DragZone.PINNED) {
                                if (pinnedWidgets.getOrNull(targetIdx)?.action == DUMMY) {
                                    onMoveMoreToPinned(latest.sourceIndex)
                                } else {
                                    onSwapMoreToPinned(latest.sourceIndex, targetIdx)
                                }

                                dragState.dragging = latest.copy(
                                    sourceZone = DragZone.PINNED,
                                    sourceIndex = targetIdx
                                )
                            }
                        }
                    }
                }
            } else {
                crossZoneHoverJob?.cancel()
            }
        }

        // Auto-scroll pages when dragging near the HorizontalPager edges
        // Only trigger if we're dragging a MORE widget (cross-zone drag)
        dragState.dragging?.let {
            if (it.sourceZone != DragZone.MORE) return@let  // ← ADD THIS GUARD

            pagerRootBounds?.let { bounds ->
                val relX = it.fingerRootOffset.x - bounds.left
                val edge = bounds.width * 0.20f
                when {
                    relX < edge && morePage > 0 -> {
                        if (pagerState.isScrollInProgress.not()) {
                            scope.launch {
                                val keysToRemove = dragState.slotBounds.keys.filter { p ->
                                    p.first == DragZone.MORE && p.second / MORE_SIZE != morePage
                                }
                                keysToRemove.forEach { p -> dragState.slotBounds.remove(p) }
                                pagerState.animateScrollToPage(morePage - 1)
                            }
                        }
                    }
                    relX > bounds.width - edge && morePage < moreWidgets.size - 1 -> {
                        if (pagerState.isScrollInProgress.not()) {
                            scope.launch {
                                val keysToRemove = dragState.slotBounds.keys.filter { p ->
                                    p.first == DragZone.MORE && p.second / MORE_SIZE != morePage
                                }
                                keysToRemove.forEach { p -> dragState.slotBounds.remove(p) }
                                pagerState.animateScrollToPage(morePage + 1)
                            }
                        }
                    }
                }
            }
        }
    }

    val onDragEnd: (Offset) -> Unit = { fingerOffset ->
        resolveDropOnEnd(
            fingerOffset = fingerOffset,
            dragState = dragState,
            morePage = morePage,
            moreWidgets = moreWidgets,
            pinnedWidgets = pinnedWidgets,
            onMovePinnedToMore = onMovePinnedToMore,
            onSwapPinnedToMore = onSwapPinnedToMore,
            onMoveMoreToPinned = onMoveMoreToPinned,
            onSwapMoreToPinned = onSwapMoreToPinned
        )
        dragState.dragging = null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { containerRootOffset = it.boundsInRoot().topLeft }
            .then(
                if (isEditMode) {
                    Modifier.pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { localOffset ->
                                val rootOffset = localOffset + containerRootOffset
                                // Find which item was hit
                                val hit = dragState.slotBounds.entries.firstOrNull {
                                    it.value.contains(rootOffset)
                                }
                                if (hit != null) {
                                    val (zone, index) = hit.key
                                    val widget = when (zone) {
                                        DragZone.PINNED -> pinnedWidgets.getOrNull(index)
                                        DragZone.MORE -> moreWidgets.flatten().getOrNull(index)
                                    }
                                    if (widget != null && widget.action != DUMMY) {
                                        dragState.dragging = DragInfo(
                                            widget = widget,
                                            sourceZone = zone,
                                            sourceIndex = index,
                                            fingerRootOffset = rootOffset
                                        )
                                        onDragUpdate(rootOffset)
                                    }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragState.dragging?.let {
                                    onDragUpdate(it.fingerRootOffset + dragAmount)
                                }
                            },
                            onDragEnd = { onDragEnd(dragState.dragging?.fingerRootOffset ?: Offset.Zero) },
                            onDragCancel = { onDragEnd(dragState.dragging?.fingerRootOffset ?: Offset.Zero) }
                        )
                    }
                } else Modifier
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Pinned header ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "📌  Pinned",
                    color      = Color.White,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (isEditMode) {
                    Text(
                        text       = "Reset",
                        color      = if (enableReset) Color(0xFFFF6B3D) else Color.White.copy(0.2f),
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier.clickable(enabled = enableReset) { onReset() }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Pinned 3-col manual grid ──────────────────────────────────
            val pinnedRows = (pinnedWidgets.size + PINNED_COLS - 1) / PINNED_COLS
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (row in 0 until pinnedRows) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (col in 0 until PINNED_COLS) {
                            val index = row * PINNED_COLS + col
                            if (index < pinnedWidgets.size) {
                                val widget = pinnedWidgets[index]
                                val isDraggingThis = dragState.dragging?.let {
                                    it.sourceZone == DragZone.PINNED && it.sourceIndex == index
                                } ?: false
                                val isDropTarget = dragState.dragging?.let { info ->
                                    !(info.sourceZone == DragZone.PINNED && info.sourceIndex == index) &&
                                            dragState.slotBounds[DragZone.PINNED to index]
                                                ?.contains(info.fingerRootOffset) == true
                                } ?: false

                                // ── Per-item position animation ──────────
                                // key = widget.action so the animatable follows
                                // the *widget identity*, not the slot index.
                                val posAnim = rememberSlotAnimatable(
                                    key          = widget.action,
                                    slotBounds   = dragState.slotBounds,
                                    zone         = DragZone.PINNED,
                                    currentIndex = index
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .onGloballyPositioned {
                                            dragState.slotBounds[DragZone.PINNED to index] =
                                                it.boundsInRoot()
                                        }
                                        .graphicsLayer {
                                            // Combine drag-hide alpha with position offset
                                            alpha       = if (isDraggingThis) 0f else 1f
                                            translationX = posAnim.value.x
                                            translationY = posAnim.value.y
                                        }
                                        .then(
                                            if (isDropTarget) Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFFE84C1E).copy(alpha = 0.18f))
                                            else Modifier
                                        )
                                ) {
                                    PinnedWidgetCard(
                                        widget      = widget,
                                        isDragging  = isDraggingThis,
                                        isEditMode  = isEditMode,
                                        onRemove    = { onRemoveFromPinned(index) },
                                        onLongClick = onEnterEditMode
                                    )
                                }
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── More header ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "⋯  More",
                    color      = Color.White,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (moreWidgets.size > 1) {
                    Text(
                        text     = "${morePage + 1} / ${moreWidgets.size}",
                        color    = Color.White.copy(0.4f),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── More pager — each page is a manual 4-col grid ────────────
            var maxMoreHeightPx by remember { mutableIntStateOf(0) }
            val density = LocalDensity.current

            HorizontalPager(
                state    = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (maxMoreHeightPx > 0)
                            Modifier.height(with(density) { maxMoreHeightPx.toDp() })
                        else
                            Modifier.wrapContentHeight()
                    )
                    .onGloballyPositioned { pagerRootBounds = it.boundsInRoot() },
                userScrollEnabled = !isEditMode && dragState.dragging == null
            ) { page ->
                val pageWidgets = moreWidgets.getOrElse(page) { emptyList() }
                val pageRows    = (pageWidgets.size + MORE_COLS - 1) / MORE_COLS

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .onSizeChanged { size ->
                            if (size.height > maxMoreHeightPx) maxMoreHeightPx = size.height
                        },
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (row in 0 until pageRows) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0 until MORE_COLS) {
                                val localIdx = row * MORE_COLS + col
                                val flatIdx  = page * MORE_SIZE + localIdx
                                if (localIdx < pageWidgets.size) {
                                    val widget = pageWidgets[localIdx]
                                    val isDraggingThis = dragState.dragging?.let {
                                        it.sourceZone == DragZone.MORE && it.sourceIndex == flatIdx
                                    } ?: false
                                    val isDropTarget = dragState.dragging?.let { info ->
                                        !(info.sourceZone == DragZone.MORE && info.sourceIndex == flatIdx) &&
                                                dragState.slotBounds[DragZone.MORE to flatIdx]
                                                    ?.contains(info.fingerRootOffset) == true
                                    } ?: false

                                    // ── Per-item position animation ──────
                                    val posAnim = rememberSlotAnimatable(
                                        key          = widget.action,
                                        slotBounds   = dragState.slotBounds,
                                        zone         = DragZone.MORE,
                                        currentIndex = flatIdx
                                    )

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .onGloballyPositioned {
                                                dragState.slotBounds[DragZone.MORE to flatIdx] =
                                                    it.boundsInRoot()
                                            }
                                            .graphicsLayer {
                                                alpha        = if (isDraggingThis) 0f else 1f
                                                translationX = posAnim.value.x
                                                translationY = posAnim.value.y
                                            }
                                            .then(
                                                if (isDropTarget) Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFFE84C1E).copy(alpha = 0.18f))
                                                else Modifier
                                            )
                                    ) {
                                        MoreWidgetCard(
                                            widget      = widget,
                                            isDragging  = isDraggingThis,
                                            isEditMode  = isEditMode,
                                            canAdd      = canAddToPinned,
                                            onAdd       = { onAddToPinned(widget) },
                                            onLongClick = onEnterEditMode
                                        )
                                    }
                                } else {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // ── Dot indicator ─────────────────────────────────────────────
            if (moreWidgets.size > 1) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    repeat(moreWidgets.size) { index ->
                        val selected = morePage == index
                        val dotSize by animateDpAsState(
                            targetValue   = if (selected) 8.dp else 5.dp,
                            animationSpec = tween(200),
                            label         = "dot_$index"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(dotSize)
                                .clip(CircleShape)
                                .background(
                                    if (selected) Color(0xFFE84C1E)
                                    else Color.White.copy(0.25f)
                                )
                        )
                    }
                }
            }
        }

        // Ghost — floats above everything
        dragState.dragging?.let { info ->
            DragGhost(
                widget              = info.widget,
                offset              = info.fingerRootOffset,
                isPinned            = info.sourceZone == DragZone.PINNED,
                containerRootOffset = containerRootOffset
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Drop resolution — unchanged from original
// ─────────────────────────────────────────────────────────────────────────────

private fun resolveDropOnEnd(
    fingerOffset: Offset,
    dragState: QuickAccessDragState,
    morePage: Int,
    moreWidgets: List<List<ActionWidget>>,
    pinnedWidgets: List<ActionWidget>,
    onMovePinnedToMore: (Int) -> Unit,
    onSwapPinnedToMore: (Int, Int) -> Unit,
    onMoveMoreToPinned: (Int) -> Unit,
    onSwapMoreToPinned: (Int, Int) -> Unit
) {
    val info = dragState.dragging ?: return
    val hit  = dragState.slotBounds.entries
        .firstOrNull { (_, rect) -> rect.contains(fingerOffset) }
        ?: return

    val (targetZone, targetIdx) = hit.key
    if (targetZone == info.sourceZone) return

    val currentPageWidgets = moreWidgets.getOrElse(morePage) { emptyList() }

    when (info.sourceZone) {
        DragZone.PINNED -> when (targetZone) {
            DragZone.MORE -> {
                if (currentPageWidgets.size >= MORE_SIZE)
                    onSwapPinnedToMore(info.sourceIndex, targetIdx)
                else
                    onMovePinnedToMore(info.sourceIndex)
            }
            else -> Unit
        }
        DragZone.MORE -> when (targetZone) {
            DragZone.PINNED -> {
                if (pinnedWidgets.getOrNull(targetIdx)?.action == DUMMY)
                    onMoveMoreToPinned(info.sourceIndex)
                else
                    onSwapMoreToPinned(info.sourceIndex, targetIdx)
            }
            else -> Unit
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Ghost
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DragGhost(
    widget: ActionWidget,
    offset: Offset,
    isPinned: Boolean,
    containerRootOffset: Offset
) {
    val density     = LocalDensity.current
    val ghostSizeDp = if (isPinned) 80.dp else 64.dp
    val ghostSizePx = with(density) { ghostSizeDp.toPx() }

    val localX = offset.x - containerRootOffset.x - ghostSizePx / 2
    val localY = offset.y - containerRootOffset.y - ghostSizePx / 2

    Box(
        modifier = Modifier
            .offset { IntOffset(localX.roundToInt(), localY.roundToInt()) }
            .size(ghostSizeDp)
            .zIndex(99f)
            .graphicsLayer { scaleX = 1.12f; scaleY = 1.12f; alpha = 0.93f }
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(widget.emoji, fontSize = if (isPinned) 28.sp else 22.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text      = widget.label,
                fontSize  = 9.sp,
                color     = Color.White,
                textAlign = TextAlign.Center,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis,
                modifier  = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Widget cards — unchanged from original
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PinnedWidgetCard(
    widget: ActionWidget,
    isDragging: Boolean,
    isEditMode: Boolean,
    onRemove: () -> Unit,
    onLongClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue   = if (isDragging) 1.07f else 1f,
        animationSpec = tween(150),
        label         = "ps"
    )
    val isDummy = widget.action == DUMMY

    Column(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .aspectRatio(0.9f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDragging) Color.White.copy(0.18f) else Color.White.copy(0.08f))
            .then(
                if (!isDummy && !isEditMode)
                    Modifier.combinedClickable(onLongClick = onLongClick, onClick = {})
                else Modifier
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isDummy) {
            Text("＋", fontSize = 22.sp, color = Color.White.copy(0.2f))
        } else {
            Box(contentAlignment = Alignment.TopEnd) {
                Text(widget.emoji, fontSize = 28.sp, modifier = Modifier.padding(top = 8.dp))
                if (isEditMode) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE84C1E))
                            .clickable { onRemove() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text      = widget.label,
                fontSize  = 10.sp,
                color     = Color.White,
                textAlign = TextAlign.Center,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis,
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun MoreWidgetCard(
    widget: ActionWidget,
    isDragging: Boolean,
    isEditMode: Boolean,
    canAdd: Boolean,
    onAdd: () -> Unit,
    onLongClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue   = if (isDragging) 1.07f else 1f,
        animationSpec = tween(150),
        label         = "ms"
    )
    Column(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDragging) Color.White.copy(0.18f) else Color.White.copy(0.07f))
            .padding(vertical = 10.dp)
            .then(
                if (!isEditMode)
                    Modifier.combinedClickable(onLongClick = onLongClick, onClick = {})
                else Modifier
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Text(widget.emoji, fontSize = 22.sp)
            if (isEditMode && canAdd) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E))
                        .clickable { onAdd() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("＋", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text      = widget.label,
            fontSize  = 9.sp,
            color     = Color.White.copy(0.8f),
            textAlign = TextAlign.Center,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
        )
    }
}