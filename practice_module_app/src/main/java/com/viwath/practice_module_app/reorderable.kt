package com.viwath.practice_module_app

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// 1. DATA MODEL
// ─────────────────────────────────────────────────────────────────────────────

data class ActionWidget(
    val action: String,
    val label: String,
    val emoji: String,
    val ordering: Int = 0,
    val isEnabled: Boolean = true
)

private const val DUMMY      = "DUMMY"
private const val MORE_SIZE  = 8
private const val MORE_COLS  = 4
private const val PINNED_COLS = 3
private const val PINNED_MAX = 9

// ─────────────────────────────────────────────────────────────────────────────
// 2. SAMPLE DATA
// ─────────────────────────────────────────────────────────────────────────────

private val ALL_WIDGETS = listOf(
    ActionWidget("TRANSFER",   "Transfer",   "💸"),
    ActionWidget("PAY_BILL",   "Pay Bill",   "🧾"),
    ActionWidget("QR_PAY",     "QR Pay",     "📱"),
    ActionWidget("TOP_UP",     "Top Up",     "⚡"),
    ActionWidget("HISTORY",    "History",    "🕒"),
    ActionWidget("LOAN",       "Loan",       "🏦"),
    ActionWidget("INVESTMENT", "Invest",     "📈"),
    ActionWidget("LOCATOR",    "ATM",        "📍"),
    ActionWidget("FX_RATE",    "FX Rate",    "💱"),
    ActionWidget("INSURANCE",  "Insurance",  "🛡️"),
    ActionWidget("SAVINGS",    "Savings",    "🏛️"),
    ActionWidget("CARD",       "Card",       "💳"),
    ActionWidget("REWARDS",    "Rewards",    "🎁"),
    ActionWidget("SPLIT",      "Split",      "✂️"),
    ActionWidget("SCHEDULE",   "Schedule",   "📅"),
    ActionWidget("FIXED_DEP",  "Fixed Dep.", "🔒"),
    ActionWidget("STATEMENT",  "Statement",  "📄"),
    ActionWidget("SUPPORT",    "Support",    "🎧"),
    ActionWidget("EXCHANGE",   "Exchange",   "🔄"),
    ActionWidget("DEPOSIT",    "Deposit",    "💰"),
    ActionWidget("WITHDRAW",   "Withdraw",   "🏧"),
    ActionWidget("GAME",       "Game",       "🎮"),
    ActionWidget("MUSIC",      "Music",      "🎵"),
    ActionWidget("CAMERA",     "Camera",     "📷"),
    ActionWidget("MAPS",       "Maps",       "🗺️"),
)

// ─────────────────────────────────────────────────────────────────────────────
// 3. DRAG STATE  (from doc23 architecture)
// ─────────────────────────────────────────────────────────────────────────────

enum class DragZone { PINNED, MORE }

data class DragInfo(
    val widget: ActionWidget,
    val sourceZone: DragZone,
    val sourceIndex: Int,
    val fingerRootOffset: Offset
)

