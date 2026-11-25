package me.moiclathy.dism.schedule.replay

import kotlin.time.Instant

/**
 * 13.15.18
 *
 * 一次重放的**执行标识**。
 *
 * 它回答的是"哪一次执行"：
 *
 * ```text
 * snapshotId
 * engineVersion
 * replayedAt
 * ```
 *
 * ### 为什么不把它叫 fingerprint
 *
 * 因为它**天然**会随执行时间变化：
 *
 * ```text
 * 同一个历史 Decision
 * Replay at 14:00  → 一个执行标识
 * Replay at 15:00  → 另一个执行标识
 * ```
 *
 * 这是对的——它们确实是两次不同的执行。
 *
 * 但如果把 [replayedAt] 也拿去做"行为是否改变"的判断，
 * 就会得到"每次重放行为都变了"这种毫无意义的结论。
 *
 * 所以这里把它明确命名为**执行标识**，
 * 与 [DynamicScheduleReplayBehaviorFingerprint] 严格分开：
 *
 * ```text
 * ReplayExecutionId      → 哪一次执行
 * ReplayBehaviorFingerprint → 行为是否相同
 * ```
 */
data class DynamicScheduleReplayExecutionId(
    val snapshotId: String,

    val engineVersion: String,

    val replayedAt: Instant,

    /**
     * 结果内容的指纹。
     *
     * 只由重放结果决定，不含时间。
     */
    val resultFingerprint: String,
) {

    init {
        require(snapshotId.isNotBlank())
        require(engineVersion.isNotBlank())
        require(resultFingerprint.isNotBlank())
    }
}
