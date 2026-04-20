package com.viwath.practice_module_app.drag_drop

@Composable
fun QuickAccessMenuDragGrid(
    pinnedWidgets: List<ActionWidget>,
    moreWidgets: List<List<ActionWidget>>,
    isEditMode: Boolean,
    canAddToPinned: Boolean,
    onIntent: (QuickAccessMenuIntentional.Intention) -> Unit
) {
    var morePage by remember { mutableIntStateOf(0) }
    val moreTotalPages = moreWidgets.size.coerceAtLeast(1)

    LaunchedEffect(moreTotalPages) {
        if (morePage >= moreTotalPages) morePage = (moreTotalPages - 1).coerceAtLeast(0)
    }

    val currentMorePage = moreWidgets.getOrElse(morePage) { emptyList() }

    val displayItems = remember(pinnedWidgets, currentMorePage, morePage, moreTotalPages) {
        buildList {
            add(GridItem.Header("Pinned"))
            addAll(pinnedWidgets.map { GridItem.Widget(it, isPinned = true) })
            add(GridItem.Header("More"))
            addAll(currentMorePage.map { GridItem.Widget(it, isPinned = false) })
            if (moreTotalPages > 1) {
                add(GridItem.Pager("More", morePage, moreTotalPages))
            }
        }
    }

    val gridState = rememberLazyGridState()
    val reorderableState = rememberReorderableLazyGridState(gridState) { from, to ->
        val fromItem = displayItems.getOrNull(from.index) as? GridItem.Widget ?: return@rememberReorderableLazyGridState
        handleDragMove(
            fromItem    = fromItem,
            fromIdx     = from.index,
            toIdx       = to.index,
            displayItems = displayItems,
            pinnedSize  = pinnedWidgets.size,
            moreFlat    = moreWidgets.flatten(),
            morePage    = morePage,
            onIntent    = onIntent
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(12),
        state = gridState,
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(
            displayItems,
            key = { _, item ->
                when (item) {
                    is GridItem.Header -> "header_${item.title}"
                    is GridItem.Widget -> "widget_${item.widget.action}"
                    is GridItem.Pager  -> "pager_${item.section}"
                }
            },
            span = { _, item ->
                when (item) {
                    is GridItem.Header -> GridItemSpan(12)
                    is GridItem.Widget -> if (item.isPinned) GridItemSpan(4) else GridItemSpan(3)
                    is GridItem.Pager  -> GridItemSpan(12)
                }
            }
        ) { _, item ->
            val key = when (item) {
                is GridItem.Header -> "header_${item.title}"
                is GridItem.Widget -> "widget_${item.widget.action}"
                is GridItem.Pager  -> "pager_${item.section}"
            }

            ReorderableItem(reorderableState, key = key) { isDragging ->
                when (item) {
                    is GridItem.Header -> {
                        Text(
                            text = item.title,
                            style = AppTypography.headline5.copy(fontWeight = FontWeight.SemiBold),
                            color = AppLightColors.naturalLight2,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    is GridItem.Widget -> {
                        // Reuse your existing widget card composable here.
                        // Wire remove / add / click to the same intents as before.
                        YourWidgetCard(
                            widget       = item.widget,
                            isDragging   = isDragging,
                            isPinned     = item.isPinned,
                            isEditMode   = isEditMode,
                            canAdd       = canAddToPinned && !item.isPinned,
                            dragModifier = if (isEditMode) Modifier.draggableHandle() else Modifier,
                            onRemove     = {
                                onIntent(QuickAccessMenuIntentional.Intention.OnRemoveFromGrid(item.widget.ordering - 1))
                            },
                            onAdd        = {
                                onIntent(QuickAccessMenuIntentional.Intention.OnAddToGrid(item.widget))
                            },
                            onClick      = {
                                onIntent(QuickAccessMenuIntentional.Intention.OnSetDeepLinkNavAction(item.widget))
                            },
                            onLongClick  = {
                                onIntent(QuickAccessMenuIntentional.Intention.EnterEditMode)
                            }
                        )
                    }

                    is GridItem.Pager -> {
                        PagerControls(
                            currentPage  = morePage,
                            totalPages   = item.totalPages,
                            onPageChange = { morePage = it }
                        )
                    }
                }
            }
        }
    }
}

// ─── handleDragMove ──────────────────────────────────────────────────────────

private fun handleDragMove(
    fromItem: GridItem.Widget,
    fromIdx: Int,
    toIdx: Int,
    displayItems: List<GridItem>,
    pinnedSize: Int,
    moreFlat: List<ActionWidget>,
    morePage: Int,
    onIntent: (QuickAccessMenuIntentional.Intention) -> Unit
) {
    val moreHeaderIdx = displayItems.indexOfFirst {
        it is GridItem.Header && (it as GridItem.Header).title == "More"
    }

    val fromIsPinned = fromIdx < moreHeaderIdx
    val toIsPinned   = toIdx   < moreHeaderIdx

    val PINNED_MAX     = 9
    val MORE_PAGE_SIZE = 8

    when {
        // Reorder within Pinned
        fromIsPinned && toIsPinned -> {
            val from = (fromIdx - 1).coerceAtLeast(0)
            val to   = (toIdx   - 1).coerceAtLeast(0)
            onIntent(QuickAccessMenuIntentional.Intention.OnMovePinned(from, to))
        }

        // Reorder within More (translate page-local idx → flat idx)
        !fromIsPinned && !toIsPinned -> {
            val pageOffset = morePage * MORE_PAGE_SIZE
            val from = (fromIdx - moreHeaderIdx - 1) + pageOffset
            val to   = (toIdx   - moreHeaderIdx - 1) + pageOffset
            onIntent(QuickAccessMenuIntentional.Intention.OnMoveMore(from, to))
        }

        // Pinned → More
        fromIsPinned && !toIsPinned -> {
            val pinnedSourceIdx = (fromIdx - 1).coerceAtLeast(0)
            val pageOffset      = morePage * MORE_PAGE_SIZE
            val moreTargetIdx   = (toIdx - moreHeaderIdx - 1) + pageOffset

            if (moreFlat.size >= MORE_PAGE_SIZE) {
                onIntent(QuickAccessMenuIntentional.Intention.OnSwapPinnedToMore(pinnedSourceIdx, moreTargetIdx))
            } else {
                onIntent(QuickAccessMenuIntentional.Intention.OnMovePinnedToMore(pinnedSourceIdx))
            }
        }

        // More → Pinned
        !fromIsPinned && toIsPinned -> {
            val pageOffset      = morePage * MORE_PAGE_SIZE
            val moreSourceIdx   = (fromIdx - moreHeaderIdx - 1) + pageOffset
            val pinnedTargetIdx = (toIdx   - 1).coerceAtLeast(0)

            if (pinnedSize >= PINNED_MAX) {
                onIntent(QuickAccessMenuIntentional.Intention.OnSwapMoreToPinned(moreSourceIdx, pinnedTargetIdx))
            } else {
                onIntent(QuickAccessMenuIntentional.Intention.OnMoveMoreToPinned(moreSourceIdx))
            }
        }
    }
}