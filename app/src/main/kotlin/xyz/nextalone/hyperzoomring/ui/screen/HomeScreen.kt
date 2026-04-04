package xyz.nextalone.hyperzoomring.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import xyz.nextalone.hyperzoomring.config.ConfigScope
import xyz.nextalone.hyperzoomring.config.DispatchMode
import xyz.nextalone.hyperzoomring.ring.GestureType
import xyz.nextalone.hyperzoomring.ui.Screen

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigate: (Screen) -> Unit = {},
    refreshTrigger: Int = 0,
) {
    val context = LocalContext.current
    val config = remember { ConfigManager.fromContext(context) }
    var isEnabled by remember(refreshTrigger) { mutableStateOf(config.isEnabled) }
    var dispatchMode by remember(refreshTrigger) { mutableStateOf(config.dispatchMode) }
    var overrideCamera by remember(refreshTrigger) { mutableStateOf(config.overrideCamera) }
    var speedThreshold by remember(refreshTrigger) { mutableFloatStateOf(config.speedThreshold.toFloat()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
    ) {
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
        SmallTitle("模式选择")
        Card(modifier = Modifier.fillMaxWidth()) {
            SuperArrow(
                title = "分发模式",
                summary = dispatchMode.displayName,
                onClick = { onNavigate(Screen.ModeConfig) },
            )
        }

        Spacer(Modifier.height(12.dp))
        SmallTitle("相机设置")
        Card(modifier = Modifier.fillMaxWidth()) {
            SuperSwitch(
                title = "覆盖相机应用",
                summary = if (overrideCamera) "变焦环在相机内执行自定义动作" else "相机内保持原始变焦功能",
                checked = overrideCamera,
                onCheckedChange = { overrideCamera = it; config.overrideCamera = it },
            )
        }

        Spacer(Modifier.height(12.dp))
        SmallTitle(
            if (dispatchMode == DispatchMode.GLOBAL) "手势配置" else "默认手势配置"
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            GestureType.entries.forEach { gesture ->
                val actionId = config.getActionId(gesture)
                val actionName = actionId?.let { ActionRegistry.get(it)?.displayName } ?: "未设置"
                SuperArrow(
                    title = gesture.displayName,
                    summary = "动作: $actionName",
                    onClick = { onNavigate(Screen.GestureConfig(gesture, ConfigScope.Global)) },
                )
            }
        }

        if (dispatchMode == DispatchMode.PER_APP) {
            Spacer(Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                SuperArrow(
                    title = "管理应用配置",
                    summary = "${config.getConfiguredPackages().size} 个应用已配置",
                    onClick = { onNavigate(Screen.AppList) },
                )
            }
        }

        if (dispatchMode == DispatchMode.PER_SCENE) {
            Spacer(Modifier.height(12.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                SuperArrow(
                    title = "管理场景配置",
                    summary = "媒体播放 · 手电筒 · 全屏",
                    onClick = { onNavigate(Screen.SceneList) },
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
