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
    val shapeProgress: Animatable<Float, AnimationVector1D>,
    // 0 = floating panel, 1 = fills the whole screen (same shape, just bigger)
    val fullscreen: Animatable<Float, AnimationVector1D>,
    private val screenHeight: Float,
    private val screenWidth: Float,
) {
    val isOpen: Boolean get() = scale.value > 0.05f
    val isFullscreen: Boolean get() = fullscreen.value > 0.95f

    /** Open as a small floating panel. */
    suspend fun expand() {
        coroutineScope {
            launch { fullscreen.animateTo(0f, tween(300)) }
            launch { scale.animateTo(1f, spring(stiffness = Spring.StiffnessMediumLow)) }
            launch { alpha.animateTo(1f, tween(250)) }
            launch { shapeProgress.animateTo(1f, tween(400, easing = FastOutSlowInEasing)) }
        }
    }

    /** Hide the panel completely. */
    suspend fun collapse() {
        coroutineScope {
            launch { fullscreen.animateTo(0f, tween(250)) }
            launch { scale.animateTo(0f, tween(300)) }
            launch { alpha.animateTo(0f, tween(200)) }
            launch { shapeProgress.animateTo(0f, tween(300)) }
        }
    }

    /**
     * Grow to fill the screen — the shape stays exactly the same (circle stays
     * circle, star stays star), it just expands until it covers the screen.
     */
    suspend fun expandFullscreen() {
        coroutineScope {
            launch { scale.animateTo(1f, tween(350, easing = FastOutSlowInEasing)) }
            launch { alpha.animateTo(1f, tween(200)) }
            launch { shapeProgress.animateTo(1f, tween(350, easing = FastOutSlowInEasing)) }
            launch { fullscreen.animateTo(1f, tween(400, easing = FastOutSlowInEasing)) }
        }
    }

    /** Snap the floating-panel open amount during swipe-up drag. */
    suspend fun snap(s: Float) {
        val c = s.coerceIn(0f, 1f)
        scale.snapTo(c)
        alpha.snapTo(c)
        shapeProgress.snapTo(c)
    }

    /** Snap the fullscreen expansion fraction during swipe-down drag. */
    suspend fun snapFullscreen(fs: Float) {
        fullscreen.snapTo(fs.coerceIn(0f, 1f))
    }
}

@Composable
fun rememberPanelContainerState(
    screenHeight: Float,
    screenWidth: Float,
): PanelContainerState {
    val scale         = remember { Animatable(0f) }
    val alpha         = remember { Animatable(0f) }
    val shapeProgress = remember { Animatable(0f) }
    val fullscreen    = remember { Animatable(0f) }
    return remember {
        PanelContainerState(scale, alpha, shapeProgress, fullscreen, screenHeight, screenWidth)
    }
}

sealed class PanelContainerShape {
    object Circle : PanelContainerShape()
    data class RoundedRect(val cornerPercent: Float = 0.1f) : PanelContainerShape()
    data class Custom(val path: (Size) -> Path) : PanelContainerShape()
}