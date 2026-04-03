package xyz.nextalone.hyperzoomring.hook

import android.util.Log
import com.highcapable.yukihookapi.hook.param.PackageParam

object InputInterceptorHook {

    private const val TAG = "HyperZoomRing"

    fun hook(param: PackageParam) = with(param) {
        Log.i(TAG, "InputInterceptorHook loaded in system_server")
    }
}
