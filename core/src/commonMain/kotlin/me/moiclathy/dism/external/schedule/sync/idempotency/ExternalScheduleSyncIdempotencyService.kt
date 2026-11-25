package me.moiclathy.dism.external.schedule.sync.idempotency

import kotlin.time.Instant

/**
 * Sync 幂等服务。
 */
interface ExternalScheduleSyncIdempotencyService {

    /**
     * 尝试开始一次 Action。
     *
     * 返回：
     *
     * true
     * → 当前调用获得执行权
     *
     * false
     * → 已经存在可以复用的执行状态
     */
    fun begin(
        key:
            ExternalScheduleSyncIdempotencyKey,

        startedAt:
            Instant,
    ):
        ExternalScheduleSyncIdempotencyResult<Boolean>

    /**
     * 标记 Action 成功。
     */
    fun complete(
        key:
            ExternalScheduleSyncIdempotencyKey,

        completedAt:
            Instant,

        eventId:
            String? = null,
    ):
        ExternalScheduleSyncIdempotencyResult<
            ExternalScheduleSyncIdempotencyRecord
        >

    /**
     * 标记 Action 失败。
     */
    fun fail(
        key:
            ExternalScheduleSyncIdempotencyKey,

        completedAt:
            Instant,

        message:
            String,
    ):
        ExternalScheduleSyncIdempotencyResult<
            ExternalScheduleSyncIdempotencyRecord
        >

    /**
     * 获取当前状态。
     */
    fun find(
        key:
            ExternalScheduleSyncIdempotencyKey,
    ):
        ExternalScheduleSyncIdempotencyRecord?
}
