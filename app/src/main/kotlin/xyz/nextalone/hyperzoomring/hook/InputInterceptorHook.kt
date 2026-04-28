package xyz.nextalone.hyperzoomring.hook

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import android.view.InputDevice
import android.view.InputEvent
import android.view.MotionEvent
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.hook.type.java.IntType
import xyz.nextalone.hyperzoomring.action.ActionRegistry
import xyz.nextalone.hyperzoomring.action.actions.LaunchAppAction
import xyz.nextalone.hyperzoomring.config.ConfigManager
import xyz.nextalone.hyperzoomring.ring.ZoomRingConstants
import xyz.nextalone.hyperzoomring.ring.ZoomRingDetector
import xyz.nextalone.hyperzoomring.ring.ZoomRingEvent

@Suppress("DEPRECATION")
object InputInterceptorHook {
    private const val TAG = "HyperZoomRing"
    const val ACTION_ZOOM_RING_EVENT = "xyz.nextalone.hyperzoomring.ZOOM_RING_EVENT"
    const val EXTRA_TIMESTAMP = "timestamp"
    const val EXTRA_VALUE = "value"
    const val EXTRA_DIRECTION = "direction"
    const val EXTRA_GESTURE = "gesture"
    const val EXTRA_INTENSITY = "intensity"

    private val detector = ZoomRingDetector()
    private var sensorListener: ZoomRingSensorListener? = null
    private lateinit var config: ConfigManager

    private const val DIRECTION_STALE_MS = 500L

    @Volatile
    private var lastZoomRingEventMs: Long = 0
    private const val CAMERA_BLOCK_WINDOW_MS = 2000L

    /** Track which hook fired to avoid double-processing. */
    @Volatile
    private var lastProcessedEventTime: Long = 0

    /** Host reference from our pass-through InputFilter. */
    @Volatile
    private var inputFilterHost: Any? = null

    fun hook(param: PackageParam, config: ConfigManager) = with(param) {
        this@InputInterceptorHook.config = config
        Log.i(TAG, "Hooking InputManagerService + optical tracking sensor")

        // Start sensor listener
        val ctx = appContext
        if (ctx != null) {
            try {
                sensorListener = ZoomRingSensorListener(ctx).also { it.start() }
                Log.i(TAG, "Optical tracking sensor listener started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start sensor listener", e)
            }

            try {
                SceneDetector.init(ctx)
                Log.i(TAG, "SceneDetector initialized")
            } catch (e: Exception) {
                Log.w(TAG, "SceneDetector init failed", e)
            }
        }

        // Block camera launch from zoom ring
        try {
            "com.miui.server.input.util.ShortCutActionsUtils".toClass().hook {
                injectMember {
                    allMethods("launchCamera")
                    beforeHook {
                        if (!config.isEnabled) return@beforeHook
                        val age = System.currentTimeMillis() - lastZoomRingEventMs
                        if (age < CAMERA_BLOCK_WINDOW_MS) {
                            Log.i(TAG, "Blocked zoom ring camera launch (${age}ms after ring event)")
                            resultTrue()
                        }
                    }
                }.result {
                    onHookingFailure { Log.w(TAG, "Failed to hook launchCamera", it) }
                }
            }
            Log.i(TAG, "Hooked ShortCutActionsUtils.launchCamera")
        } catch (e: Exception) {
            Log.w(TAG, "ShortCutActionsUtils not found", e)
        }

        val imsClass = "com.android.server.input.InputManagerService".toClass()

        // Hook 1: filterInputEvent (works when InputFilter is registered)
        try {
            imsClass.hook {
                injectMember {
                    method {
                        name = "filterInputEvent"
                        param(InputEvent::class.java, IntType)
                    }
                    beforeHook {
                        val event = args(0).cast<InputEvent>() ?: return@beforeHook
                        if (handleZoomRingEvent(event, appContext, canConsume = true)) {
                            resultFalse()
                        }
                    }
                }.result {
                    onHookingFailure { Log.w(TAG, "filterInputEvent hook failed", it) }
                }
            }
            Log.i(TAG, "Hooked filterInputEvent")
        } catch (e: Exception) {
            Log.w(TAG, "filterInputEvent not available", e)
        }

        // Hook 2: injectInputEvent (fallback, always called)
        try {
            imsClass.hook {
                injectMember {
                    allMethods("injectInputEvent")
                    beforeHook {
                        val event = args.firstOrNull() as? InputEvent ?: return@beforeHook
                        if (handleZoomRingEvent(event, appContext, canConsume = true)) {
                            resultFalse()
                        }
                    }
                }.result {
                    onHookingFailure { Log.w(TAG, "injectInputEvent hook failed", it) }
                }
            }
            Log.i(TAG, "Hooked injectInputEvent")
        } catch (e: Exception) {
            Log.w(TAG, "injectInputEvent not available", e)
        }

        // Hook 3: dispatchUnhandledKey won't help for MotionEvent, but let's hook
        // interceptKeyBeforeQueueing to catch any KeyEvent-based zoom ring events
        try {
            imsClass.hook {
                injectMember {
                    allMethods("interceptKeyBeforeQueueing")
                    beforeHook {
                        val event = args.firstOrNull() as? InputEvent ?: return@beforeHook
                        val device = event.device ?: return@beforeHook
                        if (isZoomRingDevice(device)) {
                            Log.i(TAG, "ZoomRing KeyEvent intercepted via interceptKeyBeforeQueueing")
                            lastZoomRingEventMs = System.currentTimeMillis()
                        }
                    }
                }.result {
                    onHookingFailure { /* expected if signature differs */ }
                }
            }
        } catch (_: Exception) {}

        // CRITICAL: Register an InputFilter so that native InputDispatcher routes ALL
        // hardware events through filterInputEvent (Java). Without this, the hooks above
        // are never triggered because real hardware events bypass them entirely.
        registerPassThroughFilter()

        // Fullscreen detection removed — hooking layout-pass methods
        // (beginPostLayoutPolicyLw / applyPostLayoutPolicyLw) causes system_server
        // to hang due to per-frame reflection overhead. Fullscreen scene is disabled
        // until a low-cost detection method is found.
    }

