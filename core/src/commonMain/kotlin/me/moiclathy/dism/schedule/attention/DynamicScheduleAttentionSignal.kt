package me.moiclathy.dism.schedule.attention

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

data class DynamicScheduleAttentionSignal(
    val itemId: DynamicScheduleItemId,

    /**
     * Schedule 原始 priority。
     */
    val schedulePriority: Double,

    /**
     * 全局 Attention Memory 对 priority 的调整。
     */
    val globalMemoryFactor: Double,

    /**
     * Context Attention Memory 对 priority 的调整。
     */
    val contextMemoryFactor: Double,

    /**
     * Context Binding / dependency 产生的确定性信号。
     */
    val constraints:
        List<DynamicScheduleAttentionConstraint> =
        emptyList(),
) {
    init {
        require(schedulePriority in 0.0..1.0)
        require(globalMemoryFactor >= 0.0)
        require(contextMemoryFactor >= 0.0)
    }

    val hardConstraints: List<DynamicScheduleAttentionConstraint>
        get() = constraints.filter { it.isHard }

    val softConstraints: List<DynamicScheduleAttentionConstraint>
        get() = constraints.filterNot { it.isHard }
}
