package me.moiclathy.dism.schedule.migration

import me.moiclathy.dism.schedule.replay.DynamicScheduleDecisionReplayInputSnapshot
import me.moiclathy.dism.schedule.version.DynamicScheduleHistoryEngineVersion

/**
 * 13.10.11
 *
 * 历史 ReplayInput 的迁移。
 *
 * ### 迁移的是 ReplayInput，不是 Snapshot / Trace
 *
 * ```text
 * Snapshot / Trace  = 历史证据     → 永远不迁移、不覆盖
 * ReplayInput       = 重放输入工件 → 可以生成新版本
 * ```
 *
 * ### 迁移不覆盖原始历史
 *
 * ```text
 * 13.1 ReplayInput
 *       │
 *       ▼ Migration（返回新对象）
 * 13.10 ReplayInput
 *
 * 原始对象保持原样 → Original + Migrated Artifact 并存
 * ```
 *
 * 所以 [migrate] 是**纯函数**：它拿一个 ReplayInput，返回一个新的，
 * 不接触任何 Store。这样"迁移"在结构上就不可能改坏原始数据。
 *
 * 至于要不要把迁移结果持久化成第二套产物
 * （`DynamicScheduleDecisionReplayInputMigrationRecord`），
 * 13.10 只定义边界，不急着加数据库历史表。
 */
interface DynamicScheduleHistoryMigration {

    val migrationId: String

    val fromVersion:
        DynamicScheduleHistoryEngineVersion

    val toVersion:
        DynamicScheduleHistoryEngineVersion

    fun migrate(
        input:
            DynamicScheduleDecisionReplayInputSnapshot,
    ): DynamicScheduleDecisionReplayInputSnapshot
}
