package me.moiclathy.dism.event.runtime

import kotlin.time.Duration

/**
 * Time-flow Runtime 策略。
 */
data class EventTimeRuntimePolicy(

    /**
     * 默认运行间隔。
     */
    val tickInterval: Duration =
        Duration.parse("1m"),

    /**
     * Runtime 是否允许重复处理同一时间点。
     */
    val allowRepeatedEvaluation: Boolean = true,
) {

    init {
        require(tickInterval > Duration.ZERO) {
            "tickInterval must be positive."
        }
    }
}
