package xyz.nextalone.hyperzoomring.action

import android.content.Context

interface Action {
    val id: String
    val displayName: String
    fun execute(context: Context, intensity: Float)
}
