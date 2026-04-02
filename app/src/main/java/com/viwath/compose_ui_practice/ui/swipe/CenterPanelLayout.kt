package com.viwath.compose_ui_practice.ui.swipe

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp

@Composable
fun CenterPanelLayout(
    state: CenterPanelState,
    screenHeightPx: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {

    val density = LocalDensity.current

    if (state.height.value > 0f) {

        val progress =
            (state.height.value / screenHeightPx).coerceIn(0f, 1f)

        val cornerRadius = lerp(24.dp, 0.dp, progress)

        val heightDp = with(density) { state.height.value.toDp() }
        val widthDp = with(density) { state.width.value.toDp() }

        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(widthDp)
                    .height(heightDp)
                    .clip(RoundedCornerShape(cornerRadius))
            ) {
                content()
            }
        }
    }
}