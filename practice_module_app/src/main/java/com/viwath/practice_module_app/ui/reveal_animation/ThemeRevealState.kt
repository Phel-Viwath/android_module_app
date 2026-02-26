package com.viwath.practice_module_app.ui.reveal_animation

import android.app.Activity
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
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
import com.viwath.practice_module_app.ui.reveal_animation.RevealDirection.Expand
import com.viwath.practice_module_app.ui.reveal_animation.RevealDirection.Shrink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.hypot

private fun View.findActivity(): Activity? {
    var c = context
    while (c is android.content.ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

private suspend fun View.snapshotSafely(): ImageBitmap? {
    val activity = findActivity()
    val window: Window? = activity?.window

    // Prefer PixelCopy on API 26+
    if (window != null) {
        val root = window.decorView
        val bitmap = createBitmap(root.width, root.height)

        val result = suspendCancellableCoroutine<Int> { cont ->
            PixelCopy.request(
                window,
                bitmap,
                { copyResult -> cont.resume(copyResult) },
                Handler(Looper.getMainLooper())
            )
        }

        if (result == PixelCopy.SUCCESS) return bitmap.asImageBitmap()
        // else fall through to drawToBitmap fallback
    }

    // Fallback (may still fail if hardware bitmaps exist)
    return try {
        drawToBitmap(config = Bitmap.Config.ARGB_8888).asImageBitmap()
    } catch (t: Throwable) {
        null // don't crash; you just won't get the overlay snapshot
    }
}

/**
 * Defines the visual behavior of the reveal animation.
 * [Expand]: The new content grows from the origin point.
 * [Shrink]: The old content shrinks toward the origin point.
 */
enum class RevealDirection { Expand, Shrink }

/**
 * Configuration class for the animation timing and curve.
 * @property durationMillis Length of the transition in milliseconds.
 * @property easing The interpolator for the animation (e.g., [FastOutSlowInEasing]).
 */
@Immutable
data class ThemeRevealSpec(
    val durationMillis: Int = 450,
    val easing: Easing = FastOutSlowInEasing,
) {
    companion object {
        val Default = ThemeRevealSpec()
    }
}


/**
 * Captures a tap and automatically converts the local coordinate to Root (screen) coordinates.
 * This is essential when the toggle button is inside a nested layout (like a TopAppBar)
 * but the animation needs to happen across the whole screen.
 */
@Composable
fun Modifier.revealClickInRoot(
    enabled: Boolean = true,
    onTapInRoot: (Offset) -> Unit
): Modifier {
    if (!enabled) return this

    var cords: LayoutCoordinates? by remember { mutableStateOf(null) }

    return this
        .onGloballyPositioned { cords = it }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val down = awaitFirstDown()
                    val c = cords
                    if (c != null) {
                        // Crucial step: Mapping local icon tap to global screen position
                        val rootOffset = c.localToRoot(down.position)
                        onTapInRoot(rootOffset)
                    } else {
                        onTapInRoot(down.position)
                    }
                }
            }
        }
}

/**
 * State holder that manages the animation progress and the bitmap snapshot.
 */
@Stable
class ThemeRevealState internal constructor(
    private val scope: CoroutineScope,
) {
    /** The center point where the circle starts or ends. */
    var origin: Offset? by mutableStateOf(null)
        private set

    /** Animation progress from 0f to 1f. */
    internal val progress = Animatable(1f)

    /** The bitmap of the UI captured right before the theme change. */
    internal var overlay: ImageBitmap? by mutableStateOf(null)

    /** Whether the circle is expanding or shrinking. */
    internal var direction: RevealDirection by mutableStateOf(Expand)

    /**
     * Triggers the theme transition.
     * @param hostView The Android View to snapshot (usually [LocalView]).
     * @param originInRoot The center point of the effect.
     * @param direction Whether to expand or shrink.
     * @param swapTheme Lambda to update your app's theme state variables.
     */
    fun reveal(
        hostView: View,
        originInRoot: Offset,
        direction: RevealDirection,
        spec: ThemeRevealSpec = ThemeRevealSpec.Default,
        swapTheme: () -> Unit,
    ) {
        scope.launch {
            // 1. Capture the current UI state as it looks RIGHT NOW
            overlay = hostView.snapshotSafely()

            this@ThemeRevealState.origin = originInRoot
            this@ThemeRevealState.direction = direction

            // 2. Set starting point for progress
            progress.snapTo(if (direction == Expand) 0f else 1f)

            // 3. Change the actual theme state (UI will recompose underneath the host)
            swapTheme()

            // 4. Animate the clipping path
            progress.animateTo(
                targetValue = if (direction == Expand) 1f else 0f,
                animationSpec = tween(spec.durationMillis, easing = spec.easing)
            )

            // 5. Clean up bitmap to prevent memory leaks
            overlay = null
        }
    }
}

