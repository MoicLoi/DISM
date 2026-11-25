package me.moiclathy.dism.state.query

import me.moiclathy.dism.evidence.Evidence
import kotlin.time.Instant

/**
 * 某个时间点"发生了什么变化、为什么"。
 *
 * 注意：evidence 优先来自 Transition log，
 * 而不是 Ended state 自身的 supports——
 * 那是"这个状态为什么曾经成立"，
 * 不是"它为什么在此时结束"。
 */
data class TransitionExplanation(
    val instant: Instant,
    val changes: List<TemporalStateChange>,
    val evidence: List<Evidence>,
)
