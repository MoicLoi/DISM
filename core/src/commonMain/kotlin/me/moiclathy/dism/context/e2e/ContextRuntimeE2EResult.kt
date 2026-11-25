package me.moiclathy.dism.context.e2e

/**
 * E2E 执行结果。
 */
data class ContextRuntimeE2EResult(
    val success: Boolean,

    val trace: ContextRuntimeE2ETrace,

    val failures: List<String> = emptyList(),
)
