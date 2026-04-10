package com.viwath.practice_module_app.drag_drop2

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.ceil


@Composable
fun MenuScreen(viewModel: MenuViewModel = viewModel()) {

    val pinnedIndices by viewModel.pinnedIndices.collectAsState()
    val moreIndices   by viewModel.moreIndices.collectAsState()

    // Resolve indices → actual MenuItems
    val pinnedItems = pinnedIndices.map { allMenuItems[it] }
    val moreItems   = moreIndices.map { allMenuItems[it] }

    // ── Pinned grid state ────────────────────────────────────────────────────
    val gridState = rememberLazyGridState()
    val reorderableGridState = rememberReorderableLazyGridState(gridState) { from, to ->
        viewModel.movePinnedItem(from.index, to.index)
    }

    // ── More list state ──────────────────────────────────────────────────────
    val listState = rememberLazyListState()
    val reorderableListState = rememberReorderableLazyListState(listState) { from, to ->
        // "from" and "to" account for the header item offset; list has no header here
        viewModel.moveMoreItem(from.index, to.index)
    }

    // Outer scroll container wraps both sections
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp)
    ) {

        // ── PINNED section label ─────────────────────────────────────────────
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Pinned",
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            color = Color(0xFF1C1C1E)
        )
        Spacer(Modifier.height(12.dp))

        // ── PINNED 3×3 reorderable grid ──────────────────────────────────────
        // We fix the height so the outer Column can scroll; grid itself is non-scrollable.
        // Height = 3 rows × cell height (100dp) + 2 gaps (8dp) = 316dp
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = gridState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 340.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false
        ) {
            itemsIndexed(pinnedItems, key = { _, item -> "pinned_${item.action}" }) { index, item ->
                ReorderableItem(reorderableGridState, key = "pinned_${item.action}") { isDragging ->
                    MenuGridCell(
                        item = item,
                        isDragging = isDragging,
                        onLongClick = {
                            // Long-press handled by ReorderableItem internally
                        },
                        dragModifier = Modifier.draggableHandle(
                            onDragStarted = {},
                            onDragStopped = {}
                        ),
                        onDoubleTap = {
                            // Double-tap moves pinned → more
                            viewModel.movePinnedToMore(index)
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── MORE section label ───────────────────────────────────────────────
        Text(
            text = "More",
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            color = Color(0xFF1C1C1E)
        )

        Spacer(Modifier.height(12.dp))

        val pageSize = 8
        val pageCount = ceil(moreItems.size / pageSize.toFloat()).toInt()

        val pagerState = rememberPagerState(pageCount = { pageCount })
        val moreGridState = rememberLazyGridState()

        val reorderableMoreGridState =
            rememberReorderableLazyGridState(moreGridState) { from, to ->
                viewModel.moveMoreItem(from.index, to.index)
            }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ){ page ->

            val start = page * pageSize
            val end = minOf(start + pageSize, moreItems.size)

            val pageItems = moreItems.subList(start, end)

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                itemsIndexed(pageItems) { index, item ->

                    val realIndex = start + index

                    ReorderableItem(
                        reorderableMoreGridState,
                        key = "more_${item.action}"
                    ) { isDragging ->

                        MenuHorizontalItem(
                            item = item,
                            isDragging = isDragging,
                            dragModifier = Modifier.draggableHandle(
                                onDragStarted = {},
                                onDragStopped = {}
                            ),
                            onDoubleTap = {
                                viewModel.moveMoreToPinned(realIndex)
                            }
                        )

                    }

                }

            }

        }
        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {

            repeat(pageCount) { index ->

                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .height(6.dp)
                        .width(
                            if (pagerState.currentPage == index) 18.dp
                            else 6.dp
                        )
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (pagerState.currentPage == index)
                                Color(0xFF4A90E2)
                            else
                                Color.Gray.copy(alpha = 0.3f)
                        )
                )

            }

        }
    }
}

// ── Pinned grid cell ─────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MenuGridCell(
    item: MenuItem,
    isDragging: Boolean,
    onLongClick: () -> Unit,
    dragModifier: Modifier,
    onDoubleTap: () -> Unit
) {
    val elevation = if (isDragging) 8.dp else 0.dp

    Box(
        modifier = Modifier
            .aspectRatio(1.15f)
            .shadow(elevation, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDragging) Color(0xFFE8F0FE) else Color(0xFFF2F2F7))
            .border(
                width = if (isDragging) 1.5.dp else 0.dp,
                color = if (isDragging) Color(0xFF4A90E2) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .combinedClickable(
                onDoubleClick = onDoubleTap,
                onClick = {}
            )
            .then(dragModifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = item.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = Color(0xFF1C1C1E),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
    }
}

// ── More list row ────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MenuListRow(
    item: MenuItem,
    isDragging: Boolean,
    dragModifier: Modifier,
    onDoubleTap: () -> Unit
) {
    val elevation = if (isDragging) 6.dp else 0.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .shadow(elevation, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDragging) Color(0xFFE8F0FE) else Color(0xFFF2F2F7))
            .border(
                width = if (isDragging) 1.5.dp else 0.dp,
                color = if (isDragging) Color(0xFF4A90E2) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .combinedClickable(
                onDoubleClick = onDoubleTap,
                onClick = {}
            )
            .then(dragModifier),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = item.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF1C1C1E),
            modifier = Modifier.padding(horizontal = 16.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MenuHorizontalItem(
    item: MenuItem,
    isDragging: Boolean,
    dragModifier: Modifier,
    onDoubleTap: () -> Unit
) {

    val elevation = if (isDragging) 6.dp else 0.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .height(64.dp)
                .aspectRatio(1f)
                .shadow(elevation, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isDragging) Color(0xFFE8F0FE)
                    else Color(0xFFF2F2F7)
                )
                .border(
                    width = if (isDragging) 1.5.dp else 0.dp,
                    color = if (isDragging) Color(0xFF4A90E2)
                    else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                )
                .combinedClickable(
                    onDoubleClick = onDoubleTap,
                    onClick = {}
                )
                .then(dragModifier),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = item.name.take(1),
                fontWeight = FontWeight.Bold
            )

        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = item.name,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

}