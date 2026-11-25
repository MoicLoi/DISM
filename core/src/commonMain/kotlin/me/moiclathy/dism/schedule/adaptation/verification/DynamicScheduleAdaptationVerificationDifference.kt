package me.moiclathy.dism.schedule.adaptation.verification

/**
 * 14.13
 *
 * 一处具体的验证差异。
 *
 * `factor` 是**字段名**（`"basePriority"` / `"adaptationFactor"` /
 * `"effectivePriority"` / `"reasons"` / `"item"`），
 * 不是权重倍数——这样"改了哪个字段"一眼可见。
 */
data class DynamicScheduleAdaptationVerificationDifference(
    val itemId: String,

    val factor: String,

    val historicalValue: String?,

    val replayedValue: String?,

    val description: String,
)
