package xyz.nextalone.hyperzoomring.action.actions

import android.content.Context
import android.media.AudioManager
import xyz.nextalone.hyperzoomring.action.Action

object VolumeAction : Action {
    override val id = "volume"
    override val displayName = "调节音量"

    override fun execute(context: Context, intensity: Float) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
    }
}
