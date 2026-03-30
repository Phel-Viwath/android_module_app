package com.viwath.compose_ui_practice.ui.swipe

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
//  State holder  –  hoist this in your screen / ViewModel
// ─────────────────────────────────────────────────────────────────────────────

class SwipeDownPanelState {
    // 0f = fully hidden (above screen), 1f = fully expanded
    var progress by mutableFloatStateOf(0f)
        internal set

    val isVisible: Boolean get() = progress > 0f

    // expose for your ViewModel / LaunchedEffect if needed
    suspend fun expand(spec: AnimationSpec<Float> = panelSpring()) {
        animate(progress, 1f, animationSpec = spec) { v, _ -> progress = v }
    }

    suspend fun collapse(spec: AnimationSpec<Float> = panelSpring()) {
        animate(progress, 0f, animationSpec = spec) { v, _ -> progress = v }
    }
}

@Composable
fun rememberSwipeDownPanelState() = remember { SwipeDownPanelState() }

private fun panelSpring() = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness    = Spring.StiffnessMedium
)

// ─────────────────────────────────────────────────────────────────────────────
//  SwipeDownPanel  –  wrap your screen content with this
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A container that slides in from the top when the user swipes down on [screenContent],
 * exactly like a device quick-settings / notification shade.
 *
 * Usage:
 * ```
 * val panelState = rememberSwipeDownPanelState()
 *
 * SwipeDownPanel(
 *     state = panelState,
 *     panelContent = {
 *         // anything you want inside the drop-down
 *         QuickTile(icon = Icons.Default.Wifi, label = "Wi-Fi")
 *         QuickTile(icon = Icons.Default.Bluetooth, label = "Bluetooth")
 *     }
 * ) {
 *     // your normal screen UI here
 *     MyMainScreen()
 * }
 * ```
 *
 * @param state          Hoisted [SwipeDownPanelState]; use [rememberSwipeDownPanelState].
 * @param panelHeight    Maximum height of the revealed panel (default 340 dp).
 * @param swipeThreshold Drag distance needed to trigger auto-expand (default 80 dp).
 * @param dimColor       Colour of the scrim behind the panel when open.
 * @param panelContent   Content to render inside the panel.
 * @param screenContent  The main screen that receives the swipe gesture.
 */
@Composable
fun SwipeDownPanel(
    state:         SwipeDownPanelState = rememberSwipeDownPanelState(),
    panelHeight:   Dp    = 340.dp,
    dimColor:      Color = Color.Black.copy(alpha = 0.45f),
    panelContent:  @Composable ColumnScope.() -> Unit,
    screenContent: @Composable () -> Unit,
) {
    val density       = LocalDensity.current
    val panelHeightPx = with(density) { panelHeight.toPx() }
    val scope         = rememberCoroutineScope()

    // How many px the panel has travelled downward
    val offsetY = (state.progress * panelHeightPx)

    Box(modifier = Modifier.fillMaxSize()) {

        // ── 1. Main screen content + swipe detector ───────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(state) {
                    var dragAccum = 0f
                    detectVerticalDragGestures(
                        onDragStart = { dragAccum = 0f },
                        onDragEnd   = {
                            // Always auto-hide when finger lifts
                            scope.launch { state.collapse() }
                            dragAccum = 0f
                        },
                        onDragCancel = {
                            scope.launch { state.collapse() }
                            dragAccum = 0f
                        },
                        onVerticalDrag = { change, delta ->
                            change.consume()
                            // Only respond when swiping DOWN from near the top
                            // (first 72 dp) OR when panel is already open
                            val fromTop = change.position.y - delta < with(density) { 72.dp.toPx() }
                            if (fromTop || state.isVisible) {
                                dragAccum += delta
                                val newProgress = (state.progress + delta / panelHeightPx)
                                    .coerceIn(0f, 1f)
                                state.progress = newProgress
                            }
                        }
                    )
                }
        ) {
            screenContent()
        }

        // ── 2. Scrim (dim only, no click — swipe-only interaction) ───────
        if (state.isVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(dimColor.copy(alpha = dimColor.alpha * state.progress))
            )
        }

        // ── 3. The sliding panel ──────────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(panelHeight)
                .offset(y = (-panelHeight + offsetY.toDp(density)))
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                // Allow dragging the panel itself upward to close
                .pointerInput(state) {
                    var dragAccum = 0f
                    detectVerticalDragGestures(
                        onDragStart  = { dragAccum = 0f },
                        onDragEnd    = {
                            // Always auto-hide when finger lifts
                            scope.launch { state.collapse() }
                            dragAccum = 0f
                        },
                        onDragCancel = {
                            scope.launch { state.collapse() }
                            dragAccum = 0f
                        },
                        onVerticalDrag = { change, delta ->
                            change.consume()
                            dragAccum += delta
                            val newProgress = (state.progress + delta / panelHeightPx)
                                .coerceIn(0f, 1f)
                            state.progress = newProgress
                        }
                    )
                },
            color      = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 16.dp,
            tonalElevation  = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Drag handle indicator
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(4.dp))

                // Caller-supplied items
                panelContent()
            }
        }
    }
}

// Helper: convert px Float → Dp without needing a Composable receiver
private fun Float.toDp(density: Density): Dp =
    with(density) { this@toDp.toDp() }