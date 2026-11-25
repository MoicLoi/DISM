package me.moiclathy.dism.context.e2e

/**
 * E2E 中单个阶段的执行记录。
 *
 * 不保存具体 Domain Object，
 * 只保存这个阶段是否成功以及关键数值。
 */
data class ContextSemanticAttentionE2EStep(
    val phase: ContextSemanticAttentionE2EPhase,
    val successful: Boolean,
    val message: String,
)
