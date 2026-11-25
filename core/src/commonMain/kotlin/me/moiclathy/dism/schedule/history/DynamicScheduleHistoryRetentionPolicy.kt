package me.moiclathy.dism.schedule.history

import kotlin.time.Duration

/**
 * 13.5
 *
 * 历史保留策略。
 *
 * 默认值刻意保守：
 *
 * ```text
 * 每个 item 最多 100 条
 * 时间不限
 * ```
 *
 * 而不是默认 `30.days`。
 *
 * 原因（13.5.7）：
 * "多少天之后可以删除用户历史"是**产品策略**，
 * 不应该由核心引擎偷偷替用户决定。
 * 核心只提供一个明确的旋钮。
 */
data class DynamicScheduleHistoryRetentionPolicy(
    /**
     * 每个 item 最多保留多少条 Snapshot。
     *
     * null = 不限制数量。
     */
    val maxSnapshotsPerItem: Int? = 100,

    /**
     * 最长保留时间。
     *
     * null = 不按时间删除。
     */
    val maxAge: Duration? = null,

    /**
     * 永远保护最新一条 Snapshot。
     *
     * 产品正常运行时必须保持 true：
     * 用户当前正在看的那次决策不能被压缩掉。
     */
    val preserveLatest: Boolean = true,
) {

    init {
        require(
            maxSnapshotsPerItem == null ||
                maxSnapshotsPerItem > 0,
        ) {
            "maxSnapshotsPerItem must be positive when present"
        }

        require(
            maxAge == null ||
                maxAge.isPositive(),
        ) {
            "maxAge must be positive when present"
        }

        /*
         * 两个都放开等于"从不压缩"，
         * 那不是保留策略而是一个容易被误用的陷阱，
         * 所以显式禁止。
         */
        require(
            maxSnapshotsPerItem != null ||
                maxAge != null,
        ) {
            "At least one retention limit must be configured."
        }
    }
}
