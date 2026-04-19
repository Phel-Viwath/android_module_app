package com.viwath.practice_module_app.drag_drop2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

private const val PINNED_PAGE_SIZE = 9
private const val MORE_PAGE_SIZE = 8

sealed class GridItem {
    data class Header(val title: String, val currentPage: Int, val totalPages: Int) : GridItem()
    data class Menu(val item: MenuItem, val isPinned: Boolean) : GridItem()
    data class Pager(val section: String, val currentPage: Int, val totalPages: Int) : GridItem()
}

@Composable
fun MainMenuGrid(
    viewModel: MenuViewModel = viewModel(),
    isEditMode: Boolean = true
) {
    val pinnedIndices by viewModel.pinnedIndices.collectAsState()
    val moreIndices by viewModel.moreIndices.collectAsState()

    var pinnedPage by remember { mutableIntStateOf(0) }
    var morePage by remember { mutableIntStateOf(0) }

    // Reset to page 0 if items shrink below current page
    val pinnedTotalPages = remember(pinnedIndices) {
        ((pinnedIndices.size + PINNED_PAGE_SIZE - 1) / PINNED_PAGE_SIZE).coerceAtLeast(1)
    }
    val moreTotalPages = remember(moreIndices) {
        ((moreIndices.size + MORE_PAGE_SIZE - 1) / MORE_PAGE_SIZE).coerceAtLeast(1)
    }

    LaunchedEffect(pinnedTotalPages) {
        if (pinnedPage >= pinnedTotalPages) pinnedPage = (pinnedTotalPages - 1).coerceAtLeast(0)
    }
    LaunchedEffect(moreTotalPages) {
        if (morePage >= moreTotalPages) morePage = (moreTotalPages - 1).coerceAtLeast(0)
    }

    val pinnedPageItems = remember(pinnedIndices, pinnedPage) {
        pinnedIndices
            .drop(pinnedPage * PINNED_PAGE_SIZE)
            .take(PINNED_PAGE_SIZE)
    }
    val morePageItems = remember(moreIndices, morePage) {
        moreIndices
            .drop(morePage * MORE_PAGE_SIZE)
            .take(MORE_PAGE_SIZE)
    }

    val displayItems = remember(pinnedPageItems, morePageItems, pinnedPage, pinnedTotalPages, morePage, moreTotalPages) {
        buildList {
            add(GridItem.Header("Pinned", pinnedPage, pinnedTotalPages))
            addAll(pinnedPageItems.map { GridItem.Menu(allMenuItems[it], true) })
            if (pinnedTotalPages > 1) {
                add(GridItem.Pager("Pinned", pinnedPage, pinnedTotalPages))
            }
            add(GridItem.Header("More", morePage, moreTotalPages))
            addAll(morePageItems.map { GridItem.Menu(allMenuItems[it], false) })
            if (moreTotalPages > 1) {
                add(GridItem.Pager("More", morePage, moreTotalPages))
            }
        }
    }

    val gridState = rememberLazyGridState()

    val reorderableState = rememberReorderableLazyGridState(gridState) { from, to ->
        val fromItem = displayItems.getOrNull(from.index)
        if (fromItem is GridItem.Menu) {
            handleMove(fromItem, from.index, to.index, displayItems, viewModel)
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(12),
        state = gridState,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(
            displayItems,
            key = { _, item ->
                when (item) {
                    is GridItem.Header -> "header_${item.title}"
                    is GridItem.Menu -> "menu_${item.item.action}"
                    is GridItem.Pager -> "pager_${item.section}"
                }
            },
            span = { _, item ->
                when (item) {
                    is GridItem.Header -> GridItemSpan(12)
                    is GridItem.Menu -> if (item.isPinned) GridItemSpan(4) else GridItemSpan(3)
                    is GridItem.Pager -> GridItemSpan(12)
                }
            }
        ) { index, item ->
            val itemKey = remember(item) {
                when (item) {
                    is GridItem.Header -> "header_${item.title}"
                    is GridItem.Menu -> "menu_${item.item.action}"
                    is GridItem.Pager -> "pager_${item.section}"
                }
            }

            ReorderableItem(reorderableState, key = itemKey) { isDragging ->
                when (item) {
                    is GridItem.Header -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            if (item.totalPages > 1) {
                                Text(
                                    text = "${item.currentPage + 1} / ${item.totalPages}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    is GridItem.Menu -> {
                        MenuBox(
                            item = item.item,
                            isDragging = isDragging,
                            dragModifier = if (isEditMode) Modifier.draggableHandle() else Modifier,
                            onDoubleTap = {},
                            onLongClick = {}
                        )
                    }

                    is GridItem.Pager -> {
                        val currentPage = if (item.section == "Pinned") pinnedPage else morePage
                        val setPage: (Int) -> Unit = if (item.section == "Pinned") {
                            { pinnedPage = it }
                        } else {
                            { morePage = it }
                        }

                        PagerControls(
                            currentPage = currentPage,
                            totalPages = item.totalPages,
                            onPageChange = setPage
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PagerControls(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { if (currentPage > 0) onPageChange(currentPage - 1) },
            enabled = currentPage > 0
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Previous page"
            )
        }

        // Dot indicators
        repeat(totalPages) { page ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (page == currentPage) 8.dp else 6.dp)
                    .background(
                        color = if (page == currentPage)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
            )
        }

        IconButton(
            onClick = { if (currentPage < totalPages - 1) onPageChange(currentPage + 1) },
            enabled = currentPage < totalPages - 1
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Next page"
            )
        }
    }
}

// ─── In handleMove ────────────────────────────────────────────────────────────

private fun handleMove(
    fromItem: GridItem.Menu,
    fromIdx: Int,
    toIdx: Int,
    displayItems: List<GridItem>,
    viewModel: MenuViewModel
) {
    val moreHeaderIdx = displayItems.indexOfFirst {
        it is GridItem.Header && (it as GridItem.Header).title == "More"
    }

    val fromIsPinned = fromIdx < moreHeaderIdx
    val toIsPinned   = toIdx   < moreHeaderIdx

    when {
        // CASE 1: Reorder within Pinned
        fromIsPinned && toIsPinned -> {
            val pinnedFrom = fromIdx - 1  // -1 for "Pinned" header
            val pinnedTo   = (toIdx - 1).coerceAtLeast(0)
            viewModel.movePinnedItem(pinnedFrom, pinnedTo)
        }

        // CASE 2: Reorder within More
        !fromIsPinned && !toIsPinned -> {
            val moreFrom = fromIdx - moreHeaderIdx - 1
            val moreTo   = (toIdx - moreHeaderIdx - 1).coerceAtLeast(0)
            viewModel.moveMoreItem(moreFrom, moreTo)
        }

        // CASE 3: Pinned -> More (always allowed)
        fromIsPinned && !toIsPinned -> {
            viewModel.movePinnedToMore(fromIdx - 1)
        }

        // CASE 4: More -> Pinned
        // If Pinned is full, SWAP instead of blocking
        !fromIsPinned && toIsPinned -> {
            val moreSourceIdx  = fromIdx - moreHeaderIdx - 1
            val pinnedTargetIdx = (toIdx - 1).coerceAtLeast(0)

            if (viewModel.isPinnedFull()) {
                // Swap: bump the pinned item at targetIdx out → More, pull More item in → Pinned
                viewModel.swapMoreToPinned(
                    moreSourceIdx   = moreSourceIdx,
                    pinnedTargetIdx = pinnedTargetIdx
                )
            } else {
                viewModel.moveMoreToPinned(moreSourceIdx)
            }
        }

        // CASE 3: Pinned -> More
        // If More is full, SWAP instead of just moving
        fromIsPinned && !toIsPinned -> {
            val pinnedSourceIdx = fromIdx - 1
            val moreTargetIdx   = (toIdx - moreHeaderIdx - 1).coerceAtLeast(0)

            if (viewModel.isMoreFull()) {
                viewModel.swapPinnedToMore(
                    pinnedSourceIdx = pinnedSourceIdx,
                    moreTargetIdx   = moreTargetIdx
                )
            } else {
                viewModel.movePinnedToMore(pinnedSourceIdx)
            }
        }
    }
}