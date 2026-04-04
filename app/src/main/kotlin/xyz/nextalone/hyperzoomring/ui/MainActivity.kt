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
import xyz.nextalone.hyperzoomring.config.ConfigScope
import xyz.nextalone.hyperzoomring.config.SceneType
import xyz.nextalone.hyperzoomring.ring.GestureType
import xyz.nextalone.hyperzoomring.ui.screen.*

sealed class Screen {
    data object Home : Screen()
    data object Diagnostic : Screen()
    data object ModeConfig : Screen()
    data class GestureConfig(val gesture: GestureType, val scope: ConfigScope) : Screen()
    data object AppList : Screen()
    data class AppGestureOverview(val packageName: String) : Screen()
    data object SceneList : Screen()
    data class SceneGestureOverview(val scene: SceneType) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { MiuixTheme { MainContent() } }
    }
}

@Composable
private fun MainContent() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var screenStack by remember { mutableStateOf<List<Screen>>(emptyList()) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val currentScreen = screenStack.lastOrNull()

    fun push(screen: Screen) { screenStack = screenStack + screen }
    fun pop() { screenStack = screenStack.dropLast(1); refreshTrigger++ }

    if (currentScreen != null) {
        BackHandler { pop() }

        val title = when (currentScreen) {
            is Screen.ModeConfig -> "模式选择"
            is Screen.GestureConfig -> "${currentScreen.gesture.displayName} 配置"
            is Screen.AppList -> "应用配置"
            is Screen.AppGestureOverview -> "应用手势配置"
            is Screen.SceneList -> "场景配置"
            is Screen.SceneGestureOverview -> currentScreen.scene.displayName
            else -> ""
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = title,
                    navigationIcon = {
                        IconButton(onClick = { pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                        }
                    },
                )
            },
        ) { padding ->
            val mod = Modifier.padding(padding)
            when (currentScreen) {
                is Screen.ModeConfig -> ModeConfigScreen(modifier = mod)
                is Screen.GestureConfig -> GestureConfigScreen(
                    gestureType = currentScreen.gesture,
                    scope = currentScreen.scope,
                    modifier = mod,
                )
                is Screen.AppList -> AppListScreen(
                    onAppClick = { push(Screen.AppGestureOverview(it)) },
                    modifier = mod,
                )
                is Screen.AppGestureOverview -> AppGestureOverviewScreen(
                    packageName = currentScreen.packageName,
                    onGestureClick = { push(Screen.GestureConfig(it, ConfigScope.App(currentScreen.packageName))) },
                    modifier = mod,
                )
                is Screen.SceneList -> SceneListScreen(
                    onSceneClick = { push(Screen.SceneGestureOverview(it)) },
                    modifier = mod,
                )
                is Screen.SceneGestureOverview -> SceneGestureOverviewScreen(
                    scene = currentScreen.scene,
                    onGestureClick = { push(Screen.GestureConfig(it, ConfigScope.Scene(currentScreen.scene))) },
                    modifier = mod,
                )
                else -> {}
            }
        }
    } else {
        Scaffold(
            topBar = { TopAppBar(title = "HyperZoomRing") },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, label = "主页", icon = Icons.Default.Home)
                    NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, label = "诊断", icon = Icons.Default.Info)
                }
            },
        ) { padding ->
            when (selectedTab) {
                0 -> HomeScreen(
                    modifier = Modifier.padding(padding),
                    onNavigate = { push(it) },
                    refreshTrigger = refreshTrigger,
                )
                1 -> DiagnosticScreen(modifier = Modifier.padding(padding))
            }
        }
    }
}
