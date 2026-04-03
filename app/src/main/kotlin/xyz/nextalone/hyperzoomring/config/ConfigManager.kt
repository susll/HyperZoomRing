package xyz.nextalone.hyperzoomring.config

import android.content.Context
import android.content.SharedPreferences
import com.highcapable.yukihookapi.hook.xposed.prefs.YukiHookPrefsBridge
import xyz.nextalone.hyperzoomring.ring.GestureType
import xyz.nextalone.hyperzoomring.ring.ZoomRingConstants

class ConfigManager private constructor(private val store: PrefsStore) {

    fun getActionId(gesture: GestureType): String? =
        store.getString("gesture_${gesture.name}_action_id", null)

    fun setActionId(gesture: GestureType, actionId: String?) {
        if (actionId != null) {
            store.putString("gesture_${gesture.name}_action_id", actionId)
        } else {
            store.remove("gesture_${gesture.name}_action_id")
        }
    }

    fun getActionConfig(gesture: GestureType): String? =
        store.getString("gesture_${gesture.name}_action_config", null)

    fun setActionConfig(gesture: GestureType, config: String?) {
        if (config != null) {
            store.putString("gesture_${gesture.name}_action_config", config)
        } else {
            store.remove("gesture_${gesture.name}_action_config")
        }
    }

    var speedThreshold: Int
        get() = store.getInt("speed_threshold", ZoomRingConstants.DEFAULT_SPEED_THRESHOLD)
        set(value) = store.putInt("speed_threshold", value)

    var isEnabled: Boolean
        get() = store.getBoolean("module_enabled", true)
        set(value) = store.putBoolean("module_enabled", value)

    companion object {
        const val PREFS_NAME = "hyperzoomring_config"

        fun fromContext(context: Context): ConfigManager {
            @Suppress("DEPRECATION")
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE)
            return ConfigManager(SharedPrefsStore(prefs))
        }

        fun fromYukiPrefs(prefs: YukiHookPrefsBridge): ConfigManager =
            ConfigManager(YukiPrefsStore(prefs))
    }
}

private interface PrefsStore {
    fun getString(key: String, default: String?): String?
    fun getInt(key: String, default: Int): Int
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putString(key: String, value: String)
    fun putInt(key: String, value: Int)
    fun putBoolean(key: String, value: Boolean)
    fun remove(key: String)
}

private class SharedPrefsStore(private val prefs: SharedPreferences) : PrefsStore {
    override fun getString(key: String, default: String?): String? = prefs.getString(key, default)
    override fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)
    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    override fun putString(key: String, value: String) { prefs.edit().putString(key, value).apply() }
    override fun putInt(key: String, value: Int) { prefs.edit().putInt(key, value).apply() }
    override fun putBoolean(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }
    override fun remove(key: String) { prefs.edit().remove(key).apply() }
}

private class YukiPrefsStore(private val prefs: YukiHookPrefsBridge) : PrefsStore {
    override fun getString(key: String, default: String?): String? {
        val result = prefs.getString(key, default ?: "")
        return if (result.isEmpty() && default == null) null else result
    }
    override fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)
    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    override fun putString(key: String, value: String) { prefs.edit { putString(key, value) } }
    override fun putInt(key: String, value: Int) { prefs.edit { putInt(key, value) } }
    override fun putBoolean(key: String, value: Boolean) { prefs.edit { putBoolean(key, value) } }
    override fun remove(key: String) { prefs.edit { remove(key) } }
}
