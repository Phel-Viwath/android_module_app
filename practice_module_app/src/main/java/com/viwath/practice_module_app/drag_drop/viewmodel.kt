package com.viwath.practice_module_app.drag_drop

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import com.viwath.practice_module_app.drag_drop.GridPops.DUMMY
import com.viwath.practice_module_app.drag_drop.GridPops.MORE_SIZE
import com.viwath.practice_module_app.drag_drop.GridPops.PINNED_MAX
import kotlin.collections.chunked

// Data class for quick action widgets
data class ActionWidget(
    val action: String,
    val label: String,
    val emoji: String,
    val ordering: Int = 0,
    val isEnabled: Boolean = true
)


// Sample data - just random banking actions I thought would make sense
private val ALL_WIDGETS = listOf(
    ActionWidget("TRANSFER", "Transfer", "💸"),
    ActionWidget("PAY_BILL", "Pay Bill", "🧾"),
    ActionWidget("QR_PAY", "QR Pay", "📱"),
    ActionWidget("TOP_UP", "Top Up", "⚡"),
    ActionWidget("HISTORY", "History", "🕒"),
    ActionWidget("LOAN", "Loan", "🏦"),
    ActionWidget("INVESTMENT", "Invest", "📈"),
    ActionWidget("LOCATOR", "ATM", "📍"),
    ActionWidget("FX_RATE", "FX Rate", "💱"),
    ActionWidget("INSURANCE", "Insurance", "🛡️"),
    ActionWidget("SAVINGS", "Savings", "🏛️"),
    ActionWidget("CARD", "Card", "💳"),
    ActionWidget("REWARDS", "Rewards", "🎁"),
    ActionWidget("SPLIT", "Split", "✂️"),
    ActionWidget("SCHEDULE", "Schedule", "📅"),
    ActionWidget("FIXED_DEP", "Fixed Dep.", "🔒"),
    ActionWidget("STATEMENT", "Statement", "📄"),
    ActionWidget("SUPPORT", "Support", "🎧"),
    ActionWidget("EXCHANGE", "Exchange", "🔄"),
    ActionWidget("DEPOSIT", "Deposit", "💰"),
    ActionWidget("WITHDRAW", "Withdraw", "🏧"),
    ActionWidget("GAME", "Game", "🎮"),
    ActionWidget("MUSIC", "Music", "🎵"),
    ActionWidget("CAMERA", "Camera", "📷"),
    ActionWidget("MAPS", "Maps", "🗺️"),
)

// Drag zones for cross-zone dragging
enum class DragZone { PINNED, MORE }

// Extended DragInfo with phase tracking
data class DragInfo(
    val widget: ActionWidget,
    val sourceZone: DragZone,
    val sourceIndex: Int,       // current position in its zone
    val pinnedPlaceholderIndex: Int? = null,  // original Pinned index while crossing
    val fingerRootOffset: Offset = Offset.Zero,
)

class QuickAccessDragState {
    var dragging by mutableStateOf<DragInfo?>(null)
    val slotBounds = mutableStateMapOf<Pair<DragZone, Int>, Rect>()
}

// Main UI state
data class QuickAccessState(
    val pinnedWidgets: List<ActionWidget> = emptyList(),
    val moreWidgets: List<List<ActionWidget>> = emptyList(),
    val isEditMode: Boolean = false,
    val canAddToPinned: Boolean = false,
    val enableSave: Boolean = false,
    val enableReset: Boolean = false
)

// ViewModel - handles all the business logic
class QuickAccessTestViewModel : ViewModel() {

    private val _state = mutableStateOf(QuickAccessState())
    val state: State<QuickAccessState> = _state

    private fun updateState(update: (QuickAccessState) -> QuickAccessState) {
        _state.value = update(_state.value)
    }

