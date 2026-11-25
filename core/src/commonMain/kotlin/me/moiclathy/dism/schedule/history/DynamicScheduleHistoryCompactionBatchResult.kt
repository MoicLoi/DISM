package me.moiclathy.dism.schedule.history

/**
 * 13.5
 *
 * 一次批量压缩（多个 item）的结果。
 */
data class DynamicScheduleHistoryCompactionBatchResult(
    val results:
        List<DynamicScheduleHistoryCompactionResult>,
) {

    val snapshotsDeleted: Int
        get() =
            results.sumOf {
                it.snapshotsDeleted
            }

    val replayInputsDeleted: Int
        get() =
            results.sumOf {
                it.replayInputsDeleted
            }

    val tracesDeleted: Int
        get() =
            results.sumOf {
                it.tracesDeleted
            }

    val totalDeleted: Int
        get() =
            snapshotsDeleted +
                replayInputsDeleted +
                tracesDeleted

    val isEmpty: Boolean
        get() = totalDeleted == 0
}