/**
 * Creates and remembers the [ThemeRevealState].
 */
@Composable
fun rememberThemeRevealState(): ThemeRevealState {
    val scope = rememberCoroutineScope()
    return remember(scope) { ThemeRevealState(scope) }
}

/**
 * The container that performs the custom drawing.
 * Wrap your entire app content (Scaffold, etc.) inside this.
 */
@Composable
fun ThemeRevealHost(
    state: ThemeRevealState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var size by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .onSizeChanged { size = it }
            .drawWithContent {
                // First, draw the content (the NEW theme after swapTheme was called)
                drawContent()

                val o = state.origin ?: return@drawWithContent
                val overlay = state.overlay ?: return@drawWithContent
                if (size.width == 0 || size.height == 0) return@drawWithContent

                // Calculate max radius needed to cover the furthest corner of the screen
                val maxR = hypot(
                    maxOf(o.x, size.width - o.x),
                    maxOf(o.y, size.height - o.y),
                )

                val p = state.progress.value
                val radius = maxR * p

                val path = Path().apply { addOval(rectFromCenterRadius(o, radius)) }

                // Layer the OLD theme snapshot on top using specific clipping rules
                when (state.direction) {
                    // Show old theme snapshot EXCEPT where the circle is (revealing new theme inside circle)
                    Expand -> {
                        clipPath(path, clipOp = ClipOp.Difference) {
                            drawImage(overlay)
                        }
                    }

                    // Show old theme snapshot ONLY inside the circle (revealing new theme outside circle)
                    Shrink -> {
                        clipPath(path, clipOp = ClipOp.Intersect) {
                            drawImage(overlay)
                        }
                    }
                }
            }
    ) { content() }
}

/**
 * Helper function to create a [Rect] centered at [center] with [radius].
 */
private fun rectFromCenterRadius(center: Offset, radius: Float) =
    Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)

/**
 * Example screen containing a TopAppBar with a theme toggle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    onToggleTheme: (Offset) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    // Store the position of the "More" icon as a fallback origin
    var iconOffset by remember { mutableStateOf(Offset.Zero) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Telegram Style Reveal") },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.onGloballyPositioned {
                                // Get the center of the vertical dots icon
                                iconOffset = it.localToRoot(it.size.center.toOffset())
                            }
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }

                        DropdownMenu(showMenu, { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Toggle Theme") },
                                onClick = {
                                    showMenu = false
                                    // We use the icon's position because the menu item
                                    // itself is about to be destroyed/dismissed.
                                    onToggleTheme(iconOffset)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.LightMode, null)
                                }
                            )
                        }
                    }
                }
            )
        }
    ){ innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()
            .background(MaterialTheme.colorScheme.background)){


            val link = "https://static.vecteezy.com/system/resources/thumbnails/057/068/323/small_2x/single-fresh-red-strawberry-on-table-green-background-food-fruit-sweet-macro-juicy-plant-image-photo.jpg"

            AsyncImage(
                model = link,
                contentDescription = "Strawberry Image",
                modifier = Modifier.size(200.dp),
                // Shows while loading
                // Shows if it fails (like when internet is off)
                error = rememberVectorPainter(Icons.Default.Warning),
            )

        }
    }
}

/**
 * The entry point for the application.
 */
@Composable
fun AppRoot() {
    val reveal = rememberThemeRevealState()
    val hostView = LocalView.current
    var dark by rememberSaveable { mutableStateOf(false) }

    ThemeRevealHost(state = reveal) {
        MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
            MainScaffold(
                onToggleTheme = { tapInRoot ->
                    val goingToDark = !dark

                    // Logic: Light to Dark expands from tap, Dark to Light shrinks back
                    val dir = if (goingToDark) Expand else Shrink

                    // Custom spec to balance the speed
                    val customSpec = ThemeRevealSpec(
                        // We increase the duration for Shrink to make it feel less 'snappy'
                        durationMillis = if (dir == Shrink) 600 else 850,
                        // Use a more natural easing for the shrinking effect
                        easing = FastOutSlowInEasing
                    )

                    reveal.reveal(
                        hostView = hostView,
                        originInRoot = tapInRoot,
                        direction = dir,
                        spec = customSpec,
                        swapTheme = { dark = !dark }
                    )
                }
            )
        }
    }
}