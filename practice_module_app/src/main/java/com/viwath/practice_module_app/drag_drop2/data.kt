package com.viwath.practice_module_app.drag_drop2

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "menu_prefs")

class MenuPreferencesDataStore(private val context: Context) {

    companion object {
        private val PINNED_ORDER_KEY = stringPreferencesKey("pinned_order")
        private val MORE_ORDER_KEY = stringPreferencesKey("more_order")

        // Default pinned indices: first 9 items (0-8) → 3×3 grid
        val DEFAULT_PINNED = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
    }

    /** Emits a list of menuItem indices that are currently pinned (in order). */
    val pinnedIndicesFlow: Flow<List<Int>> = context.dataStore.data.map { prefs ->
        prefs[PINNED_ORDER_KEY]
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.takeIf { it.isNotEmpty() }
            ?: DEFAULT_PINNED
    }

    /** Emits a list of menuItem indices that are in "More" (in order). */
    val moreIndicesFlow: Flow<List<Int>> = context.dataStore.data.map { prefs ->
        prefs[MORE_ORDER_KEY]
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.takeIf { it.isNotEmpty() }
            ?: (9..24).toList()        // items 9-24 go to "More" by default
    }

    suspend fun savePinnedOrder(indices: List<Int>) {
        context.dataStore.edit { prefs ->
            prefs[PINNED_ORDER_KEY] = indices.joinToString(",")
        }
    }

    suspend fun saveMoreOrder(indices: List<Int>) {
        context.dataStore.edit { prefs ->
            prefs[MORE_ORDER_KEY] = indices.joinToString(",")
        }
    }
}
