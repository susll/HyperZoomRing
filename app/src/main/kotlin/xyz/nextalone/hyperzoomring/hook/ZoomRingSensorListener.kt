package xyz.nextalone.hyperzoomring.hook

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import xyz.nextalone.hyperzoomring.ring.ZoomRingConstants

/**
 * Listens to the mt350x optical tracking sensor for direction data.
 * Initialization is deferred to a background thread to avoid blocking system_server startup.
 */
class ZoomRingSensorListener(private val context: Context) : SensorEventListener {

    private val tag = "HyperZoomRing"

    /** Last known direction: 1 = CW, -1 = CCW, 0 = unknown. */
    @Volatile
    var lastDirection: Int = 0
        private set

    @Volatile
    var lastSensorTimestampMs: Long = 0
        private set

    @Volatile
    private var registered = false

    private val handlerThread = HandlerThread("ZoomRingSensor").apply { start() }
    private val handler = Handler(handlerThread.looper)

    /**
     * Start sensor listening on a background thread.
     * Retries until SensorService is available (it starts after system_server init).
     */
    fun start() {
        handler.post { startWithRetry(retries = 20, delayMs = 3000L) }
    }

    private fun startWithRetry(retries: Int, delayMs: Long) {
        if (registered) return
        if (retries <= 0) {
            Log.e(tag, "Gave up waiting for SensorService after all retries")
            return
        }

        try {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            if (sensorManager == null) {
                Log.w(tag, "SensorManager not available yet, retrying in ${delayMs}ms ($retries left)")
                handler.postDelayed({ startWithRetry(retries - 1, delayMs) }, delayMs)
                return
            }

            val sensor = findOpticalTrackingSensor(sensorManager)
            if (sensor == null) {
                Log.w(tag, "mt350x sensor not found yet, retrying in ${delayMs}ms ($retries left)")
                handler.postDelayed({ startWithRetry(retries - 1, delayMs) }, delayMs)
                return
            }

            Log.i(tag, "Registering sensor listener: ${sensor.name} (type=${sensor.type})")
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST, handler)
            registered = true
            Log.i(tag, "Sensor listener registered successfully")
        } catch (e: Exception) {
            Log.w(tag, "Sensor registration failed, retrying in ${delayMs}ms ($retries left)", e)
            handler.postDelayed({ startWithRetry(retries - 1, delayMs) }, delayMs)
        }
    }

    private fun findOpticalTrackingSensor(sensorManager: SensorManager): Sensor? {
        val byType = sensorManager.getSensorList(ZoomRingConstants.SENSOR_TYPE_OPTICAL_TRACKING)
        if (byType.isNotEmpty()) return byType[0]

        return sensorManager.getSensorList(Sensor.TYPE_ALL).firstOrNull { sensor ->
            sensor.name.contains("mt350x", ignoreCase = true) ||
                sensor.name.contains("OPTICALTRACKING", ignoreCase = true)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.values.isEmpty()) return
        val deltaX = event.values[0]
        if (deltaX != 0f) {
            lastDirection = if (deltaX > 0) 1 else -1
            lastSensorTimestampMs = System.currentTimeMillis()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
