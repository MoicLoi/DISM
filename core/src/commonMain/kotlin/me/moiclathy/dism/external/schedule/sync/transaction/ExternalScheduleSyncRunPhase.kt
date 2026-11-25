package me.moiclathy.dism.external.schedule.sync.transaction

/**
 * Sync Run 所处阶段。
 */
enum class ExternalScheduleSyncRunPhase {

    CREATED,

    PLANNING,

    EXECUTING,

    PERSISTING,

    RECONCILING,

    RECOVERING,

    COMPLETED,
}
