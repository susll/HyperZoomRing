package xyz.nextalone.hyperzoomring.hook

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

                    val value = event.getAxisValue(MotionEvent.AXIS_SCROLL).toInt()
                    val zoomEvent = ZoomRingEvent(timestampMs = event.eventTime, value = value)
                    Log.d(TAG, "ZoomRing: value=$value, time=${event.eventTime}")

                    val gesture = detector.onEvent(zoomEvent)
                    val intensity = detector.currentIntensity
                    Log.d(TAG, "Gesture: $gesture, intensity=$intensity, cameraMode=${detector.isCameraMode}")

                    // Send broadcast to app for diagnostic display
                    try {
                        val ctx = appContext ?: return@beforeHook
                        val intent = Intent(ACTION_ZOOM_RING_EVENT).apply {
                            setPackage("xyz.nextalone.hyperzoomring")
                            putExtra(EXTRA_TIMESTAMP, zoomEvent.timestampMs)
                            putExtra(EXTRA_VALUE, zoomEvent.value)
                            putExtra(EXTRA_GESTURE, gesture.name)
                            putExtra(EXTRA_INTENSITY, intensity)
                        }
                        ctx.sendBroadcast(intent)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to send diagnostic broadcast", e)
                    }

                    if (!config.isEnabled || detector.isCameraMode) return@beforeHook

                    val actionId = config.getActionId(gesture)
                    Log.d(TAG, "Config lookup: gesture=${gesture.name}, actionId=$actionId, enabled=${config.isEnabled}")
                    if (actionId == null) return@beforeHook
                    val action = ActionRegistry.get(actionId) ?: return@beforeHook

                    if (action is LaunchAppAction) {
                        LaunchAppAction.targetPackage = config.getActionConfig(gesture)
                    }

                    val ctx = appContext ?: return@beforeHook
                    try {
                        Log.i(TAG, "Executing action: ${action.id}")
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

    private fun isZoomRingDevice(device: InputDevice): Boolean {
        return device.name == ZoomRingConstants.DEVICE_NAME ||
            (device.vendorId == ZoomRingConstants.VENDOR_ID && device.productId == ZoomRingConstants.PRODUCT_ID)
    }
}
