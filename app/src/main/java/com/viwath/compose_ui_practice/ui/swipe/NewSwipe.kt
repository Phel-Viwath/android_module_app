package com.viwath.compose_ui_practice.ui.swipe

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun SwipeableDrawerScreen(
    modifier: Modifier = Modifier,
    enableRightDrawer: Boolean = true,
    enableLeftDrawer: Boolean = true,
    drawerState: Float,
    onDrawerStateChanged: (Float) -> Unit = {},
    onCenterPanelStateChanged: (Boolean) -> Unit = {},
    mainContent: @Composable BoxScope.() -> Unit,
    rightDrawer: @Composable BoxScope.() -> Unit,
    leftDrawer: @Composable BoxScope.() -> Unit,
    centerPanel: @Composable BoxScope.() -> Unit,
    onSwipeUp: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val drawerWidth: Dp = LocalConfiguration.current.screenWidthDp.dp

    val density = LocalDensity.current
    val screenHeightPx = with(density) { screenHeight.toPx() }
    val screenWidthPx = with(density) { screenWidth.toPx() }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val centerPanelState = rememberCenterPanelState(
        screenHeightPx,
        screenWidthPx
    )

    val isCenterPanelOpen = centerPanelState.isOpen

    // Notify caller whenever open state flips.
    LaunchedEffect(isCenterPanelOpen) {
        onCenterPanelStateChanged(isCenterPanelOpen)
    }

    // ── BackHandler: collapse center panel when it is open ─────────────────
    BackHandler(enabled = centerPanelState.isOpen) {
        scope.launch {
            centerPanelState.collapse()
        }
    }

    val offsetRightDrawerX = remember {
        Animatable(
            when (drawerState) {
                0.0f -> screenWidth.value
                1.0f -> screenWidth.value - drawerWidth.value
                else -> screenWidth.value - drawerWidth.value * drawerState
            }
        )
    }

    val offsetLeftDrawerX = remember {
        Animatable(
            when (drawerState) {
                0.0f -> -screenWidth.value
                -1.0f -> screenWidth.value - drawerWidth.value
                else -> -(screenWidth.value - -(drawerWidth.value * drawerState))
            }
        )
    }

    val wasOpen = remember { mutableStateOf(false) }

    var isSwipeUp = false


    LaunchedEffect(offsetLeftDrawerX.value) {
        val drawerStateTarget = -((offsetLeftDrawerX.value / screenWidth.value) + 1)
        onDrawerStateChanged(drawerStateTarget)
    }

    LaunchedEffect(offsetRightDrawerX.value) {
        val drawerStateTarget = (offsetRightDrawerX.value / screenWidth.value)
        onDrawerStateChanged(drawerStateTarget)
    }

    Box(
        modifier = modifier.then(
            if (enableRightDrawer || enableLeftDrawer) {
                Modifier.pointerInput(Unit) {
                    var startY = 0f
                    var hasReachedThreshold = false
                    var isHandlingVertical = false
                    detectDragGestures(
                        onDragStart = { offset ->
                            startY = offset.y
                            hasReachedThreshold = false
                            isHandlingVertical = false
                        },
                        onDragEnd = {
                            if (offsetRightDrawerX.value == screenWidth.value &&
                                offsetLeftDrawerX.value == -screenWidth.value
                            ) {
                                if (isHandlingVertical) {
                                    scope.launch {
                                        if (centerPanelState.height.value > screenHeightPx * 0.1f) {
                                            centerPanelState.expand(statusBarHeight.toPx())
                                        } else {
                                            centerPanelState.collapse()
                                        }
                                    }

                                    val isCenterBoxOpened = centerPanelState.isOpen
                                    if (hasReachedThreshold && isSwipeUp) {
                                        if (isCenterBoxOpened) {
                                            scope.launch {
                                                centerPanelState.collapse()
                                            }
                                        } else {
                                            onDrawerStateChanged(0.0f)
                                            onSwipeUp()
                                        }
                                    }
                                }
                            }
                            scope.launch {
                                val visibleFraction =
                                    (screenWidth.value - offsetRightDrawerX.value) / drawerWidth.value
                                val visibleLeftDrawerFraction =
                                    -((screenWidth.value + offsetLeftDrawerX.value) / drawerWidth.value)

                                if (visibleFraction > 0) {
                                    val target = when {
                                        visibleFraction >= 0.3 && !wasOpen.value ->
                                            screenWidth.value - drawerWidth.value
                                        visibleFraction < 0.7 && wasOpen.value ->
                                            screenWidth.value
                                        else -> {
                                            if (offsetRightDrawerX.value < (screenWidth.value - drawerWidth.value / 2))
                                                screenWidth.value - drawerWidth.value
                                            else
                                                screenWidth.value
                                        }
                                    }
                                    wasOpen.value = target == screenWidth.value - drawerWidth.value
                                    offsetRightDrawerX.animateTo(target, tween(300))
                                }

                                if (visibleLeftDrawerFraction < 0) {
                                    val target = when {
                                        visibleLeftDrawerFraction <= -0.3 && !wasOpen.value ->
                                            screenWidth.value - drawerWidth.value
                                        visibleLeftDrawerFraction > -0.7 && wasOpen.value ->
                                            -screenWidth.value
                                        else -> {
                                            if (offsetLeftDrawerX.value < (-screenWidth.value - drawerWidth.value / 2))
                                                -screenWidth.value
                                            else
                                                -screenWidth.value + drawerWidth.value
                                        }
                                    }
                                    wasOpen.value = -target == screenWidth.value - drawerWidth.value
                                    offsetLeftDrawerX.animateTo(target, tween(300))
                                }
                            }
                            hasReachedThreshold = false
                        },
                        onDrag = { change, dragAmount ->
                            val isCenterPanelOpen = centerPanelState.isOpen

                            if (!isHandlingVertical && abs(dragAmount.y) > abs(dragAmount.x) * 5) {
                                if (offsetRightDrawerX.value == screenWidth.value &&
                                    offsetLeftDrawerX.value == -screenWidth.value
                                ) {
                                    isHandlingVertical = true
                                }
                            }
                            if (isHandlingVertical) {
                                if (dragAmount.y > 0 || centerPanelState.height.value > 0) {
                                    scope.launch {

                                        val newHeight =
                                            (centerPanelState.height.value + dragAmount.y).coerceIn(0f, screenHeightPx)

                                        val collapseThreshold = screenHeightPx * 0.2f
                                        val collapseWidth = screenWidthPx * 0.8f

                                        val newWidth =
                                            if (newHeight < collapseThreshold) {
                                                (centerPanelState.width.value + dragAmount.y)
                                                    .coerceIn(collapseWidth, screenWidthPx)
                                            } else { screenWidthPx }

                                        centerPanelState.snap(
                                            newHeight,
                                            newWidth
                                        )
                                    }
                                }

                                val currentDistance = startY - change.position.y
                                if (currentDistance > 200f) {
                                    hasReachedThreshold = true
                                    isSwipeUp = true
                                }
                                if (currentDistance < -100) {
                                    isSwipeUp = false
                                }
                            } else {
                                if (!isCenterPanelOpen) {
                                    scope.launch {
                                        val scaledDragAmount = dragAmount.x * 0.4f
                                        if (enableLeftDrawer &&
                                            offsetRightDrawerX.value >= screenWidth.value
                                        ) {
                                            val newLeftOffSet =
                                                offsetLeftDrawerX.value + scaledDragAmount
                                            offsetLeftDrawerX.snapTo(
                                                newLeftOffSet.coerceIn(
                                                    -screenWidth.value,
                                                    screenWidth.value - drawerWidth.value
                                                )
                                            )
                                        }
                                        if (enableRightDrawer &&
                                            offsetLeftDrawerX.value <= -screenWidth.value
                                        ) {
                                            val newOffset =
                                                offsetRightDrawerX.value + scaledDragAmount
                                            offsetRightDrawerX.snapTo(
                                                newOffset.coerceIn(
                                                    screenWidth.value - drawerWidth.value,
                                                    screenWidth.value
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        },
                    )
                }
            } else Modifier,
        ),
    ) {

        // Main content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
        ) {
            mainContent()
        }

        // Right drawer
        Box(
            modifier = Modifier
                .offset(x = offsetRightDrawerX.value.dp, y = 0.dp)
                .width(drawerWidth)
                .fillMaxHeight()
        ) {
            rightDrawer()
        }

        // Left drawer
        Box(
            modifier = Modifier
                .offset(x = offsetLeftDrawerX.value.dp, y = 0.dp)
                .width(drawerWidth)
                .fillMaxHeight()
        ) {
            leftDrawer()
        }

        // ── Center panel with animated corner radius ───────────────────────
        CenterPanelLayout(
            state = centerPanelState,
            screenHeightPx = screenHeightPx
        ) {
            centerPanel()
        }
    }
}