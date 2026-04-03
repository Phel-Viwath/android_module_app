package com.viwath.compose_ui_practice.ui.swipe

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.animation.core.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path


@Stable
class PanelContainerState(
    val scale: Animatable<Float, AnimationVector1D>,
    val alpha: Animatable<Float, AnimationVector1D>,
    val shapeProgress: Animatable<Float, AnimationVector1D>, // 0 = start shape, 1 = end shape
    private val screenHeight: Float,
    private val screenWidth: Float,
) {
    val isOpen: Boolean get() = scale.value > 0.05f

    suspend fun expand() {
        coroutineScope {
            launch { scale.animateTo(1f, spring(stiffness = Spring.StiffnessMediumLow)) }
            launch { alpha.animateTo(1f, tween(250)) }
            launch { shapeProgress.animateTo(1f, tween(400, easing = FastOutSlowInEasing)) }
        }
    }

    suspend fun collapse() {
        scale.animateTo(0f, tween(300))
        alpha.animateTo(0f, tween(200))
        shapeProgress.animateTo(0f, tween(300))
    }

    suspend fun snap(s: Float) {
        scale.snapTo(s.coerceIn(0f, 1f))
        alpha.snapTo(s.coerceIn(0f, 1f))
    }
}

@Composable
fun rememberPanelContainerState(
    screenHeight: Float,
    screenWidth: Float,
): PanelContainerState {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    val shapeProgress = remember { Animatable(0f) }
    return remember {
        PanelContainerState(scale, alpha, shapeProgress, screenHeight, screenWidth)
    }
}

// PanelContainerShape.kt
sealed class PanelContainerShape {
    object Circle : PanelContainerShape()
    data class RoundedRect(val cornerPercent: Float = 0.1f) : PanelContainerShape()
    object Diamond : PanelContainerShape()
    object Stadium : PanelContainerShape()          // pill
    data class Custom(val path: (Size) -> Path) : PanelContainerShape()
}