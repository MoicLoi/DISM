package me.moiclathy.dism.persistence.repository

import me.moiclathy.dism.state.transition.StateTransitionRecord
import kotlin.time.Instant

/**
 * Transition log 持久化边界。
 *
 * StateTransitionRecord 记录"世界是怎么变化的"（What changed），
 * 与 RelationState（What is true now）、Evidence（Why we believe it）职责分离。
 */
interface TransitionLogRepository {

    fun append(
        record: StateTransitionRecord,
    )

    fun all():
        List<StateTransitionRecord>

    fun at(
        instant: Instant,
    ): List<StateTransitionRecord>

    /**
     * 用完整记录集一次性替换 transition log。
     *
     * 默认实现逐条 append（供没有批量能力的实现复用）；
     * 内存实现覆写为单次批量赋值，使 StateCommit 成为真正的批量提交，
     * 而不是"replaceAll 成功后逐条 append、中途失败留下半成品"。
     */
    fun replaceAll(
        records: List<StateTransitionRecord>,
    ) {
        records.forEach(::append)
    }
}
