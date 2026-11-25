package me.moiclathy.dism.schedule.history

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 13.5
 *
 * 单个 item 的一次压缩结果。
 *
 * 分开统计三类数据，而不是只报一个 total：
 * App / 诊断需要知道"到底删掉的是决策本身，还是可重放输入"。
 */
data class DynamicScheduleHistoryCompactionResult(
    val itemId: DynamicScheduleItemId,

    val snapshotsDeleted: Int,

    val replayInputsDeleted: Int,

    val tracesDeleted: Int,
) {

    init {
        require(snapshotsDeleted >= 0)
        require(replayInputsDeleted >= 0)
        require(tracesDeleted >= 0)
    }

    val totalDeleted: Int
        get() =
            snapshotsDeleted +
                replayInputsDeleted +
                tracesDeleted
}
