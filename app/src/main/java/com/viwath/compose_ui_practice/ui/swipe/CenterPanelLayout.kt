// PanelContainer.kt
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
import androidx.compose.ui.draw.scale
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
    sizeFraction: Float = 0.75f,          // fraction of screen to occupy
    screenWidthPx: Float,
    screenHeightPx: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    if (state.scale.value <= 0.01f) return

    val density = LocalDensity.current
    val containerSize = min(screenWidthPx, screenHeightPx) * sizeFraction
    val containerSizeDp = with(density) { containerSize.toDp() }

    val resolvedShape = resolveShape(shape, state.shapeProgress.value)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(containerSizeDp)
                .scale(state.scale.value)
                .alpha(state.alpha.value)
                .clip(resolvedShape)
                .background(Color.Gray),
            content = content
        )
    }
}

// ── Shape resolver ────────────────────────────────────────────────────────────

private fun resolveShape(shape: PanelContainerShape, progress: Float): Shape =
    when (shape) {
        is PanelContainerShape.Circle -> CircleShape
        is PanelContainerShape.Stadium -> StadiumShape
        is PanelContainerShape.Diamond -> DiamondShape
        is PanelContainerShape.RoundedRect -> AnimatedRoundedShape(
            cornerFraction = shape.cornerPercent * progress
        )
        is PanelContainerShape.Custom -> {
            CustomPathShape(shape.path)
        }
    }

// ── Shape implementations ─────────────────────────────────────────────────────

private val CircleShape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Generic(Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
        })
}

private val StadiumShape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Rounded(
            androidx.compose.ui.geometry.RoundRect(
                0f, 0f, size.width, size.height,
                radiusX = size.height / 2f,
                radiusY = size.height / 2f
            )
        )
}

private val DiamondShape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Generic(Path().apply {
            moveTo(size.width / 2f, 0f)
            lineTo(size.width, size.height / 2f)
            lineTo(size.width / 2f, size.height)
            lineTo(0f, size.height / 2f)
            close()
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