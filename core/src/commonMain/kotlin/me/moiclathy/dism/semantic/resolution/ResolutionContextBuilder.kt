package me.moiclathy.dism.semantic.resolution

import me.moiclathy.dism.context.Context
import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.state.model.RelationState
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * 根据 DISM 当前状态自动生成 Entity Resolution 上下文。
 *
 * 第一版支持三类 Context signal：
 * 1. Active Event 的 requires 关系（relevance = 1.0 × confidence）
 * 2. Upcoming Event 的 requires 关系（relevance = 0.85 × confidence）
 * 3. 最近实体引用（relevance 按时间衰减 × confidence）
 *
 * 各 signal 的时间衰减逻辑只收敛在对应的处理函数中，
 * 以便未来单独调整曲线。
 *
 * 设计原则：
 * - State / Semantic Engine 负责生成 contextual hints
 * - EntityResolver 只负责利用 hints 做 resolution
 * - Resolver 不应该自己变成半个 World Model
 */
class ResolutionContextBuilder {

    fun build(
        states: List<RelationState>,
        context: Context,
        memory: ResolutionMemory? = null,
    ): ResolutionContext {

        val activeStates =
            states.filter { it.isActive }

        val hints =
            mutableMapOf<Uuid, Double>()

        applyActiveEventRequirements(
            activeStates = activeStates,
            context = context,
            hints = hints,
        )

        applyUpcomingEventRequirements(
            activeStates = activeStates,
            context = context,
            hints = hints,
        )

        applyRecentEntityReferences(
            context = context,
            memory = memory,
            hints = hints,
        )

        return ResolutionContext(
            activeEventIds = context.activeEventIds,
            upcomingEventIds =
                context.upcomingEvents
                    .map { it.eventId }
                    .toSet(),
            entityHints =
                hints.map { (entityId, relevance) ->
                    ContextEntityHint(
                        entityId = entityId,
                        relevance = relevance.coerceIn(0.0, 1.0),
                    )
                },
        )
    }

    private fun applyActiveEventRequirements(
        activeStates: List<RelationState>,
        context: Context,
        hints: MutableMap<Uuid, Double>,
    ) {
        activeStates
            .asSequence()
            .filter { state ->
                state.key.predicate == Predicates.REQUIRES
            }
            .filter { state ->
                state.key.subjectId in context.activeEventIds
            }
            .forEach { state ->
                addHint(
                    hints = hints,
                    entityId = state.key.objectId,
                    relevance = 1.0 * state.confidence,
                )
            }
    }

    private fun applyUpcomingEventRequirements(
        activeStates: List<RelationState>,
        context: Context,
        hints: MutableMap<Uuid, Double>,
    ) {
        val upcomingEventIds =
            context.upcomingEvents
                .map { it.eventId }
                .toSet()

        activeStates
            .asSequence()
            .filter { state ->
                state.key.predicate == Predicates.REQUIRES
            }
            .filter { state ->
                state.key.subjectId in upcomingEventIds
            }
            .forEach { state ->
                addHint(
                    hints = hints,
                    entityId = state.key.objectId,
                    relevance = 0.85 * state.confidence,
                )
            }
    }

    private fun applyRecentEntityReferences(
        context: Context,
        memory: ResolutionMemory?,
        hints: MutableMap<Uuid, Double>,
    ) {
        if (memory == null) return

        memory.recent(
            now = context.now,
            limit = 10,
        ).forEach { reference ->

            val relevance =
                calculateRecentReferenceRelevance(
                    now = context.now,
                    reference = reference,
                )

            // relevance 0.0 等价于没有 hint
            if (relevance > 0.0) {
                addHint(
                    hints = hints,
                    entityId = reference.entityId,
                    relevance = relevance,
                )
            }
        }
    }

    private fun calculateRecentReferenceRelevance(
        now: Instant,
        reference: RecentEntityReference,
    ): Double {
        val age =
            now - reference.referencedAt

        val base =
            when {
                age <= 1.minutes -> 1.0
                age <= 5.minutes -> 0.9
                age <= 15.minutes -> 0.7
                age <= 1.hours -> 0.4
                else -> 0.0
            }

        return (
            base *
                reference.confidence
            ).coerceIn(
            0.0,
            1.0,
        )
    }

    private fun addHint(
        hints: MutableMap<Uuid, Double>,
        entityId: Uuid,
        relevance: Double,
    ) {
        val previous =
            hints[entityId]
                ?: 0.0

        hints[entityId] =
            maxOf(
                previous,
                relevance,
            )
    }
}
