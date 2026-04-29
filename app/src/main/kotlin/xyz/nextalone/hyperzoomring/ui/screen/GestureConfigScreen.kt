package xyz.nextalone.hyperzoomring.ui.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.SuperArrow
import top.yukonga.miuix.kmp.extra.SuperRadioButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import xyz.nextalone.hyperzoomring.action.ActionRegistry
import xyz.nextalone.hyperzoomring.action.actions.LaunchAppAction
import xyz.nextalone.hyperzoomring.action.actions.SwipeAction
import xyz.nextalone.hyperzoomring.action.actions.TapCoordinateAction
import xyz.nextalone.hyperzoomring.config.ConfigManager
import xyz.nextalone.hyperzoomring.config.ConfigScope
import xyz.nextalone.hyperzoomring.ring.GestureType

@Composable
fun GestureConfigScreen(
    gestureType: GestureType,
    scope: ConfigScope = ConfigScope.Global,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val config = remember { ConfigManager.fromContext(context) }
    val pm = context.packageManager

    var selectedActionId by remember {
        mutableStateOf(
            when (scope) {
                is ConfigScope.Global -> config.getActionId(gestureType)
                is ConfigScope.App -> config.getAppActionId(scope.packageName, gestureType)
                is ConfigScope.Scene -> config.getSceneActionId(scope.scene, gestureType)
            }
        )
    }

    // 当前启动应用的包名
    var launchAppPackage by remember {
        mutableStateOf(
            when (scope) {
                is ConfigScope.Global -> config.getActionConfig(gestureType)
                is ConfigScope.App -> config.getAppActionConfig(scope.packageName, gestureType)
                is ConfigScope.Scene -> config.getSceneActionConfig(scope.scene, gestureType)
            }
        )
    }

    var showAppPicker by remember { mutableStateOf(false) }
    var showSwipeConfig by remember { mutableStateOf(false) }
    var showTapConfig by remember { mutableStateOf(false) }

    val actions = remember { ActionRegistry.allForScope(scope) }

    fun saveAction(actionId: String?) {
        when (scope) {
            is ConfigScope.Global -> config.setActionId(gestureType, actionId)
            is ConfigScope.App -> config.setAppActionId(scope.packageName, gestureType, actionId)
            is ConfigScope.Scene -> config.setSceneActionId(scope.scene, gestureType, actionId)
        }
        selectedActionId = actionId
    }

    fun saveConfig(configValue: String?) {
        when (scope) {
            is ConfigScope.Global -> config.setActionConfig(gestureType, configValue)
            is ConfigScope.App -> config.setAppActionConfig(scope.packageName, gestureType, configValue)
            is ConfigScope.Scene -> config.setSceneActionConfig(scope.scene, gestureType, configValue)
        }
        launchAppPackage = configValue
    }

    if (showAppPicker) {
        AppPickerDialog(
            excludePackages = emptySet(),
            onSelect = { pkg ->
                saveAction(LaunchAppAction.id)
                saveConfig(pkg)
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false },
        )
    }

    if (showSwipeConfig) {
        SwipeConfigDialog(
            currentConfig = launchAppPackage,
            onConfirm = { direction, distance ->
                saveConfig("$direction,$distance")
                showSwipeConfig = false
            },
            onDismiss = { showSwipeConfig = false },
        )
    }

    if (showTapConfig) {
        TapConfigDialog(
            currentConfig = launchAppPackage,
            onConfirm = { x, y ->
                saveConfig("$x,$y")
                showTapConfig = false
            },
            onDismiss = { showTapConfig = false },
        )
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        SmallTitle("${gestureType.displayName} — 选择动作")

        Card(modifier = Modifier.fillMaxWidth()) {
            SuperRadioButton(
                title = "无动作",
                selected = selectedActionId == null,
                onClick = { saveAction(null); saveConfig(null) },
            )

            actions.forEach { action ->
                when (action) {
                    is LaunchAppAction -> {
                        val appLabel = launchAppPackage?.let { pkg ->
                            try {
                                @Suppress("DEPRECATION")
                                val appInfo = pm.getApplicationInfo(pkg, 0)
                                pm.getApplicationLabel(appInfo).toString()
                            } catch (_: Exception) { pkg }
                        }

                        SuperRadioButton(
                            title = action.displayName,
                            summary = if (selectedActionId == action.id && appLabel != null) appLabel else "点击选择应用",
                            selected = selectedActionId == action.id,
                            onClick = { showAppPicker = true },
                        )
                    }
                    is SwipeAction -> {
                        val configParts = launchAppPackage?.split(",")
                        val dir = configParts?.getOrNull(0) ?: "up"
                        val dist = configParts?.getOrNull(1)?.toIntOrNull() ?: 800
                        val dirText = when (dir) {
                            "down" -> "向下"
                            "left" -> "向左"
                            "right" -> "向右"
                            else -> "向上"
                        }
                        SuperRadioButton(
                            title = action.displayName,
                            summary = if (selectedActionId == action.id) "$dirText ${dist}px" else null,
                            selected = selectedActionId == action.id,
                            onClick = {
                                saveAction(action.id)
                                showSwipeConfig = true
                            },
                        )
                    }
                    is TapCoordinateAction -> {
                        val coords = launchAppPackage?.split(",")?.mapNotNull { it.toIntOrNull() }
                        val coordText = if (coords?.size == 2) "(${coords[0]}, ${coords[1]})" else "未设置"
                        SuperRadioButton(
                            title = action.displayName,
                            summary = if (selectedActionId == action.id) "坐标: $coordText" else "点击配置",
                            selected = selectedActionId == action.id,
                            onClick = {
                                saveAction(action.id)
                                showTapConfig = true
                            },
                        )
                    }
                    else -> {
                        SuperRadioButton(
                            title = action.displayName,
                            selected = selectedActionId == action.id,
                            onClick = { saveAction(action.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeConfigDialog(
    currentConfig: String?,
    onConfirm: (String, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val configParts = currentConfig?.split(",")
    var direction by remember { mutableStateOf(configParts?.getOrNull(0) ?: "up") }
    var distanceText by remember { mutableStateOf((configParts?.getOrNull(1)?.toIntOrNull() ?: 800).toString()) }
    var showDirectionMenu by remember { mutableStateOf(false) }

    val directions = listOf("up" to "向上", "down" to "向下", "left" to "向左", "right" to "向右")
    val currentDirLabel = directions.find { it.first == direction }?.second ?: "向上"

    val borderColor = MiuixTheme.colorScheme.outline

    androidx.compose.ui.window.Dialog(onDismissRequest = {
        onConfirm(direction, distanceText.toIntOrNull() ?: 800)
    }) {
        Card(modifier = Modifier.padding(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("设置滑动", style = MiuixTheme.textStyles.title2)
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text("方向")
                    Box {
                        Card(
                            modifier = Modifier.clickable { showDirectionMenu = true }
                                .border(1.dp, borderColor, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                .width(100.dp),
                        ) {
                            Text(
                                currentDirLabel,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = showDirectionMenu,
                            onDismissRequest = { showDirectionMenu = false },
                        ) {
                            directions.forEach { (key, label) ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = { direction = key; showDirectionMenu = false },
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text("距离 (px)")
                    Card(
                        modifier = Modifier.border(1.dp, borderColor, androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                    ) {
                        BasicTextField(
                            value = distanceText,
                            onValueChange = { distanceText = it.filter { c -> c.isDigit() } },
                            modifier = Modifier.width(100.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TapConfigDialog(
    currentConfig: String?,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val coords = currentConfig?.split(",")?.mapNotNull { it.toIntOrNull() }
    var xText by remember { mutableStateOf(if (coords?.getOrNull(0) != null && coords[0] > 0) coords[0].toString() else "") }
    var yText by remember { mutableStateOf(if (coords?.getOrNull(1) != null && coords[1] > 0) coords[1].toString() else "") }

    val borderColor = MiuixTheme.colorScheme.outline

    androidx.compose.ui.window.Dialog(onDismissRequest = {
        onConfirm(xText.toIntOrNull() ?: 0, yText.toIntOrNull() ?: 0)
    }) {
        Card(modifier = Modifier.padding(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("设置点击坐标", style = MiuixTheme.textStyles.title2)
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text("X 坐标")
                    Card(
                        modifier = Modifier.border(1.dp, borderColor, androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                    ) {
                        BasicTextField(
                            value = xText,
                            onValueChange = { xText = it.filter { c -> c.isDigit() } },
                            modifier = Modifier.width(100.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text("Y 坐标")
                    Card(
                        modifier = Modifier.border(1.dp, borderColor, androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                    ) {
                        BasicTextField(
                            value = yText,
                            onValueChange = { yText = it.filter { c -> c.isDigit() } },
                            modifier = Modifier.width(100.dp).padding(horizontal = 16.dp, vertical = 8.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                        )
                    }
                }
            }
        }
    }
}
