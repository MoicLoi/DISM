package me.moiclathy.dism.external.schedule.sync.reconciliation

/**
 * Reconciliation 对当前状态采取的动作。
 */
enum class ExternalScheduleSyncReconciliationAction {

    /**
     * 什么都不做。
     */
NO_OP,

    /**
     * Event 已存在，只需要补齐 Sync COMPLETED。
     */
COMPLETE_SYNC,

    /**
     * Event 不存在，需要重新执行 CREATE。
     */
RETRY_CREATE,

    /**
     * Event 不存在，需要重新执行 UPDATE。
     */
RETRY_UPDATE,

    /**
     * Removed Event 操作需要重新执行。
     */
RETRY_REMOVAL,

    /**
     * Event mapping 已丢失，需要重新建立。
     */
RECREATE_EVENT_MAPPING,
}
