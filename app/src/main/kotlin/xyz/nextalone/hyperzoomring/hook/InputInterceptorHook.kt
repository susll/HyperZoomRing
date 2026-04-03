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
    const val EXTRA_DIRECTION = "direction"
    const val EXTRA_GESTURE = "gesture"
    const val EXTRA_INTENSITY = "intensity"

    private val detector = ZoomRingDetector()
    private var sensorListener: ZoomRingSensorListener? = null

    private const val DIRECTION_STALE_MS = 500L

    /** Timestamp of last zoom ring input event we intercepted. */
    @Volatile
    private var lastZoomRingEventMs: Long = 0

    /** If launchCamera is called within this window after a zoom ring event, block it. */
    private const val CAMERA_BLOCK_WINDOW_MS = 2000L

    fun hook(param: PackageParam, config: ConfigManager) = with(param) {
        Log.i(TAG, "Hooking InputManagerService.filterInputEvent + optical tracking sensor")

        // Start sensor listener for direction data
        val ctx = appContext
        if (ctx != null) {
            try {
                sensorListener = ZoomRingSensorListener(ctx).also { it.start() }
                Log.i(TAG, "Optical tracking sensor listener started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start sensor listener", e)
            }
        }

        // Block Xiaomi's camera launch ONLY when triggered by zoom ring rotation.
        // Uses time window: if a zoom ring event was intercepted within the last 2s,
        // the launchCamera call is from the zoom ring → block it.
        // Other triggers (double-tap power, etc.) won't have a recent zoom ring event.
        try {
            "com.miui.server.input.util.ShortCutActionsUtils".toClass().hook {
                injectMember {
                    allMethods("launchCamera")
                    beforeHook {
                        if (!config.isEnabled) return@beforeHook
                        val age = System.currentTimeMillis() - lastZoomRingEventMs
                        if (age < CAMERA_BLOCK_WINDOW_MS) {
                            Log.i(TAG, "Blocked zoom ring camera launch (${age}ms after ring event)")
                            resultTrue()
                        }
                    }
                }.result {
                    onHookingFailure { Log.w(TAG, "Failed to hook ShortCutActionsUtils.launchCamera", it) }
                }
            }
            Log.i(TAG, "Hooked ShortCutActionsUtils.launchCamera")
        } catch (e: Exception) {
            Log.w(TAG, "ShortCutActionsUtils not found", e)
        }

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

                    // Record timing for camera launch blocking
                    lastZoomRingEventMs = System.currentTimeMillis()

                    // Camera foreground: pass through for normal zoom ring behavior
                    if (isCameraForeground(appContext)) return@beforeHook

                    // Consume event to block original behavior
                    resultFalse()

                    val direction = getDirection()
                    val scrollValue = event.getAxisValue(MotionEvent.AXIS_SCROLL).toInt()
                    val zoomEvent = ZoomRingEvent(
                        timestampMs = event.eventTime,
                        value = scrollValue,
                        direction = direction,
                    )

                    val gesture = detector.onEvent(zoomEvent)
                    val intensity = detector.currentIntensity

                    // Broadcast to diagnostic UI
                    try {
                        val broadcastCtx = appContext ?: return@beforeHook
                        val intent = Intent(ACTION_ZOOM_RING_EVENT).apply {
                            setPackage("xyz.nextalone.hyperzoomring")
                            putExtra(EXTRA_TIMESTAMP, zoomEvent.timestampMs)
                            putExtra(EXTRA_VALUE, scrollValue)
                            putExtra(EXTRA_DIRECTION, direction)
                            putExtra(EXTRA_GESTURE, gesture.name)
                            putExtra(EXTRA_INTENSITY, intensity)
                        }
                        broadcastCtx.sendBroadcast(intent)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to send diagnostic broadcast", e)
                    }

                    if (!config.isEnabled) return@beforeHook

                    val actionId = config.getActionId(gesture) ?: return@beforeHook
                    val action = ActionRegistry.get(actionId) ?: return@beforeHook

                    if (action is LaunchAppAction) {
                        LaunchAppAction.targetPackage = config.getActionConfig(gesture)
                    }

                    val actionCtx = appContext ?: return@beforeHook
                    try {
                        action.execute(actionCtx, intensity)
                    } catch (e: Exception) {
                        Log.e(TAG, "Action execution failed: ${action.id}", e)
                    }
                }
            }.result {
                onHookingFailure { Log.e(TAG, "Failed to hook filterInputEvent", it) }
            }
        }
    }

    private fun getDirection(): Int {
        val listener = sensorListener ?: return 0
        val age = System.currentTimeMillis() - listener.lastSensorTimestampMs
        return if (age < DIRECTION_STALE_MS) listener.lastDirection else 0
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
