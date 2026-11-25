package me.moiclathy.dism.semantic.engine

import me.moiclathy.dism.attention.AttentionEngine
import me.moiclathy.dism.attention.AttentionPolicy
import me.moiclathy.dism.context.Context
import me.moiclathy.dism.domain.entity.EntityRegistry
import me.moiclathy.dism.domain.relation.Predicate
import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.evidence.Evidence
import me.moiclathy.dism.persistence.repository.EntityRepository
import me.moiclathy.dism.persistence.repository.EvidenceRepository
import me.moiclathy.dism.persistence.repository.InMemoryEvidenceRepository
import me.moiclathy.dism.persistence.repository.InMemoryRelationStateRepository
import me.moiclathy.dism.persistence.repository.RelationStateRepository
import me.moiclathy.dism.persistence.repository.StateCommitRepository
import me.moiclathy.dism.persistence.repository.InMemoryStateCommitRepository
import me.moiclathy.dism.persistence.repository.InMemoryTransitionLogRepository
import me.moiclathy.dism.persistence.repository.TransitionLogRepository
import me.moiclathy.dism.planning.recommendation.Recommendation
import me.moiclathy.dism.planning.recommendation.RecommendationEngine
import me.moiclathy.dism.semantic.observation.ObservationProcessor
import me.moiclathy.dism.semantic.observation.ObservationResolutionResult
import me.moiclathy.dism.semantic.observation.ObservationResolver
import me.moiclathy.dism.semantic.observation.SemanticObservationDraft
import me.moiclathy.dism.semantic.propagation.SemanticPropagationEngine
import me.moiclathy.dism.semantic.propagation.StateChangeEvent
import me.moiclathy.dism.semantic.propagation.key
import me.moiclathy.dism.semantic.resolution.ResolutionContextBuilder
import me.moiclathy.dism.semantic.resolution.ResolutionSession
import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.query.RelationExplanation
import me.moiclathy.dism.state.query.RelationQuery
import me.moiclathy.dism.state.query.StateQueryService
import me.moiclathy.dism.state.query.TemporalScope
import me.moiclathy.dism.state.query.TemporalStateChange
import me.moiclathy.dism.state.query.TransitionExplanation
import me.moiclathy.dism.state.query.TransitionCause
import me.moiclathy.dism.state.constraint.RelationConstraintPolicy
import me.moiclathy.dism.state.reducer.StateReducer
import me.moiclathy.dism.state.temporal.StateChange
import me.moiclathy.dism.state.transition.StateTransitionRecord
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * DISM Core 第一版的高层入口。
 *
 * 把分散的调用封装为一个 observe()：
 *
 * ResolutionContextBuilder
 *     ↓
 * ObservationResolver
 *     ↓
 * ObservationProcessor
 *     ↓
 * StateReducer
 *
 * 只负责编排，不吞掉各层职责：
 * - EntityResolver / ResolutionSession 仍负责解析
 * - ObservationProcessor 仍负责 Observation → Transition
 * - StateReducer 仍负责 Transition → State
 *
 * Evidence 闭环：
 * observe() 在开始解析前验证 draft.evidenceIds 全部存在于 EvidenceRepository，
 * 保证进入 Dynamic State 的每个 Evidence ID 都可反查回原始证据。
 *
 * states 目前先放在 Engine 内部（内存 List），
 * 验证阶段重点是状态语义是否正确，而不是 persistence architecture。
 * 以后换成 StateStore / RelationStateRepository 时，
 * 上层 engine.observe(...) 不需要改变。
 */
