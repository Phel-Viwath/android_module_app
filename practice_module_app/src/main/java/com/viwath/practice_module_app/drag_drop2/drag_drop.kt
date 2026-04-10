package com.viwath.practice_module_app.drag_drop2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class MenuItem(
    val name: String,
    val index: Int,
    val action: String
)

class MenuViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = MenuPreferencesDataStore(application)


    // Mutable backing state – indices into allMenuItems
    private val _pinnedIndices = MutableStateFlow(MenuPreferencesDataStore.DEFAULT_PINNED.toMutableList())
    private val _moreIndices   = MutableStateFlow((9..24).toMutableList())

    val pinnedIndices: StateFlow<MutableList<Int>> = _pinnedIndices
    val moreIndices: StateFlow<MutableList<Int>> = _moreIndices

    init {
        viewModelScope.launch {

            val saved = combine(
                dataStore.pinnedIndicesFlow,
                dataStore.moreIndicesFlow
            ) { pinned, more ->
                Pair(pinned, more)
            }.first()

            _pinnedIndices.value = saved.first
                .take(MAX_PINNED)
                .toMutableList()

            _moreIndices.value = saved.second.toMutableList()
        }
    }

    // ── Pinned grid reorder ──────────────────────────────────────────────────

    fun movePinnedItem(from: Int, to: Int) {
        val list = _pinnedIndices.value.toMutableList()
        val item = list.removeAt(from)
        list.add(to, item)
        _pinnedIndices.value = list
        persistPinned(list)
    }

    // ── More list reorder ────────────────────────────────────────────────────

    fun moveMoreItem(from: Int, to: Int) {
        val list = _moreIndices.value.toMutableList()
        val item = list.removeAt(from)
        list.add(to, item)
        _moreIndices.value = list
        persistMore(list)
    }

    // ── Cross-section moves ──────────────────────────────────────────────────

    /** Move item from Pinned → More. Pinned must have > 1 item so grid stays valid. */
    fun movePinnedToMore(pinnedPosition: Int) {
        val pinned = _pinnedIndices.value.toMutableList()
        val more   = _moreIndices.value.toMutableList()

        if (pinned.isEmpty()) return

        val idx = pinned.removeAt(pinnedPosition)

        more.add(0, idx)

        _pinnedIndices.value = pinned
        _moreIndices.value = more

        persistPinned(pinned)
        persistMore(more)
    }

    /** Move item from More → Pinned (appended to end of pinned list). */
    fun moveMoreToPinned(morePosition: Int) {
        val pinned = _pinnedIndices.value.toMutableList()
        val more   = _moreIndices.value.toMutableList()

        if (more.isEmpty()) return
        if (pinned.size >= MAX_PINNED) return // limit 9

        val idx = more.removeAt(morePosition)

        pinned.add(idx)

        _pinnedIndices.value = pinned
        _moreIndices.value = more

        persistPinned(pinned)
        persistMore(more)
    }

    // ── Persistence helpers ──────────────────────────────────────────────────

    private fun persistPinned(list: List<Int>) {
        viewModelScope.launch {
            dataStore.savePinnedOrder(list.take(MAX_PINNED))
        }
    }
    private fun persistMore(list: List<Int>) {
        viewModelScope.launch { dataStore.saveMoreOrder(list) }
    }

    companion object {
        const val MAX_PINNED = 9
    }
}



val allMenuItems = listOf(
    MenuItem("Home", 0, "ACTION_HOME"),
    MenuItem("Search", 1, "ACTION_SEARCH"),
    MenuItem("Library", 2, "ACTION_LIBRARY"),
    MenuItem("Playlist", 3, "ACTION_PLAYLIST"),
    MenuItem("Favorites", 4, "ACTION_FAVORITES"),
    MenuItem("Downloads", 5, "ACTION_DOWNLOADS"),
    MenuItem("Albums", 6, "ACTION_ALBUMS"),
    MenuItem("Artists", 7, "ACTION_ARTISTS"),
    MenuItem("Genres", 8, "ACTION_GENRES"),
    MenuItem("Recently Played", 9, "ACTION_RECENT"),
    MenuItem("Top Songs", 10, "ACTION_TOP_SONGS"),
    MenuItem("Podcasts", 11, "ACTION_PODCASTS"),
    MenuItem("Radio", 12, "ACTION_RADIO"),
    MenuItem("Queue", 13, "ACTION_QUEUE"),
    MenuItem("Now Playing", 14, "ACTION_NOW_PLAYING"),
    MenuItem("Sleep Timer", 15, "ACTION_SLEEP_TIMER"),
    MenuItem("Equalizer", 16, "ACTION_EQUALIZER"),
    MenuItem("Settings", 17, "ACTION_SETTINGS"),
    MenuItem("Profile", 18, "ACTION_PROFILE"),
    MenuItem("Notifications", 19, "ACTION_NOTIFICATIONS"),
    MenuItem("History", 20, "ACTION_HISTORY"),
    MenuItem("Trending", 21, "ACTION_TRENDING"),
    MenuItem("New Releases", 22, "ACTION_NEW_RELEASES"),
    MenuItem("Recommended", 23, "ACTION_RECOMMENDED"),
    MenuItem("About", 24, "ACTION_ABOUT")
)