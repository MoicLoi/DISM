package me.moiclathy.dism.context.e2e

/**
 * 9.15 E2E 执行结果。
 */
data class ContextSemanticAttentionE2EResult(
    val trace: ContextSemanticAttentionE2ETrace,

    val contractSatisfied: Boolean,

    val errors: List<String> = emptyList(),
)
