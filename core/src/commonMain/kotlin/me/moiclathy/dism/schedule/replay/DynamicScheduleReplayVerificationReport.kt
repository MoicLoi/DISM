package me.moiclathy.dism.schedule.replay

/**
 * 12.28 / 13.15
 *
 * Historical Replay Verification 最终报告。
 *
 * ### 13.15：四个时间各自独立
 *
 * ```text
 * historicalEvaluatedAt  历史 Runtime 当时什么时候评估
 * replayedAt             这次重放什么时候执行
 * verifiedAt             这次校验什么时候执行
 * ```
 *
 * [verifiedAt] 是 13.15 补上的：
 * "重放跑完了"和"有人校验了重放结果"可能隔了很久，
 * 把它们挤成同一个时间点会让审计链少一环。
 */
data class DynamicScheduleReplayVerificationReport(
    val snapshotId: String,

    val status:
        DynamicScheduleReplayVerificationStatus,

    val historicalEngineVersion: String,

    val replayEngineVersion: String,

    val historicalEvaluatedAt:
        kotlin.time.Instant,

    val replayedAt:
        kotlin.time.Instant,

    /**
     * 13.15：本次校验真正执行的时间。
     */
    val verifiedAt:
        kotlin.time.Instant,

    val differences:
        List<DynamicScheduleReplayVerificationDifference> = emptyList(),

    val diagnostics:
        List<String> = emptyList(),
) {

    val verified: Boolean
        get() =
            status ==
                DynamicScheduleReplayVerificationStatus.EXACT_MATCH

    val changed: Boolean
        get() =
            status ==
                DynamicScheduleReplayVerificationStatus.BEHAVIOR_CHANGED

    val differenceCount: Int
        get() =
            differences.size
}
