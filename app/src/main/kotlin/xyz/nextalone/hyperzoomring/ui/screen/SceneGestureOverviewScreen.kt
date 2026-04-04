package xyz.nextalone.hyperzoomring.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.theme.MiuixTheme
import xyz.nextalone.hyperzoomring.action.ActionRegistry
import xyz.nextalone.hyperzoomring.config.ConfigManager
import xyz.nextalone.hyperzoomring.config.SceneType
import xyz.nextalone.hyperzoomring.ring.GestureType

@Composable
fun SceneGestureOverviewScreen(
    scene: SceneType,
    onGestureClick: (GestureType) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val config = remember { ConfigManager.fromContext(context) }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        SmallTitle(scene.displayName)

        if (scene == SceneType.FULLSCREEN) {
            Text(
                "此场景为实验性功能，部分设备可能不支持",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }

        Spacer(Modifier.height(12.dp))
        SmallTitle("手势配置")

        Card(modifier = Modifier.fillMaxWidth()) {
            GestureType.entries.forEach { gesture ->
                val actionId = config.getSceneActionId(scene, gesture)
                val actionName = actionId?.let { ActionRegistry.get(it)?.displayName } ?: "使用默认"
                SuperArrow(
                    title = gesture.displayName,
                    summary = "动作: $actionName",
                    onClick = { onGestureClick(gesture) },
                )
            }
        }
    }
}
