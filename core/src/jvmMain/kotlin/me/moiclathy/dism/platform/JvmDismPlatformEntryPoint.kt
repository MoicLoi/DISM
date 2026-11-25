package me.moiclathy.dism.platform

import me.moiclathy.dism.api.Dism
import me.moiclathy.dism.api.DismInternalApi
import me.moiclathy.dism.config.DismConfiguration

/**
 * JVM DISM entry point.
 *
 * ### 为什么它在 `jvmMain` 而不是 `commonMain`
 *
 * 它读 `System.getProperty("java.version")`，
 * 那是 JVM-only API——放进 commonMain 会直接让
 * JS / Wasm / iOS 编译不过。
 *
 * 规格的目录树把它列在 commonMain 下，是树的推断结果；
 * 真实约束由编译器决定。
 */
@DismInternalApi
class JvmDismPlatformEntryPoint : DismPlatformEntryPoint {

    override val platform =
        DismPlatformInfo(
            platform = DismPlatform.JVM,

            runtimeName = "JVM",

            runtimeVersion =
                System.getProperty("java.version"),

            /*
             * 只有真的具备的能力才声明，
             * 见 DismPlatformInfo 的说明。
             */
            capabilities =
                setOf(
                    DismPlatformCapability.LOCAL_STORAGE,
                    DismPlatformCapability.FILE_SYSTEM,
                    DismPlatformCapability.NETWORK,
                    DismPlatformCapability.BACKGROUND_EXECUTION,
                ),
        )

    override fun create(
        configuration: DismConfiguration,
    ): Dism {

        /*
         * 15.9 先委托给公共 bootstrap。
         *
         * 具体的 JVM 存储绑定会在不把数据库实现暴露给应用
         * 的前提下接入——那属于 15.10 之后的工作。
         */
        return me.moiclathy.dism.api.DismFactory.create(
            configuration,
        )
    }
}