    /**
     * Core event handler. Returns true if event was consumed (caller should block it).
     */
    private fun handleZoomRingEvent(event: InputEvent, context: Context?, canConsume: Boolean): Boolean {
        if (event !is MotionEvent) return false
        val device = event.device ?: return false
        if (!isZoomRingDevice(device)) return false

        val eventTime = event.eventTime
        if (eventTime == lastProcessedEventTime) return canConsume
        lastProcessedEventTime = eventTime
        lastZoomRingEventMs = System.currentTimeMillis()

        val foregroundPkg = getForegroundPackage(context)

        // Camera override: overrideCamera=false means camera always gets passthrough
        if (isCameraPackage(foregroundPkg) && !config.overrideCamera) return false

        val direction = getDirection()
        val scrollValue = event.getAxisValue(MotionEvent.AXIS_SCROLL).toInt()
        val zoomEvent = ZoomRingEvent(
            timestampMs = eventTime,
            value = scrollValue,
            direction = direction,
        )

        val gesture = detector.onEvent(zoomEvent)
        val intensity = detector.currentIntensity

        Log.i(TAG, "ZoomRing: scroll=$scrollValue dir=$direction gesture=$gesture intensity=%.2f".format(intensity))

        // Broadcast diagnostic event
        try {
            val ctx = context ?: return canConsume
            val intent = Intent(ACTION_ZOOM_RING_EVENT).apply {
                setPackage("xyz.nextalone.hyperzoomring")
                putExtra(EXTRA_TIMESTAMP, zoomEvent.timestampMs)
                putExtra(EXTRA_VALUE, scrollValue)
                putExtra(EXTRA_DIRECTION, direction)
                putExtra(EXTRA_GESTURE, gesture.name)
                putExtra(EXTRA_INTENSITY, intensity)
            }
            ctx.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send diagnostic broadcast", e)
        }

        if (!config.isEnabled) return canConsume

        // Scene detection
        val activeScene = context?.let { SceneDetector.detectActiveScene(it) }

        // Unified resolution
        val actionId = config.resolveActionId(gesture, foregroundPkg, activeScene) ?: return canConsume
        val action = ActionRegistry.get(actionId) ?: return canConsume

        if (action is LaunchAppAction) {
            LaunchAppAction.targetPackage = config.resolveActionConfig(gesture, foregroundPkg, activeScene)
        }

        val ctx = context ?: return canConsume
        try {
            action.execute(ctx, intensity)
        } catch (e: Exception) {
            Log.e(TAG, "Action execution failed: ${action.id}", e)
        }

        return canConsume
    }

    private fun getDirection(): Int {
        val listener = sensorListener ?: return 0
        val age = System.currentTimeMillis() - listener.lastSensorTimestampMs
        return if (age < DIRECTION_STALE_MS) listener.lastDirection else 0
    }

    private fun getForegroundPackage(context: Context?): String? {
        val ctx = context ?: return null
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return null
        val tasks = am.getRunningTasks(1)
        return tasks.firstOrNull()?.topActivity?.packageName
    }

