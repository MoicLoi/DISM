package me.moiclathy.dism.external.schedule.sync.reconciliation

enum class ExternalScheduleSyncRecoveryReconciliationStatus {
    RECONCILED,
    RECONCILED_WITH_WARNINGS,
    RECOVERY_EMPTY,
    PARTIAL,
    FAILED,
}
