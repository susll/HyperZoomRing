package xyz.nextalone.hyperzoomring.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.nextalone.hyperzoomring.ring.ZoomRingEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnosticScreen(modifier: Modifier = Modifier) {
    val events = remember { mutableStateListOf<ZoomRingEvent>() }
    val listState = rememberLazyListState()

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
                    Text(if (events.isNotEmpty()) "0x%08X (%d)".format(events.last().value, events.last().value) else "—")
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
                val mode = if (event.isCameraMode) "CAM" else "DEF"
                Text(
                    "${tf.format(Date(event.timestampMs))} | val=${event.value} | $mode",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}
