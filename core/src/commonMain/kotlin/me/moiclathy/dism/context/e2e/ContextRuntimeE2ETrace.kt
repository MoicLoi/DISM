package me.moiclathy.dism.context.e2e

import me.moiclathy.dism.context.runtime.ContextRuntimeEvent
import me.moiclathy.dism.context.runtime.ContextRuntimeState

/**
 * Context Runtime E2E 执行轨迹。
 *
 * 主要用于测试和诊断。
 */
data class ContextRuntimeE2ETrace(
    val phases:
        List<ContextRuntimeE2EPhase> = emptyList(),

    val events:
        List<ContextRuntimeEvent> = emptyList(),

    val recoveredState:
        ContextRuntimeState? = null,
) {

    fun contains(
        phase: ContextRuntimeE2EPhase,
    ): Boolean {
        return phase in phases
    }
}
