package me.moiclathy.dism.schedule.history

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 13.9
 *
 * Repair 执行边界。
 *
 * 以后可以有多种实现：
 *
 * ```text
 * Default Executor
 * SQLDelight Executor
 * Migration Executor
 * Offline Recovery Executor
 * ```
 *
 * 但目前只需要一个默认实现。
 */
interface DynamicScheduleHistoryRepairExecutor {

    suspend fun execute(
        itemId: DynamicScheduleItemId,

        plan: DynamicScheduleHistoryRepairPlan,

        policy: DynamicScheduleHistoryRepairPolicy =
            DynamicScheduleHistoryRepairPolicy(),
    ): DynamicScheduleHistoryRepairResult
}
