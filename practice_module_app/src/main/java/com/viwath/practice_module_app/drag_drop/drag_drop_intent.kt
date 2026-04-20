package com.viwath.practice_module_app.drag_drop

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface QuickAccessMenuIntentional {
    val state: StateFlow<QuickAccessMenuStateModel>
    val deeplinkNavAction: Flow<MyItem?>
    fun onHandleIntent(intent: Intention)

    sealed class Intention {
        // New drag-drop intentions
        data class OnMovePinned(val from: Int, val to: Int) : Intention()
        data class OnMoveMore(val from: Int, val to: Int) : Intention()
        data class OnSwapMoreToPinned(val moreSourceIdx: Int, val pinnedTargetIdx: Int) : Intention()
        data class OnSwapPinnedToMore(val pinnedSourceIdx: Int, val moreTargetIdx: Int) : Intention()
        data class OnMovePinnedToMore(val pinnedSourceIdx: Int) : Intention()
        data class OnMoveMoreToPinned(val moreSourceIdx: Int) : Intention()
    }
}