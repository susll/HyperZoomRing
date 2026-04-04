package xyz.nextalone.hyperzoomring.hook

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import xyz.nextalone.hyperzoomring.config.SceneType

object SceneDetector {
    private const val TAG = "HyperZoomRing"

    @Volatile
    var torchState: Boolean = false
        private set

    @Volatile
    var fullscreenState: Boolean = false

    fun init(context: Context) {
        // Delay torch callback registration to a background thread —
        // CameraService may not be ready during early system_server boot,
        // and getSystemService(CAMERA_SERVICE) blocks until it is.
        val thread = HandlerThread("HyperZoomRing-SceneDetector").apply { start() }
        Handler(thread.looper).postDelayed({
            initTorchCallback(context)
        }, 10_000) // 10s delay to ensure CameraService is up
    }

    fun detectActiveScene(context: Context): SceneType? {
        if (isMediaPlaying(context)) return SceneType.MEDIA_PLAYING
        if (torchState) return SceneType.FLASHLIGHT_ON
        if (fullscreenState) return SceneType.FULLSCREEN
        return null
    }

    private fun isMediaPlaying(context: Context): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        return am.isMusicActive
    }

    private fun initTorchCallback(context: Context) {
        try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
            cm.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    torchState = enabled
                }
            }, null)
            Log.i(TAG, "Torch callback registered")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register torch callback", e)
        }
    }
}
