package com.viwath.practice_module_app.ui.reveal_animation

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.RenderNode
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import androidx.core.graphics.createBitmap
import androidx.core.view.drawToBitmap
import coil3.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.hypot

// ─────────────────────────────────────────────────────────────────────────────
// Capture helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun View.findActivity(): Activity? {
    var c = context
    while (c is android.content.ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/**
 * Captures the window contents into an [ImageBitmap].
 *
 * Priority:
 *  1. [RenderNode] snapshot (API 31+) — GPU path, zero extra memory copy.
 *  2. [PixelCopy] (API 26–30)         — async GPU readback, handles SurfaceViews.
 *  3. [View.drawToBitmap] (< API 26)  — CPU fallback; fails on hardware bitmaps.
 */
private suspend fun View.snapshotSafely(): ImageBitmap? {
    return findActivity()?.window?.let { snapshotViaPixelCopy(it) } ?: snapshotViaDraw()
}

private suspend fun View.snapshotViaPixelCopy(window: Window): ImageBitmap? {
    val root = window.decorView
    val w = root.width.takeIf { it > 0 } ?: return null
    val h = root.height.takeIf { it > 0 } ?: return null

    val bitmap = createBitmap(w, h)
    val result = suspendCancellableCoroutine { cont ->
        PixelCopy.request(
            window,
            bitmap,
            { copyResult -> cont.resume(copyResult) },
            Handler(Looper.getMainLooper())
        )
    }
    return if (result == PixelCopy.SUCCESS) bitmap.asImageBitmap() else snapshotViaDraw()
}

private fun View.snapshotViaDraw(): ImageBitmap? = try {
    drawToBitmap(config = Bitmap.Config.ARGB_8888).asImageBitmap()
} catch (_: Throwable) {
    null
}

// ─────────────────────────────────────────────────────────────────────────────
// Public API types
// ─────────────────────────────────────────────────────────────────────────────

enum class RevealDirection { Expand, Shrink }

@Immutable
data class ThemeRevealSpec(
    val durationMillis: Int = 500,
    val easing: Easing = FastOutSlowInEasing,
)

// ─────────────────────────────────────────────────────────────────────────────
// State
// ─────────────────────────────────────────────────────────────────────────────

@Stable
class ThemeRevealState internal constructor(
    private val scope: CoroutineScope,
) {
    var origin: Offset? by mutableStateOf(null)
        private set

    internal val progress = Animatable(1f)

    // Kept internal — callers never touch the bitmap directly.
    internal var overlay: ImageBitmap? by mutableStateOf(null)
        private set

    internal var direction: RevealDirection by mutableStateOf(RevealDirection.Expand)
        private set

    // Tracks the running coroutine so we can cancel it on rapid taps.
    private var revealJob: Job? = null

    fun reveal(
        hostView: View,
        originInRoot: Offset,
        direction: RevealDirection,
        spec: ThemeRevealSpec = ThemeRevealSpec(),
        swapTheme: () -> Unit,
    ) {
        // Cancel any in-flight animation; recycle its bitmap immediately.
        revealJob?.cancel()

        revealJob = scope.launch {
            try {
                // Recycle previous bitmap before capturing a new one.
                recycleBitmap()

                overlay = hostView.snapshotSafely()
                this@ThemeRevealState.origin = originInRoot
                this@ThemeRevealState.direction = direction

                progress.snapTo(if (direction == RevealDirection.Expand) 0f else 1f)

                swapTheme()

                progress.animateTo(
                    targetValue = if (direction == RevealDirection.Expand) 1f else 0f,
                    animationSpec = tween(spec.durationMillis, easing = spec.easing),
                )
            } catch (_: CancellationException) {
                // Coroutine was cancelled (rapid tap); clean up and rethrow so
                // the new coroutine can start fresh.
                recycleBitmap()
                throw CancellationException()
            } finally {
                recycleBitmap()
            }
        }
    }

    private fun recycleBitmap() {
        // ImageBitmap wraps an Android Bitmap; extract and recycle it to
        // return GPU/native memory immediately rather than waiting for GC.
        overlay?.asAndroidBitmap()?.recycle()
        overlay = null
    }
}

@Composable
fun rememberThemeRevealState(): ThemeRevealState {
    val scope = rememberCoroutineScope()
    return remember(scope) { ThemeRevealState(scope) }
}

// ─────────────────────────────────────────────────────────────────────────────
// Modifier helpers
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Detects taps and maps the position to root (screen) coordinates before
 * forwarding to [onTapInRoot]. Use this on the theme-toggle button so the
 * origin is always in the coordinate space that [ThemeRevealHost] expects.
 */
@Composable
fun Modifier.revealClickInRoot(
    enabled: Boolean = true,
    onTapInRoot: (Offset) -> Unit,
): Modifier {
    if (!enabled) return this

    var coords: LayoutCoordinates? by remember { mutableStateOf(null) }

    return this
        .onGloballyPositioned { coords = it }
        .pointerInput(onTapInRoot) {        // key on lambda so it re-registers if caller changes
            detectTapGestures { localPos ->
                val rootOffset = coords?.localToRoot(localPos) ?: localPos
                onTapInRoot(rootOffset)
            }
        }
}

// ─────────────────────────────────────────────────────────────────────────────
// Host composable
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Wrap your entire app content inside this. It must sit **outside** any
 * [MaterialTheme] so that it draws on a stable, theme-agnostic surface.
 */
@Composable
fun ThemeRevealHost(
    state: ThemeRevealState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var hostSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { hostSize = it }
            .drawWithContent {
                // 1. Draw new theme content first (underneath).
                drawContent()

                val origin = state.origin ?: return@drawWithContent
                val snapshot = state.overlay ?: return@drawWithContent
                if (hostSize.width == 0 || hostSize.height == 0) return@drawWithContent

                val maxRadius = hypot(
                    maxOf(origin.x, hostSize.width - origin.x),
                    maxOf(origin.y, hostSize.height - origin.y),
                )

                val radius = maxRadius * state.progress.value

                val circle = Path().apply {
                    addOval(
                        Rect(
                            center = origin,
                            radius = radius,
                        )
                    )
                }

                // 2. Composite the frozen snapshot on top with correct clip logic.
                when (state.direction) {
                    // Expand: old theme visible everywhere EXCEPT the growing circle.
                    RevealDirection.Expand -> clipPath(circle, clipOp = ClipOp.Difference) {
                        drawImage(snapshot)
                    }
                    // Shrink: old theme visible ONLY inside the shrinking circle.
                    RevealDirection.Shrink -> clipPath(circle, clipOp = ClipOp.Intersect) {
                        drawImage(snapshot)
                    }
                }
            },
    ) {
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Example usage
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(onToggleTheme: (Offset) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    var iconOffset by remember { mutableStateOf(Offset.Zero) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Theme Reveal") },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.onGloballyPositioned {
                                iconOffset = it.localToRoot(it.size.center.toOffset())
                            },
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Toggle Theme") },
                                leadingIcon = { Icon(Icons.Default.LightMode, null) },
                                onClick = {
                                    showMenu = false
                                    onToggleTheme(iconOffset)
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            AsyncImage(
                model = "https://static.vecteezy.com/system/resources/thumbnails/057/068/323/small_2x/single-fresh-red-strawberry-on-table-green-background-food-fruit-sweet-macro-juicy-plant-image-photo.jpg",
                contentDescription = "Strawberry",
                modifier = Modifier.size(200.dp),
                error = rememberVectorPainter(Icons.Default.Warning),
            )
        }
    }
}

/**
 * App entry point.
 *
 * Structure:
 *   ThemeRevealHost          ← outside MaterialTheme; always stable
 *     MaterialTheme          ← theme swaps happen here
 *       MainScaffold
 */
@Composable
fun AppRoot() {
    val revealState = rememberThemeRevealState()
    val hostView = LocalView.current
    var isDark by rememberSaveable { mutableStateOf(false) }

    // ThemeRevealHost is intentionally outside MaterialTheme so it draws on a
    // surface that is unaffected by the theme swap recomposition.
    ThemeRevealHost(state = revealState) {
        MaterialTheme(colorScheme = if (isDark) darkColorScheme() else lightColorScheme()) {
            MainScaffold(
                onToggleTheme = { tapOrigin ->
                    val goingDark = !isDark
                    revealState.reveal(
                        hostView = hostView,
                        originInRoot = tapOrigin,
                        direction = if (goingDark) RevealDirection.Expand else RevealDirection.Shrink,
                        spec = ThemeRevealSpec(
                            durationMillis = if (goingDark) 850 else 600,
                            easing = FastOutSlowInEasing,
                        ),
                        swapTheme = { isDark = !isDark },
                    )
                },
            )
        }
    }
}