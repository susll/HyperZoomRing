package xyz.nextalone.hyperzoomring.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.extra.SuperRadioButton
import xyz.nextalone.hyperzoomring.config.ConfigManager
import xyz.nextalone.hyperzoomring.config.DispatchMode

@Composable
fun ModeConfigScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val config = remember { ConfigManager.fromContext(context) }
    var selected by remember { mutableStateOf(config.dispatchMode) }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        SmallTitle("选择变焦环分发模式")

        Card(modifier = Modifier.fillMaxWidth()) {
            DispatchMode.entries.forEach { mode ->
                SuperRadioButton(
                    title = mode.displayName,
                    summary = when (mode) {
                        DispatchMode.GLOBAL -> "所有应用使用相同手势配置"
                        DispatchMode.PER_APP -> "不同应用使用不同手势配置"
                        DispatchMode.PER_SCENE -> "根据系统状态自动切换手势配置"
                    },
                    selected = selected == mode,
                    onClick = { selected = mode; config.dispatchMode = mode },
                )
            }
        }
    }
}
