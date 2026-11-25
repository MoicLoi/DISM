package me.moiclathy.dism.platform

import java.io.File

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
import me.moiclathy.dism.persistence.driver.JvmDatabaseDriverFactory

/**
 * 16.14：JVM / 桌面端的**落盘**入口。
 *
 * ```kotlin
 * val dism = JvmDismFactory.create(
 *     databaseFile = File(System.getProperty("user.home"), ".dism/dism.db"),
 * )
 *
 * // 或者只要描述需求，让装配沿用传入的 configuration
 * val dism = JvmDismFactory.create(
 *     databaseFile = file,
 *     configuration = DismConfiguration(
 *         storage = DismStorageConfiguration(
 *             mode = DismStorageMode.LOCAL,
 *             databaseName = "dism",
 *         ),
 *     ),
 * )
 * ```
 *
 * ### 为什么它必须存在
 *
 * 在加它之前，**应用无法落盘**：
 *
 * ```text
 * DismFactory.create(config)          provider = null → LOCAL 直接抛错
 * DismBootstrapper(provider)          provider 要实现 DismPersistenceProvider
 * DismPersistenceProvider.create()    返回 DismPersistence
 * DismPersistence                     :core 的 internal 类型
 * ```
 *
 * 最后一行是死结：`DismPersistence` 是 internal，所以
 * **`:app:desktopApp` 里无法实现 `DismPersistenceProvider`**。
 * 平台绑定看起来"只是少写一个类"，实际上是做不到的。
 *
 * 所以绑定必须发生在 `:core` **内部**、但由平台源集
 * 暴露一个公共入口——也就是这个对象。
 *
 * ### 它为什么没有泄露内部实现
 *
 * 公共签名里只有：
 *
 * ```text
 * File / DismConfiguration / Dism
 * ```
 *
 * 没有 `DismPersistence`、没有 `DismPersistenceProvider`、
 * 没有 SQLDelight、没有 driver。内部装配细节被完全关在这个文件里，
 * 这也是它必须放在 `jvmMain` 而不是 commonMain 的原因
 * （commonMain 没有 `File`，也无法选择 driver）。
 *
 * ### 为什么不改 `DismFactory.create`
 *
 * 因为 `DismFactory` 在 commonMain，而"数据库文件在哪"
 * 是平台问题。把 `File` 塞进公共配置会立刻破坏 KMP。
 *
 * ```text
 * DismFactory      跨平台：我要 LOCAL
 * JvmDismFactory   平台：LOCAL 在这个平台上到底是什么
 * ```
 */
@DismPublicApi
object JvmDismFactory {

    /**
     * 创建一个**真实落盘**的 DISM 实例。
     *
     * @param databaseFile SQLite 文件路径；父目录会被自动创建
     * @param configuration 装配配置
     */
    @OptIn(DismInternalApi::class)
    fun create(
        databaseFile: File,
        configuration: DismConfiguration =
            DismConfiguration(),
    ): Dism =
        DismBootstrapper(
            persistenceProvider =
                JvmFilePersistenceProvider(databaseFile),
        ).create(
            configuration =
                normalize(configuration),
        )

    /**
     * 强制 storage 为 `LOCAL`。
     *
     * ### 为什么是强制而不是校验
     *
     * 调 [JvmDismFactory.create] 本身就是"我要落盘"的表达。
     * 如果调用方忘了把 `mode` 设成 `LOCAL`，而这里只是**校验**，
     * 那么最可能的结果是：
     *
     * ```text
     * 用户以为数据存下来了
     * 关掉 App 再看：什么都没了
     * ```
     *
     * 一个"看起来在落盘但其实是内存"的状态，比直接报错恶劣得多。
     * 所以这里按调用意图纠正配置，而不是让它静默退化成内存。
     */
    private fun normalize(
        configuration: DismConfiguration,
    ): DismConfiguration =
        if (
            configuration.storage.mode ==
            DismStorageMode.LOCAL
        ) {
            configuration
        } else {
            configuration.copy(
                storage =
                    configuration.storage.copy(
                        mode = DismStorageMode.LOCAL,
                    ),
            )
        }
}

/**
 * 用文件支撑 `LOCAL`。
 *
 * `private` 是刻意的：这是装配细节，不构成公共面。
 */
@DismInternalApi
private class JvmFilePersistenceProvider(
    private val databaseFile: File,
) : DismPersistenceProvider {

    @OptIn(DismInternalApi::class)
    override fun create(
        configuration: DismStorageConfiguration,
    ): DismPersistence =
        DismPersistenceFactory(
            JvmDatabaseDriverFactory(databaseFile),
        ).create()
}
