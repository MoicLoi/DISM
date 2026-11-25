package me.moiclathy.dism.schedule.adaptation.persistence

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 14.11
 *
 * Adaptive Decision 的持久化端口。
 *
 * 与 13.x 的决策历史一致：
 *
 * ```text
 * 端口在 commonMain
 * 内存实现在 commonMain
 * SQLDelight 实现在 schedule/persistence/sqldelight
 * ```
 *
 * 这样测试不依赖数据库，而产品化装配只换实现。
 */
interface DynamicScheduleAdaptationPersistenceStore {

    suspend fun save(
        record: DynamicScheduleAdaptationPersistenceRecord,
    )

    suspend fun get(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleAdaptationPersistenceRecord?

    suspend fun delete(
        itemId: DynamicScheduleItemId,
    )

    suspend fun clear()
}
