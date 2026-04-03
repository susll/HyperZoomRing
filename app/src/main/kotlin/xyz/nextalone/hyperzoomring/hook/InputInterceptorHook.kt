package xyz.nextalone.hyperzoomring.hook

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.InputDevice
import android.view.InputEvent
import android.view.MotionEvent
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.hook.type.java.IntType
import xyz.nextalone.hyperzoomring.action.ActionRegistry
import xyz.nextalone.hyperzoomring.action.actions.LaunchAppAction
import xyz.nextalone.hyperzoomring.config.ConfigManager
import xyz.nextalone.hyperzoomring.ring.ZoomRingConstants
import xyz.nextalone.hyperzoomring.ring.ZoomRingDetector
import xyz.nextalone.hyperzoomring.ring.ZoomRingEvent

@Suppress("DEPRECATION")
object InputInterceptorHook {
    private const val TAG = "HyperZoomRing"
    const val ACTION_ZOOM_RING_EVENT = "xyz.nextalone.hyperzoomring.ZOOM_RING_EVENT"
    const val EXTRA_TIMESTAMP = "timestamp"
    const val EXTRA_VALUE = "value"
    const val EXTRA_GESTURE = "gesture"
    const val EXTRA_INTENSITY = "intensity"

    private val detector = ZoomRingDetector()
    private var dumpCount = 0
    private const val MAX_DUMPS = 50

    fun hook(param: PackageParam, config: ConfigManager) = with(param) {
        Log.i(TAG, "Hooking InputManagerService.filterInputEvent")

        "com.android.server.input.InputManagerService".toClass().hook {
            injectMember {
                method {
                    name = "filterInputEvent"
                    param(InputEvent::class.java, IntType)
                }
                beforeHook {
                    val event = args(0).cast<InputEvent>() ?: return@beforeHook
                    if (event !is MotionEvent) return@beforeHook
                    val device = event.device ?: return@beforeHook
                    if (!isZoomRingDevice(device)) return@beforeHook

                    val scrollValue = event.getAxisValue(MotionEvent.AXIS_SCROLL)

                    // Always dump to capture both camera and non-camera mode data
                    if (dumpCount < MAX_DUMPS) {
                        dumpCount++
                        val sb = StringBuilder("DUMP[$dumpCount] ")
                        for (axis in 0..63) {
                            val v = event.getAxisValue(axis)
                            if (v != 0f) sb.append("ax$axis=$v ")
                        }
                        sb.append("action=${event.action} src=0x${event.source.toString(16)} t=${event.eventTime}")
                        Log.i(TAG, sb.toString())
                    }

                    // Detect camera foreground
                    val cameraInForeground = isCameraForeground(appContext)
                    if (cameraInForeground) {
                        // Camera mode: let event pass through, just log
                        Log.d(TAG, "Camera mode: scroll=$scrollValue (pass through)")
                        return@beforeHook
                    }

                    // Non-camera: consume event to block original behavior
                    resultFalse()

                    val value = scrollValue.toInt()
                    val zoomEvent = ZoomRingEvent(timestampMs = event.eventTime, value = value)
                    val gesture = detector.onEvent(zoomEvent)
                    val intensity = detector.currentIntensity

                    // Broadcast to diagnostic UI
                    try {
                        val ctx = appContext ?: return@beforeHook
                        val intent = Intent(ACTION_ZOOM_RING_EVENT).apply {
                            setPackage("xyz.nextalone.hyperzoomring")
                            putExtra(EXTRA_TIMESTAMP, zoomEvent.timestampMs)
                            putExtra(EXTRA_VALUE, value)
                            putExtra(EXTRA_GESTURE, gesture.name)
                            putExtra(EXTRA_INTENSITY, intensity)
                        }
                        ctx.sendBroadcast(intent)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to send diagnostic broadcast", e)
                    }

                    if (!config.isEnabled) return@beforeHook

                    val actionId = config.getActionId(gesture) ?: return@beforeHook
                    val action = ActionRegistry.get(actionId) ?: return@beforeHook

                    if (action is LaunchAppAction) {
                        LaunchAppAction.targetPackage = config.getActionConfig(gesture)
                    }

                    val ctx = appContext ?: return@beforeHook
                    try {
                        action.execute(ctx, intensity)
                    } catch (e: Exception) {
                        Log.e(TAG, "Action execution failed: ${action.id}", e)
                    }
                }
            }.result {
                onHookingFailure { Log.e(TAG, "Failed to hook filterInputEvent", it) }
            }
        }
    }

    private fun isCameraForeground(context: Context?): Boolean {
        val ctx = context ?: return false
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        val tasks = am.getRunningTasks(1)
        val topPackage = tasks.firstOrNull()?.topActivity?.packageName ?: return false
        return topPackage == "com.android.camera" || topPackage == "com.xiaomi.camera"
    }

    private fun isZoomRingDevice(device: InputDevice): Boolean {
        return device.name == ZoomRingConstants.DEVICE_NAME ||
            (device.vendorId == ZoomRingConstants.VENDOR_ID && device.productId == ZoomRingConstants.PRODUCT_ID)
    }
}
