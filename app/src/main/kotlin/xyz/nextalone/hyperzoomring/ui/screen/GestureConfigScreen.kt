package xyz.nextalone.hyperzoomring.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.extra.SuperRadioButton
import xyz.nextalone.hyperzoomring.action.ActionRegistry
import xyz.nextalone.hyperzoomring.config.ConfigManager
import xyz.nextalone.hyperzoomring.ring.GestureType

@Composable
fun GestureConfigScreen(
    gestureType: GestureType,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val config = remember { ConfigManager.fromContext(context) }
    var selectedActionId by remember { mutableStateOf(config.getActionId(gestureType)) }
    val actions = remember { ActionRegistry.all() }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        SmallTitle("${gestureType.displayName} — 选择动作")

        Card(modifier = Modifier.fillMaxWidth()) {
            SuperRadioButton(
                title = "无动作",
                selected = selectedActionId == null,
                onClick = {
                    selectedActionId = null
                    config.setActionId(gestureType, null)
                },
            )

            actions.forEach { action ->
                SuperRadioButton(
                    title = action.displayName,
                    selected = selectedActionId == action.id,
                    onClick = {
                        selectedActionId = action.id
                        config.setActionId(gestureType, action.id)
                    },
                )
            }
        }
    }
}
