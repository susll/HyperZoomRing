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

    var throttleMs: Int
        get() = store.getInt("throttle_ms", 200)
        set(value) = store.putInt("throttle_ms", value)

    var isEnabled: Boolean
        get() = store.getBoolean("module_enabled", true)
        set(value) = store.putBoolean("module_enabled", value)

    // --- Dispatch Mode ---
    var dispatchMode: DispatchMode
        get() = DispatchMode.fromKey(
            store.getString("dispatch_mode", DispatchMode.GLOBAL.key) ?: DispatchMode.GLOBAL.key
        )
        set(value) = store.putString("dispatch_mode", value.key)

    // --- Camera Override ---
    var overrideCamera: Boolean
        get() = store.getBoolean("override_camera", false)
        set(value) = store.putBoolean("override_camera", value)

    // --- Per-App ---
    @Volatile
    private var cachedPackages: Set<String>? = null

    fun getConfiguredPackages(): Set<String> {
        cachedPackages?.let { return it }
        val raw = store.getString("per_app_configured_packages", null) ?: return emptySet()
        val result = raw.split(",").filter { it.isNotBlank() }.toSet()
        cachedPackages = result
        return result
    }

    fun addConfiguredPackage(packageName: String) {
        val current = getConfiguredPackages().toMutableSet()
        current.add(packageName)
        store.putString("per_app_configured_packages", current.joinToString(","))
        cachedPackages = current
    }

    fun removeConfiguredPackage(packageName: String) {
        val current = getConfiguredPackages().toMutableSet()
        current.remove(packageName)
        if (current.isEmpty()) {
            store.remove("per_app_configured_packages")
        } else {
            store.putString("per_app_configured_packages", current.joinToString(","))
        }
        cachedPackages = current
        GestureType.entries.forEach { gesture ->
            store.remove("app_${packageName}_gesture_${gesture.name}_action_id")
            store.remove("app_${packageName}_gesture_${gesture.name}_action_config")
        }
    }

    fun getAppActionId(packageName: String, gesture: GestureType): String? =
        store.getString("app_${packageName}_gesture_${gesture.name}_action_id", null)

    fun setAppActionId(packageName: String, gesture: GestureType, actionId: String?) {
        val key = "app_${packageName}_gesture_${gesture.name}_action_id"
        if (actionId != null) store.putString(key, actionId) else store.remove(key)
    }

    fun getAppActionConfig(packageName: String, gesture: GestureType): String? =
        store.getString("app_${packageName}_gesture_${gesture.name}_action_config", null)

    fun setAppActionConfig(packageName: String, gesture: GestureType, config: String?) {
        val key = "app_${packageName}_gesture_${gesture.name}_action_config"
        if (config != null) store.putString(key, config) else store.remove(key)
    }

    // --- Per-Scene ---
    fun getSceneActionId(scene: SceneType, gesture: GestureType): String? =
        store.getString("scene_${scene.key}_gesture_${gesture.name}_action_id", null)

    fun setSceneActionId(scene: SceneType, gesture: GestureType, actionId: String?) {
        val key = "scene_${scene.key}_gesture_${gesture.name}_action_id"
        if (actionId != null) store.putString(key, actionId) else store.remove(key)
    }

    fun getSceneActionConfig(scene: SceneType, gesture: GestureType): String? =
        store.getString("scene_${scene.key}_gesture_${gesture.name}_action_config", null)

    fun setSceneActionConfig(scene: SceneType, gesture: GestureType, config: String?) {
        val key = "scene_${scene.key}_gesture_${gesture.name}_action_config"
        if (config != null) store.putString(key, config) else store.remove(key)
    }

    // --- Unified Resolution ---
    fun resolveActionId(gesture: GestureType, foregroundPkg: String?, activeScene: SceneType?): String? =
        when (dispatchMode) {
            DispatchMode.GLOBAL -> getActionId(gesture)
            DispatchMode.PER_APP -> {
                if (foregroundPkg != null && foregroundPkg in getConfiguredPackages()) {
                    getAppActionId(foregroundPkg, gesture) ?: getActionId(gesture)
                } else {
                    getActionId(gesture)
                }
            }
            DispatchMode.PER_SCENE -> {
                if (activeScene != null) {
                    getSceneActionId(activeScene, gesture) ?: getActionId(gesture)
                } else {
                    getActionId(gesture)
                }
            }
        }

    fun resolveActionConfig(gesture: GestureType, foregroundPkg: String?, activeScene: SceneType?): String? =
        when (dispatchMode) {
            DispatchMode.GLOBAL -> getActionConfig(gesture)
            DispatchMode.PER_APP -> {
                if (foregroundPkg != null && foregroundPkg in getConfiguredPackages()) {
                    getAppActionConfig(foregroundPkg, gesture) ?: getActionConfig(gesture)
                } else {
                    getActionConfig(gesture)
                }
            }
            DispatchMode.PER_SCENE -> {
                if (activeScene != null) {
                    getSceneActionConfig(activeScene, gesture) ?: getActionConfig(gesture)
                } else {
                    getActionConfig(gesture)
                }
            }
        }

    internal interface TestablePrefsStore : PrefsStore

    companion object {
        const val PREFS_NAME = "hyperzoomring_config"

        fun fromContext(context: Context): ConfigManager {
            val prefs = try {
                @Suppress("DEPRECATION")
                context.getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE)
            } catch (_: SecurityException) {
                // MODE_WORLD_READABLE requires Xposed/LSPosed environment;
                // fall back to private mode when running as normal app
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            }
            return ConfigManager(SharedPrefsStore(prefs))
        }

        fun fromYukiPrefs(prefs: YukiHookPrefsBridge): ConfigManager =
            ConfigManager(YukiPrefsStore(prefs))

        internal fun forTest(store: TestablePrefsStore): ConfigManager =
            ConfigManager(store)
    }
}

internal interface PrefsStore {
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
