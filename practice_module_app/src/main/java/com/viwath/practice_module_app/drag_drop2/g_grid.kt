package com.viwath.practice_module_app.drag_drop2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import kotlin.collections.buildList

sealed class GridItem {
    data class Header(val title: String) : GridItem()
    data class Menu(val item: MenuItem, val isPinned: Boolean) : GridItem()
}

@Composable
fun MainMenuGrid(
    viewModel: MenuViewModel = viewModel(),
    isEditMode: Boolean = true
) {
    val pinnedIndices by viewModel.pinnedIndices.collectAsState()
    val moreIndices by viewModel.moreIndices.collectAsState()

    val displayItems = remember(pinnedIndices, moreIndices) {
        buildList {
            add(GridItem.Header("Pinned"))
            addAll(pinnedIndices.map { GridItem.Menu(allMenuItems[it], true) })
            add(GridItem.Header("More"))
            addAll(moreIndices.map { GridItem.Menu(allMenuItems[it], false) })
        }
    }

    val gridState = rememberLazyGridState()

    val reorderableState = rememberReorderableLazyGridState(gridState) { from, to ->
        val fromItem = displayItems.getOrNull(from.index)
        // We ensure we are dragging a Menu item, not a header
        if (fromItem is GridItem.Menu) {
            handleMove(fromItem, from.index, to.index, displayItems, viewModel)
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(12),
        state = gridState,
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(
            displayItems,
            // STABLE KEY IS CRITICAL
            key = { _, item ->
                when(item) {
                    is GridItem.Header -> "header_${item.title}"
                    is GridItem.Menu -> "menu_${item.item.action}"
                }
            },
            span = { _, item ->
                when(item) {
                    is GridItem.Header -> GridItemSpan(12)
                    is GridItem.Menu -> if (item.isPinned) GridItemSpan(4) else GridItemSpan(3)
                }
            }
        ) { index, item ->
            // USE THE SAME STABLE KEY HERE
            val itemKey = remember(item) {
                when(item) {
                    is GridItem.Header -> "header_${item.title}"
                    is GridItem.Menu -> "menu_${item.item.action}"
                }
            }

            ReorderableItem(reorderableState, key = itemKey) { isDragging ->
                when (item) {
                    is GridItem.Header -> {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    is GridItem.Menu -> {
                        MenuGridCell(
                            item = item.item,
                            isDragging = isDragging,
                            // Ensure the handle is actually applied to the clickable surface
                            dragModifier = if (isEditMode) Modifier.draggableHandle() else Modifier,
                            onDoubleTap = {},
                            onLongClick = {}
                        )
                    }
                }
            }
        }
    }
}
/**
 * Maps the flat grid indices back to the ViewModel's sectioned logic
 */
private fun handleMove(
    fromItem: GridItem.Menu,
    fromIdx: Int,
    toIdx: Int,
    displayItems: List<GridItem>,
    viewModel: MenuViewModel
) {
    // Find where the "More" header is to determine the boundary
    val moreHeaderIdx = displayItems.indexOfFirst { it is GridItem.Header && it.title == "More" }

    val fromIsPinned = fromIdx < moreHeaderIdx
    val toIsPinned = toIdx < moreHeaderIdx

    when {
        // CASE 1: Reorder within Pinned
        fromIsPinned && toIsPinned -> {
            val pinnedFrom = fromIdx - 1 // -1 for "Pinned" header
            val pinnedTo = (toIdx - 1).coerceAtLeast(0)
            viewModel.movePinnedItem(pinnedFrom, pinnedTo)
        }
        // CASE 2: Reorder within More
        !fromIsPinned && !toIsPinned -> {
            val moreFrom = fromIdx - moreHeaderIdx - 1
            val moreTo = (toIdx - moreHeaderIdx - 1).coerceAtLeast(0)
            viewModel.moveMoreItem(moreFrom, moreTo)
        }
        // CASE 3: Pinned -> More
        fromIsPinned && !toIsPinned -> {
            viewModel.movePinnedToMore(fromIdx - 1)
        }
        // CASE 4: More -> Pinned
        !fromIsPinned && toIsPinned -> {
            viewModel.moveMoreToPinned(fromIdx - moreHeaderIdx - 1)
        }
    }
}