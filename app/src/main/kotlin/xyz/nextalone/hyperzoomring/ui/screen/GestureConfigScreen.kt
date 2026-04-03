package xyz.nextalone.hyperzoomring.ui.screen

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
fun GestureConfigScreen(
    gestureType: GestureType,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val config = remember { ConfigManager.fromContext(context) }
    var selectedActionId by remember { mutableStateOf(config.getActionId(gestureType)) }
    val actions = remember { ActionRegistry.all() }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("${gestureType.displayName} — 选择动作", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("无动作")
            RadioButton(selected = selectedActionId == null, onClick = {
                selectedActionId = null; config.setActionId(gestureType, null)
            })
        }
        Spacer(Modifier.height(8.dp))

        actions.forEach { action ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(action.displayName)
                RadioButton(selected = selectedActionId == action.id, onClick = {
                    selectedActionId = action.id; config.setActionId(gestureType, action.id)
                })
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
