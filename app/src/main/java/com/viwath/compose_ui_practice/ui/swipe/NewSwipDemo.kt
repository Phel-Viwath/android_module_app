package com.viwath.compose_ui_practice.ui.swipe

import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SwipeableDrawerDemo() {

    var drawerState by remember { mutableFloatStateOf(0f) }
    var showSwipeUp by remember { mutableStateOf(false) }

    Box {

        SwipeableDrawerScreen(
            drawerState = drawerState,
            onDrawerStateChanged = {
                drawerState = it
            },

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

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Drawer State : $drawerState",
                            color = Color.Cyan
                        )
                    }
                }
            },

            rightDrawer = {

                DrawerScreen(
                    emoji = "🟢",
                    title = "Right Drawer",
                    color = Color(0xFF0F3460)
                )
            },

            leftDrawer = {

                DrawerScreen(
                    emoji = "🔵",
                    title = "Left Drawer",
                    color = Color(0xFF16213E)
                )
            },

            centerPanel = {
                CenterPanel()
            },

            onSwipeUp = {
                showSwipeUp = true
            }
        )

        if (showSwipeUp) {
            SwipUp()
        }
    }
}

@Composable
private fun DrawerScreen(
    emoji: String,
    title: String,
    color: Color
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = emoji,
                fontSize = 48.sp
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


@Composable
fun CenterPanel(){
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) { }
}

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