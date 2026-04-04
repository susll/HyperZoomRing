package xyz.nextalone.hyperzoomring.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.extra.SuperArrow
import xyz.nextalone.hyperzoomring.config.ConfigManager
import xyz.nextalone.hyperzoomring.config.SceneType
import xyz.nextalone.hyperzoomring.ring.GestureType

@Composable
fun SceneListScreen(
    onSceneClick: (SceneType) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val config = remember { ConfigManager.fromContext(context) }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        SmallTitle("场景配置")

        Card(modifier = Modifier.fillMaxWidth()) {
            SceneType.entries.forEach { scene ->
                val configuredCount = GestureType.entries.count { gesture ->
                    config.getSceneActionId(scene, gesture) != null
                }
                SuperArrow(
                    title = scene.displayName,
                    summary = "已配置 $configuredCount 个手势",
                    onClick = { onSceneClick(scene) },
                )
            }
        }
    }
}
