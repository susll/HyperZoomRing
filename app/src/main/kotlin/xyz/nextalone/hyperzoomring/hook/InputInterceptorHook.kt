package xyz.nextalone.hyperzoomring.hook

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
    private val detector = ZoomRingDetector()
    private val eventListeners = mutableListOf<(ZoomRingEvent) -> Unit>()

    fun addEventListener(listener: (ZoomRingEvent) -> Unit) { eventListeners.add(listener) }
    fun removeEventListener(listener: (ZoomRingEvent) -> Unit) { eventListeners.remove(listener) }

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

                    // Zoom ring value is on AXIS_SCROLL, not AXIS_VSCROLL
                    val value = event.getAxisValue(MotionEvent.AXIS_SCROLL).toInt()
                    val zoomEvent = ZoomRingEvent(timestampMs = event.eventTime, value = value)
                    Log.d(TAG, "ZoomRing: value=$value, time=${event.eventTime}")
                    eventListeners.forEach { listener -> listener(zoomEvent) }

                    val gesture = detector.onEvent(zoomEvent)
                    val intensity = detector.currentIntensity
                    Log.d(TAG, "Gesture: $gesture, intensity=$intensity, cameraMode=${detector.isCameraMode}")

                    if (!config.isEnabled || detector.isCameraMode) return@beforeHook

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

    private fun isZoomRingDevice(device: InputDevice): Boolean {
        return device.name == ZoomRingConstants.DEVICE_NAME ||
            (device.vendorId == ZoomRingConstants.VENDOR_ID && device.productId == ZoomRingConstants.PRODUCT_ID)
    }
}
