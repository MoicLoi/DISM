package me.moiclathy.dism.schedule.history

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.replay.DynamicScheduleDecisionReplayInputSnapshotStore

/**
 * 13.9.5 / 13.9.11
 *
 * 默认 Repair 执行器。
 *
 * 它是**权限受限、默认保守**的：
 *
 * ```text
 * 可以：删除明确无父级对象的 orphan ReplayInput
 *
 * 绝不：重写 Snapshot
 *       重写 Trace
 *       重建历史 Trace
 *       修改历史 Revision
 *       修改历史 EngineVersion
 *       伪造 ReplayInput
 * ```
 *
 * 也就是说：`Snapshot` / `Trace` 是 DISM 的
 * **immutable historical evidence**，
 * 而 `ReplayInput` 更接近 **replay capability artifact**。
 *
 * 结果里 `executedActions` 在第一版基本为空，这是**故意的**：
 * 13.9 首先要建立的是"没有足够证据就不能修"，
 * 而不是"一定要修点什么"。
 */
class DefaultDynamicScheduleHistoryRepairExecutor(
    private val replayInputStore:
        DynamicScheduleDecisionReplayInputSnapshotStore,
) : DynamicScheduleHistoryRepairExecutor {

    override suspend fun execute(
        itemId: DynamicScheduleItemId,

        plan: DynamicScheduleHistoryRepairPlan,

        policy: DynamicScheduleHistoryRepairPolicy,
    ): DynamicScheduleHistoryRepairResult {

        /*
         * PLAN_ONLY 是默认安全模式：
         * 所有动作原样记成 skipped，一个字节都不写。
         */
        if (!policy.canExecute) {

            return DynamicScheduleHistoryRepairResult(
                itemId = itemId,

                plan = plan,

                skippedActions = plan.actions,

                diagnostics =
                    listOf(
                        "Repair execution disabled by PLAN_ONLY policy.",
                    ),
            )
        }

        val executed =
            mutableListOf<
                DynamicScheduleHistoryRepairAction
                >()

        val skipped =
            mutableListOf<
                DynamicScheduleHistoryRepairAction
                >()

        val diagnostics =
            mutableListOf<String>()

        plan.actions.forEach { action ->

            when (action) {

                DynamicScheduleHistoryRepairAction.NoOp -> {
                    /*
                     * 不需要执行，也不需要报告。
                     */
                }

                is DynamicScheduleHistoryRepairAction
                    .DeleteOrphanReplayInput -> {

                    if (!policy.allowOrphanReplayInputDeletion) {

                        skipped += action

                        diagnostics +=
                            "Orphan ReplayInput deletion disabled by policy."

                    } else {

                        /*
                         * 唯一被允许的写操作：
                         * 删除一个父 Snapshot 已经不存在的 ReplayInput。
                         *
                         * 它不会改变任何仍然存在的历史对象语义。
                         */
                        replayInputStore.delete(
                            action.snapshotId,
                        )

                        executed += action

                        diagnostics +=
                            "Deleted orphan ReplayInput for snapshot " +
                                action.snapshotId.value +
                                "."
                    }
                }

                is DynamicScheduleHistoryRepairAction
                    .PreserveMissingTrace -> {

                    skipped += action

                    diagnostics +=
                        "Missing Trace preserved because the original Trace cannot be reconstructed safely."
                }

                is DynamicScheduleHistoryRepairAction
                    .PreserveMissingReplayInput -> {

                    skipped += action

                    diagnostics +=
                        "Missing ReplayInput preserved because the original replay input cannot be reconstructed safely."
                }

                is DynamicScheduleHistoryRepairAction
                    .PreserveSnapshotWithoutTrace -> {

                    skipped += action

                    diagnostics +=
                        "Snapshot without Trace preserved as historical evidence."
                }

                is DynamicScheduleHistoryRepairAction
                    .PreserveConflict -> {

                    skipped += action

                    diagnostics +=
                        "Conflicting historical metadata was preserved and not overwritten."
                }
            }
        }

        return DynamicScheduleHistoryRepairResult(
            itemId = itemId,

            plan = plan,

            executedActions = executed,

            skippedActions = skipped,

            diagnostics = diagnostics,
        )
    }
}
