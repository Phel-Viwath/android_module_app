package com.viwath.practice_module_app.drag_drop2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MenuViewModel2(application: Application) : AndroidViewModel(application) {

    private val dataStore = MenuPreferencesDataStore(application)

    private val _pinnedIndices = MutableStateFlow(MenuPreferencesDataStore.DEFAULT_PINNED.toMutableList())
    private val _moreIndices   = MutableStateFlow((9..24).toMutableList())

    val pinnedIndices: StateFlow<MutableList<Int>> = _pinnedIndices
    val moreIndices:   StateFlow<MutableList<Int>> = _moreIndices

    init {
        viewModelScope.launch {
            val saved = combine(
                dataStore.pinnedIndicesFlow,
                dataStore.moreIndicesFlow
            ) { p, m -> p to m }.first()
            _pinnedIndices.value = saved.first.toMutableList()
            _moreIndices.value   = saved.second.toMutableList()
        }
    }

    fun movePinnedItem(from: Int, to: Int) {
        val list = _pinnedIndices.value.toMutableList()
        val item = list.removeAt(from)
        list.add(to, item)
        _pinnedIndices.value = list
        persistPinned(list)
    }

    fun moveMoreItem(from: Int, to: Int) {
        val list = _moreIndices.value.toMutableList()
        val item = list.removeAt(from)
        list.add(to, item)
        _moreIndices.value = list
        persistMore(list)
    }

    fun movePinnedToMore(pinnedIndex: Int, moreInsertIndex: Int = 0) {
        val pinned = _pinnedIndices.value.toMutableList()
        val more   = _moreIndices.value.toMutableList()
        if (pinnedIndex !in pinned.indices) return
        val itemIdx = pinned.removeAt(pinnedIndex)
        more.add(moreInsertIndex.coerceIn(0, more.size), itemIdx)
        _pinnedIndices.value = pinned
        _moreIndices.value   = more
        persistPinned(pinned)
        persistMore(more)
    }

    fun moveMoreToPinned(moreIndex: Int, pinnedInsertIndex: Int? = null) {
        val pinned = _pinnedIndices.value.toMutableList()
        val more   = _moreIndices.value.toMutableList()
        if (moreIndex !in more.indices) return
        val itemIdx = more.removeAt(moreIndex)
        pinned.add(pinnedInsertIndex?.coerceIn(0, pinned.size) ?: pinned.size, itemIdx)
        _pinnedIndices.value = pinned
        _moreIndices.value   = more
        persistPinned(pinned)
        persistMore(more)
    }

    private fun persistPinned(list: List<Int>) {
        viewModelScope.launch { dataStore.savePinnedOrder(list) }
    }

    private fun persistMore(list: List<Int>) {
        viewModelScope.launch { dataStore.saveMoreOrder(list) }
    }
}