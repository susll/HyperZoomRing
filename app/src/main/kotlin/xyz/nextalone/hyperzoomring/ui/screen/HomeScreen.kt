package xyz.nextalone.hyperzoomring.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.theme.MiuixTheme
import xyz.nextalone.hyperzoomring.action.ActionRegistry
import xyz.nextalone.hyperzoomring.config.ConfigManager
import xyz.nextalone.hyperzoomring.ring.GestureType

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onGestureClick: (GestureType) -> Unit = {},
) {
    val context = LocalContext.current
    val config = remember { ConfigManager.fromContext(context) }
    var isEnabled by remember { mutableStateOf(config.isEnabled) }
    var speedThreshold by remember { mutableFloatStateOf(config.speedThreshold.toFloat()) }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        SmallTitle("模块状态")

        Card(modifier = Modifier.fillMaxWidth()) {
            SuperSwitch(
                title = "启用 HyperZoomRing",
                summary = if (isEnabled) "变焦环手势已启用" else "变焦环手势已禁用",
                checked = isEnabled,
                onCheckedChange = { isEnabled = it; config.isEnabled = it },
            )
        }

        Spacer(Modifier.height(12.dp))
        SmallTitle("手势配置")

        Card(modifier = Modifier.fillMaxWidth()) {
            GestureType.entries.forEach { gesture ->
                val actionId = config.getActionId(gesture)
                val actionName = actionId?.let { ActionRegistry.get(it)?.displayName } ?: "未设置"

                SuperArrow(
                    title = gesture.displayName,
                    summary = "动作: $actionName",
                    onClick = { onGestureClick(gesture) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        SmallTitle("速度阈值")

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "200ms 内事件数 > ${speedThreshold.toInt()} 判定为快转",
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = (speedThreshold - 2f) / 13f,
                    onValueChange = { progress ->
                        val v = 2f + progress * 13f
                        speedThreshold = v
                        config.speedThreshold = v.toInt()
                    },
                )
            }
        }
    }
}
