package com.viwath.compose_ui_practice.ui.swipe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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

// ─────────────────────────────────────────────────────────────────────────────
//  Entry point — use this in setContent { }
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SwipeDownPanelDemo() {
    SwipeDownPanel(
        panelHeight  = 360.dp,
        panelContent = { PanelContent() },
    ) {
        MainScreen()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Panel content — everything that appears inside the drop-down
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ColumnScope.PanelContent() {

    // ── Quick toggle row ──────────────────────────────────────────────────────
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        QuickToggle(Icons.Default.Wifi,               "Wi-Fi",    activeColor = Color(0xFF3B82F6))
        QuickToggle(Icons.Default.Bluetooth,          "Bluetooth",activeColor = Color(0xFF8B5CF6))
        QuickToggle(Icons.Default.AirplanemodeActive, "Airplane", activeColor = Color(0xFFF59E0B), on = false)
        QuickToggle(Icons.Default.DoNotDisturb,       "Silent",   activeColor = Color(0xFFEF4444), on = false)
    }

    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
    )

    // ── Brightness ───────────────────────────────────────────────────────────
    PanelSlider(
        icon  = Icons.Default.LightMode,
        label = "Brightness",
        init  = 0.7f,
        color = Color(0xFFF59E0B),
    )

    // ── Volume ────────────────────────────────────────────────────────────────
    PanelSlider(
        icon  = Icons.Default.VolumeUp,
        label = "Volume",
        init  = 0.5f,
        color = Color(0xFF3B82F6),
    )

    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
    )

    // ── Now playing ───────────────────────────────────────────────────────────
    NowPlayingRow()
}

// ─────────────────────────────────────────────────────────────────────────────
//  Reusable panel widgets
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun QuickToggle(
    icon:        ImageVector,
    label:       String,
    activeColor: Color   = MaterialTheme.colorScheme.primary,
    on:          Boolean = true,
) {
    var enabled by remember { mutableStateOf(on) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (enabled) activeColor.copy(alpha = 0.15f)
                    else         MaterialTheme.colorScheme.surfaceVariant
                ),
        ) {
            IconButton(onClick = { enabled = !enabled }) {
                Icon(
                    imageVector        = icon,
                    contentDescription = label,
                    tint               = if (enabled) activeColor
                    else         MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                )
            }
        }
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun PanelSlider(
    icon:  ImageVector,
    label: String,
    init:  Float,
    color: Color,
) {
    var value by remember { mutableFloatStateOf(init) }

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier              = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = label,
            tint               = color,
            modifier           = Modifier.size(20.dp),
        )
        Slider(
            value         = value,
            onValueChange = { value = it },
            modifier      = Modifier.weight(1f),
            colors        = SliderDefaults.colors(
                thumbColor         = color,
                activeTrackColor   = color,
                inactiveTrackColor = color.copy(alpha = 0.2f),
            ),
        )
    }
}

@Composable
fun NowPlayingRow() {
    var playing by remember { mutableStateOf(true) }

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier              = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFFA855F7))))
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Midnight City",
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "M83 · Hurry Up, We're Dreaming",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = { playing = !playing }) {
            Icon(
                imageVector        = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (playing) "Pause" else "Play",
                tint               = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = {}) {
            Icon(Icons.Default.SkipNext, contentDescription = "Next")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Main screen — replace with your real HomeScreen
// ─────────────────────────────────────────────────────────────────────────────

private data class FeedItem(
    val title:    String,
    val subtitle: String,
    val icon:     ImageVector,
)

private val feedItems = listOf(
    FeedItem("Morning Standup",   "9:00 AM · Room 3B",           Icons.Default.Groups),
    FeedItem("Design Review",     "11:30 AM · Figma link ready", Icons.Default.DesignServices),
    FeedItem("Lunch with Sokha",  "12:30 PM · Russian Market",   Icons.Default.Restaurant),
    FeedItem("Sprint Planning",   "2:00 PM · Zoom",              Icons.Default.EventNote),
    FeedItem("Code Review: Auth", "4:00 PM · PR #142",           Icons.Default.Code),
    FeedItem("Team Retro",        "5:30 PM · Miro board",        Icons.Default.Autorenew),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Good morning 👋",
                            fontSize = 13.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text("Today's Schedule", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFFA855F7)))
                            ),
                    ) {
                        Text("V", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                // Swipe hint banner
                Surface(
                    shape    = RoundedCornerShape(16.dp),
                    color    = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier              = Modifier.padding(16.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Default.SwipeDown,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "Swipe down from the top to open quick controls",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            items(feedItems) { item ->
                FeedCard(item)
            }
        }
    }
}

@Composable
private fun FeedCard(item: FeedItem) {
    Surface(
        shape          = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        modifier       = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier              = Modifier.padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.secondary,
                    modifier           = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
        }
    }
}