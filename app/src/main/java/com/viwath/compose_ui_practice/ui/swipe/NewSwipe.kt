package com.viwath.compose_ui_practice.ui.swipe

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
    val screenWidth  = LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val drawerWidth: Dp = screenWidth

    val density = LocalDensity.current
    val screenHeightPx = with(density) { screenHeight.toPx() }
    val screenWidthPx  = with(density) { screenWidth.toPx() }

    val panelState = rememberPanelContainerState()

    LaunchedEffect(panelState.isOpen) {
        onCenterPanelStateChanged(panelState.isOpen)
    }

    // Back: if fullscreen → shrink back to panel; if panel open → collapse
    BackHandler(enabled = panelState.isOpen) {
        scope.launch {
            if (panelState.isFullscreen) panelState.expand()
            else panelState.collapse()
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
                0.0f  -> -screenWidth.value
                -1.0f -> screenWidth.value - drawerWidth.value
                else  -> -(screenWidth.value - -(drawerWidth.value * drawerState))
            }
        )
    }

    val wasOpen = remember { mutableStateOf(false) }

    LaunchedEffect(offsetLeftDrawerX.value) {
        onDrawerStateChanged(-((offsetLeftDrawerX.value / screenWidth.value) + 1))
    }
    LaunchedEffect(offsetRightDrawerX.value) {
        onDrawerStateChanged(offsetRightDrawerX.value / screenWidth.value)
    }

    Box(
        modifier = modifier.then(
            if (enableRightDrawer || enableLeftDrawer) {
                Modifier.pointerInput(Unit) {
                    var startY = 0f
                    var isHandlingVertical = false
                    // true = finger moving up, false = finger moving down
                    var directionUp = false

                    detectDragGestures(
                        onDragStart = { offset ->
                            startY = offset.y
                            isHandlingVertical = false
                            directionUp = false
                        },

                        onDragEnd = {
                            val bothDrawersClosed =
                                offsetRightDrawerX.value == screenWidth.value &&
                                        offsetLeftDrawerX.value  == -screenWidth.value

                            if (bothDrawersClosed && isHandlingVertical) {
                                scope.launch {
                                    when {
                                        // Was swiping UP → settle the floating panel
                                        directionUp -> {
                                            if (panelState.scale.value > 0.3f) panelState.expand()
                                            else panelState.collapse()
                                        }
                                        // Was swiping DOWN → settle fullscreen or collapse
                                        else -> {
                                            when {
                                                panelState.fullscreen.value > 0.4f -> panelState.expandFullscreen()
                                                panelState.scale.value > 0.3f      -> panelState.expand()
                                                else                               -> panelState.collapse()
                                            }
                                        }
                                    }
                                }
                            }

                            // Settle horizontal drawers
                            scope.launch {
                                val visR = (screenWidth.value - offsetRightDrawerX.value) / drawerWidth.value
                                val visL = -((screenWidth.value + offsetLeftDrawerX.value) / drawerWidth.value)

                                if (visR > 0) {
                                    val target = when {
                                        visR >= 0.3 && !wasOpen.value -> screenWidth.value - drawerWidth.value
                                        visR < 0.7  &&  wasOpen.value -> screenWidth.value
                                        else -> if (offsetRightDrawerX.value < screenWidth.value - drawerWidth.value / 2)
                                            screenWidth.value - drawerWidth.value else screenWidth.value
                                    }
                                    wasOpen.value = target == screenWidth.value - drawerWidth.value
                                    offsetRightDrawerX.animateTo(target, tween(300))
                                }
                                if (visL < 0) {
                                    val target = when {
                                        visL <= -0.3 && !wasOpen.value -> screenWidth.value - drawerWidth.value
                                        visL > -0.7  &&  wasOpen.value -> -screenWidth.value
                                        else -> if (offsetLeftDrawerX.value < -screenWidth.value - drawerWidth.value / 2)
                                            -screenWidth.value else -screenWidth.value + drawerWidth.value
                                    }
                                    wasOpen.value = -target == screenWidth.value - drawerWidth.value
                                    offsetLeftDrawerX.animateTo(target, tween(300))
                                }
                            }
                        },

                        onDrag = { change, dragAmount ->
                            val bothDrawersClosed =
                                offsetRightDrawerX.value == screenWidth.value &&
                                        offsetLeftDrawerX.value  == -screenWidth.value

                            // Detect if this gesture is primarily vertical
                            if (!isHandlingVertical && abs(dragAmount.y) > abs(dragAmount.x) * 5) {
                                if (bothDrawersClosed) isHandlingVertical = true
                            }

                            if (isHandlingVertical) {
                                directionUp = dragAmount.y < 0

                                if (directionUp) {
                                    // ── Swipe UP: open the floating panel ──────────────────
                                    // Only drive this when not already fullscreen
                                    if (!panelState.isFullscreen) {
                                        val progress = (panelState.scale.value - dragAmount.y / screenHeightPx)
                                            .coerceIn(0f, 1f)
                                        scope.launch { panelState.snap(progress) }

                                        // If dragged far enough up → trigger swipe-up sheet
                                        val distanceUp = startY - change.position.y
                                        if (distanceUp > 200f && !panelState.isOpen) {
                                            onDrawerStateChanged(0f)
                                            onSwipeUp()
                                        }
                                    }
                                } else {
                                    // ── Swipe DOWN: expand to fullscreen ───────────────────
                                    // Only activates when the floating panel is already open
                                    if (panelState.isOpen) {
                                        val fs = (panelState.fullscreen.value + dragAmount.y / screenHeightPx)
                                            .coerceIn(0f, 1f)
                                        scope.launch { panelState.snapFullscreen(fs) }
                                    }
                                }
                            } else {
                                // ── Horizontal: open left / right drawers ──────────────────
                                if (!panelState.isOpen) {
                                    scope.launch {
                                        val scaledDrag = dragAmount.x * 0.4f
                                        if (enableLeftDrawer && offsetRightDrawerX.value >= screenWidth.value) {
                                            offsetLeftDrawerX.snapTo(
                                                (offsetLeftDrawerX.value + scaledDrag)
                                                    .coerceIn(-screenWidth.value, screenWidth.value - drawerWidth.value)
                                            )
                                        }
                                        if (enableRightDrawer && offsetLeftDrawerX.value <= -screenWidth.value) {
                                            offsetRightDrawerX.snapTo(
                                                (offsetRightDrawerX.value + scaledDrag)
                                                    .coerceIn(screenWidth.value - drawerWidth.value, screenWidth.value)
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
        Box(modifier = Modifier.fillMaxSize().align(Alignment.Center)) {
            mainContent()
        }

        // Right drawer
        Box(
            modifier = Modifier
                .offset(x = offsetRightDrawerX.value.dp)
                .width(drawerWidth)
                .fillMaxHeight()
        ) { rightDrawer() }

        // Left drawer
        Box(
            modifier = Modifier
                .offset(x = offsetLeftDrawerX.value.dp)
                .width(drawerWidth)
                .fillMaxHeight()
        ) { leftDrawer() }

        // Center panel — shape is always preserved, fullscreen just grows it
        PanelContainer(
            state = panelState,

            sizeFraction = 0.75f,
        ) {
            centerPanel()
        }
    }
}