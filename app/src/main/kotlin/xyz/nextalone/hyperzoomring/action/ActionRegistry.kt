package xyz.nextalone.hyperzoomring.action

import xyz.nextalone.hyperzoomring.action.actions.BrightnessAction
import xyz.nextalone.hyperzoomring.action.actions.LaunchAppAction
import xyz.nextalone.hyperzoomring.action.actions.VolumeAction

object ActionRegistry {
    private val actions = mutableMapOf<String, Action>()

    init {
        register(VolumeAction)
        register(BrightnessAction)
        register(LaunchAppAction)
    }

    private fun register(action: Action) { actions[action.id] = action }
    fun get(id: String): Action? = actions[id]
    fun all(): List<Action> = actions.values.toList()
}
