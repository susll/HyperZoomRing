package xyz.nextalone.hyperzoomring.ring

data class ZoomRingEvent(
    val timestampMs: Long,
    val value: Int,
) {
    val isCameraMode: Boolean get() = value == ZoomRingConstants.TICK_VALUE_CAMERA
}
