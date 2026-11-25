package me.moiclathy.dism.schedule.trace

/**
 * 13.2
 *
 * Trace 的输入不再是 Runtime Result，
 * 而是 Runtime 已经算好的 [DynamicScheduleDecisionTraceEvidence]。
 *
 * 这样 `schedule.trace` 包不再 import `schedule.runtime.*`，
 * 循环依赖消失。
 *
 * `conflictMessage` / `explanationMessage` 仍然由 Runtime 注入：
 * Trace 只负责"记录"，不负责判断某个 Conflict 重不重要。
 */
data class DynamicScheduleDecisionTraceInput(
    val evidence:
        DynamicScheduleDecisionTraceEvidence,

    /**
     * 允许上层 Runtime 注入额外的冲突信息。
     */
    val conflictMessage: String? = null,

    val conflictDecisive: Boolean = false,

    /**
     * 允许上层 Runtime 注入解释摘要。
     */
    val explanationMessage: String? = null,
)
