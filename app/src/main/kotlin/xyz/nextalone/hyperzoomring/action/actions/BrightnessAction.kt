package xyz.nextalone.hyperzoomring.action.actions

import android.content.Context
import android.provider.Settings
import xyz.nextalone.hyperzoomring.action.Action

object BrightnessAction : Action {
    override val id = "brightness"
    override val displayName = "调节亮度"

    override fun execute(context: Context, intensity: Float) {
        val resolver = context.contentResolver
        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
        val current = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        val step = (intensity * 25).toInt().coerceAtLeast(5)
        val newBrightness = (current + step).coerceIn(1, 255)
        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, newBrightness)
    }
}
