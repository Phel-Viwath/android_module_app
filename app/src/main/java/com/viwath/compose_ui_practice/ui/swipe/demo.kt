package com.viwath.compose_ui_practice.ui.swipe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
//  Demo screen – shows SwipeDownPanel in action
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SwipeDownPanelDemo() {
    val panelState = rememberSwipeDownPanelState()
    val scope      = rememberCoroutineScope()

    SwipeDownPanel(
        state        = panelState,
        panelHeight  = 380.dp,
        panelContent = { PanelItems() }         // ← drop your items here
    ) {
        MainScreenContent(                       // ← your real screen
            onSwipeHintTap = { scope.launch { panelState.expand() } }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Panel items  –  swap / extend these freely
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ColumnScope.PanelItems() {
    Text(
        text       = "Quick Controls",
        style      = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color      = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(4.dp))

    // Quick-toggle row
    Row(
        modifier            = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickToggle(icon = Icons.Default.Wifi,      label = "Wi-Fi")
        QuickToggle(icon = Icons.Default.Bluetooth, label = "Bluetooth")
        QuickToggle(icon = Icons.Default.AirplanemodeActive, label = "Airplane")
        QuickToggle(icon = Icons.Default.Notifications, label = "Do Not Disturb", initialOn = false)
    }

    Spacer(Modifier.height(8.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    Spacer(Modifier.height(8.dp))

    // Brightness slider
    BrightnessSlider()

    Spacer(Modifier.height(8.dp))

    // Media card
    NowPlayingCard()
}

// ─── Quick toggle tile ───────────────────────────────────────────────────────

@Composable
fun QuickToggle(
    icon:      ImageVector,
    label:     String,
    initialOn: Boolean = true,
) {
    var on by remember { mutableStateOf(initialOn) }
    val bg = if (on) MaterialTheme.colorScheme.primaryContainer
    else    MaterialTheme.colorScheme.surface

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick  = { on = !on },
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bg)
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = label,
                tint               = if (on) MaterialTheme.colorScheme.primary
                else    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ─── Brightness slider ───────────────────────────────────────────────────────

@Composable
fun BrightnessSlider() {
    var brightness by remember { mutableFloatStateOf(0.65f) }
    Row(
        modifier     = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector        = Icons.Default.BrightnessLow,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier           = Modifier.size(20.dp)
        )
        Slider(
            value         = brightness,
            onValueChange = { brightness = it },
            modifier      = Modifier.weight(1f),
        )
        Icon(
            imageVector        = Icons.Default.BrightnessHigh,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier           = Modifier.size(20.dp)
        )
    }
}

// ─── Now playing card ────────────────────────────────────────────────────────

@Composable
fun NowPlayingCard() {
    Surface(
        shape            = RoundedCornerShape(16.dp),
        color            = MaterialTheme.colorScheme.secondaryContainer,
        modifier         = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier             = Modifier.padding(12.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFFA855F7)))
                    )
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Midnight City",
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    "M83",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            Row {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.SkipPrevious, null)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.PlayArrow, null)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.SkipNext, null)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Placeholder main screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MainScreenContent(onSwipeHintTap: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Swipe down from the top", fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Text(
                "or",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onSwipeHintTap) {
                Text("Open Panel")
            }
        }
    }
}