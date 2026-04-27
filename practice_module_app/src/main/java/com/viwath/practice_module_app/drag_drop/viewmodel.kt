package com.viwath.practice_module_app.drag_drop

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.viwath.practice_module_app.drag_drop.GridPops.DUMMY
import com.viwath.practice_module_app.drag_drop.GridPops.MORE_SIZE
import com.viwath.practice_module_app.drag_drop.GridPops.PINNED_MAX

// ─────────────────────────────────────────────────────────────────────────────
// Data
// ─────────────────────────────────────────────────────────────────────────────

data class ActionWidget(
    val action: String,
    val label: String,
    val emoji: String,
    val ordering: Int = 0,
)

private val ALL_WIDGETS = listOf(
    ActionWidget("TRANSFER",  "Transfer",  "💸"),
    ActionWidget("PAY_BILL",  "Pay Bill",  "🧾"),
    ActionWidget("QR_PAY",    "QR Pay",    "📱"),
    ActionWidget("TOP_UP",    "Top Up",    "⚡"),
    ActionWidget("HISTORY",   "History",   "🕒"),
    ActionWidget("LOAN",      "Loan",      "🏦"),
    ActionWidget("INVESTMENT","Invest",    "📈"),
    ActionWidget("LOCATOR",   "ATM",       "📍"),
    ActionWidget("FX_RATE",   "FX Rate",   "💱"),
    ActionWidget("INSURANCE", "Insurance", "🛡️"),
    ActionWidget("SAVINGS",   "Savings",   "🏛️"),
    ActionWidget("CARD",      "Card",      "💳"),
    ActionWidget("REWARDS",   "Rewards",   "🎁"),
    ActionWidget("SPLIT",     "Split",     "✂️"),
    ActionWidget("SCHEDULE",  "Schedule",  "📅"),
    ActionWidget("FIXED_DEP", "Fixed Dep.","🔒"),
    ActionWidget("STATEMENT", "Statement", "📄"),
    ActionWidget("SUPPORT",   "Support",   "🎧"),
    ActionWidget("EXCHANGE",  "Exchange",  "🔄"),
    ActionWidget("DEPOSIT",   "Deposit",   "💰"),
)

// ─────────────────────────────────────────────────────────────────────────────
// UI state — no edit-mode fields needed
// ─────────────────────────────────────────────────────────────────────────────

data class QuickAccessState(
    val pinnedWidgets: List<ActionWidget>       = emptyList(),
    val moreWidgets:   List<List<ActionWidget>> = emptyList(),
)

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

class QuickAccessViewModel : ViewModel() {

    private val _state = mutableStateOf(QuickAccessState())
    val state: State<QuickAccessState> = _state

    private fun update(block: (QuickAccessState) -> QuickAccessState) {
        _state.value = block(_state.value)
    }

    init { loadDefault() }

    private fun loadDefault() {
        val pinned = ALL_WIDGETS.take(PINNED_MAX).mapIndexed { i, w -> w.copy(ordering = i) }
        val more   = ALL_WIDGETS.drop(PINNED_MAX).chunked(MORE_SIZE)
        update { it.copy(pinnedWidgets = pinned, moreWidgets = more) }
    }

    // ── Intra-zone reorder ────────────────────────────────────────────────────
// In QuickAccessViewModel.kt

    fun movePinned(from: Int, to: Int) {
        val pinned = _state.value.pinnedWidgets.toMutableList()
        if (from !in pinned.indices || to !in pinned.indices || from == to) return

        // Standard reorder logic: Pull out and insert
        val item = pinned.removeAt(from)
        pinned.add(to, item)

        // We must update the ordering property so it matches the new index
        val updatedList = pinned.mapIndexed { index, widget ->
            widget.copy(ordering = index)
        }

        update { it.copy(pinnedWidgets = updatedList) }
    }

    fun moveMore(from: Int, to: Int) {
        val flat = _state.value.moreWidgets.flatten().toMutableList()
        if (from !in flat.indices || to !in flat.indices || from == to) return

        val item = flat.removeAt(from)
        flat.add(to, item)

        update { it.copy(moreWidgets = flat.chunked(MORE_SIZE)) }
    }

    // ── Cross-zone moves ──────────────────────────────────────────────────────

    /** Pinned widget dropped onto an empty (DUMMY) pinned slot — shouldn't happen normally. */
    fun movePinnedToMore(pinnedIdx: Int) {
        val pinned = _state.value.pinnedWidgets.toMutableList()
        val flat   = _state.value.moreWidgets.flatten().toMutableList()
        val widget = pinned[pinnedIdx]

        pinned[pinnedIdx] = ActionWidget(DUMMY, "", "", pinnedIdx)
        flat.add(0, widget)

        update {
            it.copy(
                pinnedWidgets = pinned,
                moreWidgets   = flat.chunked(MORE_SIZE).ifEmpty { listOf(emptyList()) },
            )
        }
    }

    /** Pinned → More when target More slot is occupied: swap the two widgets. */
    fun swapPinnedToMore(pinnedIdx: Int, moreIdx: Int) {
        val pinned = _state.value.pinnedWidgets.toMutableList()
        val flat   = _state.value.moreWidgets.flatten().toMutableList()
        if (pinnedIdx !in pinned.indices || moreIdx !in flat.indices) return

        val fromPinned = pinned[pinnedIdx]
        val fromMore   = flat[moreIdx]

        pinned[pinnedIdx] = fromMore.copy(ordering = pinnedIdx)
        flat[moreIdx]     = fromPinned

        update {
            it.copy(
                pinnedWidgets = pinned,
                moreWidgets   = flat.chunked(MORE_SIZE).ifEmpty { listOf(emptyList()) },
            )
        }
    }

    /** More widget dropped onto an empty (DUMMY) pinned slot. */
    fun moveMoreToPinned(moreIdx: Int) {
        val pinned   = _state.value.pinnedWidgets.toMutableList()
        val flat     = _state.value.moreWidgets.flatten().toMutableList()
        if (moreIdx !in flat.indices) return

        val dummyIdx = pinned.indexOfFirst { it.action == DUMMY }
        if (dummyIdx == -1) return

        val incoming = flat.removeAt(moreIdx)
        pinned[dummyIdx] = incoming.copy(ordering = dummyIdx)

        update {
            it.copy(
                pinnedWidgets = pinned,
                moreWidgets   = flat.chunked(MORE_SIZE).ifEmpty { listOf(emptyList()) },
            )
        }
    }

    /** More widget dropped onto an occupied pinned slot: swap. */
    fun swapMoreToPinned(moreIdx: Int, pinnedIdx: Int) {
        val pinned = _state.value.pinnedWidgets.toMutableList()
        val flat   = _state.value.moreWidgets.flatten().toMutableList()
        if (moreIdx !in flat.indices || pinnedIdx !in pinned.indices) return

        val incoming  = flat.removeAt(moreIdx)
        val displaced = pinned[pinnedIdx]

        pinned[pinnedIdx] = incoming.copy(ordering = pinnedIdx)
        flat.add(0, displaced)

        update {
            it.copy(
                pinnedWidgets = pinned,
                moreWidgets   = flat.chunked(MORE_SIZE).ifEmpty { listOf(emptyList()) },
            )
        }
    }
}