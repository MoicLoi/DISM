package me.moiclathy.dism.bootstrap

import me.moiclathy.dism.api.Dism
import me.moiclathy.dism.api.DismApiVersion
import me.moiclathy.dism.api.DismCapabilities
import me.moiclathy.dism.api.DismException
import me.moiclathy.dism.api.DismFailure
import me.moiclathy.dism.api.DismFailureCode
import me.moiclathy.dism.api.DismInternalApi
import me.moiclathy.dism.config.DismConfiguration
import me.moiclathy.dism.config.DismRuntimeConfiguration
import me.moiclathy.dism.config.DismStorageConfiguration
import me.moiclathy.dism.config.DismStorageMode
import me.moiclathy.dism.diagnostics.DismDiagnostics
import me.moiclathy.dism.diagnostics.DismDiagnosticSinks
import me.moiclathy.dism.persistence.DismPersistence
import me.moiclathy.dism.runtime.DismRuntimeApi
import me.moiclathy.dism.runtime.DismRuntimeFactory

/**
 * 15.4 / 15.5：DISM 的内部启动装配边界。
 *
 * ```text
 * DismConfiguration（应用描述"我需要什么"）
 *          ↓
 *     DismBootstrap（把配置变成具体基础设施）
 *          ↓
 *      DismRuntime
 *          ↓
 *       Dism API
 * ```
 *
 * ### 15.5 的关键修正：`LOCAL` 不许假装已经持久化
 *
 * 规格这一版对 `LOCAL` 的处理是把 commonMain 的
 * `createLocalPersistence` 直接写成返回 `DismPersistence()`
 * （也就是内存实现），并注明"以后由平台 bootstrap 绑定"。
 *
 * 那会产生一个非常糟糕的失败模式：
 *
 * ```text
 * 用户配置 LOCAL
 *     ↓
 * 系统默默给内存存储
 *     ↓
 * 重启后数据全没了
 *     ↓
 * 没有任何报错
 * ```
 *
 * "一个什么都不做的开关，比没有这个开关更糟"——
 * 所以这里改成：
 *
 * ```text
 * IN_MEMORY → 直接可用
 * LOCAL     → 必须有平台 Provider，否则**立刻报错**
 * ```
 *
 * 平台通过 [DismBootstrapper] 绑定 Provider。
 */
@DismInternalApi
object DismBootstrap {

    /**
     * 把公开的存储配置解析成具体的 Persistence 实现。
     */
    internal fun createPersistence(
        configuration: DismStorageConfiguration,

        provider: DismPersistenceProvider? = null,

        diagnostics: DismDiagnostics = DismDiagnostics(),
    ): DismPersistence {

        return when (configuration.mode) {

            DismStorageMode.IN_MEMORY -> {

                diagnostics.debug(
                    message =
                        "Resolved IN_MEMORY storage.",
                    component = "bootstrap",
                    operation = "createPersistence",
                    code = DismDiagnosticCodes.STORAGE_RESOLVED,
                )

                DismPersistence()
            }

            DismStorageMode.LOCAL ->
                createLocalPersistence(
                    configuration = configuration,
                    provider = provider,
                    diagnostics = diagnostics,
                )
        }
    }

    /**
     * LOCAL 必须由平台实现。
     *
     * 注意这里**不是** fallback 到内存，而是明确失败：
     * 一个"看起来成功、其实没落盘"的 LOCAL 比一个
     * 启动时就报错的 LOCAL 危险得多。
     *
     * 15.6：失败以 [DismException] + [DismFailure] 的形式抛出，
     * 所以 App 可以按 `code` 分支，而不是解析错误文本。
     *
     * 15.7：抛出之前先发一条 ERROR 诊断——
     * 这样接了 sink 的应用即使不 catch 也能在日志里看到。
     */
    private fun createLocalPersistence(
        configuration: DismStorageConfiguration,
        provider: DismPersistenceProvider?,
        diagnostics: DismDiagnostics,
    ): DismPersistence {

        if (provider == null) {

            val failure =
                DismFailure(
                    code =
                        DismFailureCode.UNSUPPORTED_CAPABILITY,

                    message =
                        "DismStorageMode.LOCAL requires a platform " +
                            "DismPersistenceProvider. " +
                            "commonMain cannot resolve a database path " +
                            "or driver on its own. " +
                            "Bind one through DismBootstrapper, " +
                            "or use DismStorageMode.IN_MEMORY.",

                    operation = "createPersistence",

                    details =
                        buildMap {
                            put(
                                "mode",
                                configuration.mode.name,
                            )

                            configuration.databaseName?.let {
                                put("databaseName", it)
                            }

                            put(
                                "storageMode",
                                DismStorageMode.LOCAL.name,
                            )
                        },
                )

            diagnostics.error(
                message = failure.message,
                component = "bootstrap",
                operation = "createPersistence",
                code = DismDiagnosticCodes.STORAGE_UNAVAILABLE,
                attributes = failure.details,
            )

            throw DismException(failure)
        }

        val persistence =
            provider.create(configuration)

        diagnostics.info(
            message = "Resolved LOCAL storage via platform provider.",
            component = "bootstrap",
            operation = "createPersistence",
            code = DismDiagnosticCodes.STORAGE_RESOLVED,
            attributes =
                buildMap {
                    configuration.databaseName?.let {
                        put("databaseName", it)
                    }
                },
        )

        return persistence
    }

