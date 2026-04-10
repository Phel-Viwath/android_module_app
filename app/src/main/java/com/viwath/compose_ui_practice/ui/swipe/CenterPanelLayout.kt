package com.viwath.compose_ui_practice.ui.swipe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min

@Composable
fun PanelContainer(
    state: PanelContainerState,
    sizeFraction: Float = 0.75f,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    if (state.scale.value <= 0.01f && state.fullscreen.value <= 0.01f) return

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val smallSize = min(screenWidthPx, screenHeightPx) * sizeFraction

    val width = lerp(smallSize, screenWidthPx, state.fullscreen.value)
    val height = lerp(smallSize, screenHeightPx, state.fullscreen.value)

    val widthDp = with(density) { width.toDp() }
    val heightDp = with(density) { height.toDp() }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(widthDp, heightDp)
                .alpha(state.alpha.value)
                .clip(CircleShape)
                .background(Color.Gray),
            content = content
        )
    }
}
private val CircleShape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ) = Outline.Generic(
        Path().apply {
            addOval(
                androidx.compose.ui.geometry.Rect(
                    0f,
                    0f,
                    size.width,
                    size.height
                )
            )
        }
    )
}


private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}