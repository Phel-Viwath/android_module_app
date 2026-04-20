package com.viwath.practice_module_app.drag_drop

@HiltViewModel
class QuickAccessMenuViewModel @Inject constructor(
    private val dataStore: CBADataStorePreference,
    private val db: AppDatabase,
    private val widgetRepo: WidgetRepository,
    private val firestoreProvider: FireStoreFeaturesProvider,
    private val gson: Gson
) : BaseMVIViewModel<QuickAccessMenuStateModel>(), QuickAccessMenuIntentional {

    private val _deepLinkNavAction = Channel<ActionWidget?>()

    private val widgets get() = widgetRepo.loginActionWidgets
    private val isSupportAccountOpening get() = firestoreProvider.isSupportOpenAccount()

    companion object {
        private const val DUMMY = "DUMMY"
        private const val PINNED_MAX = 9
        private const val MORE_PAGE_SIZE = 8
    }

    init {
        getQuickActionPanelShowState()
        loadAllWidget()
        runInViewModel {
            db.homeWidgetDao().resetLoginWidget()
        }
    }

    override fun initialState() = QuickAccessMenuStateModel()

    override val deeplinkNavAction: Flow<ActionWidget?>
        get() = _deepLinkNavAction.receiveAsFlow()

    override fun onHandleIntent(intent: QuickAccessMenuIntentional.Intention) {
        when (intent) {
            // Drag-drop
            is QuickAccessMenuIntentional.Intention.OnMovePinned ->
                onMovePinned(intent.from, intent.to)
            is QuickAccessMenuIntentional.Intention.OnMoveMore ->
                onMoveMore(intent.from, intent.to)
            is QuickAccessMenuIntentional.Intention.OnSwapMoreToPinned ->
                swapMoreToPinned(intent.moreSourceIdx, intent.pinnedTargetIdx)
            is QuickAccessMenuIntentional.Intention.OnSwapPinnedToMore ->
                swapPinnedToMore(intent.pinnedSourceIdx, intent.moreTargetIdx)
            is QuickAccessMenuIntentional.Intention.OnMovePinnedToMore ->
                movePinnedToMore(intent.pinnedSourceIdx)
            is QuickAccessMenuIntentional.Intention.OnMoveMoreToPinned ->
                moveMoreToPinned(intent.moreSourceIdx)
        }
    }

    // ─── All your existing methods stay exactly as they are ──────────────────

    private fun lockQuickActionPanel() {
        runInViewModel {
            val isLocked = dataStore.getLoginQuickActionPanelState()
            setState { state -> state.copy(quickActionPanelOpened = !isLocked) }
            dataStore.setLoginQuickActionPanelState(!isLocked)
        }
    }

    private fun onDismissQuickActionPanel(onDismiss: Boolean) {
        setState { it.copy(quickActionPanelOpened = onDismiss) }
    }

    private fun getQuickActionPanelShowState() {
        runInViewModel {
            val isLocked = dataStore.getLoginQuickActionPanelState()
            setState { it.copy(quickActionPanelOpened = isLocked) }
        }
    }

    private fun onEnterEditMode() {
        val pinnedActionWidget = state.value.pinnedActionWidget
        when {
            pinnedActionWidget.countDummy() == 0 -> setState { it.copy(isInEditMode = true, enableSave = true) }
            else -> setState { it.copy(isInEditMode = true, enableReset = true) }
        }
    }

    private fun onExitEditMode() = runInViewModel {
        val pinnedWidget = state.value.pinnedActionWidget.toMutableList()
        val moreWidget = state.value.moreActionWidget
        val dummyIndexes = pinnedWidget.mapIndexedNotNull { index, widget ->
            if (widget.action == DUMMY) index else null
        }
        if (dummyIndexes.isEmpty()) {
            setState { it.copy(isInEditMode = false, canAddToPinned = false) }
            return@runInViewModel
        }
        val moreWidgets = moreWidget.flatten().toMutableList()
        val forceUpdatePinned = pinnedWidget.apply {
            dummyIndexes.forEachIndexed { index, dummyIndex ->
                if (index < moreWidgets.size) {
                    this[dummyIndex] = moreWidgets[index].copy(ordering = dummyIndex + 1)
                }
            }
        }.mapGridToPinnedWidgetEntity()
        db.homeWidgetDao().insertAll(forceUpdatePinned)
        val mapPinned = forceUpdatePinned.toListActionWidget()
        setState {
            it.copy(
                pinnedActionWidget = mapPinned,
                moreActionWidget = calculateMoreWidgets(mapPinned),
                isInEditMode = false,
                canAddToPinned = false
            )
        }
    }

    private fun loadAllWidget() = runInViewModel {
        val dbPinned = db.homeWidgetDao().getPinnedWidget()
        if (dbPinned.isEmpty()) {
            val default = widgets.take(PINNED_MAX).mapGridToPinnedWidgetEntity()
            db.homeWidgetDao().insertAll(default)
            setState {
                it.copy(
                    pinnedActionWidget = default.mapToListActionWidget(),
                    moreActionWidget = calculateMoreWidgets(default.toListActionWidget())
                )
            }
        } else {
            setState {
                it.copy(
                    pinnedActionWidget = dbPinned.mapToListActionWidget(),
                    moreActionWidget = calculateMoreWidgets(dbPinned.toListActionWidget())
                )
            }
        }
    }

    private fun updatePinnedWidget() = runInViewModel {
        val pinned = state.value.pinnedActionWidget.map {
            if (it.action == DUMMY) return@runInViewModel
            it
        }.mapGridToPinnedWidgetEntity()
        db.homeWidgetDao().apply {
            resetLoginWidget()
            insertAll(pinned)
        }
        val mappedPinned = pinned.mapToListActionWidget()
        setState {
            it.copy(
                pinnedActionWidget = mappedPinned,
                moreActionWidget = calculateMoreWidgets(pinned.toListActionWidget()),
                isInEditMode = false,
                canAddToPinned = false
            )
        }
    }

    private fun onRemoveFromPinned(index: Int) {
        val pinned = state.value.pinnedActionWidget.toMutableList()
        val moreWidget = state.value.moreActionWidget.flatten().toMutableList()
        val removed = pinned[index]
        pinned[index] = ActionWidget.generateDummy(index)
        moreWidget.add(0, removed)
        setState {
            it.copy(
                pinnedActionWidget = pinned,
                moreActionWidget = moreWidget.chunked(MORE_PAGE_SIZE),
                canAddToPinned = true,
                enableSave = false,
                enableReset = true
            )
        }
    }

    private fun onAddToPinned(widget: ActionWidget) {
        val pinnedWidget = state.value.pinnedActionWidget.toMutableList()
        val moreWidget = state.value.moreActionWidget.flatten().toMutableList()
        val emptyIndex = pinnedWidget.indexOfFirst { it.action == DUMMY }
        if (emptyIndex == -1) return
        val updatedPinned = pinnedWidget.apply {
            this[emptyIndex] = widget.copy(ordering = emptyIndex + 1)
        }
        moreWidget.remove(widget)
        when {
            pinnedWidget.countDummy() == 0 -> setState {
                it.copy(
                    pinnedActionWidget = updatedPinned,
                    moreActionWidget = moreWidget.chunked(MORE_PAGE_SIZE),
                    canAddToPinned = false,
                    enableSave = true
                )
            }
            else -> setState {
                it.copy(
                    pinnedActionWidget = updatedPinned,
                    moreActionWidget = moreWidget.chunked(MORE_PAGE_SIZE)
                )
            }
        }
    }

    private fun onResetPinned() = runInViewModel {
        val default = widgets.take(PINNED_MAX).mapGridToPinnedWidgetEntity()
        db.homeWidgetDao().resetLoginWidget()
        db.homeWidgetDao().insertAll(default)
        setState {
            it.copy(
                pinnedActionWidget = default.mapToListActionWidget(),
                moreActionWidget = calculateMoreWidgets(default.toListActionWidget()),
                isInEditMode = false,
                canAddToPinned = false
            )
        }
    }

    private fun setDeepLinkNavigation(widget: ActionWidget?) {
        runInViewModel { _deepLinkNavAction.send(widget) }
    }

    // ─── New drag-drop methods ────────────────────────────────────────────────

    private fun isPinnedFull() = state.value.pinnedActionWidget.none { it.action == DUMMY }
    private fun isMoreFull(page: Int = 0): Boolean {
        val page0 = state.value.moreActionWidget.getOrNull(page) ?: return false
        return page0.size >= MORE_PAGE_SIZE
    }

    /** Reorder within Pinned */
    private fun onMovePinned(from: Int, to: Int) {
        val pinned = state.value.pinnedActionWidget.toMutableList()
        if (from !in pinned.indices || to !in pinned.indices) return
        pinned.add(to, pinned.removeAt(from))
        setState { it.copy(pinnedActionWidget = pinned, enableSave = true) }
    }

    /** Reorder within More (flat across all pages) */
    private fun onMoveMore(from: Int, to: Int) {
        val more = state.value.moreActionWidget.flatten().toMutableList()
        if (from !in more.indices || to !in more.indices) return
        more.add(to, more.removeAt(from))
        setState { it.copy(moreActionWidget = more.chunked(MORE_PAGE_SIZE)) }
    }

    /** More → Pinned: simple move when Pinned has a DUMMY slot */
    private fun moveMoreToPinned(moreSourceIdx: Int) {
        val pinned = state.value.pinnedActionWidget.toMutableList()
        val more = state.value.moreActionWidget.flatten().toMutableList()
        if (moreSourceIdx !in more.indices) return
        val dummyIdx = pinned.indexOfFirst { it.action == DUMMY }
        if (dummyIdx == -1) return
        val incoming = more.removeAt(moreSourceIdx)
        pinned[dummyIdx] = incoming.copy(ordering = dummyIdx + 1)
        setState {
            it.copy(
                pinnedActionWidget = pinned,
                moreActionWidget = more.chunked(MORE_PAGE_SIZE),
                enableSave = true
            )
        }
    }

    /** Pinned → More: simple move when More page has room */
    private fun movePinnedToMore(pinnedSourceIdx: Int) {
        val pinned = state.value.pinnedActionWidget.toMutableList()
        val more = state.value.moreActionWidget.flatten().toMutableList()
        if (pinnedSourceIdx !in pinned.indices) return
        val removed = pinned[pinnedSourceIdx]
        pinned[pinnedSourceIdx] = ActionWidget.generateDummy(pinnedSourceIdx)
        more.add(0, removed)
        setState {
            it.copy(
                pinnedActionWidget = pinned,
                moreActionWidget = more.chunked(MORE_PAGE_SIZE),
                canAddToPinned = true,
                enableSave = false,
                enableReset = true
            )
        }
    }

    /** More → Pinned swap: Pinned is full, swap exact slots */
    private fun swapMoreToPinned(moreSourceIdx: Int, pinnedTargetIdx: Int) {
        val pinned = state.value.pinnedActionWidget.toMutableList()
        val more = state.value.moreActionWidget.flatten().toMutableList()
        if (moreSourceIdx !in more.indices || pinnedTargetIdx !in pinned.indices) return
        val incoming = more.removeAt(moreSourceIdx)
        val displaced = pinned[pinnedTargetIdx]
        pinned[pinnedTargetIdx] = incoming.copy(ordering = pinnedTargetIdx + 1)
        more.add(0, displaced)
        setState {
            it.copy(
                pinnedActionWidget = pinned,
                moreActionWidget = more.chunked(MORE_PAGE_SIZE),
                enableSave = true
            )
        }
    }

    /** Pinned → More swap: More page is full, swap exact slots */
    private fun swapPinnedToMore(pinnedSourceIdx: Int, moreTargetIdx: Int) {
        val pinned = state.value.pinnedActionWidget.toMutableList()
        val more = state.value.moreActionWidget.flatten().toMutableList()
        if (pinnedSourceIdx !in pinned.indices || moreTargetIdx !in more.indices) return
        val incoming = pinned.removeAt(pinnedSourceIdx)
        val displaced = more[moreTargetIdx]
        more[moreTargetIdx] = incoming
        pinned.add(pinnedSourceIdx, displaced.copy(ordering = pinnedSourceIdx + 1))
        setState {
            it.copy(
                pinnedActionWidget = pinned,
                moreActionWidget = more.chunked(MORE_PAGE_SIZE),
                canAddToPinned = pinned.any { w -> w.action == DUMMY },
                enableSave = true
            )
        }
    }

    // ─── Existing helpers ─────────────────────────────────────────────────────

    private fun List<ActionWidget>.mapGridToPinnedWidgetEntity(): List<HomeWidgetEntity> {
        return mapIndexed { i, w ->
            w.asEntity().copy(isLoginWidget = true, ordering = i)
        }
    }

    private fun List<HomeWidgetEntity>.toListActionWidget(): List<ActionWidget> {
        return mapNotNull { entity ->
            val targetName = when {
                !isSupportAccountOpening && entity.name == "NEW_ACCOUNT" -> "LOCATOR"
                !isSupportAccountOpening && entity.name == "NEW_FIXED_DEPOSIT" -> "EXCHANGE_RATE"
                else -> entity.name
            }
            widgets.firstOrNull { it.name == targetName }
        }
    }

    private fun List<HomeWidgetEntity>.mapToListActionWidget(): List<ActionWidget> {
        return mapNotNull { entity ->
            widgets.firstOrNull { it.name == entity.name }?.copy(ordering = entity.ordering + 1)
        }
    }

    private fun calculateMoreWidgets(pinned: List<ActionWidget>): List<List<ActionWidget>> {
        return widgets.filterNot { it in pinned }.chunked(MORE_PAGE_SIZE)
    }

    private fun List<ActionWidget>.countDummy() = count { it.action == DUMMY }

    fun toJsonString(widget: ActionWidget) = gson.toJson(widget)
}