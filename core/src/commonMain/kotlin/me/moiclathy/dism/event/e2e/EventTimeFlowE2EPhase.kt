package me.moiclathy.dism.event.e2e

/**
 * Time-flow E2E 的完整阶段。
 */
enum class EventTimeFlowE2EPhase {

    EVENT,

    TEMPORAL,

    DURATION,

    CONSTRAINT,

    SLACK,

    ALARM,

    RUNTIME,

    PERSISTENCE,

    RECOVERY,

    FINAL,
}
