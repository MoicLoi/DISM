package me.moiclathy.dism.schedule.engine

/**
 * 13.11.8
 *
 * "这条历史该怎么处理"的**执行计划**。
 *
 * 把版本策略细节收在这里，上层的 Replay 消费者只需要认三种结果：
 *
 * ```text
 * DirectReplay         → 直接重放
 * ReplayAfterMigration → 先迁移再重放
 * Blocked              → 停，并说明原因
 * ```
 *
 * 于是调用方不必知道
 * EXACT / REPLAY_COMPATIBLE / MIGRATABLE / INCOMPATIBLE / UNKNOWN
 * 这五个枚举的分支关系。
 */
sealed class DynamicScheduleEngineExecutionPlan {

    /**
     * 历史版本可以直接交给当前 Replay Engine。
     */
    data object DirectReplay :
        DynamicScheduleEngineExecutionPlan()

    /**
     * 需要先迁移 ReplayInput，再重放。
     */
    data class ReplayAfterMigration(
        val migrationId: String,
    ) : DynamicScheduleEngineExecutionPlan() {

        init {
            require(migrationId.isNotBlank()) {
                "migrationId must not be blank"
            }
        }
    }

    /**
     * 不能重放，并带有面向人的原因。
     */
    data class Blocked(
        val reason: String,
    ) : DynamicScheduleEngineExecutionPlan() {

        init {
            require(reason.isNotBlank()) {
                "Blocked reason must not be blank"
            }
        }
    }

    val canProceed: Boolean
        get() = this !is Blocked
}
