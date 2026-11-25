package me.moiclathy.dism.schedule.adaptation.replay

/**
 * 14.12
 *
 * 历史适配与重放适配之间的差异类型。
 */
enum class DynamicScheduleAdaptationReplayDifferenceType {

    BASE_PRIORITY_CHANGED,

    ADAPTATION_FACTOR_CHANGED,

    EFFECTIVE_PRIORITY_CHANGED,

    REASONS_CHANGED,

    ITEM_MISSING,

    ITEM_ADDED,
}

/**
 * 14.12
 *
 * 一处具体差异。
 *
 * `historicalValue` / `replayedValue` 刻意用 `String`：
 * 差异可能来自 Double（`0.6500000001` vs `0.65`），
 * 也可能来自 reasons 列表。用 String 保留原样，
 * 让排查的人看到真实数值，而不是被格式化过的结果。
 */
data class DynamicScheduleAdaptationReplayDifference(
    val itemId: String,
    val type: DynamicScheduleAdaptationReplayDifferenceType,
    val historicalValue: String?,
    val replayedValue: String?,
    val message: String,
)
