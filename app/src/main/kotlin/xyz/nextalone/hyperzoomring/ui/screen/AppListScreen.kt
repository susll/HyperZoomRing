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
import top.yukonga.miuix.kmp.theme.MiuixTheme
import xyz.nextalone.hyperzoomring.config.ConfigManager
import xyz.nextalone.hyperzoomring.ring.GestureType

@Composable
fun AppListScreen(
    onAppClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val config = remember { ConfigManager.fromContext(context) }
    val pm = context.packageManager
    var configuredPackages by remember { mutableStateOf(config.getConfiguredPackages()) }
    var showPicker by remember { mutableStateOf(false) }

    if (showPicker) {
        AppPickerDialog(
            excludePackages = configuredPackages,
            onSelect = { pkg ->
                config.addConfiguredPackage(pkg)
                configuredPackages = config.getConfiguredPackages()
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        SmallTitle("应用配置")

        Card(modifier = Modifier.fillMaxWidth()) {
            SuperArrow(
                title = "添加应用",
                summary = "从已安装应用中选择",
                onClick = { showPicker = true },
            )
        }

        if (configuredPackages.isEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text(
                "尚未配置任何应用，请点击上方添加",
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        } else {
            Spacer(Modifier.height(12.dp))
            SmallTitle("已配置应用")

            Card(modifier = Modifier.fillMaxWidth()) {
                configuredPackages.forEach { pkg ->
                    val appLabel = remember(pkg) {
                        try {
                            @Suppress("DEPRECATION")
                            val appInfo = pm.getApplicationInfo(pkg, 0)
                            pm.getApplicationLabel(appInfo).toString()
                        } catch (_: Exception) { pkg }
                    }

                    val configuredCount = GestureType.entries.count { gesture ->
                        config.getAppActionId(pkg, gesture) != null
                    }

                    SuperArrow(
                        title = appLabel,
                        summary = "$pkg · 已配置 $configuredCount 个手势",
                        onClick = { onAppClick(pkg) },
                    )
                }
            }
        }
    }
}
