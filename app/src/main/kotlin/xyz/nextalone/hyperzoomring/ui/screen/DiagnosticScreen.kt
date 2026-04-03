package xyz.nextalone.hyperzoomring.ui.screen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.nextalone.hyperzoomring.hook.InputInterceptorHook
import xyz.nextalone.hyperzoomring.ring.ZoomRingEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MAX_EVENTS = 200

@Composable
fun DiagnosticScreen(modifier: Modifier = Modifier) {
    val events = remember { mutableStateListOf<ZoomRingEvent>() }
    val lastGesture = remember { mutableStateOf("—") }
    val lastIntensity = remember { mutableFloatStateOf(0f) }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Register broadcast receiver for zoom ring events from system_server
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val timestamp = intent.getLongExtra(InputInterceptorHook.EXTRA_TIMESTAMP, 0)
                val value = intent.getIntExtra(InputInterceptorHook.EXTRA_VALUE, 0)
                val gesture = intent.getStringExtra(InputInterceptorHook.EXTRA_GESTURE) ?: ""
                val intensity = intent.getFloatExtra(InputInterceptorHook.EXTRA_INTENSITY, 0f)

                val direction = intent.getIntExtra(InputInterceptorHook.EXTRA_DIRECTION, 0)
                events.add(ZoomRingEvent(timestampMs = timestamp, value = value, direction = direction))
                lastGesture.value = gesture
                lastIntensity.floatValue = intensity

                // Prune old events
                while (events.size > MAX_EVENTS) {
                    events.removeAt(0)
                }
            }
        }
        val filter = IntentFilter(InputInterceptorHook.ACTION_ZOOM_RING_EVENT)
        @Suppress("UnspecifiedRegisterReceiverFlag")
        context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    LaunchedEffect(events.size) {
        if (events.isNotEmpty()) listState.animateScrollToItem(events.lastIndex)
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text("诊断信息", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("事件总数"); Text("${events.size}")
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("最新值")
                    Text(if (events.isNotEmpty()) "${events.last().value}" else "—")
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("手势")
                    Text(lastGesture.value)
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("强度")
                    Text("%.2f".format(lastIntensity.floatValue))
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("模式")
                    Text(if (events.isNotEmpty() && events.last().isCameraMode) "相机" else "默认")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("事件日志", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
            items(events.toList()) { event ->
                val tf = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }
                val dir = when { event.isClockwise -> "CW"; event.isCounterClockwise -> "CCW"; else -> "?" }
                Text(
                    "${tf.format(Date(event.timestampMs))} | val=${event.value} | $dir",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}
