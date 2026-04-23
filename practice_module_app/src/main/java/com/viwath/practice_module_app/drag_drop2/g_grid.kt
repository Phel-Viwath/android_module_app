package com.viwath.practice_module_app.drag_drop2

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
    data class Header(val title: String) : GridItem()
    data class Menu(val item: MenuItem, val isPinned: Boolean) : GridItem()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainMenuGrid(
    viewModel: MenuViewModel = viewModel(),
    isEditMode: Boolean = true
) {
    val pinnedIndices by viewModel.pinnedIndices.collectAsState()
    val moreIndices by viewModel.moreIndices.collectAsState()

    // Pinned items — capped at PINNED_PAGE_SIZE
    val pinnedItems = remember(pinnedIndices) {
        pinnedIndices.take(PINNED_PAGE_SIZE).map { GridItem.Menu(allMenuItems[it], true) }
    }

    // More items — chunked into pages of MORE_PAGE_SIZE
    val morePages = remember(moreIndices) {
        moreIndices
            .map { GridItem.Menu(allMenuItems[it], false) }
            .chunked(MORE_PAGE_SIZE)
    }

    val morePagerState = rememberPagerState(pageCount = { morePages.size.coerceAtLeast(1) })

    // Flat display list used for drag-drop (pinned section only)
    val pinnedDisplayItems = remember(pinnedItems) {
        buildList<GridItem> {
            add(GridItem.Header("Pinned"))
            addAll(pinnedItems)
        }
    }

    val gridState = rememberLazyGridState()

    val reorderableState = rememberReorderableLazyGridState(gridState) { from, to ->
        val fromItem = pinnedDisplayItems.getOrNull(from.index)
        if (fromItem is GridItem.Menu) {
            handleMove(fromItem, from.index, to.index, pinnedDisplayItems, viewModel)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        // ── Pinned Section (reorderable grid) ──────────────────────────────
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = gridState,
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            // Let the grid be as tall as its content (no scrolling here)
            userScrollEnabled = false
        ) {
            itemsIndexed(
                pinnedDisplayItems,
                key = { _, item ->
                    when (item) {
                        is GridItem.Header -> "header_${item.title}"
                        is GridItem.Menu -> "menu_${item.item.action}"
                    }
                },
                span = { _, item ->
                    when (item) {
                        is GridItem.Header -> GridItemSpan(3) // full width
                        is GridItem.Menu -> GridItemSpan(1)   // 3 per row → 3 rows = 9 items
                    }
                }
            ) { _, item ->
                val itemKey = remember(item) {
                    when (item) {
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
                            MenuBox(
                                item = item.item,
                                isDragging = isDragging,
                                dragModifier = if (isEditMode) Modifier.draggableHandle() else Modifier,
                                onDoubleTap = {},
                                onLongClick = {}
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── More Section (paginated, horizontal swipe) ─────────────────────
        Text(
            text = "More",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (morePages.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No items", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            HorizontalPager(
                state = morePagerState,
                modifier = Modifier.fillMaxWidth()
            ) { pageIndex ->
                val pageItems = morePages.getOrElse(pageIndex) { emptyList() }

                // Each page is a non-scrolling grid of up to 8 items (4 cols × 2 rows)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    userScrollEnabled = false
                ) {
                    itemsIndexed(
                        pageItems,
                        key = { _, item -> "more_${item.item.action}_p${pageIndex}" }
                    ) { _, item ->
                        // More items are not reorderable across pages in this simplified version;
                        // wrap in ReorderableItem only if you extend drag-drop to the More section.
                        MenuBox(
                            item = item.item,
                            isDragging = false,
                            dragModifier = Modifier,
                            onDoubleTap = {},
                            onLongClick = {}
                        )
                    }
                }
            }

            // Page indicator dots
            if (morePages.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(morePages.size) { index ->
                        val isSelected = morePagerState.currentPage == index
                        val dotSize by animateDpAsState(
                            targetValue = if (isSelected) 8.dp else 6.dp,
                            label = "dot_size"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(dotSize)
                        ) {
                            // Simple dot — replace with your design token colours
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    color = if (isSelected)
                                        androidx.compose.ui.graphics.Color(0xFF6200EE)
                                    else
                                        androidx.compose.ui.graphics.Color(0xFFBBBBBB)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── handleMove (pinned section only — unchanged logic) ─────────────────────────
private fun handleMove(
    fromItem: GridItem.Menu,
    fromIdx: Int,
    toIdx: Int,
    displayItems: List<GridItem>,
    viewModel: MenuViewModel
) {
    val moreHeaderIdx = displayItems.indexOfFirst { it is GridItem.Header && it.title == "More" }
        .takeIf { it >= 0 } ?: displayItems.size  // no "More" header in pinned-only list

    val fromIsPinned = fromIdx < moreHeaderIdx
    val toIsPinned = toIdx < moreHeaderIdx

    when {
        fromIsPinned && toIsPinned -> {
            val pinnedFrom = fromIdx - 1
            val pinnedTo = (toIdx - 1).coerceAtLeast(0)
            viewModel.movePinnedItem(pinnedFrom, pinnedTo)
        }
        !fromIsPinned && !toIsPinned -> {
            val moreFrom = fromIdx - moreHeaderIdx - 1
            val moreTo = (toIdx - moreHeaderIdx - 1).coerceAtLeast(0)
            viewModel.moveMoreItem(moreFrom, moreTo)
        }
        fromIsPinned && !toIsPinned -> {
            viewModel.movePinnedToMore(fromIdx - 1)
        }
        !fromIsPinned && toIsPinned -> {
            viewModel.moveMoreToPinned(fromIdx - moreHeaderIdx - 1)
        }
    }
}