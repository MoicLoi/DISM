package me.moiclathy.dism.context.e2e

/**
 * Context Runtime E2E 的阶段。
 */
enum class ContextRuntimeE2EPhase {

    SOURCE,

    RESOLUTION,

    TRANSITION,

    CONFIDENCE,

    RUNTIME_EVENT,

    PERSISTED,

    RECOVERY,

    FINAL_STATE,
}
