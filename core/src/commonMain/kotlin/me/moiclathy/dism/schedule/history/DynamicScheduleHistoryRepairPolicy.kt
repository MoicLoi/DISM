package me.moiclathy.dism.schedule.history

/**
 * 13.9
 *
 * Repair 执行策略。
 *
 * 三个默认值一起表达了同一件事：
 *
 * ```text
 * 没有足够证据，就不修。
 * ```
 *
 * `init` 里的两条 require 是**结构性的**：
 * `PLAN_ONLY` 与"允许破坏性操作"在语义上互斥，
 * 与其在运行期忽略其中一个，不如直接拒绝这种组合，
 * 免得留下"我明明设了 PLAN_ONLY，为什么删了数据"的隐患。
 */
data class DynamicScheduleHistoryRepairPolicy(
    val permission:
        DynamicScheduleHistoryRepairPermission =
        DynamicScheduleHistoryRepairPermission.PLAN_ONLY,

    /**
     * 是否允许删除已经明确确认是孤儿的 ReplayInput。
     *
     * 默认关闭。
     */
    val allowOrphanReplayInputDeletion: Boolean = false,

    /**
     * 是否允许对 BROKEN 历史执行任何写操作。
     *
     * 默认禁止，而且**当前版本没有任何代码路径会使用它**——
     * 它存在的意义是把"以后要不要允许"这件事变成一个
     * 显式的、需要有人主动打开的策略，而不是随手就修。
     */
    val allowBrokenHistoryMutation: Boolean = false,
) {

    init {
        if (
            permission ==
            DynamicScheduleHistoryRepairPermission.PLAN_ONLY
        ) {
            require(!allowOrphanReplayInputDeletion) {
                "PLAN_ONLY cannot enable destructive repair."
            }

            require(!allowBrokenHistoryMutation) {
                "PLAN_ONLY cannot mutate broken history."
            }
        }
    }

    /**
     * 是否允许真正写入。
     */
    val canExecute: Boolean
        get() =
            permission !=
                DynamicScheduleHistoryRepairPermission.PLAN_ONLY
}
