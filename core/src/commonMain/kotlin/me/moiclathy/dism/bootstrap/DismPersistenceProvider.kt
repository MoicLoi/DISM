package me.moiclathy.dism.bootstrap

import me.moiclathy.dism.api.DismInternalApi
import me.moiclathy.dism.config.DismStorageConfiguration
import me.moiclathy.dism.persistence.DismPersistence

/**
 * 15.5：平台持久化 Provider。
 *
 * Android / JVM / iOS / Server 各自实现它，
 * 而**不需要**改动任何 Public API。
 *
 * ```text
 * DismConfiguration（应用说"我要 LOCAL"）
 *          ↓
 * DismPersistenceProvider（平台决定"LOCAL 到底是什么"）
 *          ↓
 *     DismPersistence
 * ```
 *
 * ### 为什么必须存在这一层
 *
 * commonMain 没有可靠的 `File` / `Path` / SQLite driver /
 * Android `Context` / iOS Application Support。
 *
 * 把这些塞进 commonMain 会立刻破坏 KMP；
 * 而"假装 LOCAL 已经可用"会让用户在真实设备上
 * 默默丢掉全部数据。
 *
 * 所以这里把选择权交给平台：
 *
 * ```kotlin
 * // jvmMain
 * class JvmPersistenceProvider(val file: File) : DismPersistenceProvider {
 *     override fun create(configuration: DismStorageConfiguration) =
 *         DismPersistenceFactory(JvmDatabaseDriverFactory(file)).create()
 * }
 * ```
 */
@DismInternalApi
interface DismPersistenceProvider {

    fun create(
        configuration: DismStorageConfiguration,
    ): DismPersistence
}
