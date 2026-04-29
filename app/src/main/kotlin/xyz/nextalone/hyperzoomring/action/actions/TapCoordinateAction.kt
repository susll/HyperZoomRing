package xyz.nextalone.hyperzoomring.action.actions

import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import xyz.nextalone.hyperzoomring.action.Action

object TapCoordinateAction : Action {
    override val id = "tap_coordinate"
    override val displayName = "点击屏幕坐标"

    var coordinateX: Int = 0
    var coordinateY: Int = 0

    internal const val TAG = "HyperZoomRing"
    internal const val TAP_DURATION_MS = 50L

    override fun execute(context: Context, intensity: Float) {
        if (coordinateX <= 0 || coordinateY <= 0) return
        performTap(context, coordinateX.toFloat(), coordinateY.toFloat())
    }

    fun parseConfig(config: String?) {
        val coords = config?.split(",")?.mapNotNull { it.toIntOrNull() }
        if (coords?.size == 2) {
            coordinateX = coords[0]
            coordinateY = coords[1]
        }
    }
}

private fun performTap(context: Context, x: Float, y: Float) {
    try {
        val imClass = Class.forName("android.hardware.input.InputManager")
        val getInstance = imClass.getDeclaredMethod("getInstance")
        val im = getInstance.invoke(null) ?: return

        val downTime = SystemClock.uptimeMillis()

        injectEvent(im, imClass, MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0).apply {
            source = InputDevice.SOURCE_TOUCHSCREEN
        })

        val upTime = downTime + TapCoordinateAction.TAP_DURATION_MS
        injectEvent(im, imClass, MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, x, y, 0).apply {
            source = InputDevice.SOURCE_TOUCHSCREEN
        })
    } catch (e: Exception) {
        Log.e(TapCoordinateAction.TAG, "Tap failed", e)
    }
}

private fun injectEvent(im: Any, imClass: Class<*>, event: MotionEvent) {
    try {
        val injectMethod = imClass.getDeclaredMethod("injectInputEvent", android.view.InputEvent::class.java, Int::class.javaPrimitiveType)
        injectMethod.invoke(im, event, 0)
        event.recycle()
    } catch (e: Exception) {
        Log.e(TapCoordinateAction.TAG, "injectInputEvent failed", e)
    }
}
