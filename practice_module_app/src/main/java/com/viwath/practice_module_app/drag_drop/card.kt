package com.viwath.practice_module_app.drag_drop

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viwath.practice_module_app.drag_drop.GridPops.DUMMY

// ─────────────────────────────────────────────────────────────────────────────
// Pinned card
// Long-press is handled by the parent grid's pointerInput, so no combinedClickable
// needed here — just the visual representation.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PinnedWidgetCard(
    widget: ActionWidget,
    isDragging: Boolean,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue   = if (isDragging) 1.07f else 1f,
        animationSpec = tween(150),
        label         = "pinned_scale_${widget.action}",
    )
    val isDummy = widget.action == DUMMY

    Column(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .aspectRatio(0.9f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isDragging) Color.White.copy(alpha = 0.18f)
                else            Color.White.copy(alpha = 0.08f)
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isDummy) {
            // Empty placeholder slot — visually subtle
            Text("＋", fontSize = 22.sp, color = Color.White.copy(alpha = 0.2f))
        } else {
            Text(
                text     = widget.emoji,
                fontSize = 28.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text      = widget.label,
                fontSize  = 10.sp,
                color     = Color.White,
                textAlign = TextAlign.Center,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis,
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// More card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MoreWidgetCard(
    widget: ActionWidget,
    isDragging: Boolean,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue   = if (isDragging) 1.07f else 1f,
        animationSpec = tween(150),
        label         = "more_scale_${widget.action}",
    )

    Column(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isDragging) Color.White.copy(alpha = 0.18f)
                else            Color.White.copy(alpha = 0.07f)
            )
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(widget.emoji, fontSize = 22.sp)
        Spacer(Modifier.height(5.dp))
        Text(
            text      = widget.label,
            fontSize  = 9.sp,
            color     = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
        )
    }
}