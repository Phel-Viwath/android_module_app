package com.viwath.practice_module_app.drag_drop

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import com.viwath.practice_module_app.drag_drop.GridPops.GRID_SPAN
import com.viwath.practice_module_app.drag_drop.GridPops.MORE_COLS
import com.viwath.practice_module_app.drag_drop.GridPops.MORE_SIZE
import com.viwath.practice_module_app.drag_drop.GridPops.PINNED_COLS
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import kotlin.math.roundToInt


// Main screen composable
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
                        onClick = { vm.exitEditMode() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(Color.White.copy(0.3f)))
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { vm.saveLayout() },
                        modifier = Modifier.weight(1f),
                        enabled = state.enableSave,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE84C1E),
                            disabledContainerColor = Color(0xFFE84C1E).copy(alpha = 0.4f)
                        )
                    ) {
                        Text("Save")
                    }
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
                    pinnedWidgets = state.pinnedWidgets,
                    moreWidgets = state.moreWidgets,
                    isEditMode = state.isEditMode,
                    canAddToPinned = state.canAddToPinned,
                    enableReset = state.enableReset,
                    onEnterEditMode = { vm.enterEditMode() },
                    onReset = { vm.reset() },
                    onRemoveFromPinned = { vm.removeFromPinned(it) },
                    onAddToPinned = { vm.addToPinned(it) },
                    onMovePinned = { from, to -> vm.movePinned(from, to) },
                    onMoveMore = { from, to -> vm.moveMore(from, to) },
                    onMovePinnedToMore = { vm.movePinnedToMore(it) },
                    onSwapPinnedToMore = { pinned, more -> vm.swapPinnedToMore(pinned, more) },
                    onMoveMoreToPinned = { vm.moveMoreToPinned(it) },
                    onSwapMoreToPinned = { more, pinned -> vm.swapMoreToPinned(more, pinned) }
                )
            }
        }
    }
}

