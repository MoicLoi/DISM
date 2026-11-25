package me.moiclathy.dism.context.confidence

import kotlin.math.ln

/**
 * Context Confidence 策略。
 */
data class ContextConfidencePolicy(

    /**
     * Confidence 半衰期，单位秒。
     *
     * 默认 6 小时。
     *
     * 注意：
     * 这是 Context Confidence，
     * 与 Attention Memory 的 7 天半衰期是不同层级。
     */
    val halfLifeSeconds: Double = 6.0 * 60.0 * 60.0,

    /**
     * Confidence 的最低保留值。
     *
     * 低于这个值后直接视为 0。
     */
    val minimumRetention: Double = 0.05,

    /**
     * 新证据最多增加多少 Confidence。
     */
    val maximumEvidenceBoost: Double = 0.50,

    /**
     * Confidence 最大值。
     */
    val maximumConfidence: Double = 1.0,
) {

    init {
        require(halfLifeSeconds > 0.0) {
            "halfLifeSeconds must be greater than zero."
        }

        require(
            minimumRetention in 0.0..1.0
        ) {
            "minimumRetention must be between 0.0 and 1.0."
        }

        require(
            maximumEvidenceBoost in 0.0..1.0
        ) {
            "maximumEvidenceBoost must be between 0.0 and 1.0."
        }

        require(
            maximumConfidence in 0.0..1.0
        ) {
            "maximumConfidence must be between 0.0 and 1.0."
        }
    }

    /**
     * 计算经过时间衰减后的 Confidence。
     *
     * exponential half-life:
     *
     * retained =
     *     2 ^ (-elapsed / halfLife)
     */
    fun decay(
        value: Double,
        elapsedSeconds: Double,
    ): Double {

        require(value in 0.0..1.0) {
            "value must be between 0.0 and 1.0."
        }

        if (elapsedSeconds <= 0.0) {
            return value
        }

        val retention =
            2.0.pow(
                -elapsedSeconds / halfLifeSeconds
            )

        val result =
            value * retention

        return if (
            result < minimumRetention
        ) {
            0.0
        } else {
            result
        }
    }

    /**
     * Evidence 对 Confidence 的最大增量。
     */
    fun evidenceBoost(
        strength: Double,
    ): Double {

        require(strength in 0.0..1.0) {
            "strength must be between 0.0 and 1.0."
        }

        return (
            strength * maximumEvidenceBoost
        ).coerceIn(
            0.0,
            maximumEvidenceBoost,
        )
    }

    private fun Double.pow(
        exponent: Double,
    ): Double {
        return kotlin.math.exp(
            exponent * ln(this)
        )
    }
}
