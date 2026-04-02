package com.viwath.compose_ui_practice.ui.swipe

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

@Stable
class CenterPanelState(
    val height: Animatable<Float, *>,
    val width: Animatable<Float, *>,
    private val screenHeight: Float,
    private val screenWidth: Float,
    private val collapseWidth: Float
) {

    val isOpen: Boolean
        get() = height.value > screenHeight * 0.05f

    suspend fun expand(statusBar: Float) {
        height.animateTo(
            screenHeight + statusBar,
            spring(stiffness = Spring.StiffnessLow)
        )
        width.animateTo(
            screenWidth,
            spring(stiffness = Spring.StiffnessLow)
        )
    }

    suspend fun collapse() {
        height.animateTo(0f, tween(300))
        width.animateTo(collapseWidth, tween(300))
    }

    suspend fun snap(newHeight: Float, newWidth: Float) {
        height.snapTo(newHeight)
        width.snapTo(newWidth)
    }
}


@Composable
fun rememberCenterPanelState(
    screenHeight: Float,
    screenWidth: Float
): CenterPanelState {

    val height = remember { Animatable(0f) }
    val width = remember { Animatable(0f) }

    val collapseWidth = screenWidth * 0.6f

    return remember {
        CenterPanelState(
            height = height,
            width = width,
            screenHeight = screenHeight,
            screenWidth = screenWidth,
            collapseWidth = collapseWidth
        )
    }
}