package me.moiclathy.dism.external.schedule.sync.reconciliation

/**
 * Event 存在性查询边界。
 *
 * 11.20 只需要知道：
 *
 * eventId 是否存在
 *
 * 不需要知道 Event 内部结构。
 */
fun interface ExternalScheduleSyncEventLookup {

    /**
     * 判断指定 Event 是否仍然存在。
     */
    fun exists(
        eventId:
            String,
    ):
        Boolean
}
