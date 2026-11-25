package me.moiclathy.dism.schedule.integration

import me.moiclathy.dism.api.DismInternalApi
import me.moiclathy.dism.schedule.api.DynamicScheduleDecisionView
import me.moiclathy.dism.schedule.api.DynamicScheduleEvaluation
import me.moiclathy.dism.schedule.api.DynamicScheduleFacade
import me.moiclathy.dism.schedule.api.DynamicScheduleHistoryEntry
import me.moiclathy.dism.schedule.api.DynamicScheduleHistoryFacade
import me.moiclathy.dism.schedule.api.DynamicScheduleHistoryQuery
import me.moiclathy.dism.schedule.api.DynamicScheduleHistoryResult
import me.moiclathy.dism.schedule.history.DynamicScheduleDecisionHistoryQuery
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleModel
import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceRuntimePolicy
import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceRuntimeState
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot

/**
 * 15.3 implementation adapter.
 *
 * This is the only bridge from the product facade to the old
 * integration session.
 *
 * The Session can continue to contain complex internal services
 * without enlarging the supported Public API.
 */
@DismInternalApi
internal class DefaultDynamicScheduleFacade(
    private val session: DynamicScheduleSession,
) : DynamicScheduleFacade {

    override val history: DynamicScheduleHistoryFacade =
        DefaultDynamicScheduleHistoryFacade(
            session = session,
        )

    /**
     * 16.20：只评估，不写历史。
     *
     * 与 [evaluate] 走完全相同的 Runtime 链路，
     * 唯一的差别是 `session.preview` 而不是 `session.evaluate`。
     *
     * 两处的投影逻辑刻意共用 [toDynamicScheduleEvaluation]，
     * 否则"preview 与 evaluate 结果一致"这句话就只是注释里的承诺。
     */
    override suspend fun preview(
        model: DynamicScheduleModel,
        now: kotlin.time.Instant,
    ): DynamicScheduleEvaluation =
        toDynamicScheduleEvaluation(
            intelligence =
                session.preview(
                    model = model,
                    now = now,

                    activeContextKeys = emptySet(),

                    contextBindings =
                        me.moiclathy.dism.schedule.context
                            .DynamicScheduleContextBindingSet(),

                    dependencies =
                        me.moiclathy.dism.schedule.dependency
                            .DynamicScheduleDependencySet(),

                    previous =
                        DynamicScheduleIntelligenceRuntimeState(),

                    policy =
                        DynamicScheduleIntelligenceRuntimePolicy(),
                ),
            now = now,
        )

    override suspend fun evaluate(
        model: DynamicScheduleModel,
        now: kotlin.time.Instant,
    ): DynamicScheduleEvaluation {

        val evaluation =
            session.evaluate(
                model = model,
                now = now,

                /*
                 * 15.3 deliberately uses the existing deterministic
                 * defaults.
                 *
                 * Advanced configuration becomes a later public
                 * Configuration Surface concern.
                 */
                activeContextKeys = emptySet(),

                contextBindings =
                    me.moiclathy.dism.schedule.context
                        .DynamicScheduleContextBindingSet(),

                dependencies =
                    me.moiclathy.dism.schedule.dependency
                        .DynamicScheduleDependencySet(),

                previous =
                    DynamicScheduleIntelligenceRuntimeState(),

                policy =
                    DynamicScheduleIntelligenceRuntimePolicy(),
            )

        return toDynamicScheduleEvaluation(
            intelligence = evaluation.intelligence,
            now = now,
        )
    }

    override suspend fun latestDecision(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleDecisionView? {

        val snapshot =
            session.latestDecision(itemId)
                ?: return null

        return snapshot.toPublicView()
    }

    /**
     * 16.32：批量版本。
     *
     * 空集合直接返回——它是完全正常的一次调用
     * （模型里一个条目都没有），不该走到 SQL 层。
     */
    override suspend fun latestDecisions(
        itemIds: Collection<DynamicScheduleItemId>,
    ): Map<
        DynamicScheduleItemId,
        DynamicScheduleDecisionView,
        > {

        if (itemIds.isEmpty()) {
            return emptyMap()
        }

        return session
            .latestDecisions(itemIds)
            .associate {
                it.itemId to it.toPublicView()
            }
    }
}

/**
 * 16.20：`DynamicScheduleIntelligenceResult -> DynamicScheduleEvaluation`。
 *
 * [DefaultDynamicScheduleFacade.preview] 与
 * [DefaultDynamicScheduleFacade.evaluate] 共用这一处投影，
 * 这样"只评估不落盘时结果完全一致"才是**结构上**成立的，
 * 而不是靠两段复制粘贴的代码保持着一致。
 */
private fun toDynamicScheduleEvaluation(
    intelligence:
        me.moiclathy.dism.schedule.runtime
            .DynamicScheduleIntelligenceResult,
    now: kotlin.time.Instant,
): DynamicScheduleEvaluation =
    DynamicScheduleEvaluation(
        evaluatedAt = now,

        decisions =
            intelligence
                .inputModel
                .items
                .map { item ->

                    val conflict =
                        intelligence
                            .attentionConflicts[item.id]

                    val explanation =
                        intelligence
                            .attentionExplanations[item.id]

                    DynamicScheduleDecisionView(
                        itemId = item.id,
                        title = item.title,

                        priority =
                            conflict
                                ?.resolvedPriority
                                ?: item.priority,

                        attentionRequired =
                            conflict?.attentionRequired,

                        explanation =
                            explanation?.summary,
                    )
                },

        diagnostics =
            intelligence.diagnostics,
    )

/**
 * History capability adapter.
 */
@DismInternalApi
private class DefaultDynamicScheduleHistoryFacade(
    private val session: DynamicScheduleSession,
) : DynamicScheduleHistoryFacade {

    override suspend fun query(
        itemId: DynamicScheduleItemId,
        query: DynamicScheduleHistoryQuery,
    ): DynamicScheduleHistoryResult {

        val result =
            session.decisionHistory(
                itemId = itemId,

                query =
                    DynamicScheduleDecisionHistoryQuery(
                        itemId = itemId,
                        limit = query.limit,

                        /*
                         * The public History Facade currently promises
                         * whether a trace exists, so load trace metadata.
                         */
                        includeTrace = true,
                    ),
            )

        return DynamicScheduleHistoryResult(
            entries =
                result.entries.map {
                    it.toPublicView()
                },

            truncated =
                result.truncated,

            totalMatched =
                result.totalMatched,
        )
    }

    override suspend fun latest(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleHistoryEntry? =
        session
            .latestHistory(
                itemId = itemId,
                includeTrace = true,
            )
            ?.toPublicView()
}

/**
 * Internal Snapshot → Public Decision View.
 */
private fun DynamicScheduleDecisionSnapshot.toPublicView():
    DynamicScheduleDecisionView =
    DynamicScheduleDecisionView(
        itemId = itemId,

        title =
            item.title,

        priority =
            item.priority,

        /*
         * A persisted Snapshot is a historical fact.
         * It does not by itself represent a freshly evaluated
         * attention decision.
         */
        attentionRequired = null,

        explanation = null,
    )

/**
 * Internal history entry → Public history view.
 */
private fun
    me.moiclathy.dism.schedule.history
        .DynamicScheduleDecisionHistoryEntry
    .toPublicView():
    DynamicScheduleHistoryEntry =

    DynamicScheduleHistoryEntry(
        itemId =
            itemId,

        capturedAt =
            capturedAt,

        scheduleRevision =
            scheduleRevision,

        engineVersion =
            engineVersion,

        hasTrace =
            hasTrace,
    )
