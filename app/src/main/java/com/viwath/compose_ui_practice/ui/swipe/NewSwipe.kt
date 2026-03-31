package com.viwath.compose_ui_practice.ui.swipe

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
    mainContent: @Composable BoxScope.() -> Unit,
    rightDrawer: @Composable BoxScope.() -> Unit,
    leftDrawer: @Composable BoxScope.() -> Unit,
    centerPanel: @Composable BoxScope.() -> Unit = {},
    onSwipeUp: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val drawerWidth: Dp = LocalConfiguration.current.screenWidthDp.dp // Default drawer width to full screen
    val density = LocalDensity.current

    val centerBoxHeight = remember { Animatable(0f) }

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
                -1.0f -> 0f
                else -> -screenWidth.value + (drawerWidth.value * abs(drawerState))
            }
        )
    }

    val wasOpen = remember { mutableStateOf(false) }

    // This is to control the drawer state when the drawerState parameter changes from outside instead of scrolling
    LaunchedEffect(drawerState) {
        wasOpen.value = drawerState == 1.0f || drawerState == -1.0f
        if (drawerState == 0.0f) {
            offsetRightDrawerX.animateTo(
                screenWidth.value,
                tween(300),
            )
            offsetLeftDrawerX.animateTo(
                -screenWidth.value,
                tween(300),
            )
        }

        if (drawerState > 0) {
            offsetRightDrawerX.animateTo(
                when (drawerState) {
                    1.0f -> screenWidth.value - drawerWidth.value
                    else -> screenWidth.value - (drawerWidth.value * drawerState)
                },
                tween(300),
            )
        }
        if (drawerState < 0) {
            offsetLeftDrawerX.animateTo(
                when (drawerState) {
                    -1.0f -> 0f
                    else -> -screenWidth.value + (drawerWidth.value * abs(drawerState))
                },
                tween(300),
            )
        }
    }

    LaunchedEffect(offsetLeftDrawerX.value) {
        val drawerStateTarget = -((screenWidth.value + offsetLeftDrawerX.value) / drawerWidth.value)
        onDrawerStateChanged(drawerStateTarget)
    }

    LaunchedEffect(offsetRightDrawerX.value) {
        val drawerStateTarget = (screenWidth.value - offsetRightDrawerX.value) / drawerWidth.value
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

                                        val screenHeightPx = with(density) {
                                            screenHeight.toPx()
                                        }

                                        if (centerBoxHeight.value > screenHeightPx * 0.2f) {
                                            centerBoxHeight.animateTo(
                                                screenHeightPx,
                                                spring(stiffness = Spring.StiffnessLow)
                                            )
                                        } else {
                                            centerBoxHeight.animateTo(0f, tween(300))
                                        }

                                        if (hasReachedThreshold) {
                                            onDrawerStateChanged(0.0f)
                                            onSwipeUp()
                                        }
                                    }
                                }
                            }
                            scope.launch {
                                // Compute the percentage of the drawer that is visible
                                val visibleFraction =
                                    (screenWidth.value - offsetRightDrawerX.value) / drawerWidth.value
                                val visibleLeftDrawerFraction =
                                    (screenWidth.value + offsetLeftDrawerX.value) / drawerWidth.value

                                if (visibleFraction > 0) {
                                    val target = when {
                                        visibleFraction >= 0.3 && !wasOpen.value -> {
                                            // If 30% or more of the drawer is visible, snap it open
                                            screenWidth.value - drawerWidth.value
                                        }

                                        visibleFraction < 0.7 && wasOpen.value -> {
                                            // If less than 30% of the drawer is visible, snap it closed
                                            screenWidth.value
                                        }

                                        else -> {
                                            // Default behavior, should not be needed but added for robustness
                                            if (offsetRightDrawerX.value < (screenWidth.value - drawerWidth.value / 2)) {
                                                screenWidth.value - drawerWidth.value // More towards open
                                            } else {
                                                screenWidth.value // More towards close
                                            }
                                        }
                                    }
                                    wasOpen.value = target == screenWidth.value - drawerWidth.value
                                    offsetRightDrawerX.animateTo(target, tween(300))
                                }

                                if (visibleLeftDrawerFraction > 0) {
                                    val target = when {
                                        visibleLeftDrawerFraction >= 0.3 && !wasOpen.value -> {
                                            // If 30% or more of the drawer is visible, snap it open
                                            0f
                                        }

                                        visibleLeftDrawerFraction < 0.7 && wasOpen.value -> {
                                            // If less than 30% of the drawer is visible, snap it closed
                                            -screenWidth.value
                                        }

                                        else -> {
                                            // Default behavior, should not be needed but added for robustness
                                            if (offsetLeftDrawerX.value > (-screenWidth.value + drawerWidth.value / 2)) {
                                                0f // More towards open
                                            } else {
                                                -screenWidth.value // More towards close
                                            }
                                        }
                                    }
                                    wasOpen.value = target == 0f
                                    offsetLeftDrawerX.animateTo(target, tween(300))
                                }
                            }
                            hasReachedThreshold = false
                        },
                        onDrag = { change, dragAmount ->
                            // Determine drag direction on first movement
                            if (!isHandlingVertical && abs(dragAmount.y) > abs(dragAmount.x) * 5) {
                                if (offsetRightDrawerX.value == screenWidth.value &&
                                    offsetLeftDrawerX.value == -screenWidth.value
                                ) {
                                    isHandlingVertical = true
                                }
                            }

                            if (isHandlingVertical) {

                                val screenHeightPx = with(density) { screenHeight.toPx() }

                                if (dragAmount.y > 0 || centerBoxHeight.value > 0) {
                                    scope.launch {
                                        val newHeight =
                                            (centerBoxHeight.value + dragAmount.y)
                                                .coerceIn(0f, screenHeightPx)

                                        centerBoxHeight.snapTo(newHeight)
                                    }
                                }

                                val currentDistance = startY - change.position.y
                                if (currentDistance > 200f) {
                                    hasReachedThreshold = true
                                }

                            } else {
                                scope.launch {
                                    val scaledDragAmount = dragAmount.x * 0.4f
                                    if (enableLeftDrawer && offsetRightDrawerX.value >= screenWidth.value) {
                                        val newLeftOffSet =
                                            offsetLeftDrawerX.value + scaledDragAmount
                                        offsetLeftDrawerX.snapTo(
                                            newLeftOffSet.coerceIn(
                                                -screenWidth.value,
                                                0f
                                            )
                                        )
                                    }
                                    if (enableRightDrawer && offsetLeftDrawerX.value <= -screenWidth.value) {
                                        val newOffset = offsetRightDrawerX.value + scaledDragAmount
                                        offsetRightDrawerX.snapTo(
                                            newOffset.coerceIn(
                                                screenWidth.value - drawerWidth.value,
                                                screenWidth.value
                                            )
                                        )
                                    }
                                }
                            }
                        },
                    )
                }
            } else Modifier,
        ),
    ) {
        // Main content provided by parameter
        Box(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
        ) {
            mainContent()
        }

        // Drawer provided by parameter
        Box(
            modifier = Modifier
                .offset(x = offsetRightDrawerX.value.dp, y = 0.dp)
                .width(drawerWidth)
                .fillMaxHeight()
        ) {
            rightDrawer()
        }
        // Drawer provided by parameter
        Box(
            modifier = Modifier
                .offset(x = offsetLeftDrawerX.value.dp, y = 0.dp)
                .width(drawerWidth)
                .fillMaxHeight()
        ) {
            leftDrawer()
        }

        val centerBoxHeightDp = with(density) {
            centerBoxHeight.value.toDp()
        }

        if (centerBoxHeight.value > 0f) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(screenWidth)
                        .height(centerBoxHeightDp)
                ) {
                    centerPanel()
                }
            }
        }

    }
}