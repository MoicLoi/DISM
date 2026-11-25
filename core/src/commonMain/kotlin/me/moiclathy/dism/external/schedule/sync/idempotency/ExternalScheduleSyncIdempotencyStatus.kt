package me.moiclathy.dism.external.schedule.sync.idempotency

/**
 * 幂等状态。
 */
enum class ExternalScheduleSyncIdempotencyStatus {

    /**
     * 从未执行。
     */
    NEW,

    /**
     * 已经开始执行。
     */
    IN_PROGRESS,

    /**
     * 已经成功完成。
     */
    COMPLETED,

    /**
     * 上一次执行失败。
     */
    FAILED,
}
