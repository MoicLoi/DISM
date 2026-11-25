package me.moiclathy.dism.schedule.adaptation.replay

import me.moiclathy.dism.schedule.adaptation.DynamicSchedulePriorityAdaptation

/**
 * 14.12
 *
 * 一次 Adaptation Replay 的结果。
 */
data class DynamicScheduleAdaptationReplayResult(
    /**
     * 14.11 保存下来的历史 Adaptation。
     */
    val historical:
        List<DynamicSchedulePriorityAdaptation>,

    /**
     * 根据历史输入重新计算出的 Adaptation。
     */
    val replayed:
        List<DynamicSchedulePriorityAdaptation>,

    /**
     * 两者之间的差异。
     */
    val differences:
        List<DynamicScheduleAdaptationReplayDifference>,

    /**
     * 是否完全一致。
     */
    val equivalent: Boolean,
) {

    val changed: Boolean
        get() = !equivalent

    val historicalSize: Int
        get() = historical.size

    val replayedSize: Int
        get() = replayed.size
}
