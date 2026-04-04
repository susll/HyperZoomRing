-keep class xyz.nextalone.hyperzoomring.HookEntry
-keep class * extends com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit

# KavaRef reflection API references classes not available on Android
-dontwarn java.lang.reflect.AnnotatedType
