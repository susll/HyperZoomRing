package xyz.nextalone.hyperzoomring.action

import xyz.nextalone.hyperzoomring.action.actions.BrightnessDownAction
import xyz.nextalone.hyperzoomring.action.actions.BrightnessUpAction
import xyz.nextalone.hyperzoomring.action.actions.FlashlightBrightnessDownAction
import xyz.nextalone.hyperzoomring.action.actions.FlashlightBrightnessUpAction
import xyz.nextalone.hyperzoomring.action.actions.LaunchAppAction
import xyz.nextalone.hyperzoomring.action.actions.VolumeDownAction
import xyz.nextalone.hyperzoomring.action.actions.VolumeUpAction
import xyz.nextalone.hyperzoomring.config.ConfigScope
import xyz.nextalone.hyperzoomring.config.SceneType

object ActionRegistry {
    private val actions = mutableMapOf<String, Action>()

    init {
        register(VolumeUpAction)
        register(VolumeDownAction)
        register(BrightnessUpAction)
        register(BrightnessDownAction)
        register(LaunchAppAction)
        register(FlashlightBrightnessUpAction)
        register(FlashlightBrightnessDownAction)
    }

    private fun register(action: Action) { actions[action.id] = action }
    fun get(id: String): Action? = actions[id]
    fun all(): List<Action> = actions.values.toList()

    fun allForScope(scope: ConfigScope): List<Action> = actions.values.filter { action ->
        val sceneOnly = action.sceneOnly ?: return@filter true
        scope is ConfigScope.Scene && scope.scene == sceneOnly
    }
}
