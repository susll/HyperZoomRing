package xyz.nextalone.hyperzoomring.ring

class ZoomRingDetector(
    private val speedWindowMs: Long = ZoomRingConstants.SPEED_WINDOW_MS,
    private val speedThreshold: Int = ZoomRingConstants.DEFAULT_SPEED_THRESHOLD,
) {
    private val recentEvents = ArrayDeque<Long>()
    var currentIntensity: Float = 0f; private set
    var isCameraMode: Boolean = false; private set

    fun onEvent(event: ZoomRingEvent): GestureType {
        isCameraMode = event.isCameraMode
        val now = event.timestampMs
        recentEvents.addLast(now)
        while (recentEvents.isNotEmpty() && recentEvents.first() < now - speedWindowMs) {
            recentEvents.removeFirst()
        }
        val count = recentEvents.size
        currentIntensity = (count.toFloat() / (speedThreshold * 2)).coerceIn(0f, 1f)
        return if (count > speedThreshold) GestureType.ROTATE_FAST else GestureType.ROTATE_SLOW
    }

    fun reset() {
        recentEvents.clear()
        currentIntensity = 0f
        isCameraMode = false
    }
}
