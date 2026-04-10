package com.viwath.compose_ui_practice.ui.swipe

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Stable
class PanelContainerState(
    val scale: Animatable<Float, AnimationVector1D>,
    val alpha: Animatable<Float, AnimationVector1D>,
    val fullscreen: Animatable<Float, AnimationVector1D>,
) {

    val isOpen: Boolean get() = scale.value > 0.05f
    val isFullscreen: Boolean get() = fullscreen.value > 0.95f

    suspend fun expand() {
        coroutineScope {
            launch { scale.animateTo(1f, spring()) }
            launch { alpha.animateTo(1f, tween(250)) }
            launch { fullscreen.animateTo(0f) }
        }
    }

    suspend fun collapse() {
        coroutineScope {
            launch { scale.animateTo(0f, tween(300)) }
            launch { alpha.animateTo(0f, tween(200)) }
            launch { fullscreen.animateTo(0f) }
        }
    }

    suspend fun expandFullscreen() {
        coroutineScope {
            launch { fullscreen.animateTo(1f, tween(400)) }
            launch { scale.animateTo(1f) }
            launch { alpha.animateTo(1f) }
        }
    }

    suspend fun snap(scaleValue: Float) {
        val c = scaleValue.coerceIn(0f, 1f)
        scale.snapTo(c)
        alpha.snapTo(c)
    }

    suspend fun snapFullscreen(fs: Float) {
        fullscreen.snapTo(fs.coerceIn(0f, 1f))
    }
}

@Composable
fun rememberPanelContainerState(): PanelContainerState {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    val fullscreen = remember { Animatable(0f) }

    return remember {
        PanelContainerState(
            scale,
            alpha,
            fullscreen
        )
    }
}

sealed class PanelContainerShape {
    object Circle : PanelContainerShape()
    data class RoundedRect(val cornerPercent: Float = 0.1f) : PanelContainerShape()
    data class Custom(val path: (Size) -> Path) : PanelContainerShape()
}