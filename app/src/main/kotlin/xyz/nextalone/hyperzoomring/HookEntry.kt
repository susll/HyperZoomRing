package xyz.nextalone.hyperzoomring

import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import xyz.nextalone.hyperzoomring.hook.InputInterceptorHook

@InjectYukiHookWithXposed
object HookEntry : IYukiHookXposedInit {

    override fun onHook() = encase {
        loadSystem {
            InputInterceptorHook.hook(this)
        }
    }
}