    /**
     * 装配 Runtime 与 Dism 外观。
     */
    internal fun create(
        persistence: DismPersistence,
        configuration: DismConfiguration,
    ): Dism {

        val diagnostics =
            DismDiagnostics(
                sink =
                    if (configuration.diagnostics.enabled) {
                        configuration.diagnostics.sink
                    } else {
                        DismDiagnosticSinks.none
                    },
            )

        diagnostics.info(
            message = "DISM runtime is starting.",
            component = "bootstrap",
            operation = "create",
            code = DismDiagnosticCodes.BOOTSTRAP_STARTED,
        )

        val runtime =
            DismRuntimeFactory(
                persistence = persistence,
            ).create()

        diagnostics.info(
            message = "DISM runtime is ready.",
            component = "bootstrap",
            operation = "create",
            code = DismDiagnosticCodes.BOOTSTRAP_READY,
            attributes =
                mapOf(
                    "apiVersion" to DismApiVersion.STRING,
                ),
        )

        return BootstrappedDism(
            runtime = runtime,
            configuration = configuration.runtime,
        )
    }
}

/**
 * 15.7 §15.7.8：诊断 code 规范。
 *
 * 统一形态：
 *
 * ```text
 * DISM.<COMPONENT>.<EVENT>
 * ```
 *
 * 之所以用常量而不是散落的字符串字面量：
 * 日志聚合是按 code 分组的，一次拼写错误
 * 就会在监控里凭空多出一个"新事件"。
 */
@DismInternalApi
object DismDiagnosticCodes {

    const val BOOTSTRAP_STARTED: String =
        "DISM.BOOTSTRAP.STARTED"

    const val BOOTSTRAP_READY: String =
        "DISM.BOOTSTRAP.READY"

    const val STORAGE_RESOLVED: String =
        "DISM.STORAGE.RESOLVED"

    const val STORAGE_UNAVAILABLE: String =
        "DISM.STORAGE.UNAVAILABLE"
}

/**
 * 15.5：平台装配入口。
 *
 * ```kotlin
 * val dism =
 *     DismBootstrapper(
 *         persistenceProvider = JvmPersistenceProvider(dbFile),
 *     ).create(
 *         DismConfiguration(
 *             storage = DismStorageConfiguration(
 *                 mode = DismStorageMode.LOCAL,
 *                 databaseName = "dism",
 *             ),
 *         ),
 *     )
 * ```
 *
 * 它刻意**不**暴露给 [me.moiclathy.dism.api.DismFactory]：
 * 平台绑定属于下一层 assembly，而不是产品 API。
 */
@DismInternalApi
class DismBootstrapper(
    private val persistenceProvider:
        DismPersistenceProvider,
) {

    fun create(
        configuration: DismConfiguration =
            DismConfiguration(),
    ): Dism {

        val persistence =
            DismBootstrap.createPersistence(
                configuration = configuration.storage,
                provider = persistenceProvider,
            )

        return DismBootstrap.create(
            persistence = persistence,
            configuration = configuration,
        )
    }
}

/**
 * Dism 的默认实现。
 *
 * 刻意是 `private`：否则装配路径又会分裂成两条。
 *
 * ### capabilities 是**运行时广告**，不是静态宣传
 *
 * 既然 `DismRuntimeConfiguration` 可以关掉 attention，
 * 那么 `dism.capabilities.attention` 就必须如实变成 false。
 * 否则这个字段就只是贴在包装盒上的参数表。
 */
private class BootstrappedDism(
    override val runtime:
        DismRuntimeApi,

    private val configuration:
        DismRuntimeConfiguration,
) : Dism {

    override val apiVersion:
        String =
        DismApiVersion.STRING

    override val capabilities:
        DismCapabilities =
        DismCapabilities(
            semanticState = true,

            dynamicSchedule =
                configuration.enableDynamicSchedule,

            attention =
                configuration.enableAttention,

            persistence = true,

            replay = true,

            verification = true,

            adaptation =
                configuration.enableAdaptation,

            localFirst = true,
        )
}
