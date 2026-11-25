package me.moiclathy.dism.platform

import me.moiclathy.dism.api.Dism
import me.moiclathy.dism.api.DismInternalApi
import me.moiclathy.dism.config.DismConfiguration

/**
 * Default common platform entry point.
 *
 * This implementation is intentionally infrastructure-neutral.
 *
 * ### 为什么它叫 "Default" 而不是 "Common"
 *
 * 它不会假装自己知道在哪个平台上：
 *
 * ```text
 * platform = UNKNOWN
 * runtimeName = "common"
 * capabilities = emptySet()
 * ```
 *
 * 一个声称自己是 JVM 的 commonMain 版本会在 JS 构建里
 * 悄悄给出错误答案——`UNKNOWN` 和空集合才是诚实的。
 */
@DismInternalApi
class DefaultDismPlatformEntryPoint : DismPlatformEntryPoint {

    override val platform =
        DismPlatformInfo(
            platform = DismPlatform.UNKNOWN,
            runtimeName = "common",
        )

    override fun create(
        configuration: DismConfiguration,
    ): Dism {
        return me.moiclathy.dism.api.DismFactory.create(
            configuration,
        )
    }
}