    init {
        // Fill pinned with first 9 items, rest go to more section
        val pinned = ALL_WIDGETS.take(PINNED_MAX).mapIndexed { i, w -> w.copy(ordering = i) }
        val more = ALL_WIDGETS.drop(PINNED_MAX).chunked(MORE_SIZE)
        updateState { it.copy(pinnedWidgets = pinned, moreWidgets = more) }
    }

    fun enterEditMode() {
        val hasDummy = _state.value.pinnedWidgets.any { it.action == DUMMY }
        updateState { it.copy(isEditMode = true, enableSave = !hasDummy, enableReset = hasDummy) }
    }

    fun exitEditMode() {
        val pinned = _state.value.pinnedWidgets.toMutableList()
        val flat = _state.value.moreWidgets.flatten().toMutableList()

        // Replace dummy slots with actual widgets
        pinned.forEachIndexed { i, w ->
            if (w.action == DUMMY && flat.isNotEmpty())
                pinned[i] = flat.removeAt(0).copy(ordering = i)
        }

        updateState {
            it.copy(
                pinnedWidgets = pinned,
                moreWidgets = flat.chunked(MORE_SIZE).ifEmpty { listOf(emptyList()) },
                isEditMode = false,
                canAddToPinned = false,
                enableSave = false,
                enableReset = false
            )
        }
    }

    fun saveLayout() = updateState {
        it.copy(isEditMode = false, canAddToPinned = false, enableSave = false, enableReset = false)
    }

    fun reset() {
        val pinned = ALL_WIDGETS.take(PINNED_MAX).mapIndexed { i, w -> w.copy(ordering = i) }
        val more = ALL_WIDGETS.drop(PINNED_MAX).chunked(MORE_SIZE)
        updateState {
            it.copy(
                pinnedWidgets = pinned,
                moreWidgets = more,
                isEditMode = false,
                canAddToPinned = false,
                enableSave = false,
                enableReset = false
            )
        }
    }

    fun removeFromPinned(index: Int) {
        val pinned = _state.value.pinnedWidgets.toMutableList()
        val flat = _state.value.moreWidgets.flatten().toMutableList()
        val removed = pinned[index]

        pinned[index] = ActionWidget(DUMMY, "", "", index)
        flat.add(0, removed)

        updateState {
            it.copy(
                pinnedWidgets = pinned,
                moreWidgets = flat.chunked(MORE_SIZE).ifEmpty { listOf(emptyList()) },
                canAddToPinned = true,
                enableSave = false,
                enableReset = true
            )
        }
    }

    fun addToPinned(widget: ActionWidget) {
        val pinned = _state.value.pinnedWidgets.toMutableList()
        val flat = _state.value.moreWidgets.flatten().toMutableList()
        val emptyIdx = pinned.indexOfFirst { it.action == DUMMY }

        if (emptyIdx == -1) return

        pinned[emptyIdx] = widget.copy(ordering = emptyIdx)
        flat.remove(widget)
        val noDummy = pinned.none { it.action == DUMMY }

        updateState {
            it.copy(
                pinnedWidgets = pinned,
                moreWidgets = flat.chunked(MORE_SIZE).ifEmpty { listOf(emptyList()) },
                canAddToPinned = !noDummy,
                enableSave = noDummy,
                enableReset = !noDummy
            )
        }
    }

    // Reordering within pinned zone
    fun movePinned(from: Int, to: Int) {
//        val pinned = _state.value.pinnedWidgets.toMutableList()
//        if (from !in pinned.indices || to !in pinned.indices) return
//        val tmp      = pinned[from]
//        pinned[from] = pinned[to].copy(ordering = from)
//        pinned[to]   = tmp.copy(ordering = to)
        val pinned = _state.value.pinnedWidgets.toMutableList()
        if (from !in pinned.indices || to !in pinned.indices) return
        // Remove from source, insert at target — everything in between shifts
        val item = pinned.removeAt(from)
        pinned.add(to, item)
        updateState { it.copy(pinnedWidgets = pinned, enableSave = true) }
    }