class SemanticStateEngine(
    val entities: EntityRepository =
        EntityRegistry(),

    val evidence: EvidenceRepository =
        InMemoryEvidenceRepository(),

    private val stateRepository:
        RelationStateRepository =
        InMemoryRelationStateRepository(),

    private val transitionLogRepository:
        TransitionLogRepository =
        InMemoryTransitionLogRepository(),

    private val stateCommitRepository:
        StateCommitRepository =
        InMemoryStateCommitRepository(
            stateRepository,
            transitionLogRepository,
        ),

    private val resolutionSession: ResolutionSession =
        ResolutionSession(),

    private val resolutionContextBuilder:
        ResolutionContextBuilder =
        ResolutionContextBuilder(),

    private val observationProcessor:
        ObservationProcessor =
        ObservationProcessor(),

    private val stateReducer:
        StateReducer =
        StateReducer(),

    private val stateQueryService:
        StateQueryService =
        StateQueryService(),

    private val propagationEngine:
        SemanticPropagationEngine =
        SemanticPropagationEngine(),

    private val recommendationEngine:
        RecommendationEngine =
        RecommendationEngine(),

    private val recommendationLifecycleReconciler:
        me.moiclathy.dism.planning.recommendation.RecommendationLifecycleReconciler =
        me.moiclathy.dism.planning.recommendation.RecommendationLifecycleReconciler(),

    private val attentionEngine:
        AttentionEngine =
        AttentionEngine(),
) {

    private val observationResolver =
        ObservationResolver(
            resolutionSession = resolutionSession,
        )

    private var states:
        List<RelationState> =
        stateRepository.loadAll()

    // 当前 risk / recommendation 快照。
    // 只保留"当前快照"，不保存不可恢复的历史；
    // 生命周期由无状态 Reconciler 基于前后快照推导。
    private var requirementRisks:
        List<me.moiclathy.dism.semantic.propagation.RequirementRisk> =
        emptyList()

    private var activeRecommendations:
        List<Recommendation> =
        emptyList()

    fun observe(
        draft: SemanticObservationDraft,
        context: Context,
    ): ObserveResult {

        validateEvidence(draft)

        val resolutionContext =
            resolutionContextBuilder.build(
                states = states,
                context = context,
                memory = resolutionSession.memory(),
            )

        val resolution =
            observationResolver.resolve(
                draft = draft,
                registry = entities,
                context = resolutionContext,
            )

        return when (resolution) {

            is ObservationResolutionResult.Unresolved ->
                ObserveResult.Unresolved(
                    resolution = resolution,
                    currentStates = states,
                )

            is ObservationResolutionResult.Resolved ->
                applyResolvedObservation(
                    resolution = resolution,
                )
        }
    }

    fun currentStates(): List<RelationState> {
        return states
    }

    // ── READ API ──────────────────────────────────────────────

    /**
     * 结构化查询当前 Dynamic Reality。
     *
     * 上层不再需要自己写：
     * engine.currentStates().filter { ... }.firstOrNull { ... }
     */
    fun query(
        query: RelationQuery,
    ): List<RelationState> {
        return stateQueryService.query(
            states = states,
            query = query,
        )
    }

    fun queryFirst(
        query: RelationQuery,
    ): RelationState? {
        return stateQueryService.firstOrNull(
            states = states,
            query = query,
        )
    }

    /**
     * 实体当前的全部活动关系。
     */
    fun currentRelationsOf(
        entityId: kotlin.uuid.Uuid,
    ): List<RelationState> {
        return query(
            RelationQuery(
                subjectId = entityId,
            )
        )
    }

    fun carriedBy(
        entityId: kotlin.uuid.Uuid,
    ): RelationState? {
        return queryFirst(
            RelationQuery(
                subjectId = entityId,
                predicate = Predicates.CARRIED_BY,
            )
        )
    }

    fun locatedAt(
        entityId: kotlin.uuid.Uuid,
    ): RelationState? {
        return queryFirst(
            RelationQuery(
                subjectId = entityId,
                predicate = Predicates.LOCATED_AT,
            )
        )
    }

    fun inside(
        entityId: kotlin.uuid.Uuid,
    ): RelationState? {
        return queryFirst(
            RelationQuery(
                subjectId = entityId,
                predicate = Predicates.INSIDE,
            )
        )
    }

    /**
     * 事件需要什么（直接服务 Recommendation）。
     */
    fun requirementsOf(
        eventId: kotlin.uuid.Uuid,
    ): List<RelationState> {
        return query(
            RelationQuery(
                subjectId = eventId,
                predicate = Predicates.REQUIRES,
            )
        )
    }

    /**
     * 哪些事件需要该实体。
     */
    fun eventsRequiring(
        entityId: Uuid,
    ): List<RelationState> {
        return query(
            RelationQuery(
                predicate = Predicates.REQUIRES,
                objectId = entityId,
            )
        )
    }

    /**
     * 查询某一具体时间点的世界模型状态。
     *
     * 例如：
     * engine.queryAt(
     *     instant = lastNight,
     *     subjectId = powerBank.id,
     * )
     */
    fun queryAt(
        instant: Instant,
        subjectId: Uuid? = null,
        predicate: Predicate? = null,
        objectId: Uuid? = null,
        minimumConfidence: Double = 0.0,
    ): List<RelationState> {
        return query(
            RelationQuery(
                subjectId = subjectId,
                predicate = predicate,
                objectId = objectId,
                temporalScope =
                    TemporalScope.At(
                        instant
                    ),
                minimumConfidence =
                    minimumConfidence,
            )
        )
    }

    /**
     * 某实体的完整状态历史，按成立时间排序。
     *
     * 返回：
     * 07:00 located_at Dorm
     * 08:00 carried_by User
     * 12:10 located_at Classroom
     * 17:30 inside Backpack
     *
     * Core 只负责回答 "What state existed when?"；
     * 生成自然语言 timeline 属于 Presentation / Explanation。
     */
    fun historyOf(
        entityId: Uuid,
    ): List<RelationState> {
        return query(
            RelationQuery(
                subjectId = entityId,
                temporalScope =
                    TemporalScope.All,
            )
        )
            .sortedBy {
                it.validFrom
            }
    }

    /**
     * "为什么系统认为这件事成立？"
     */
    fun explain(
        state: RelationState,
    ): RelationExplanation {
        val establishedBy = findEstablishingCause(state)
        val endedBy = findEndingCause(state)

        return RelationExplanation(
            state = state,
            evidence = evidenceFor(state),
            establishedBy = establishedBy,
            endedBy = endedBy,
        )
    }

    /**
     * 返回一条完整的状态证据链。
     *
     * 这是 explain() 的结构化别名，方便上层明确表达“我要解释链”。
     */
    fun explainChain(
        state: RelationState,
    ): RelationExplanation {
        return explain(state)
    }

    private fun findEstablishingCause(
        state: RelationState,
    ): TransitionCause? {
        val record = transitionLogRepository.all()
            .asSequence()
            .filter { it.effectiveAt == state.validFrom }
            .filter { record ->
                when (val transition = record.transition) {
                    is me.moiclathy.dism.state.transition.StateTransition.AssertRelation ->
                        transition.key == state.key
                    is me.moiclathy.dism.state.transition.StateTransition.SupersedeRelation ->
                        transition.newKey == state.key
                    is me.moiclathy.dism.state.transition.StateTransition.InvalidateRelation ->
                        false
                }
            }
            .firstOrNull()
            ?: return null

        return TransitionCause(
            record = record,
            evidence = evidenceForTransition(record),
        )
    }

    private fun findEndingCause(
        state: RelationState,
    ): TransitionCause? {
        val end = state.validUntil ?: return null

        val constraintPolicy = RelationConstraintPolicy()

        val record = transitionLogRepository.all()
            .asSequence()
            .filter { it.effectiveAt == end }
            .filter { record ->
                when (val transition = record.transition) {
                    is me.moiclathy.dism.state.transition.StateTransition.InvalidateRelation ->
                        transition.key == state.key
                    is me.moiclathy.dism.state.transition.StateTransition.SupersedeRelation ->
                        transition.oldKey == state.key
                    is me.moiclathy.dism.state.transition.StateTransition.AssertRelation ->
                        transition.key.subjectId == state.key.subjectId &&
                            constraintPolicy.conflicts(
                                transition.key.predicate,
                                state.key.predicate,
                            ) &&
                            transition.key != state.key
                }
            }
            .firstOrNull()
            ?: return null

        return TransitionCause(
            record = record,
            evidence = evidenceForTransition(record),
        )
    }

    private fun evidenceForTransition(
        record: StateTransitionRecord,
    ): List<Evidence> {
        return record.transition.evidenceIds
            .mapNotNull { evidence.find(it) }
    }

    /**
     * 某个时间点发生了什么状态切换。
     *
     * 例如 22:47：
     * Ended: PowerBank located_at Classroom
     * Started: PowerBank located_at Dorm
     */
    fun changesAt(
        instant: Instant,
    ): List<TemporalStateChange> {
        return stateQueryService
            .changesAt(
                states = states,
                instant = instant,
            )
    }

    /**
     * 某个时间点被成功应用的语义 transition。
     */
    fun transitionsAt(
        instant: Instant,
    ): List<StateTransitionRecord> {
        return transitionLogRepository.at(
            instant
        )
    }

    /**
     * "这个时间点为什么发生了变化？"
     *
     * 优先从 transition log 取证据——
     * 不是拿 Ended state 自身的 supports
     * （那是"为什么它曾经成立"，不是"为什么在此时结束"）。
     */
    fun explainChangesAt(
        instant: Instant,
    ): TransitionExplanation {

        val changes =
            changesAt(instant)

        val transitionRecords =
            transitionsAt(instant)

        val evidenceIds =
            transitionRecords
                .flatMap {
                    it.transition.evidenceIds
                }
                .toSet()

        val supportingEvidence =
            evidenceIds.mapNotNull {
                evidence.find(it)
            }

        return TransitionExplanation(
            instant = instant,
            changes = changes,
            evidence = supportingEvidence,
        )
    }

    /**
     * 返回支撑某条状态的全部原始证据。
     *
     * 反查链：RelationState.evidenceIds → EvidenceRepository → Evidence。
     * 供 UI / explain layer 使用。
     */
    fun evidenceFor(
        state: RelationState,
    ): List<Evidence> {
        return state.evidenceIds
            .mapNotNull {
                evidence.find(it)
            }
    }

    private fun validateEvidence(
        draft: SemanticObservationDraft,
    ) {
        val missing =
            draft.evidenceIds.filterNot {
                evidence.contains(it)
            }

        require(missing.isEmpty()) {
            "Observation references unknown evidence IDs: $missing"
        }
    }

    private fun applyResolvedObservation(
        resolution:
            ObservationResolutionResult.Resolved,
    ): ObserveResult.Applied {

        val observation =
            resolution.observation

        val observationResult =
            observationProcessor.process(
                observation
            )

        val previousStates =
            states

        var updatedStates =
            states

        val appliedTransitions =
            mutableListOf<AppliedTransition>()

        val pendingRecords =
            mutableListOf<StateTransitionRecord>()

        observationResult.transitions
            .forEach { transition ->

                val transitionResult =
                    stateReducer.apply(
                        states = updatedStates,
                        transition = transition,
                        evidenceRegistry = evidence,
                    )

                updatedStates =
                    transitionResult.currentStates

                val record =
                    StateTransitionRecord(
                        effectiveAt =
                            transition.effectiveAt,
                        transition =
                            transition,
                    )

                pendingRecords += record

                appliedTransitions +=
                    AppliedTransition(
                        record = record,
                        changes =
                            transitionResult.changes,
                    )
            }

        // 所有 transition 都先在 candidate state 上成功应用，
        // 然后一次性提交 Dynamic State + Transition Log。
        // 如果提交失败，Engine 内部的 states 仍保持旧状态。
        stateCommitRepository.commit(
            states = updatedStates,
            transitions = pendingRecords,
        )

        states =
            updatedStates

        return ObserveResult.Applied(
            observation = observation,
            appliedTransitions =
                appliedTransitions,
            previousStates =
                previousStates,
            currentStates =
                states,
        )
    }

    /**
     * 一次状态变化自动走完整链路：
     *
     * StateChange
     *     → SemanticPropagation → RequirementRisk
     *     → RecommendationEngine
     *     → AttentionEngine → visible recommendations
     *
     * observe() 本身仍是"只维护世界状态"的纯入口。
     */
    fun observeAndEvaluate(
        draft: SemanticObservationDraft,
        context: Context,
        userId: Uuid,
        attentionPolicy: AttentionPolicy =
            AttentionPolicy(),
        previousRequirementRisks: List<me.moiclathy.dism.semantic.propagation.RequirementRisk> =
            emptyList(),
    ): ReactiveEvaluationResult {

        // 调用方显式传入的上一快照优先（用于重启后重建）；
        // 否则沿用 Engine 维护的当前快照。
        if (previousRequirementRisks.isNotEmpty()) {
            requirementRisks = previousRequirementRisks
        }

        val observeResult =
            observe(
                draft = draft,
                context = context,
            )

        if (
            observeResult !is
            ObserveResult.Applied
        ) {
            // Unresolved 不改变世界，也不清空既有提醒。
            return ReactiveEvaluationResult(
                observeResult =
                    observeResult,
                propagationResults =
                    emptyList(),
                recommendations =
                    activeRecommendations,
                visibleRecommendations =
                    attentionEngine.select(
                        recommendations = activeRecommendations,
                        policy = attentionPolicy,
                    ),
            )
        }

        // 逐个 transition 传播，并把当前 risk snapshot 传给下一条。
        // 只更新受影响事件，未受影响事件的旧 risk 保留，
        // 避免无关状态变化把旧提醒误判为 RESOLVED。
        val propagationResults =
            mutableListOf<me.moiclathy.dism.semantic.propagation.PropagationResult>()

        observeResult.appliedTransitions.forEach { applied ->
            val propagation =
                propagationEngine.propagate(
                    event =
                        StateChangeEvent(
                            transitionRecord =
                                applied.record,
                            changes =
                                applied.changes,
                        ),
                    states =
                        observeResult.currentStates,
                    context =
                        context,
                    userId =
                        userId,
                    previousRequirementRisks =
                        requirementRisks,
                )

            propagationResults += propagation

            if (propagation.affectedEventIds.isNotEmpty()) {
                requirementRisks =
                    requirementRisks
                        .filterNot {
                            it.eventId in propagation.affectedEventIds
                        } +
                        propagation.requirementRisks
            }
        }

        requirementRisks =
            requirementRisks
                .distinctBy { it.key() }

        val riskLifecycleChanges =
            propagationResults
                .flatMap { it.requirementRiskLifecycleChanges }

        val recommendations =
            recommendationEngine
                .fromRequirementRisks(
                    risks = requirementRisks,
                    entities = entities,
                )

        val recommendationLifecycleChanges =
            recommendationLifecycleReconciler.reconcile(
                previousRecommendations = activeRecommendations,
                currentRecommendations = recommendations,
                riskLifecycleChanges = riskLifecycleChanges,
            )

        activeRecommendations = recommendations

        val visibleRecommendations =
            attentionEngine.select(
                recommendations =
                    recommendations,
                policy =
                    attentionPolicy,
            )

        return ReactiveEvaluationResult(
            observeResult =
                observeResult,
            propagationResults =
                propagationResults,
            recommendations =
                recommendations,
            visibleRecommendations =
                visibleRecommendations,
            recommendationLifecycleChanges =
                recommendationLifecycleChanges,
        )
    }
}
