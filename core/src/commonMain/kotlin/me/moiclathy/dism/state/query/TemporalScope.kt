package me.moiclathy.dism.state.query

import kotlin.time.Instant

/**
 * 查询的时间范围。
 *
 * 把时间语义单独建模，避免与
 * "当前是否 active" 混在一起产生歧义。
 *
 * 例如：
 * 是查"现在 active 的状态"，
 * 还是查"昨天这个时刻 active 的状态"？
 * 用 TemporalScope 明确区分。
 */
sealed interface TemporalScope {

    /**
     * 查询当前仍有效的状态。
     */
    data object Current : TemporalScope

    /**
     * 查询某一具体时间点上有效的状态。
     */
    data class At(
        val instant: Instant,
    ) : TemporalScope

    /**
     * 不限制时间，包含历史状态。
     */
    data object All : TemporalScope
}
