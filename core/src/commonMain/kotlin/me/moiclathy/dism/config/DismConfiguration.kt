package me.moiclathy.dism.config

import me.moiclathy.dism.api.DismPublicApi
import me.moiclathy.dism.diagnostics.DismDiagnosticsConfiguration

/**
 * Public configuration surface for DISM.
 *
 * 15.5 的原则：
 *
 * Application 只描述“我需要什么”，
 * 不描述“DISM 应该如何创建数据库”。
 */
@DismPublicApi
data class DismConfiguration(
    val storage: DismStorageConfiguration =
        DismStorageConfiguration(),

    val runtime: DismRuntimeConfiguration =
        DismRuntimeConfiguration(),

    /**
     * 15.7：诊断事件出口。
     *
     * 默认 `enabled = true` 但 sink 是 `none`——
     * 也就是"允许诊断、但不落地"。
     *
     * 这样应用只要提供一个 sink 就能立刻拿到事件，
     * 不必再额外改一个开关。
     */
    val diagnostics: DismDiagnosticsConfiguration =
        DismDiagnosticsConfiguration(),
)

/**
 * Persistence behavior requested by the application.
 *
 * The actual database implementation is selected by the
 * platform bootstrap layer.
 */
@DismPublicApi
data class DismStorageConfiguration(
    val mode: DismStorageMode =
        DismStorageMode.IN_MEMORY,

    /**
     * Optional logical database name.
     *
     * This is deliberately NOT a filesystem path.
     *
     * Platform-specific path resolution belongs to the
     * platform bootstrap implementation.
     */
    val databaseName: String? = null,
)

/**
 * Supported storage modes.
 */
@DismPublicApi
enum class DismStorageMode {

    /**
     * Non-persistent runtime.
     *
     * Useful for:
     * - tests
     * - previews
     * - temporary sessions
     */
    IN_MEMORY,

    /**
     * Persistent local storage.
     *
     * The platform decides the concrete driver.
     */
    LOCAL,
}

/**
 * Runtime-level configuration.
 *
 * Keep this intentionally small in 15.5.
 * Intelligence policy configuration belongs to later
 * configuration work instead of leaking internal engines here.
 */
@DismPublicApi
data class DismRuntimeConfiguration(
    val enableAttention: Boolean = true,
    val enableDynamicSchedule: Boolean = true,
    val enableAdaptation: Boolean = true,
)
