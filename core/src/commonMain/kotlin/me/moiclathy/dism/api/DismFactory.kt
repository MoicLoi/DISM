package me.moiclathy.dism.api

import me.moiclathy.dism.bootstrap.DismBootstrap
import me.moiclathy.dism.config.DismConfiguration
import me.moiclathy.dism.config.DismStorageMode

/**
 * 15.1 / 15.4 / 15.5：DISM Public Factory。
 *
 * ```kotlin
 * // 最简单：默认配置（内存存储）
 * val dism = DismFactory.create()
 *
 * // 描述需求，而不是描述数据库
 * val dism = DismFactory.create(
 *     DismConfiguration(
 *         storage = DismStorageConfiguration(
 *             mode = DismStorageMode.LOCAL,
 *             databaseName = "dism",
 *         ),
 *     ),
 * )
 *
 * dism.runtime.observe(...)
 * dism.runtime.dynamicSchedule().evaluate(...)
 * ```
 *
 * ### 15.4：这里不再出现任何 Persistence 类型
 *
 * 15.3 结束时签名还是：
 *
 * ```kotlin
 * fun create(persistenceFactory: DismPersistenceFactory): Dism
 * ```
 *
 * 也就是 App 仍然必须理解 `DismPersistenceFactory`、
 * `DatabaseDriverFactory`、SQLDelight 这一整条链路。
 *
 * ### 15.5：应用描述"我需要什么"，不描述"怎么建库"
 *
 * [DismConfiguration] 里只有"要不要持久化""要不要 attention"，
 * 没有路径、没有 driver、没有 SQL。
 * 具体的数据库实现由平台 bootstrap 决定。
 *
 * ### 一个 Public API 的签名不能暴露 internal 类型
 *
 * 所以不能写成 `create(persistence: DismPersistence)`。
 * 但实现内部可以引用它——见 [DismBootstrap]。
 *
 * ### 注意 `LOCAL` 需要平台绑定
 *
 * commonMain 无法自己解析数据库路径与 driver，
 * 所以请求 `LOCAL` 却没有平台 Provider 时会**直接报错**，
 * 而不是悄悄退化成内存存储。
 */
@DismPublicApi
object DismFactory {

    /**
     * 使用默认配置创建一个 DISM 实例。
     *
     * 等价于 [create] 传入
     * [me.moiclathy.dism.config.DismConfigurationDefaults.standard]。
     */
    fun create(): Dism =
        create(
            DismConfiguration(),
        )

    /**
     * 使用给定配置创建 DISM 实例。
     *
     * 适用于：
     *
     * - Demo
     * - 单元测试
     * - 临时运行
     * - 无持久化需求场景
     *
     * 需要真实落盘时请在平台层使用 `DismBootstrapper` 绑定
     * `DismPersistenceProvider`，否则 `DismStorageMode.LOCAL`
     * 会明确失败。
     */
    fun create(
        configuration: DismConfiguration,
    ): Dism {

        /*
         * 诊断 sink 要在**解析存储之前**就接上，
         * 否则 LOCAL 失败时那条 ERROR 诊断就发不出去。
         */
        val diagnostics =
            me.moiclathy.dism.diagnostics.DismDiagnostics(
                sink =
                    if (configuration.diagnostics.enabled) {
                        configuration.diagnostics.sink
                    } else {
                        me.moiclathy.dism.diagnostics
                            .DismDiagnosticSinks.none
                    },
            )

        return DismBootstrap.create(
            persistence =
                DismBootstrap.createPersistence(
                    configuration = configuration.storage,
                    provider = null,
                    diagnostics = diagnostics,
                ),
            configuration = configuration,
        )
    }
}

/**
 * 内部一致性检查：默认配置必须走内存模式。
 *
 * 这只是一个编译期可见的常量，不参与运行时逻辑；
 * 它让"默认配置 = 内存"这件事在代码里显式可见，
 * 而不是藏在 `DismConfiguration` 的默认参数里。
 */
private val defaultStorageMode: DismStorageMode =
    DismConfiguration().storage.mode
