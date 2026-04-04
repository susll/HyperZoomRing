package xyz.nextalone.hyperzoomring.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import xyz.nextalone.hyperzoomring.ring.GestureType
import xyz.nextalone.hyperzoomring.ui.screen.DiagnosticScreen
import xyz.nextalone.hyperzoomring.ui.screen.GestureConfigScreen
import xyz.nextalone.hyperzoomring.ui.screen.HomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MiuixTheme {
                MainContent()
            }
        }
    }
}

@Composable
private fun MainContent() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingGesture by remember { mutableStateOf<GestureType?>(null) }

    val gesture = editingGesture
    if (gesture != null) {
        BackHandler { editingGesture = null }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = "${gesture.displayName} 配置",
                    navigationIcon = {
                        IconButton(onClick = { editingGesture = null }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                            )
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
            topBar = { TopAppBar(title = "HyperZoomRing") },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = "主页",
                        icon = Icons.Default.Home,
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = "诊断",
                        icon = Icons.Default.Info,
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
