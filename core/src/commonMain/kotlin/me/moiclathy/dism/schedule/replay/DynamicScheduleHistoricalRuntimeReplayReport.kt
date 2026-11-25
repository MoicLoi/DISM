package me.moiclathy.dism.schedule.replay

/**
 * 12.27
 *
 * Historical Runtime Replay 的最终报告。
 */
data class DynamicScheduleHistoricalRuntimeReplayReport(
    val snapshotId:
        String,

    val engineVersion:
        String,

    val evaluatedAt:
        kotlin.time.Instant,

    val replayedAt:
        kotlin.time.Instant,

    val historicalInputComplete:
        Boolean,

    val difference:
        DynamicScheduleHistoricalRuntimeReplayDifference,
) {

    val equivalent:
        Boolean
        get() =
            !difference.changed
}
