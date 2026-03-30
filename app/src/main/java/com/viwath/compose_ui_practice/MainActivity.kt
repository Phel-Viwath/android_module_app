package com.viwath.compose_ui_practice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.unit.dp
import com.viwath.compose_ui_practice.ui.DragDropScreen
import com.viwath.compose_ui_practice.ui.swipe.BrightnessSlider
import com.viwath.compose_ui_practice.ui.swipe.MainScreenContent
import com.viwath.compose_ui_practice.ui.swipe.QuickToggle
import com.viwath.compose_ui_practice.ui.swipe.SwipeDownPanel
import com.viwath.compose_ui_practice.ui.swipe.SwipeDownPanelDemo
import com.viwath.compose_ui_practice.ui.swipe.rememberSwipeDownPanelState
import com.viwath.compose_ui_practice.ui.theme.Compose_ui_practiceTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Compose_ui_practiceTheme {
                //PracticeModule()
                //DragDropScreen()
                //WidgetDashboardScreen()
                //MixedWidgetDashboard()

                SwipeDownPanelDemo()
            }
        }
    }
}
