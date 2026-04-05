package com.viwath.compose_ui_practice.ui.swipe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.min

@Composable
fun PanelContainer(
    state: PanelContainerState,
    shape: PanelContainerShape = PanelContainerShape.RoundedRect(),
    sizeFraction: Float = 0.75f,
    screenWidthPx: Float,
    screenHeightPx: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    if (state.scale.value <= 0.01f && state.fullscreen.value <= 0.01f) return

    val density = LocalDensity.current
    val fs = state.fullscreen.value   // 0 → floating panel, 1 → full screen

    // The "small" container is a square based on the shorter screen dimension.
    // As fullscreen goes 0→1 we lerp width and height toward the full screen size.
    val smallSize = min(screenWidthPx, screenHeightPx) * sizeFraction
    val lerpedW = lerp(smallSize, screenWidthPx,  fs)
    val lerpedH = lerp(smallSize, screenHeightPx, fs)

    val widthDp  = with(density) { lerpedW.toDp() }
    val heightDp = with(density) { lerpedH.toDp() }

    // The shape never changes — circle stays circle, star stays star.
    // Only the container size grows.
    val resolvedShape = resolveShape(shape, state.shapeProgress.value)

    // While in fullscreen mode the scale animation is irrelevant (size already = screen).
    val effectiveAlpha = state.alpha.value.coerceAtLeast(fs)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = widthDp, height = heightDp)
                .alpha(effectiveAlpha)
                .clip(resolvedShape)
                .background(Color.Gray),
            content = content
        )
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

private fun resolveShape(shape: PanelContainerShape, progress: Float): Shape =
    when (shape) {
        is PanelContainerShape.Circle      -> CircleShape
        is PanelContainerShape.RoundedRect -> AnimatedRoundedShape(shape.cornerPercent * progress)
        is PanelContainerShape.Custom      -> CustomPathShape(shape.path)
    }

// ---------------------------------------------------------------------------
// Shape implementations — none of these change on fullscreen
// ---------------------------------------------------------------------------

private val CircleShape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Generic(Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
        })
}


private class AnimatedRoundedShape(private val cornerFraction: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Rounded(
            androidx.compose.ui.geometry.RoundRect(
                0f, 0f, size.width, size.height,
                radiusX = size.width * cornerFraction,
                radiusY = size.height * cornerFraction
            )
        )
}

private class CustomPathShape(private val pathBuilder: (Size) -> Path) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Generic(pathBuilder(size))
}