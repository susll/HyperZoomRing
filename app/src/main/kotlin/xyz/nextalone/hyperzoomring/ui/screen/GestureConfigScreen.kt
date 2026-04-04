package xyz.nextalone.hyperzoomring.ui.screen

import androidx.compose.foundation.layout.*
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

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        SmallTitle("${gestureType.displayName} — 选择动作")

        Card(modifier = Modifier.fillMaxWidth()) {
            SuperRadioButton(
                title = "无动作",
                selected = selectedActionId == null,
                onClick = { saveAction(null); saveConfig(null) },
            )

            actions.forEach { action ->
                if (action is LaunchAppAction) {
                    // 启动应用特殊处理：显示当前选中的应用
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
                } else {
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
