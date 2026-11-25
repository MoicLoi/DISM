package me.moiclathy.dism.external.schedule.sync.e2e

enum class ExternalScheduleSyncE2EPhase {
    RECOVERY,
    RECONCILIATION,
    ACTION_MAPPING,
    IDEMPOTENCY,
    EVENT_ACTION,
    OBSERVATION,
    FINAL,
}
