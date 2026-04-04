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
import xyz.nextalone.hyperzoomring.ring.GestureType

@Composable
fun AppGestureOverviewScreen(
    packageName: String,
    onGestureClick: (GestureType) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val config = remember { ConfigManager.fromContext(context) }
    val pm = context.packageManager
    val appLabel = remember {
        try {
            @Suppress("DEPRECATION")
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) { packageName }
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        SmallTitle(appLabel)
        Text(
            packageName,
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )

        Spacer(Modifier.height(12.dp))
        SmallTitle("手势配置")

        Card(modifier = Modifier.fillMaxWidth()) {
            GestureType.entries.forEach { gesture ->
                val actionId = config.getAppActionId(packageName, gesture)
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
