package com.viwath.practice_module_app.drag_drop

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
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
// Screen entry point
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun QuickAccessMenuScreen(vm: QuickAccessViewModel = viewModel()) {
    val state by vm.state

    Scaffold(containerColor = Color(0xFF0A0E1A)) { padding ->
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
                    onMovePinned       = { from, to -> vm.movePinned(from, to) },
                    onMoveMore         = { from, to -> vm.moveMore(from, to) },
                    onMovePinnedToMore = { vm.movePinnedToMore(it) },
                    onSwapPinnedToMore = { pinned, more -> vm.swapPinnedToMore(pinned, more) },
                    onMoveMoreToPinned = { vm.moveMoreToPinned(it) },
                    onSwapMoreToPinned = { more, pinned -> vm.swapMoreToPinned(more, pinned) },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main drag grid
// Long-press always activates drag — no edit mode gate
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickAccessMenuDragGrid(
    pinnedWidgets: List<ActionWidget>,
    moreWidgets: List<List<ActionWidget>>,
    onMovePinned: (from: Int, to: Int) -> Unit,
    onMoveMore: (from: Int, to: Int) -> Unit,
    onMovePinnedToMore: (pinnedIdx: Int) -> Unit,
    onSwapPinnedToMore: (pinnedIdx: Int, moreIdx: Int) -> Unit,
    onMoveMoreToPinned: (moreIdx: Int) -> Unit,
    onSwapMoreToPinned: (moreIdx: Int, pinnedIdx: Int) -> Unit,
) {
    val scope     = rememberCoroutineScope()
    val dragState = remember { QuickAccessDragState() }

    // rememberUpdatedState — the pointerInput block uses key=Unit so it never
    // restarts, meaning its lambda captures a stale closure. These refs always
    // point to the latest list so onDragStart reads the correct widget even
    // after reorders have shuffled the indices.
    val currentPinned by rememberUpdatedState(pinnedWidgets)
    val currentMore   by rememberUpdatedState(moreWidgets)

    val pagerState = rememberPagerState(pageCount = { moreWidgets.size.coerceAtLeast(1) })
    val morePage by remember { derivedStateOf { pagerState.currentPage } }

    var pagerRootBounds       by remember { mutableStateOf<Rect?>(null) }
    var containerRootOffset   by remember { mutableStateOf(Offset.Zero) }
    var crossZoneHoverJob     by remember { mutableStateOf<Job?>(null) }
    // Tracks which (zone, index) slot the pending cross-zone job is targeting.
    // Prevents the 300ms delay from resetting on every drag-move event when the
    // finger hasn't actually moved to a different slot.
    var crossZoneHoverTarget  by remember { mutableStateOf<Pair<DragZone, Int>?>(null) }

    // Clamp pager page if moreWidgets shrinks
    LaunchedEffect(moreWidgets.size) {
        if (pagerState.currentPage >= moreWidgets.size)
            pagerState.animateScrollToPage((moreWidgets.size - 1).coerceAtLeast(0))
    }

    // ── Intra-zone hover reorder ──────────────────────────────────────────────

    fun handleIntraZoneHover(rootOffset: Offset) {
        val info = dragState.dragging ?: return

        if (info.sourceZone == DragZone.PINNED) {
            dragState.slotBounds.entries
                .firstOrNull { (key, rect) ->
                    key.first == DragZone.PINNED &&
                            key.second != info.sourceIndex &&
                            rect.contains(rootOffset)
                }
                ?.let { (key, _) ->
                    onMovePinned(info.sourceIndex, key.second)
                    dragState.updateSource(DragZone.PINNED, key.second)
                }
        }

        if (info.sourceZone == DragZone.MORE) {
            dragState.slotBounds.entries
                .firstOrNull { (key, rect) ->
                    key.first == DragZone.MORE &&
                            key.second != info.sourceIndex &&
                            key.second / MORE_SIZE == morePage &&   // current page only
                            rect.contains(rootOffset)
                }
                ?.let { (key, _) ->
                    onMoveMore(info.sourceIndex, key.second)
                    dragState.updateSource(DragZone.MORE, key.second)
                }
        }
    }

    // ── Cross-zone hover (delayed to avoid ABA flicker) ───────────────────────
    //
    // Key fix: we only launch a new job when the finger moves to a *different*
    // target slot. If the finger is still over the same slot the job is already
    // counting down for, we leave it alone — previously we cancelled+restarted
    // it on every single drag-move event, so the 300ms countdown never elapsed
    // and the swap never fired.

    fun handleCrossZoneHover(rootOffset: Offset) {
        val info = dragState.dragging ?: return

        val hoveredCross = dragState.slotBounds.entries.firstOrNull { (key, rect) ->
            key.first != info.sourceZone &&
                    rect.contains(rootOffset) &&
                    (key.first != DragZone.MORE || key.second / MORE_SIZE == morePage)
        }

        if (hoveredCross != null) {
            val targetKey = hoveredCross.key

            // Same slot as the running job → let it finish, don't reset
            if (targetKey == crossZoneHoverTarget) return

            // New slot → cancel old job, start fresh for this slot
            crossZoneHoverJob?.cancel()
            crossZoneHoverTarget = targetKey

            val (targetZone, targetIdx) = targetKey
            crossZoneHoverJob = scope.launch {
                delay(300L)
                val latest = dragState.dragging ?: return@launch
                when (latest.sourceZone) {
                    DragZone.PINNED -> if (targetZone == DragZone.MORE) {
                        val pageWidgets = moreWidgets.getOrElse(morePage) { emptyList() }
                        if (pageWidgets.size >= MORE_SIZE)
                            onSwapPinnedToMore(latest.sourceIndex, targetIdx)
                        else
                            onMovePinnedToMore(latest.sourceIndex)
                        dragState.updateSource(DragZone.MORE, targetIdx)
                    }
                    DragZone.MORE -> if (targetZone == DragZone.PINNED) {
                        if (pinnedWidgets.getOrNull(targetIdx)?.action == DUMMY)
                            onMoveMoreToPinned(latest.sourceIndex)
                        else
                            onSwapMoreToPinned(latest.sourceIndex, targetIdx)
                        dragState.updateSource(DragZone.PINNED, targetIdx)
                    }
                }
                // Job completed — clear target so re-entering the same slot
                // after leaving it starts a fresh countdown
                crossZoneHoverTarget = null
            }
        } else {
            // Finger left all cross-zone slots — cancel and reset
            crossZoneHoverJob?.cancel()
            crossZoneHoverTarget = null
        }
    }

    // ── Auto-scroll pager edges ───────────────────────────────────────────────
    //
    // Allowed for ANY drag (PINNED or MORE) as long as the finger is physically
    // inside the pager's screen bounds. This lets a pinned widget be dragged
    // across to a different MORE page.
    //
    // The previous guard `if (sourceZone != MORE) return` was too broad — it
    // prevented pinned-to-More cross-page drags entirely. The correct guard is
    // purely spatial: if the finger isn't over the pager, don't scroll.

    fun handlePagerAutoScroll(rootOffset: Offset) {
        if (dragState.dragging == null) return

        val bounds = pagerRootBounds ?: return

        // Only scroll when the finger is actually hovering over the pager area
        if (!bounds.contains(rootOffset)) return

        val relX = rootOffset.x - bounds.left
        val edge = bounds.width * 0.20f

        val targetPage = when {
            relX < edge                && morePage > 0                     -> morePage - 1
            relX > bounds.width - edge && morePage < moreWidgets.size - 1  -> morePage + 1
            else -> return
        }

        if (!pagerState.isScrollInProgress) {
            scope.launch {
                // Clear stale slot bounds for the page we're leaving
                dragState.slotsWhere { it.first == DragZone.MORE && it.second / MORE_SIZE != morePage }
                    .forEach { dragState.slotBounds.remove(it) }
                pagerState.animateScrollToPage(targetPage)
            }
        }
    }

    // ── Full drag update ──────────────────────────────────────────────────────

    fun onDragUpdate(rootOffset: Offset) {
        dragState.updateFinger(rootOffset)
        handleIntraZoneHover(rootOffset)
        handleCrossZoneHover(rootOffset)
        handlePagerAutoScroll(rootOffset)
    }

    // ── Drop on finger-up ─────────────────────────────────────────────────────

    fun onDragEnd(fingerOffset: Offset) {
        crossZoneHoverJob?.cancel()
        crossZoneHoverTarget = null
        resolveDropOnEnd(
            fingerOffset       = fingerOffset,
            dragState          = dragState,
            morePage           = morePage,
            moreWidgets        = moreWidgets,
            pinnedWidgets      = pinnedWidgets,
            onMovePinnedToMore = onMovePinnedToMore,
            onSwapPinnedToMore = onSwapPinnedToMore,
            onMoveMoreToPinned = onMoveMoreToPinned,
            onSwapMoreToPinned = onSwapMoreToPinned,
        )
        dragState.endDrag()
    }

    // ── Root container ────────────────────────────────────────────────────────

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { containerRootOffset = it.boundsInRoot().topLeft }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { localOffset ->
                        val rootOffset = localOffset + containerRootOffset
                        val hit = dragState.hitTest(rootOffset) ?: return@detectDragGesturesAfterLongPress
                        val (zone, index) = hit
                        val widget = when (zone) {
                            DragZone.PINNED -> currentPinned.getOrNull(index)
                            DragZone.MORE   -> currentMore.flatten().getOrNull(index)
                        }
                        // Don't allow dragging dummy/empty slots
                        if (widget != null && widget.action != DUMMY) {
                            dragState.startDrag(widget, zone, index, rootOffset)
                            onDragUpdate(rootOffset)
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragState.dragging?.let { onDragUpdate(it.fingerRootOffset + dragAmount) }
                    },
                    onDragEnd    = { onDragEnd(dragState.dragging?.fingerRootOffset ?: Offset.Zero) },
                    onDragCancel = { onDragEnd(dragState.dragging?.fingerRootOffset ?: Offset.Zero) },
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Pinned header ─────────────────────────────────────────────────
            Text(
                text       = "📌  Pinned",
                color      = Color.White,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(12.dp))

            // ── Pinned grid ───────────────────────────────────────────────────
            PinnedGrid(
                pinnedWidgets = pinnedWidgets,
                dragState     = dragState,
            )

            Spacer(Modifier.height(20.dp))

            // ── More header ───────────────────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    text       = "⋯  More",
                    color      = Color.White,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (moreWidgets.size > 1) {
                    Text(
                        text     = "${morePage + 1} / ${moreWidgets.size}",
                        color    = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── More pager ────────────────────────────────────────────────────
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
                userScrollEnabled = dragState.dragging == null,  // disable swipe while dragging
            ) { page ->
                MoreGrid(
                    page        = page,
                    moreWidgets = moreWidgets,
                    dragState   = dragState,
                    modifier    = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { if (it.height > maxMoreHeightPx) maxMoreHeightPx = it.height },
                )
            }

            // ── Dot indicator ─────────────────────────────────────────────────
            if (moreWidgets.size > 1) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    repeat(moreWidgets.size) { index ->
                        val selected = morePage == index
                        val dotSize by animateDpAsState(
                            targetValue   = if (selected) 8.dp else 5.dp,
                            animationSpec = tween(200),
                            label         = "dot_$index",
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(dotSize)
                                .clip(CircleShape)
                                .background(
                                    if (selected) Color(0xFFE84C1E)
                                    else          Color.White.copy(alpha = 0.25f)
                                )
                        )
                    }
                }
            }
        }

        // Floating ghost — always on top
        dragState.dragging?.let { info ->
            DragGhost(
                widget              = info.widget,
                offset              = info.fingerRootOffset,
                isPinned            = info.sourceZone == DragZone.PINNED,
                containerRootOffset = containerRootOffset,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pinned grid — extracted for readability
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PinnedGrid(
    pinnedWidgets: List<ActionWidget>,
    dragState: QuickAccessDragState,
    modifier: Modifier = Modifier,
) {
    val rows = (pinnedWidgets.size + PINNED_COLS - 1) / PINNED_COLS

    Column(
        modifier            = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        for (row in 0 until rows) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (col in 0 until PINNED_COLS) {
                    val index = row * PINNED_COLS + col
                    if (index < pinnedWidgets.size) {
                        val widget         = pinnedWidgets[index]
                        val isDraggingThis = dragState.dragging?.let {
                            it.sourceZone == DragZone.PINNED && it.sourceIndex == index
                        } ?: false
                        val isDropTarget   = dragState.dragging?.let { info ->
                            !(info.sourceZone == DragZone.PINNED && info.sourceIndex == index) &&
                                    dragState.slotBounds[DragZone.PINNED to index]
                                        ?.contains(info.fingerRootOffset) == true
                        } ?: false

                        val posAnim = rememberSlotAnimatable(
                            key          = widget.action,
                            slotBounds   = dragState.slotBounds,
                            zone         = DragZone.PINNED,
                            currentIndex = index,
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned {
                                    dragState.slotBounds[DragZone.PINNED to index] = it.boundsInRoot()
                                }
                                .graphicsLayer {
                                    alpha        = if (isDraggingThis) 0f else 1f
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
                            PinnedWidgetCard(widget = widget, isDragging = isDraggingThis)
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// More grid (single page) — extracted for readability
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MoreGrid(
    page: Int,
    moreWidgets: List<List<ActionWidget>>,
    dragState: QuickAccessDragState,
    modifier: Modifier = Modifier,
) {
    val pageWidgets = moreWidgets.getOrElse(page) { emptyList() }
    val rows        = (pageWidgets.size + MORE_COLS - 1) / MORE_COLS

    Column(
        modifier            = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        for (row in 0 until rows) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (col in 0 until MORE_COLS) {
                    val localIdx = row * MORE_COLS + col
                    val flatIdx  = page * MORE_SIZE + localIdx

                    if (localIdx < pageWidgets.size) {
                        val widget         = pageWidgets[localIdx]
                        val isDraggingThis = dragState.dragging?.let {
                            it.sourceZone == DragZone.MORE && it.sourceIndex == flatIdx
                        } ?: false
                        val isDropTarget   = dragState.dragging?.let { info ->
                            !(info.sourceZone == DragZone.MORE && info.sourceIndex == flatIdx) &&
                                    dragState.slotBounds[DragZone.MORE to flatIdx]
                                        ?.contains(info.fingerRootOffset) == true
                        } ?: false

                        val posAnim = rememberSlotAnimatable(
                            key          = widget.action,
                            slotBounds   = dragState.slotBounds,
                            zone         = DragZone.MORE,
                            currentIndex = flatIdx,
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned {
                                    dragState.slotBounds[DragZone.MORE to flatIdx] = it.boundsInRoot()
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
                            MoreWidgetCard(widget = widget, isDragging = isDraggingThis)
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Drop resolution on finger-up
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
    onSwapMoreToPinned: (Int, Int) -> Unit,
) {
    val info       = dragState.dragging ?: return
    val (targetZone, targetIdx) = dragState.hitTest(fingerOffset) ?: return
    if (targetZone == info.sourceZone) return

    val pageWidgets = moreWidgets.getOrElse(morePage) { emptyList() }

    when (info.sourceZone) {
        DragZone.PINNED -> if (targetZone == DragZone.MORE) {
            if (pageWidgets.size >= MORE_SIZE)
                onSwapPinnedToMore(info.sourceIndex, targetIdx)
            else
                onMovePinnedToMore(info.sourceIndex)
        }
        DragZone.MORE -> if (targetZone == DragZone.PINNED) {
            if (pinnedWidgets.getOrNull(targetIdx)?.action == DUMMY)
                onMoveMoreToPinned(info.sourceIndex)
            else
                onSwapMoreToPinned(info.sourceIndex, targetIdx)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Drag ghost — floats under finger while dragging
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DragGhost(
    widget: ActionWidget,
    offset: Offset,
    isPinned: Boolean,
    containerRootOffset: Offset,
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
            .graphicsLayer { alpha = 0.93f }
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
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
                modifier  = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}