    private fun isCameraPackage(packageName: String?): Boolean =
        packageName == "com.android.camera" || packageName == "com.xiaomi.camera"

    private fun isZoomRingDevice(device: InputDevice): Boolean {
        return device.name == ZoomRingConstants.DEVICE_NAME ||
            (device.vendorId == ZoomRingConstants.VENDOR_ID && device.productId == ZoomRingConstants.PRODUCT_ID)
    }

    /**
     * Register a pass-through IInputFilter to enable the native filterInputEvent callback.
     *
     * Without an InputFilter registered, InputDispatcher (native) never calls back into
     * InputManagerService.filterInputEvent for real hardware events — it dispatches them
     * directly to windows via InputChannel. By registering a pass-through filter, ALL
     * hardware events flow through filterInputEvent, where our [beforeHook] can intercept
     * zoom ring events and consume them before the filter re-injects the rest.
     *
     * Retries on a background thread because the "input" service may not be registered yet
     * when system_server boots.
     */
    private fun registerPassThroughFilter() {
        val thread = HandlerThread("ZoomRingInputFilter").apply { start() }
        val handler = Handler(thread.looper)
        handler.post { registerWithRetry(handler, retries = 30, delayMs = 2000L) }
    }

    private fun registerWithRetry(handler: Handler, retries: Int, delayMs: Long) {
        if (retries <= 0) {
            Log.e(TAG, "Gave up registering InputFilter after all retries")
            return
        }

        try {
            // Get IInputManager binder via ServiceManager
            val smClass = Class.forName("android.os.ServiceManager")
            val getService = smClass.getDeclaredMethod("getService", String::class.java)
            val binder = getService.invoke(null, "input") as? IBinder
            if (binder == null) {
                Log.w(TAG, "InputManagerService not ready yet, retrying in ${delayMs}ms ($retries left)")
                handler.postDelayed({ registerWithRetry(handler, retries - 1, delayMs) }, delayMs)
                return
            }

            val iimStubClass = Class.forName("android.hardware.input.IInputManager\$Stub")
            val asInterface = iimStubClass.getDeclaredMethod("asInterface", IBinder::class.java)
            val ims = asInterface.invoke(null, binder)
            if (ims == null) {
                Log.w(TAG, "IInputManager.Stub.asInterface returned null, retrying in ${delayMs}ms ($retries left)")
                handler.postDelayed({ registerWithRetry(handler, retries - 1, delayMs) }, delayMs)
                return
            }

            // Create a pass-through IInputFilter via dynamic proxy
            val iInputFilterClass = Class.forName("android.view.IInputFilter")
            val localBinder = android.os.Binder()

            val filter = java.lang.reflect.Proxy.newProxyInstance(
                iInputFilterClass.classLoader,
                arrayOf(iInputFilterClass)
            ) { _, method, args ->
                when (method.name) {
                    "asBinder" -> localBinder
                    "install" -> {
                        inputFilterHost = args?.firstOrNull()
                        Log.d(TAG, "Pass-through InputFilter installed")
                        null
                    }
                    "uninstall" -> {
                        inputFilterHost = null
                        Log.w(TAG, "Pass-through InputFilter uninstalled")
                        null
                    }
                    "filterInputEvent" -> {
                        // Pass every event through to the host so non-zoom-ring
                        // events continue to reach foreground windows normally.
                        val event = args?.get(0) as? InputEvent
                        val policyFlags = args?.get(1) as? Int ?: 0
                        val host = inputFilterHost
                        if (host != null && event != null) {
                            try {
                                val sendMethod = host.javaClass.getMethod(
                                    "sendInputEvent",
                                    InputEvent::class.java,
                                    Int::class.javaPrimitiveType
                                )
                                sendMethod.invoke(host, event, policyFlags)
                            } catch (e: Exception) {
                                Log.w(TAG, "InputFilter.sendInputEvent failed", e)
                            }
                        }
                        null
                    }
                    else -> null
                }
            }

            // Call InputManagerService.setInputFilter(filter)
            val setInputFilter = ims.javaClass.getMethod("setInputFilter", iInputFilterClass)
            setInputFilter.invoke(ims, filter)

            Log.i(TAG, "Registered pass-through InputFilter — events will now be intercepted")
        } catch (e: Exception) {
            Log.w(TAG, "InputFilter registration failed, retrying in ${delayMs}ms ($retries left)", e)
            handler.postDelayed({ registerWithRetry(handler, retries - 1, delayMs) }, delayMs)
        }
    }
}
