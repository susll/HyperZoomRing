package xyz.nextalone.hyperzoomring.action.actions

import android.content.Context
import android.media.AudioManager
import android.media.session.MediaSessionManager
import android.util.Log
import android.view.KeyEvent
import xyz.nextalone.hyperzoomring.action.Action
import xyz.nextalone.hyperzoomring.config.SceneType

// --- 上一曲 / 下一曲 ---

object MediaPreviousAction : Action {
    override val id = "media_previous"
    override val displayName = "上一曲"
    override val sceneOnly = SceneType.MEDIA_PLAYING

    override fun execute(context: Context, intensity: Float) {
        MediaHelper.dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }
}

object MediaNextAction : Action {
    override val id = "media_next"
    override val displayName = "下一曲"
    override val sceneOnly = SceneType.MEDIA_PLAYING

    override fun execute(context: Context, intensity: Float) {
        MediaHelper.dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_NEXT)
    }
}

// --- 快进 / 快退 ---

object MediaFastForwardAction : Action {
    override val id = "media_fast_forward"
    override val displayName = "快进"
    override val sceneOnly = SceneType.MEDIA_PLAYING

    override fun execute(context: Context, intensity: Float) {
        MediaHelper.dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)
    }
}

object MediaRewindAction : Action {
    override val id = "media_rewind"
    override val displayName = "快退"
    override val sceneOnly = SceneType.MEDIA_PLAYING

    override fun execute(context: Context, intensity: Float) {
        MediaHelper.dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_REWIND)
    }
}

// --- 前进 / 后退 10 秒 ---

object MediaSeekForwardAction : Action {
    override val id = "media_seek_forward"
    override val displayName = "前进 10 秒"
    override val sceneOnly = SceneType.MEDIA_PLAYING

    override fun execute(context: Context, intensity: Float) {
        if (!MediaHelper.seekByOffset(context, 10_000L)) {
            MediaHelper.dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD)
        }
    }
}

object MediaSeekBackwardAction : Action {
    override val id = "media_seek_backward"
    override val displayName = "后退 10 秒"
    override val sceneOnly = SceneType.MEDIA_PLAYING

    override fun execute(context: Context, intensity: Float) {
        if (!MediaHelper.seekByOffset(context, -10_000L)) {
            MediaHelper.dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD)
        }
    }
}

internal object MediaHelper {
    private const val TAG = "HyperZoomRing"

    fun dispatchMediaKey(context: Context, keyCode: Int) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    /**
     * Seek by offset using MediaSessionManager.
     * Returns true if seek succeeded, false if caller should fall back to media key.
     */
    fun seekByOffset(context: Context, offsetMs: Long): Boolean {
        return try {
            val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE)
                as? MediaSessionManager ?: return false
            val controllers = msm.getActiveSessions(null)
            val controller = controllers.firstOrNull() ?: return false

            val state = controller.playbackState ?: return false
            val currentPos = state.position
            val newPos = (currentPos + offsetMs).coerceAtLeast(0)

            controller.transportControls.seekTo(newPos)
            true
        } catch (e: Exception) {
            Log.w(TAG, "MediaSession seek failed", e)
            false
        }
    }
}
