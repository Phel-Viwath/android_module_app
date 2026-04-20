package com.viwath.compose_ui_practice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.viwath.compose_ui_practice.ui.theme.Compose_ui_practiceTheme
import com.viwath.practice_module_app.drag_drop2.MainMenuGrid


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
                //SwipeableDrawerDemo()
                //MenuScreen2()
                //MenuScreen()
                MainMenuGrid()
            }
        }
    }
}