class QuickAccessDragState {
    var dragging by mutableStateOf<DragInfo?>(null)
    val slotBounds = mutableStateMapOf<Pair<DragZone, Int>, Rect>()
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. STATE MODEL
// ─────────────────────────────────────────────────────────────────────────────

data class QuickAccessState(
    val pinnedWidgets: List<ActionWidget> = emptyList(),
    val moreWidgets: List<List<ActionWidget>> = emptyList(),
    val isEditMode: Boolean = false,
    val canAddToPinned: Boolean = false,
    val enableSave: Boolean = false,
    val enableReset: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// 5. VIEW MODEL
// ─────────────────────────────────────────────────────────────────────────────

class QuickAccessTestViewModel : ViewModel() {

    private val _state = mutableStateOf(QuickAccessState())
    val state: State<QuickAccessState> = _state

    private fun setState(update: (QuickAccessState) -> QuickAccessState) {
        _state.value = update(_state.value)
    }

    init {
        val pinned = ALL_WIDGETS.take(PINNED_MAX).mapIndexed { i, w -> w.copy(ordering = i) }
        val more   = ALL_WIDGETS.drop(PINNED_MAX).chunked(MORE_SIZE)
        setState { it.copy(pinnedWidgets = pinned, moreWidgets = more) }
    }

    fun enterEditMode() {
        val hasDummy = _state.value.pinnedWidgets.any { it.action == DUMMY }
        setState { it.copy(isEditMode = true, enableSave = !hasDummy, enableReset = hasDummy) }
    }

    fun exitEditMode() {
        val pinned = _state.value.pinnedWidgets.toMutableList()
        val flat   = _state.value.moreWidgets.flatten().toMutableList()
        pinned.forEachIndexed { i, w ->
            if (w.action == DUMMY && flat.isNotEmpty())
                pinned[i] = flat.removeAt(0).copy(ordering = i)
        }
        setState {
            it.copy(
                pinnedWidgets  = pinned,
                moreWidgets    = flat.chunked(MORE_SIZE).ifEmpty { listOf(emptyList()) },
                isEditMode     = false,
                canAddToPinned = false,
                enableSave     = false,
                enableReset    = false
            )
        }
    }

    fun saveLayout() = setState {
        it.copy(isEditMode = false, canAddToPinned = false, enableSave = false, enableReset = false)
    }

    fun reset() {
        val pinned = ALL_WIDGETS.take(PINNED_MAX).mapIndexed { i, w -> w.copy(ordering = i) }
        val more   = ALL_WIDGETS.drop(PINNED_MAX).chunked(MORE_SIZE)
        setState {
            it.copy(
                pinnedWidgets  = pinned,
                moreWidgets    = more,
                isEditMode     = false,
                canAddToPinned = false,
                enableSave     = false,
                enableReset    = false
            )
        }
    }

    fun removeFromPinned(index: Int) {
        val pinned  = _state.value.pinnedWidgets.toMutableList()
        val flat    = _state.value.moreWidgets.flatten().toMutableList()
        val removed = pinned[index]
        pinned[index] = ActionWidget(DUMMY, "", "", index)
        flat.add(0, removed)
        setState {
            it.copy(
                pinnedWidgets  = pinned,
                moreWidgets    = flat.chunked(MORE_SIZE).ifEmpty { listOf(emptyList()) },
                canAddToPinned = true,
                enableSave     = false,
                enableReset    = true
            )
        }
    }

    fun addToPinned(widget: ActionWidget) {
        val pinned   = _state.value.pinnedWidgets.toMutableList()
        val flat     = _state.value.moreWidgets.flatten().toMutableList()
        val emptyIdx = pinned.indexOfFirst { it.action == DUMMY }
        if (emptyIdx == -1) return
        pinned[emptyIdx] = widget.copy(ordering = emptyIdx)
        flat.remove(widget)
        val noDummy = pinned.none { it.action == DUMMY }
        setState {
            it.copy(
                pinnedWidgets  = pinned,
                moreWidgets    = flat.chunked(MORE_SIZE).ifEmpty { listOf(emptyList()) },
                canAddToPinned = !noDummy,
                enableSave     = noDummy,
                enableReset    = !noDummy
            )
        }
    }

    // Within-zone reorder — direct swap
    fun movePinned(from: Int, to: Int) {
        val pinned = _state.value.pinnedWidgets.toMutableList()
        if (from !in pinned.indices || to !in pinned.indices) return
        val temp     = pinned[from]
        pinned[from] = pinned[to].copy(ordering = from)
        pinned[to]   = temp.copy(ordering = to)
        setState { it.copy(pinnedWidgets = pinned, enableSave = true) }
    }

    fun moveMore(from: Int, to: Int) {
        val flat = _state.value.moreWidgets.flatten().toMutableList()
        if (from !in flat.indices || to !in flat.indices) return
        val temp   = flat[from]
        flat[from] = flat[to]
        flat[to]   = temp
        setState { it.copy(moreWidgets = flat.chunked(MORE_SIZE).ifEmpty { listOf(emptyList()) }) }
    }

    // Cross-zone: Pinned → More
    fun movePinnedToMore(pinnedIdx: Int) {
        val pinned  = _state.value.pinnedWidgets.toMutableList()
        val flat    = _state.value.moreWidgets.flatten().toMutableList()
        val removed = pinned[pinnedIdx]
        pinned[pinnedIdx] = ActionWidget(DUMMY, "", "", pinnedIdx)
        flat.add(0, removed)
        setState {
            it.copy(
                pinnedWidgets  = pinned,
                moreWidgets    = flat.chunked(MORE_SIZE).ifEmpty { listOf(emptyList()) },
                canAddToPinned = true,
                enableSave     = false,
                enableReset    = true
            )
        }
    }

    fun swapPinnedToMore(pinnedIdx: Int, moreIdx: Int) {
        val pinned = _state.value.pinnedWidgets.toMutableList()
        val flat   = _state.value.moreWidgets.flatten().toMutableList()
        if (pinnedIdx !in pinned.indices || moreIdx !in flat.indices) return
        val fromPinned    = pinned[pinnedIdx]
        val fromMore      = flat[moreIdx]
        pinned[pinnedIdx] = fromMore.copy(ordering = pinnedIdx)
        flat[moreIdx]     = fromPinned
        setState {
            it.copy(
                pinnedWidgets = pinned,
                moreWidgets   = flat.chunked(MORE_SIZE).ifEmpty { listOf(emptyList()) },
                enableSave    = true
            )
        }
    }

    // Cross-zone: More → Pinned
    fun moveMoreToPinned(moreIdx: Int) {
        val pinned   = _state.value.pinnedWidgets.toMutableList()
        val flat     = _state.value.moreWidgets.flatten().toMutableList()
        if (moreIdx !in flat.indices) return
        val dummyIdx = pinned.indexOfFirst { it.action == DUMMY }
        if (dummyIdx == -1) return
        val incoming     = flat.removeAt(moreIdx)
        pinned[dummyIdx] = incoming.copy(ordering = dummyIdx)
        val noDummy = pinned.none { it.action == DUMMY }
        setState {
            it.copy(
                pinnedWidgets  = pinned,
                moreWidgets    = flat.chunked(MORE_SIZE).ifEmpty { listOf(emptyList()) },
                canAddToPinned = !noDummy,
                enableSave     = noDummy,
                enableReset    = !noDummy
            )
        }
    }

    fun swapMoreToPinned(moreIdx: Int, pinnedIdx: Int) {
        val pinned   = _state.value.pinnedWidgets.toMutableList()
        val flat     = _state.value.moreWidgets.flatten().toMutableList()
        if (moreIdx !in flat.indices || pinnedIdx !in pinned.indices) return
        val incoming      = flat.removeAt(moreIdx)
        val displaced     = pinned[pinnedIdx]
        pinned[pinnedIdx] = incoming.copy(ordering = pinnedIdx)
        flat.add(0, displaced)
        setState {
            it.copy(
                pinnedWidgets = pinned,
                moreWidgets   = flat.chunked(MORE_SIZE).ifEmpty { listOf(emptyList()) },
                enableSave    = true
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. SCREEN
// ─────────────────────────────────────────────────────────────────────────────

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
                    pinnedWidgets  = state.pinnedWidgets,
                    moreWidgets    = state.moreWidgets,
                    isEditMode     = state.isEditMode,
                    canAddToPinned = state.canAddToPinned,
                    enableReset    = state.enableReset,
                    onEnterEditMode    = { vm.enterEditMode() },
                    onReset            = { vm.reset() },
                    onRemoveFromPinned = { vm.removeFromPinned(it) },
                    onAddToPinned      = { vm.addToPinned(it) },
                    onMovePinned       = { f, t -> vm.movePinned(f, t) },
                    onMoveMore         = { f, t -> vm.moveMore(f, t) },
                    onMovePinnedToMore = { vm.movePinnedToMore(it) },
                    onSwapPinnedToMore = { p, m -> vm.swapPinnedToMore(p, m) },
                    onMoveMoreToPinned = { vm.moveMoreToPinned(it) },
                    onSwapMoreToPinned = { m, p -> vm.swapMoreToPinned(m, p) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. DRAG GRID  (doc23 architecture, callbacks replace intentional)
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
    val scope     = rememberCoroutineScope()
    val dragState = remember { QuickAccessDragState() }

    val pagerState = rememberPagerState(pageCount = { moreWidgets.size.coerceAtLeast(1) })
    val morePage by remember { derivedStateOf { pagerState.currentPage } }
    var pagerRootBounds by remember { mutableStateOf<Rect?>(null) }

    LaunchedEffect(moreWidgets.size) {
        if (pagerState.currentPage >= moreWidgets.size)
            pagerState.animateScrollToPage((moreWidgets.size - 1).coerceAtLeast(0))
    }

    val onDragUpdate: (Offset) -> Unit = { rootOffset ->
        dragState.dragging = dragState.dragging?.copy(fingerRootOffset = rootOffset)
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

    val onDragEnd: (Offset) -> Unit = { fingerOffset ->
        resolveDropOnEnd(
            fingerOffset       = fingerOffset,
            dragState          = dragState,
            morePage           = morePage,
            moreWidgets        = moreWidgets,
            pinnedWidgets      = pinnedWidgets,
            onMovePinned       = onMovePinned,
            onMoveMore         = onMoveMore,
            onMovePinnedToMore = onMovePinnedToMore,
            onSwapPinnedToMore = onSwapPinnedToMore,
            onMoveMoreToPinned = onMoveMoreToPinned,
            onSwapMoreToPinned = onSwapMoreToPinned
        )
        dragState.dragging = null
    }

    // Root Box — track its own root position so the ghost offset can be
    // converted from root coords to local Box coords correctly
    var containerRootOffset by remember { mutableStateOf(Offset.Zero) }
    Box(modifier = Modifier
        .fillMaxWidth()
        .onGloballyPositioned { containerRootOffset = it.boundsInRoot().topLeft }
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
                    text       = "📌  Pinned",
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

            // ── Pinned 3×n grid ───────────────────────────────────────────
            PinnedGrid(
                widgets         = pinnedWidgets,
                isEditMode      = isEditMode,
                dragState       = dragState,
                onDragStart     = { widget, index ->
                    dragState.dragging = DragInfo(
                        widget           = widget,
                        sourceZone       = DragZone.PINNED,
                        sourceIndex      = index,
                        fingerRootOffset = Offset.Zero
                    )
                },
                onDragUpdate    = onDragUpdate,
                onDragEnd       = onDragEnd,
                onEnterEditMode = onEnterEditMode,
                onRemove        = onRemoveFromPinned
            )

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
                    text       = "⋯  More",
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

            // ── More HorizontalPager ──────────────────────────────────────
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        pagerRootBounds = coords.boundsInRoot()
                    },
                userScrollEnabled = !isEditMode && dragState.dragging == null
            ) { page ->
                MorePageGrid(
                    page            = page,
                    widgets         = moreWidgets.getOrElse(page) { emptyList() },
                    isEditMode      = isEditMode,
                    canAddToPinned  = canAddToPinned,
                    dragState       = dragState,
                    onDragStart     = { widget, flatIdx ->
                        dragState.dragging = DragInfo(
                            widget           = widget,
                            sourceZone       = DragZone.MORE,
                            sourceIndex      = flatIdx,
                            fingerRootOffset = Offset.Zero
                        )
                    },
                    onDragUpdate    = onDragUpdate,
                    onDragEnd       = onDragEnd,
                    onEnterEditMode = onEnterEditMode,
                    onAdd           = onAddToPinned
                )
            }

            // ── Page dot indicator ────────────────────────────────────────
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

        // ── Ghost — direct child of root Box, NOT inside Column ───────────
        // Modifier.offset here is relative to the Box so the ghost can
        // appear anywhere on screen without being clipped by the Column
        dragState.dragging?.let { info ->
            DragGhost(
                widget               = info.widget,
                offset               = info.fingerRootOffset,
                isPinned             = info.sourceZone == DragZone.PINNED,
                containerRootOffset  = containerRootOffset
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 8. PINNED GRID
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PinnedGrid(
    widgets: List<ActionWidget>,
    isEditMode: Boolean,
    dragState: QuickAccessDragState,
    onDragStart: (ActionWidget, Int) -> Unit,
    onDragUpdate: (Offset) -> Unit,
    onDragEnd: (Offset) -> Unit,
    onEnterEditMode: () -> Unit,
    onRemove: (Int) -> Unit
) {
    val rows = (widgets.size + PINNED_COLS - 1) / PINNED_COLS
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (row in 0 until rows) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (col in 0 until PINNED_COLS) {
                    val index = row * PINNED_COLS + col
                    if (index < widgets.size) {
                        val widget         = widgets[index]
                        val isDraggingThis = dragState.dragging?.let {
                            it.sourceZone == DragZone.PINNED && it.sourceIndex == index
                        } ?: false
                        val isDropTarget   = dragState.dragging?.let { info ->
                            !(info.sourceZone == DragZone.PINNED && info.sourceIndex == index) &&
                                    dragState.slotBounds[DragZone.PINNED to index]
                                        ?.contains(info.fingerRootOffset) == true
                        } ?: false

                        PinnedSlot(
                            widget         = widget,
                            index          = index,
                            isDraggingThis = isDraggingThis,
                            isDropTarget   = isDropTarget,
                            isEditMode     = isEditMode,
                            dragState      = dragState,
                            modifier       = Modifier.weight(1f),
                            onDragStart    = { onDragStart(widget, index) },
                            onDragUpdate   = onDragUpdate,
                            onDragEnd      = onDragEnd,
                            onEnterEditMode= onEnterEditMode,
                            onRemove       = { onRemove(index) }
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 9. MORE PAGE GRID
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MorePageGrid(
    page: Int,
    widgets: List<ActionWidget>,
    isEditMode: Boolean,
    canAddToPinned: Boolean,
    dragState: QuickAccessDragState,
    onDragStart: (ActionWidget, Int) -> Unit,
    onDragUpdate: (Offset) -> Unit,
    onDragEnd: (Offset) -> Unit,
    onEnterEditMode: () -> Unit,
    onAdd: (ActionWidget) -> Unit
) {
    val rows = (widgets.size + MORE_COLS - 1) / MORE_COLS
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (row in 0 until rows) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (col in 0 until MORE_COLS) {
                    val pageLocalIdx   = row * MORE_COLS + col
                    val flatIdx        = page * MORE_SIZE + pageLocalIdx
                    if (pageLocalIdx < widgets.size) {
                        val widget         = widgets[pageLocalIdx]
                        val isDraggingThis = dragState.dragging?.let {
                            it.sourceZone == DragZone.MORE && it.sourceIndex == flatIdx
                        } ?: false
                        val isDropTarget   = dragState.dragging?.let { info ->
                            !(info.sourceZone == DragZone.MORE && info.sourceIndex == flatIdx) &&
                                    dragState.slotBounds[DragZone.MORE to flatIdx]
                                        ?.contains(info.fingerRootOffset) == true
                        } ?: false

                        MoreSlot(
                            widget          = widget,
                            flatIdx         = flatIdx,
                            isDraggingThis  = isDraggingThis,
                            isDropTarget    = isDropTarget,
                            isEditMode      = isEditMode,
                            canAddToPinned  = canAddToPinned,
                            dragState       = dragState,
                            modifier        = Modifier.weight(1f),
                            onDragStart     = { onDragStart(widget, flatIdx) },
                            onDragUpdate    = onDragUpdate,
                            onDragEnd       = onDragEnd,
                            onEnterEditMode = onEnterEditMode,
                            onAdd           = { onAdd(widget) }
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 10. PINNED SLOT
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PinnedSlot(
    widget: ActionWidget,
    index: Int,
    isDraggingThis: Boolean,
    isDropTarget: Boolean,
    isEditMode: Boolean,
    dragState: QuickAccessDragState,
    modifier: Modifier = Modifier,
    onDragStart: () -> Unit,
    onDragUpdate: (Offset) -> Unit,
    onDragEnd: (Offset) -> Unit,
    onEnterEditMode: () -> Unit,
    onRemove: () -> Unit
) {
    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                dragState.slotBounds[DragZone.PINNED to index] = coords.boundsInRoot()
            }
            .then(
                if (isDropTarget) Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE84C1E).copy(alpha = 0.18f))
                else Modifier
            )
            .graphicsLayer { alpha = if (isDraggingThis) 0f else 1f }
            .then(
                if (isEditMode) Modifier.dragSource(
                    zone         = DragZone.PINNED,
                    slotIndex    = index,
                    dragState    = dragState,
                    onDragStart  = onDragStart,
                    onDragUpdate = onDragUpdate,
                    onDragEnd    = onDragEnd
                ) else Modifier
            )
    ) {
        PinnedWidgetCard(
            widget          = widget,
            isDragging      = isDraggingThis,
            isEditMode      = isEditMode,
            onRemove        = onRemove,
            onLongClick     = onEnterEditMode
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 11. MORE SLOT
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MoreSlot(
    widget: ActionWidget,
    flatIdx: Int,
    isDraggingThis: Boolean,
    isDropTarget: Boolean,
    isEditMode: Boolean,
    canAddToPinned: Boolean,
    dragState: QuickAccessDragState,
    modifier: Modifier = Modifier,
    onDragStart: () -> Unit,
    onDragUpdate: (Offset) -> Unit,
    onDragEnd: (Offset) -> Unit,
    onEnterEditMode: () -> Unit,
    onAdd: () -> Unit
) {
    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                dragState.slotBounds[DragZone.MORE to flatIdx] = coords.boundsInRoot()
            }
            .then(
                if (isDropTarget) Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE84C1E).copy(alpha = 0.18f))
                else Modifier
            )
            .graphicsLayer { alpha = if (isDraggingThis) 0f else 1f }
            .then(
                if (isEditMode) Modifier.dragSource(
                    zone         = DragZone.MORE,
                    slotIndex    = flatIdx,
                    dragState    = dragState,
                    onDragStart  = onDragStart,
                    onDragUpdate = onDragUpdate,
                    onDragEnd    = onDragEnd
                ) else Modifier
            )
    ) {
        MoreWidgetCard(
            widget          = widget,
            isDragging      = isDraggingThis,
            isEditMode      = isEditMode,
            canAdd          = canAddToPinned,
            onAdd           = onAdd,
            onLongClick     = onEnterEditMode
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 12. DRAG SOURCE MODIFIER
// ─────────────────────────────────────────────────────────────────────────────

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
            // 1. Create DragInfo first
            onDragStart()
            // 2. Immediately resolve root coords from localOffset so the
            //    ghost appears at the finger from frame 0, not Offset.Zero
            val bounds = dragState.slotBounds[zone to slotIndex]
            if (bounds != null) {
                onDragUpdate(Offset(
                    x = bounds.left + localOffset.x,
                    y = bounds.top  + localOffset.y
                ))
            }
        },
        onDrag = { change, _ ->
            change.consume()
            val bounds = dragState.slotBounds[zone to slotIndex]
            if (bounds != null) {
                onDragUpdate(Offset(
                    x = bounds.left + change.position.x,
                    y = bounds.top  + change.position.y
                ))
            }
        },
        onDragEnd    = { onDragEnd(dragState.dragging?.fingerRootOffset ?: Offset.Zero) },
        onDragCancel = { onDragEnd(dragState.dragging?.fingerRootOffset ?: Offset.Zero) }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 13. DRAG GHOST
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DragGhost(
    widget: ActionWidget,
    offset: Offset,              // root-coordinate finger position
    isPinned: Boolean,
    containerRootOffset: Offset  // root-coordinate top-left of the parent Box
) {
    val density     = LocalDensity.current
    val ghostSizeDp = if (isPinned) 80.dp else 64.dp
    val ghostSizePx = with(density) { ghostSizeDp.toPx() }

    // Convert root finger position → local position inside the parent Box,
    // then shift by half the ghost size so the ghost centre is under the finger
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
// 14. DROP RESOLUTION
// ─────────────────────────────────────────────────────────────────────────────

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

    when {
        // Pinned → Pinned
        info.sourceZone == DragZone.PINNED && targetZone == DragZone.PINNED ->
            onMovePinned(info.sourceIndex, targetIdx)

        // More → More
        info.sourceZone == DragZone.MORE && targetZone == DragZone.MORE ->
            onMoveMore(info.sourceIndex, targetIdx)

        // Pinned → More
        info.sourceZone == DragZone.PINNED && targetZone == DragZone.MORE -> {
            if (currentPageWidgets.size >= MORE_SIZE)
                onSwapPinnedToMore(info.sourceIndex, targetIdx)
            else
                onMovePinnedToMore(info.sourceIndex)
        }

        // More → Pinned
        info.sourceZone == DragZone.MORE && targetZone == DragZone.PINNED -> {
            val targetWidget = pinnedWidgets.getOrNull(targetIdx)
            if (targetWidget?.action == DUMMY)
                onMoveMoreToPinned(info.sourceIndex)
            else
                onSwapMoreToPinned(info.sourceIndex, targetIdx)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 15. WIDGET CARDS  (emoji-based, no drawable dependencies)
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
        label         = "pinned_scale"
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
                Text(
                    text     = widget.emoji,
                    fontSize = 28.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
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
        label         = "more_scale"
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
            Text(text = widget.emoji, fontSize = 22.sp)
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