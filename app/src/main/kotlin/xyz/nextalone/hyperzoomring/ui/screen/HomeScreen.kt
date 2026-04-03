package xyz.nextalone.hyperzoomring.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("模块状态", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("启用 HyperZoomRing")
                Text(
                    if (isEnabled) "变焦环手势已启用" else "变焦环手势已禁用",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(checked = isEnabled, onCheckedChange = { isEnabled = it; config.isEnabled = it })
        }

        Spacer(Modifier.height(24.dp))
        Text("手势配置", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        GestureType.entries.forEach { gesture ->
            val actionId = config.getActionId(gesture)
            val actionName = actionId?.let { ActionRegistry.get(it)?.displayName } ?: "未设置"

            Card(modifier = Modifier.fillMaxWidth().clickable { onGestureClick(gesture) }) {
                Column(Modifier.padding(16.dp)) {
                    Text(gesture.displayName)
                    Spacer(Modifier.height(4.dp))
                    Text("动作: $actionName", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(16.dp))
        Text("速度阈值", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text("200ms 内事件数 > ${speedThreshold.toInt()} 判定为快转")
        Slider(
            value = speedThreshold,
            onValueChange = { speedThreshold = it; config.speedThreshold = it.toInt() },
            valueRange = 2f..15f,
            steps = 12,
        )
    }
}