    // Reordering within more zone
    fun moveMore(from: Int, to: Int) {
//        val flat = _state.value.moreWidgets.flatten().toMutableList()
//        if (from !in flat.indices || to !in flat.indices) return
//
//        val temp = flat[from]
//        flat[from] = flat[to]
//        flat[to] = temp
        val flat = _state.value.moreWidgets.flatten().toMutableList()
        if (from !in flat.indices || to !in flat.indices) return
        if (from == to) return
        val item = flat.removeAt(from)
        flat.add(to, item)
        updateState { it.copy(moreWidgets = flat.chunked(MORE_SIZE).ifEmpty { listOf(emptyList()) }) }
    }

    // Move from pinned to more (inserts at beginning of more)
    fun movePinnedToMore(pinnedIdx: Int) {
        val pinned = _state.value.pinnedWidgets.toMutableList()
        val flat = _state.value.moreWidgets.flatten().toMutableList()
        val removed = pinned[pinnedIdx]

        pinned[pinnedIdx] = ActionWidget(DUMMY, "", "", pinnedIdx)
        flat.add(0, removed)

        updateState {
            it.copy(
                pinnedWidgets = pinned,
                moreWidgets = flat.chunked(MORE_SIZE).ifEmpty { listOf(emptyList()) },
                canAddToPinned = true,
                enableSave = false,
                enableReset = true
            )
        }
    }

    // Swap between pinned and more (when target slot is occupied)
    fun swapPinnedToMore(pinnedIdx: Int, moreIdx: Int) {
        val pinned = _state.value.pinnedWidgets.toMutableList()
        val flat = _state.value.moreWidgets.flatten().toMutableList()

        if (pinnedIdx !in pinned.indices || moreIdx !in flat.indices) return

        val fromPinned = pinned[pinnedIdx]
        val fromMore = flat[moreIdx]

        pinned[pinnedIdx] = fromMore.copy(ordering = pinnedIdx)
        flat[moreIdx] = fromPinned

        updateState {
            it.copy(
                pinnedWidgets = pinned,
                moreWidgets = flat.chunked(MORE_SIZE).ifEmpty { listOf(emptyList()) },
                enableSave = true
            )
        }
    }

    // Move from more to pinned (if there's a dummy slot)
    fun moveMoreToPinned(moreIdx: Int) {
        val pinned = _state.value.pinnedWidgets.toMutableList()
        val flat = _state.value.moreWidgets.flatten().toMutableList()

        if (moreIdx !in flat.indices) return

        val dummyIdx = pinned.indexOfFirst { it.action == DUMMY }
        if (dummyIdx == -1) return

        val incoming = flat.removeAt(moreIdx)
        pinned[dummyIdx] = incoming.copy(ordering = dummyIdx)
        val noDummy = pinned.none { it.action == DUMMY }

        updateState {
            it.copy(
                pinnedWidgets = pinned,
                moreWidgets = flat.chunked(MORE_SIZE).ifEmpty { listOf(emptyList()) },
                canAddToPinned = !noDummy,
                enableSave = noDummy,
                enableReset = !noDummy
            )
        }
    }

    // Swap between more and pinned (when target pinned slot is occupied)
    fun swapMoreToPinned(moreIdx: Int, pinnedIdx: Int) {
        val pinned = _state.value.pinnedWidgets.toMutableList()
        val flat = _state.value.moreWidgets.flatten().toMutableList()

        if (moreIdx !in flat.indices || pinnedIdx !in pinned.indices) return

        val incoming = flat.removeAt(moreIdx)
        val displaced = pinned[pinnedIdx]

        pinned[pinnedIdx] = incoming.copy(ordering = pinnedIdx)
        flat.add(0, displaced)

        updateState {
            it.copy(
                pinnedWidgets = pinned,
                moreWidgets = flat.chunked(MORE_SIZE).ifEmpty { listOf(emptyList()) },
                enableSave = true
            )
        }
    }
}
