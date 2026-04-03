package xyz.nextalone.hyperzoomring.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import xyz.nextalone.hyperzoomring.ring.GestureType
import xyz.nextalone.hyperzoomring.ui.screen.DiagnosticScreen
import xyz.nextalone.hyperzoomring.ui.screen.GestureConfigScreen
import xyz.nextalone.hyperzoomring.ui.screen.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainContent()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingGesture by remember { mutableStateOf<GestureType?>(null) }

    val gesture = editingGesture
    if (gesture != null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("${gesture.displayName} 配置") },
                    navigationIcon = {
                        IconButton(onClick = { editingGesture = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                )
            },
        ) { padding ->
            GestureConfigScreen(
                gestureType = gesture,
                onBack = { editingGesture = null },
                modifier = Modifier.padding(padding),
            )
        }
    } else {
        Scaffold(
            topBar = { TopAppBar(title = { Text("HyperZoomRing") }) },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("主页") },
                        icon = {},
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text("诊断") },
                        icon = {},
                    )
                }
            },
        ) { padding ->
            when (selectedTab) {
                0 -> HomeScreen(
                    modifier = Modifier.padding(padding),
                    onGestureClick = { editingGesture = it },
                )
                1 -> DiagnosticScreen(modifier = Modifier.padding(padding))
            }
        }
    }
}
