package me.moiclathy.dism.schedule.adaptation

import kotlin.time.Instant

/**
 * 14.1
 *
 * 一条从历史中提取出来的**客观特征**。
 *
 * ### Value 与 Confidence 必须分开
 *
 * 这是 Adaptive Layer 的地基：
 *
 * ```text
 * completion_rate = 1.0, sampleCount = 1   → confidence 0.1
 * completion_rate = 1.0, sampleCount = 50  → confidence 1.0
 * ```
 *
 * 两者是完全不同的结论。
 * 系统看到"一次完成"就直接"以后提高 Weight"，
 * 正是没有 Feature Layer 时会犯的错误。
 *
 * ### 只测量，不解释
 *
 * ```text
 * postponement_rate = 0.3
 * ```
 *
 * 只说明"十次里有三次延期"。
 *
 * 它**不**意味着"延期 = 不重要"——
 * 延期可能是因为用户时间不足、临时事件、
 * 优先级变化，或者系统安排本身不合理。
 *
 * 因果判断属于 14.2 的 Feature Interpretation。
 */
data class DynamicScheduleDecisionFeature(
    val key: String,

    /**
     * 特征值，统一归一化到 `[0, 1]`。
     *
     * 第一版刻意只用 `Double`：
     * Adaptive Layer 需要一个简单稳定的数值接口。
     * 布尔 / 分类 / 分布 / 时序特征留给以后。
     */
    val value: Double,

    /**
     * 这条特征有多可信（由样本量决定）。
     */
    val confidence: Double,

    /**
     * 这条特征是基于什么时候的历史算出来的。
     */
    val observedAt: Instant,

    val sampleCount: Int,

    val source:
        DynamicScheduleDecisionFeatureSource,
) {

    init {
        require(key.isNotBlank()) {
            "Feature key must not be blank"
        }

        require(value.isFinite()) {
            "Feature value must be finite"
        }

        require(value in 0.0..1.0) {
            "Feature value must be in [0, 1]"
        }

        require(confidence in 0.0..1.0) {
            "Feature confidence must be in [0, 1]"
        }

        require(sampleCount >= 0) {
            "sampleCount must be >= 0"
        }
    }
}
