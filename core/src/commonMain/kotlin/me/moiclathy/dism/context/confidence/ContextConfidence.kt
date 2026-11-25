package me.moiclathy.dism.context.confidence

import kotlin.time.Instant
import me.moiclathy.dism.context.model.ContextMode

/**
 * 当前 Context 的动态可信度。
 *
 * 注意：
 *
 * ContextMode = "是什么"
 * ContextConfidence = "我们有多相信它"
 *
 * 两者故意分离。
 */
data class ContextConfidence(
    val mode: ContextMode,
    val value: Double = 0.0,
    val lastUpdatedAt: Instant? = null,
) {

    init {
        require(value in 0.0..1.0) {
            "Context confidence must be between 0.0 and 1.0."
        }
    }
}
