package me.moiclathy.dism.platform

import android.content.Context

import me.moiclathy.dism.api.Dism
import me.moiclathy.dism.api.DismInternalApi
import me.moiclathy.dism.api.DismPublicApi
import me.moiclathy.dism.bootstrap.DismBootstrapper
import me.moiclathy.dism.bootstrap.DismPersistenceProvider
import me.moiclathy.dism.config.DismConfiguration
import me.moiclathy.dism.config.DismStorageConfiguration
import me.moiclathy.dism.config.DismStorageMode
import me.moiclathy.dism.persistence.DismPersistence
import me.moiclathy.dism.persistence.DismPersistenceFactory
import me.moiclathy.dism.persistence.driver.AndroidDatabaseDriverFactory

/**
 * 16.14：Android 的**落盘**入口。
 *
 * ```kotlin
 * val dism = AndroidDismFactory.create(
 *     context = applicationContext,
 *     databaseName = "dism",
 * )
 * ```
 *
 * 数据落在 `context.filesDir`（Android 的私有数据目录），
 * 由 `AndroidSqliteDriver` 自己决定具体路径。
 *
 * ### 与 [JvmDismFactory] 的差异是刻意的
 *
 * ```text
 * JvmDismFactory      收 File          桌面：应用自己决定路径
 * AndroidDismFactory  收 Context + 库名 Android：系统管路径，应用只给名字
 * ```
 *
 * 这不是不一致，而是两个平台的规矩不同：
 * Android 不允许应用随意选择数据库位置（那正是
 * "应用私有目录"要保证的事）。
 *
 * ### 为什么需要 Context
 *
 * `androidMain` 拿得到 `Context`，但 `commonMain` 不能——
 * 把 `Context` 塞进公共配置会立刻破坏 KMP。
 * 所以这个入口必须住在 `androidMain`。
 *
 * ### 调用方必须传 `applicationContext`
 *
 * 传 Activity 会导致 Activity 被 Dism 持有而无法回收。
 * 这里内部已经 `applicationContext` 兜底，
 * 但调用方仍应该一开始就传 application context。
 */
@DismPublicApi
object AndroidDismFactory {

    /**
     * 创建一个**真实落盘**的 DISM 实例。
     *
     * @param context 任意 Context；内部会取 applicationContext
     * @param databaseName 数据库名（不含路径）
     * @param configuration 装配配置
     */
    @OptIn(DismInternalApi::class)
    fun create(
        context: Context,
        databaseName: String = "dism",
        configuration: DismConfiguration =
            DismConfiguration(),
    ): Dism =
        DismBootstrapper(
            persistenceProvider =
                AndroidPersistenceProvider(
                    context = context,
                    databaseName = databaseName,
                ),
        ).create(
            configuration =
                normalize(configuration, databaseName),
        )

    /**
     * 强制 `LOCAL` 且库名一致。
     *
     * ### 为什么强制而不是校验
     *
     * 调 [create] 本身就是"我要落盘"的表达。若只是校验并放行，
     * 调用方漏设 `mode` 时最可能的结果是：
     *
     * ```text
     * 用户以为数据存下来了
     * 杀掉进程再打开：什么都没了
     * ```
     *
     * "看起来在落盘但其实是内存"比直接报错恶劣得多。
     *
     * 库名同步这件事同样重要：`databaseName` 参数与
     * `configuration.storage.databaseName` 是同一个概念的两种写法。
     * 如果两者不一致而配置里的那个生效，用户会拿到一个
     * 与他传入的名字不符的库——所以要显式对齐到参数。
     */
    private fun normalize(
        configuration: DismConfiguration,
        databaseName: String,
    ): DismConfiguration =
        configuration.copy(
            storage =
                configuration.storage.copy(
                    mode = DismStorageMode.LOCAL,
                    databaseName = databaseName,
                ),
        )
}

/**
 * 用 `context.filesDir` 支撑 `LOCAL`。
 *
 * `private` 是刻意的：这是装配细节，不构成公共面。
 */
@DismInternalApi
private class AndroidPersistenceProvider(
    context: Context,
    private val databaseName: String,
) : DismPersistenceProvider {

    private val appContext =
        context.applicationContext

    @OptIn(DismInternalApi::class)
    override fun create(
        configuration: DismStorageConfiguration,
    ): DismPersistence =
        DismPersistenceFactory(
            AndroidDatabaseDriverFactory(
                context = appContext,
                databaseName = databaseName,
            ),
        ).create()
}
