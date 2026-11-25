package me.moiclathy.dism.schedule.migration

import me.moiclathy.dism.schedule.replay.DynamicScheduleDecisionReplayInputSnapshot

/**
 * 13.11.9
 *
 * 迁移结果。
 *
 * 设计稿的提醒是对的：**不要把无条件的 `error()` 暴露给 App**。
 *
 * ```text
 * 历史数据库缺迁移实现
 *        ↓
 * 直接抛异常 → 炸掉 Runtime
 * ```
 *
 * 而"这条历史没有现成迁移"其实是一个**正常结论**，
 * 应该被表达成结果，而不是让整个启动路径失败。
 */
sealed class DynamicScheduleHistoryMigrationResult {

    /**
     * 迁移成功。
     *
     * 注意 [input] 是**新工件**；
     * 原始 ReplayInput 不会被修改（见 [DynamicScheduleHistoryMigration]）。
     */
    data class Migrated(
        val input:
            DynamicScheduleDecisionReplayInputSnapshot,

        val migrationId: String,
    ) : DynamicScheduleHistoryMigrationResult() {

        init {
            require(migrationId.isNotBlank()) {
                "migrationId must not be blank"
            }
        }
    }

    /**
     * 没有可用的迁移路径。
     */
    data class NotAvailable(
        val reason: String,
    ) : DynamicScheduleHistoryMigrationResult() {

        init {
            require(reason.isNotBlank()) {
                "NotAvailable reason must not be blank"
            }
        }
    }
}