// The main drag grid - this was a pain to get working right
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

    // Keep pager in sync if page count changes
    LaunchedEffect(moreWidgets.size) {
        if (pagerState.currentPage >= moreWidgets.size)
            pagerState.animateScrollToPage((moreWidgets.size - 1).coerceAtLeast(0))
    }

    // Cross-zone drag handlers
    val onCrossDragUpdate: (Offset) -> Unit = { rootOffset ->
        dragState.dragging = dragState.dragging?.copy(fingerRootOffset = rootOffset)

        // Auto page flipping when dragging near edges
        pagerRootBounds?.let { bounds ->
            if (rootOffset.y > bounds.top) {
                val relX = rootOffset.x - bounds.left
                val edge = bounds.width * 0.15f
                when {
                    relX < edge && morePage > 0 ->
                        scope.launch { pagerState.animateScrollToPage(morePage - 1) }
                    relX > bounds.width - edge && morePage < moreWidgets.size - 1 ->
                        scope.launch { pagerState.animateScrollToPage(morePage + 1) }
                }
            }
        }
    }

    val onCrossDragEnd: (Offset) -> Unit = { fingerOffset ->
        resolveDropOnEnd(
            fingerOffset = fingerOffset,
            dragState = dragState,
            morePage = morePage,
            moreWidgets = moreWidgets,
            pinnedWidgets = pinnedWidgets,
            onMovePinned = onMovePinned,
            onMoveMore = onMoveMore,
            onMovePinnedToMore = onMovePinnedToMore,
            onSwapPinnedToMore = onSwapPinnedToMore,
            onMoveMoreToPinned = onMoveMoreToPinned,
            onSwapMoreToPinned = onSwapMoreToPinned
        )
        dragState.dragging = null
    }

    var containerRootOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { containerRootOffset = it.boundsInRoot().topLeft }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // Pinned section header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📌  Pinned", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (isEditMode) {
                    Text(
                        text = "Reset",
                        color = if (enableReset) Color(0xFFFF6B3D) else Color.White.copy(0.2f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(enabled = enableReset) { onReset() }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Pinned grid
            val pinnedGridState = rememberLazyGridState()
            val pinnedReorderable = rememberReorderableLazyGridState(pinnedGridState) { from, to ->
                onMovePinned(from.index, to.index)
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(GRID_SPAN),
                state = pinnedGridState,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false
            ) {
                itemsIndexed(
                    items = pinnedWidgets,
                    key = { _, w -> "pinned_${w.action}" },
                    span = { _, _ -> GridItemSpan(GRID_SPAN / PINNED_COLS) }
                ) { index, widget ->
                    ReorderableItem(pinnedReorderable, key = "pinned_${widget.action}") { isDragging ->

                        val isCrossSrc = dragState.dragging?.let {
                            it.sourceZone == DragZone.PINNED && it.sourceIndex == index
                        } ?: false

                        val isDropTarget = dragState.dragging?.let { info ->
                            info.sourceZone == DragZone.MORE &&
                                    dragState.slotBounds[DragZone.PINNED to index]
                                        ?.contains(info.fingerRootOffset) == true
                        } ?: false

                        Box(
                            modifier = Modifier
                                .onGloballyPositioned {
                                    dragState.slotBounds[DragZone.PINNED to index] = it.boundsInRoot()
                                }
                                .graphicsLayer { alpha = if (isCrossSrc) 0f else 1f }
                                .then(
                                    if (isDropTarget) Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFE84C1E).copy(alpha = 0.18f))
                                    else Modifier
                                )
                                .then(
                                    if (isEditMode && widget.action != DUMMY)
                                        Modifier.dragSource(
                                            zone = DragZone.PINNED,
                                            slotIndex = index,
                                            dragState = dragState,
                                            onDragStart = {
                                                dragState.dragging = DragInfo(
                                                    widget = widget,
                                                    sourceZone = DragZone.PINNED,
                                                    sourceIndex = index,
                                                    fingerRootOffset = Offset.Zero
                                                )
                                            },
                                            onDragUpdate = onCrossDragUpdate,
                                            onDragEnd = onCrossDragEnd
                                        )
                                    else Modifier
                                )
                        ) {
                            PinnedWidgetCard(
                                widget = widget,
                                isDragging = isDragging || isCrossSrc,
                                isEditMode = isEditMode,
                                dragModifier = if (isEditMode && widget.action != DUMMY)
                                    Modifier.draggableHandle() else Modifier,
                                onRemove = { onRemoveFromPinned(index) },
                                onLongClick = onEnterEditMode
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // More section header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⋯  More", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (moreWidgets.size > 1) {
                    Text(
                        text = "${morePage + 1} / ${moreWidgets.size}",
                        color = Color.White.copy(0.4f),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // More pager
            var maxMoreHeightPx by remember { mutableIntStateOf(0) }
            val density = LocalDensity.current

            HorizontalPager(
                state = pagerState,
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
                val moreGridState = rememberLazyGridState()
                val moreReorderable = rememberReorderableLazyGridState(moreGridState) { from, to ->
                    val pageOffset = page * MORE_SIZE
                    onMoveMore(from.index + pageOffset, to.index + pageOffset)
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(GRID_SPAN),
                    state = moreGridState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(horizontal = 16.dp)
                        .onSizeChanged { size ->
                            if (size.height > maxMoreHeightPx) maxMoreHeightPx = size.height
                        },
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled = false
                ) {
                    itemsIndexed(
                        items = pageWidgets,
                        key = { _, w -> "more_${w.action}_p$page" },
                        span = { _, _ -> GridItemSpan(GRID_SPAN / MORE_COLS) }
                    ) { localIdx, widget ->
                        val flatIdx = page * MORE_SIZE + localIdx

                        ReorderableItem(moreReorderable, key = "more_${widget.action}_p$page") { isDragging ->

                            val isCrossSrc = dragState.dragging?.let {
                                it.sourceZone == DragZone.MORE && it.sourceIndex == flatIdx
                            } ?: false

                            val isDropTarget = dragState.dragging?.let { info ->
                                info.sourceZone == DragZone.PINNED &&
                                        dragState.slotBounds[DragZone.MORE to flatIdx]
                                            ?.contains(info.fingerRootOffset) == true
                            } ?: false

                            Box(
                                modifier = Modifier
                                    .onGloballyPositioned {
                                        dragState.slotBounds[DragZone.MORE to flatIdx] = it.boundsInRoot()
                                    }
                                    .graphicsLayer { alpha = if (isCrossSrc) 0f else 1f }
                                    .then(
                                        if (isDropTarget) Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFE84C1E).copy(alpha = 0.18f))
                                        else Modifier
                                    )
                                    .then(
                                        if (isEditMode)
                                            Modifier.dragSource(
                                                zone = DragZone.MORE,
                                                slotIndex = flatIdx,
                                                dragState = dragState,
                                                onDragStart = {
                                                    dragState.dragging = DragInfo(
                                                        widget = widget,
                                                        sourceZone = DragZone.MORE,
                                                        sourceIndex = flatIdx,
                                                        fingerRootOffset = Offset.Zero
                                                    )
                                                },
                                                onDragUpdate = onCrossDragUpdate,
                                                onDragEnd = onCrossDragEnd
                                            )
                                        else Modifier
                                    )
                            ) {
                                MoreWidgetCard(
                                    widget = widget,
                                    isDragging = isDragging || isCrossSrc,
                                    isEditMode = isEditMode,
                                    canAdd = canAddToPinned,
                                    dragModifier = if (isEditMode) Modifier.draggableHandle() else Modifier,
                                    onAdd = { onAddToPinned(widget) },
                                    onLongClick = onEnterEditMode
                                )
                            }
                        }
                    }
                }
            }

            // Dot indicator for pager
            if (moreWidgets.size > 1) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(moreWidgets.size) { index ->
                        val selected = morePage == index
                        val dotSize by animateDpAsState(
                            targetValue = if (selected) 8.dp else 5.dp,
                            animationSpec = tween(200),
                            label = "dot_$index"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(dotSize)
                                .clip(CircleShape)
                                .background(
                                    if (selected) Color(0xFFE84C1E) else Color.White.copy(0.25f)
                                )
                        )
                    }
                }
            }
        }

        // Ghost image while dragging across zones
        dragState.dragging?.let { info ->
            DragGhost(
                widget = info.widget,
                offset = info.fingerRootOffset,
                isPinned = info.sourceZone == DragZone.PINNED,
                containerRootOffset = containerRootOffset
            )
        }
    }
}

