package xyz.nextalone.hyperzoomring.action

import xyz.nextalone.hyperzoomring.action.actions.BrightnessDownAction
import xyz.nextalone.hyperzoomring.action.actions.BrightnessUpAction
import xyz.nextalone.hyperzoomring.action.actions.LaunchAppAction
import xyz.nextalone.hyperzoomring.action.actions.VolumeDownAction
import xyz.nextalone.hyperzoomring.action.actions.VolumeUpAction

object ActionRegistry {
    private val actions = mutableMapOf<String, Action>()

    init {
        register(VolumeUpAction)
        register(VolumeDownAction)
        register(BrightnessUpAction)
        register(BrightnessDownAction)
        register(LaunchAppAction)
    }

    private fun register(action: Action) { actions[action.id] = action }
    fun get(id: String): Action? = actions[id]
    fun all(): List<Action> = actions.values.toList()
}
