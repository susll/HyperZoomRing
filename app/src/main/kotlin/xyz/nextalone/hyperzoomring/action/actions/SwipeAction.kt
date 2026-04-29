package xyz.nextalone.hyperzoomring.action.actions

import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import xyz.nextalone.hyperzoomring.action.Action

object SwipeAction : Action {
    override val id = "swipe"
    override val displayName = "屏幕滑动"

    var distancePx: Int = 800
    var direction: String = "up"

    internal const val TAG = "HyperZoomRing"
    internal const val SWIPE_STEPS = 10
    internal const val STEP_DELAY_MS = 5L
    internal const val UP_DELAY_MS = 10L

    override fun execute(context: Context, intensity: Float) {
        performSwipe(context, direction, distancePx)
    }

    fun parseConfig(config: String?) {
        val parts = config?.split(",")
        direction = parts?.getOrNull(0) ?: "up"
        distancePx = parts?.getOrNull(1)?.toIntOrNull() ?: 800
    }
}

private fun performSwipe(context: Context, direction: String, distance: Int) {
    try {
        val imClass = Class.forName("android.hardware.input.InputManager")
        val getInstance = imClass.getDeclaredMethod("getInstance")
        val im = getInstance.invoke(null) ?: return

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager ?: return
        val display = wm.defaultDisplay
        val size = android.graphics.Point()
        display.getRealSize(size)

        val centerX = size.x / 2f
        val centerY = size.y / 2f

        val (startX, startY, endX, endY) = when (direction) {
            "down" -> listOf(centerX, centerY - distance / 2f, centerX, centerY + distance / 2f)
            "left" -> listOf(centerX + distance / 2f, centerY, centerX - distance / 2f, centerY)
            "right" -> listOf(centerX - distance / 2f, centerY, centerX + distance / 2f, centerY)
            else -> listOf(centerX, centerY + distance / 2f, centerX, centerY - distance / 2f)
        }

        val downTime = SystemClock.uptimeMillis()

        injectEvent(im, imClass, MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0).apply {
            source = InputDevice.SOURCE_TOUCHSCREEN
        })

        for (i in 1..SwipeAction.SWIPE_STEPS) {
            val moveTime = downTime + (i * SwipeAction.STEP_DELAY_MS)
            val moveX = startX + (endX - startX) * i / SwipeAction.SWIPE_STEPS
            val moveY = startY + (endY - startY) * i / SwipeAction.SWIPE_STEPS
            injectEvent(im, imClass, MotionEvent.obtain(downTime, moveTime, MotionEvent.ACTION_MOVE, moveX, moveY, 0).apply {
                source = InputDevice.SOURCE_TOUCHSCREEN
            })
        }

        val upTime = downTime + (SwipeAction.SWIPE_STEPS * SwipeAction.STEP_DELAY_MS) + SwipeAction.UP_DELAY_MS
        injectEvent(im, imClass, MotionEvent.obtain(downTime, upTime, MotionEvent.ACTION_UP, endX, endY, 0).apply {
            source = InputDevice.SOURCE_TOUCHSCREEN
        })
    } catch (e: Exception) {
        Log.e(SwipeAction.TAG, "Swipe failed", e)
    }
}

private fun injectEvent(im: Any, imClass: Class<*>, event: MotionEvent) {
    try {
        val injectMethod = imClass.getDeclaredMethod("injectInputEvent", android.view.InputEvent::class.java, Int::class.javaPrimitiveType)
        injectMethod.invoke(im, event, 0)
        event.recycle()
    } catch (e: Exception) {
        Log.e(SwipeAction.TAG, "injectInputEvent failed", e)
    }
}