// Custom drag source modifier for cross-zone dragging
private fun Modifier.dragSource(
    zone: DragZone,
    slotIndex: Int,
    dragState: QuickAccessDragState,
    onDragStart: () -> Unit,
    onDragUpdate: (Offset) -> Unit,
    onDragEnd: (Offset) -> Unit
): Modifier = this.pointerInput(zone, slotIndex) {
    detectDragGesturesAfterLongPress(
        onDragStart = { localOffset ->
            onDragStart()
            val bounds = dragState.slotBounds[zone to slotIndex]
            if (bounds != null) {
                onDragUpdate(Offset(
                    x = bounds.left + localOffset.x,
                    y = bounds.top + localOffset.y
                ))
            }
        },
        onDrag = { change, _ ->
            change.consume()
            val bounds = dragState.slotBounds[zone to slotIndex]
            if (bounds != null) {
                onDragUpdate(Offset(
                    x = bounds.left + change.position.x,
                    y = bounds.top + change.position.y
                ))
            }
        },
        onDragEnd = { onDragEnd(dragState.dragging?.fingerRootOffset ?: Offset.Zero) },
        onDragCancel = { onDragEnd(dragState.dragging?.fingerRootOffset ?: Offset.Zero) }
    )
}

// Ghost that follows your finger during cross-zone drag
@Composable
private fun DragGhost(
    widget: ActionWidget,
    offset: Offset,
    isPinned: Boolean,
    containerRootOffset: Offset
) {
    val density = LocalDensity.current
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
                text = widget.label,
                fontSize = 9.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

// Figure out where the drop should go when dragging ends
private fun resolveDropOnEnd(
    fingerOffset: Offset,
    dragState: QuickAccessDragState,
    morePage: Int,
    moreWidgets: List<List<ActionWidget>>,
    pinnedWidgets: List<ActionWidget>,
    onMovePinned: (Int, Int) -> Unit,
    onMoveMore: (Int, Int) -> Unit,
    onMovePinnedToMore: (Int) -> Unit,
    onSwapPinnedToMore: (Int, Int) -> Unit,
    onMoveMoreToPinned: (Int) -> Unit,
    onSwapMoreToPinned: (Int, Int) -> Unit
) {
    val info = dragState.dragging ?: return
    val hit = dragState.slotBounds.entries
        .firstOrNull { (_, rect) -> rect.contains(fingerOffset) }
        ?: return

    val (targetZone, targetIdx) = hit.key
    if (targetZone == info.sourceZone && targetIdx == info.sourceIndex) return

    val currentPageWidgets = moreWidgets.getOrElse(morePage) { emptyList() }

    when (info.sourceZone) {

        DragZone.PINNED -> when (targetZone) {
            DragZone.PINNED -> {
                onMovePinned(info.sourceIndex, targetIdx)
            }

            DragZone.MORE -> {
                if (currentPageWidgets.size >= MORE_SIZE)
                    onSwapPinnedToMore(info.sourceIndex, targetIdx)
                else
                    onMovePinnedToMore(info.sourceIndex)
            }
        }

        DragZone.MORE -> when (targetZone) {
            DragZone.MORE -> {
                onMoveMore(info.sourceIndex, targetIdx)
            }

            DragZone.PINNED -> {
                if (pinnedWidgets.getOrNull(targetIdx)?.action == DUMMY)
                    onMoveMoreToPinned(info.sourceIndex)
                else
                    onSwapMoreToPinned(info.sourceIndex, targetIdx)
            }
        }
    }
}


@Composable
private fun PinnedWidgetCard(
    widget: ActionWidget,
    isDragging: Boolean,
    isEditMode: Boolean,
    dragModifier: Modifier,
    onRemove: () -> Unit,
    onLongClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.07f else 1f, animationSpec = tween(150), label = "ps"
    )
    val isDummy = widget.action == DUMMY

    Column(
        modifier = dragModifier
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
                            .size(18.dp).clip(CircleShape)
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
                text = widget.label, fontSize = 10.sp, color = Color.White,
                textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
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
    dragModifier: Modifier,
    onAdd: () -> Unit,
    onLongClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.07f else 1f, animationSpec = tween(150), label = "ms"
    )
    Column(
        modifier = dragModifier
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
                        .size(16.dp).clip(CircleShape)
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
            text = widget.label, fontSize = 9.sp, color = Color.White.copy(0.8f),
            textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
        )
    }
}