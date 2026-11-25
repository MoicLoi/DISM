package me.moiclathy.dism.schedule.replay

/**
 * 13.14.3 / 13.15.15
 */
data class DynamicScheduleHistoryReplayBatchResult(
    val entries:
        List<DynamicScheduleHistoryReplayBatchEntry>,

    /**
     * 13.15：这一批是什么时候、由哪个引擎跑起来的。
     *
     * 批次内所有条目共用同一个 `startedAt`——
     * 它表示"一次用户发起的历史重演操作"，
     * 而不是 N 次互不相关的操作。
     */
    val execution:
        DynamicScheduleReplayBatchExecutionContext,
) {

    val total: Int
        get() = entries.size

    val succeeded: Int
        get() =
            entries.count {
                it is DynamicScheduleHistoryReplayBatchEntry.Success
            }

    val failed: Int
        get() =
            entries.count {
                it is DynamicScheduleHistoryReplayBatchEntry.Failure
            }

    val isComplete: Boolean
        get() = failed == 0

    val successes:
        List<DynamicScheduleHistoryReplayBatchEntry.Success>
        get() =
            entries.filterIsInstance<
                DynamicScheduleHistoryReplayBatchEntry.Success,
                >()

    val failures:
        List<DynamicScheduleHistoryReplayBatchEntry.Failure>
        get() =
            entries.filterIsInstance<
                DynamicScheduleHistoryReplayBatchEntry.Failure,
                >()
}
