package xyz.nextalone.hyperzoomring.hook

import android.util.Log
import android.view.InputDevice
import android.view.InputEvent
import android.view.MotionEvent
import com.highcapable.yukihookapi.hook.param.PackageParam
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

    fun hook(param: PackageParam) = with(param) {
        Log.i(TAG, "Hooking InputManagerService for zoom ring interception")

        "com.android.server.input.InputManagerService".toClass().hook {
            injectMember {
                method {
                    name = "onInputEvent"
                    paramCount(1..3)
                }
                beforeHook {
                    val event = args.firstOrNull() as? InputEvent ?: return@beforeHook
                    if (event !is MotionEvent) return@beforeHook
                    val device = event.device ?: return@beforeHook
                    if (!isZoomRingDevice(device)) return@beforeHook

                    val value = event.getAxisValue(MotionEvent.AXIS_VSCROLL).toInt()
                    val zoomEvent = ZoomRingEvent(timestampMs = event.eventTime, value = value)
                    Log.d(TAG, "ZoomRing event: value=$value, time=${event.eventTime}")
                    eventListeners.forEach { listener -> listener(zoomEvent) }

                    val gesture = detector.onEvent(zoomEvent)
                    val intensity = detector.currentIntensity
                    Log.d(TAG, "Gesture: $gesture, intensity=$intensity, cameraMode=${detector.isCameraMode}")
                }
            }.result {
                onHookingFailure {
                    Log.e(TAG, "Failed to hook onInputEvent, trying fallback", it)
                    hookFallback(param)
                }
            }
        }
    }

    private fun hookFallback(param: PackageParam) = with(param) {
        "com.android.server.policy.PhoneWindowManager".toClass().hook {
            injectMember {
                method {
                    name = "interceptMotionBeforeQueueingNonInteractive"
                }
                beforeHook {
                    val event = args.firstOrNull() as? MotionEvent ?: return@beforeHook
                    val device = event.device ?: return@beforeHook
                    if (!isZoomRingDevice(device)) return@beforeHook
                    val value = event.getAxisValue(MotionEvent.AXIS_VSCROLL).toInt()
                    val zoomEvent = ZoomRingEvent(timestampMs = event.eventTime, value = value)
                    Log.d(TAG, "ZoomRing event (fallback): value=$value")
                    eventListeners.forEach { listener -> listener(zoomEvent) }
                    detector.onEvent(zoomEvent)
                }
            }.result {
                onHookingFailure { Log.e(TAG, "Fallback hook also failed", it) }
            }
        }
    }

    private fun isZoomRingDevice(device: InputDevice): Boolean {
        return device.name == ZoomRingConstants.DEVICE_NAME ||
            (device.vendorId == ZoomRingConstants.VENDOR_ID && device.productId == ZoomRingConstants.PRODUCT_ID)
    }
}
