package xyz.nextalone.hyperzoomring.action

import android.content.Context
import xyz.nextalone.hyperzoomring.config.SceneType

interface Action {
    val id: String
    val displayName: String
    val sceneOnly: SceneType? get() = null
    fun execute(context: Context, intensity: Float)
}
