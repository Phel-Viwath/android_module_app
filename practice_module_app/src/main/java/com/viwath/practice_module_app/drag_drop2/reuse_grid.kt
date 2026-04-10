package com.viwath.practice_module_app.drag_drop2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

@Composable
fun <T> ReorderableGridLayout(
    items: List<T>,
    isEditMode: Boolean,
    onMove: (from: Int, to: Int) -> Unit,
    itemKey: (T) -> Any,
    modifier: Modifier = Modifier,
    columns: GridCells = GridCells.Fixed(3),
    maxHeight: Dp = 340.dp,
    content: @Composable (item: T, isDragging: Boolean, dragModifier: Modifier) -> Unit
) {
    val gridState = rememberLazyGridState()

    val reorderableGridState = rememberReorderableLazyGridState(gridState) { from, to ->
        if (isEditMode) {
            onMove(from.index, to.index)
        }
    }

    LazyVerticalGrid(
        columns = columns,
        state = gridState,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false
    ) {
        itemsIndexed(
            items,
            key = { _, item -> itemKey(item) }
        ) { index, item ->
            ReorderableItem(
                reorderableGridState,
                key = itemKey(item)
            ) { isDragging ->
                // We pass the drag logic back to the caller via the slot
                val dragModifier = if (isEditMode) {
                    Modifier.draggableHandle()
                } else {
                    Modifier
                }

                content(item, isDragging && isEditMode, dragModifier)
            }
        }
    }
}