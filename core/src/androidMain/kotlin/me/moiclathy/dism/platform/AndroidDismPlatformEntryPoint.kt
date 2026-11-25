package me.moiclathy.dism.platform

import android.content.Context

import me.moiclathy.dism.api.Dism
import me.moiclathy.dism.api.DismInternalApi
import me.moiclathy.dism.config.DismConfiguration

/**
 * Android DISM entry point.
 *
 * ### 关于 `Context` 的命名陷阱
 *
 * 这里导入的是 `android.content.Context`。
 * DISM 自己也有一个 `me.moiclathy.dism.context.Context`
 * （语义上下文），两者完全不同。
 *
 * 所以本文件**刻意不**导入 DISM 的 Context——
 * 一旦同时导入，编译器会因为简单名冲突而报错，
 * 那正是我们想要的：让这处歧义在编译期暴露。
 *
 * ### 它为什么接收 `applicationContext`
 *
 * Android 的持久化路径必须由平台解析
 * （`context.filesDir` / `getDatabasePath`），
 * 而 commonMain 完全不知道这些。
 *
 * 传 `applicationContext` 而不是 Activity：entry point 的
 * 生命周期比任何 Activity 都长，持有 Activity 会泄漏。
 */
@DismInternalApi
class AndroidDismPlatformEntryPoint(
    private val applicationContext: Context,
) : DismPlatformEntryPoint {

    override val platform =
        DismPlatformInfo(
            platform = DismPlatform.ANDROID,

            runtimeName = "Android",

            runtimeVersion =
                android.os.Build.VERSION.RELEASE,

            capabilities =
                setOf(
                    DismPlatformCapability.LOCAL_STORAGE,
                    DismPlatformCapability.FILE_SYSTEM,
                    DismPlatformCapability.NETWORK,
                    DismPlatformCapability.BACKGROUND_EXECUTION,
                    DismPlatformCapability.NOTIFICATIONS,
                ),
        )

    /**
     * 供平台层读取应用私有目录。
     *
     * 暴露的是 `File` 的绝对路径而不是 `Context` 本身，
     * 这样调用方不需要为了拿一个路径而持有 Android 类型。
     */
    fun applicationFilesDirectory(): String =
        applicationContext.filesDir.absolutePath

    override fun create(
        configuration: DismConfiguration,
    ): Dism {

        /*
         * 15.9 先委托给公共 bootstrap。
         *
         * Android 的 SQLDelight driver 绑定会在
         * 不把数据库实现暴露给应用的前提下接入。
         */
        return me.moiclathy.dism.api.DismFactory.create(
            configuration,
        )
    }
}
