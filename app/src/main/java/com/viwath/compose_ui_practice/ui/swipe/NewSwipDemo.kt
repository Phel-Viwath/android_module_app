package com.viwath.compose_ui_practice.ui.swipe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SwipeableDrawerDemo() {

    var drawerState by remember { mutableFloatStateOf(0f) }
    var isPanelExpanded by remember { mutableStateOf(false) }

    Box {
        SwipeableDrawerScreen(
            drawerState = drawerState,
            onDrawerStateChanged = { drawerState = it },

            // ── Main Screen ────────────────────────────────────────────
            mainContent = {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1A1A2E)),
                    contentAlignment = Alignment.Center
                ) {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = "Swipeable Drawer Demo",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "← Swipe left for Blue drawer",
                            color = Color(0xFFAAAAAA),
                            fontSize = 13.sp
                        )

                        Text(
                            text = "Swipe right for Green drawer →",
                            color = Color(0xFFAAAAAA),
                            fontSize = 13.sp
                        )

                        Text(
                            text = "↑ Swipe up for Center panel",
                            color = Color(0xFFAAAAAA),
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Drawer State: ${drawerState.format(2)}",
                            color = Color(0xFF00FFCC),
                            fontSize = 12.sp
                        )
                    }
                }
            },

            // ── Right Drawer ───────────────────────────────────────────
            rightDrawer = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F3460)),
                    contentAlignment = Alignment.Center
                ) {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {

                        Text(text = "🟢", fontSize = 48.sp)

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Right Drawer",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Swipe back to close",
                            color = Color(0xFFAAAAAA),
                            fontSize = 13.sp
                        )
                    }
                }
            },

            // ── Left Drawer ────────────────────────────────────────────
            leftDrawer = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF16213E)),
                    contentAlignment = Alignment.Center
                ) {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {

                        Text(text = "🔵", fontSize = 48.sp)

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Left Drawer",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Swipe back to close",
                            color = Color(0xFFAAAAAA),
                            fontSize = 13.sp
                        )
                    }
                }
            },

            // ── Center Panel ───────────────────────────────────────────
            centerPanel = {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color(0xFF533483),
                            shape = RoundedCornerShape(
                                topStart = 28.dp,
                                topEnd = 28.dp
                            )
                        )
                ) {

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        // Drag handle
                        Box(
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .size(width = 40.dp, height = 5.dp)
                                .background(
                                    color = Color(0xFFAAAAAA),
                                    shape = RoundedCornerShape(50)
                                )
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = "⬆",
                            fontSize = 36.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Center Panel",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Swipe up to expand",
                            color = Color(0xFFCCCCCC),
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Demo content
                        repeat(5) {

                            Box(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxSize()
                                    .height(60.dp)
                                    .background(
                                        Color(0xFF6B4FA3),
                                        RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Panel Item ${it + 1}",
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            },

            onSwipeUp = {
                isPanelExpanded = true

            }


        )
        if (isPanelExpanded) {
            SwipUp()
        }
    }


}




// Helper Extension
private fun Float.format(digits: Int) =
    "%.${digits}f".format(this)

@Composable
fun SwipUp(){
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = Color(0xFF533483),
                shape = RoundedCornerShape(
                    topStart = 28.dp,
                    topEnd = 28.dp
                )
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Drag Handle
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .background(
                        color = Color(0xFFBBBBBB),
                        shape = RoundedCornerShape(50)
                    )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Quick Actions",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))


            // Row 1
            ActionItem("🎵", "Music Player")
            ActionItem("📁", "Files")
            ActionItem("📸", "Gallery")

            Spacer(modifier = Modifier.height(16.dp))

            // Row 2
            ActionItem("⚙️", "Settings")
            ActionItem("🔔", "Notifications")
            ActionItem("📊", "Analytics")

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Swipe down to close",
                color = Color(0xFFCCCCCC),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun ActionItem(
    icon: String,
    title: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .height(70.dp)
            .padding(vertical = 6.dp)
            .background(
                Color(0xFF6B4FA3),
                RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.CenterStart
    ) {

        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = icon,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.size(16.dp))

            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}