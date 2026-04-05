package com.viwath.compose_ui_practice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.viwath.compose_ui_practice.ui.DragDropScreen
import com.viwath.compose_ui_practice.ui.swipe.SwipeableDrawerDemo
import com.viwath.compose_ui_practice.ui.theme.Compose_ui_practiceTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Compose_ui_practiceTheme {
                //PracticeModule()
                DragDropScreen()
                //WidgetDashboardScreen()
                //MixedWidgetDashboard()
                //SwipeableDrawerDemo()
            }
        }
    }
}
