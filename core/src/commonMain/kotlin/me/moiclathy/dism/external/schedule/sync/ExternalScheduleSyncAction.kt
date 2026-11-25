package me.moiclathy.dism.external.schedule.sync

/**
 * Sync 对某条外部记录应该采取的动作。
 */
enum class ExternalScheduleSyncAction {

    /**
     * 新建内部 Event。
     */
    CREATE_EVENT,

    /**
     * 更新已有 Event。
     */
    UPDATE_EVENT,

    /**
     * 处理已经从外部课程表消失的 Event。
     *
     * 注意：
     * 这里不直接规定是 DELETE / CANCEL / ARCHIVE。
     * 具体行为由 Event 层决定。
     */
    HANDLE_REMOVED_EVENT,

    /**
     * Event 本身仍然存在，但 external key → Event 的映射丢失，
     * 需要重新建立映射。
     *
     * 11.20 Reconciliation 会在检测到
     * "mapping missing" 时给出这个动作，
     * 11.30 Action Executor 负责执行它。
     */
    RECREATE_EVENT_MAPPING,

    /**
     * 外部记录没有变化。
     */
    NO_OP,
}
