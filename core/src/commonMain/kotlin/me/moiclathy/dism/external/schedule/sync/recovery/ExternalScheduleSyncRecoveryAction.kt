package me.moiclathy.dism.external.schedule.sync.recovery

/**
 * 启动恢复时，对某条同步记录采取的动作。
 */
enum class ExternalScheduleSyncRecoveryAction {

    /**
     * 上一次创建失败，再次尝试创建。
     */
    RETRY_CREATE,

    /**
     * 上一次更新失败，再次尝试更新。
     */
    RETRY_UPDATE,

    /**
     * 上一次 Removed 处理失败，再次处理。
     */
    RETRY_REMOVAL,

    /**
     * Event mapping 丢失，需要重新创建映射。
     */
    RECREATE_EVENT_MAPPING,

    /**
     * 发现外部记录与当前状态不一致，需要重新对账。
     */
    RECONCILE,

    /**
     * 当前状态已经足够一致，不需要动作。
     */
    SKIP,
}
