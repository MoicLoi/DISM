package me.moiclathy.dism.schedule.runtime

import kotlin.time.Instant

import me.moiclathy.dism.attention.adaptation.AttentionFeedbackAction
import me.moiclathy.dism.attention.runtime.AttentionRuntime
import me.moiclathy.dism.schedule.adaptation.DeterministicDynamicScheduleAttentionDecayEngine
import me.moiclathy.dism.schedule.adaptation.DeterministicDynamicSchedulePriorityAdapter
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleAttentionDecayInput
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleDecayedPriorityAdaptationInput
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleAttentionHistory
import me.moiclathy.dism.schedule.attention.DeterministicDynamicScheduleAttentionConflictResolver
import me.moiclathy.dism.schedule.attention.DeterministicDynamicScheduleAttentionExplanationEngine
import me.moiclathy.dism.schedule.attention.DeterministicDynamicScheduleAttentionProjector
import me.moiclathy.dism.schedule.attention.DynamicScheduleAttentionConflictInput
import me.moiclathy.dism.schedule.attention.DynamicScheduleAttentionConstraint
import me.moiclathy.dism.schedule.attention.DynamicScheduleAttentionConstraintType
import me.moiclathy.dism.schedule.attention.DynamicScheduleAttentionExplanationInput
import me.moiclathy.dism.schedule.attention.DynamicScheduleAttentionInput
import me.moiclathy.dism.schedule.attention.DynamicScheduleAttentionSignal
import me.moiclathy.dism.schedule.context.DeterministicDynamicScheduleContextAttentionMemory
import me.moiclathy.dism.schedule.context.DeterministicDynamicScheduleContextBinder
import me.moiclathy.dism.schedule.context.DynamicScheduleContextAttentionInput
import me.moiclathy.dism.schedule.context.DynamicScheduleContextAttentionMemory
import me.moiclathy.dism.schedule.context.DynamicScheduleContextAttentionResult
import me.moiclathy.dism.schedule.context.DynamicScheduleContextBindingInput
import me.moiclathy.dism.schedule.dependency.DeterministicDynamicScheduleDependencyResolver
import me.moiclathy.dism.schedule.dependency.DynamicScheduleDependencyInput
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleModel

class DeterministicDynamicScheduleIntelligenceRuntime(
    private val dependencyResolver:
        DeterministicDynamicScheduleDependencyResolver =
        DeterministicDynamicScheduleDependencyResolver(),

    private val contextBinder:
        DeterministicDynamicScheduleContextBinder =
        DeterministicDynamicScheduleContextBinder(),

    private val decayEngine:
        DeterministicDynamicScheduleAttentionDecayEngine =
        DeterministicDynamicScheduleAttentionDecayEngine(),

    /**
     * 12.11：Context-aware attention memory。
     *
     * 「这个东西在这个 Context 下值不值得提醒？」
     */
    private val contextAttentionMemory:
        DynamicScheduleContextAttentionMemory =
        DeterministicDynamicScheduleContextAttentionMemory(),

    private val priorityAdapter:
        DeterministicDynamicSchedulePriorityAdapter =
        DeterministicDynamicSchedulePriorityAdapter(),

    private val attentionProjector:
        DeterministicDynamicScheduleAttentionProjector,

    private val attentionRuntime:
        AttentionRuntime =
        AttentionRuntime(),

    /**
     * 12.21：hard constraint 与 conflict resolution。
     *
     * 必须发生在 AttentionRuntime 之前，
     * 否则 hard constraint 只会被记录而不会影响 selection。
     */
    private val conflictResolver:
        DeterministicDynamicScheduleAttentionConflictResolver =
        DeterministicDynamicScheduleAttentionConflictResolver(),

    /**
     * 12.13/12.21：只解释已经产生的 Conflict Result，不改变决策。
     */
    private val explanationEngine:
        DeterministicDynamicScheduleAttentionExplanationEngine =
        DeterministicDynamicScheduleAttentionExplanationEngine(),
) : DynamicScheduleIntelligenceRuntime {

    override fun evaluate(
        input: DynamicScheduleIntelligenceInput,
        previous: DynamicScheduleIntelligenceRuntimeState,
        now: Instant,
        policy: DynamicScheduleIntelligenceRuntimePolicy,
    ): DynamicScheduleIntelligenceResult {

        /*
         * -------------------------------------------------------------
         * Phase 1
         * Dependency Resolution
         * -------------------------------------------------------------
         */

        val dependencyResult =
            dependencyResolver.resolve(
                DynamicScheduleDependencyInput(
                    model = input.model,
                    dependencies = input.dependencies,
                    policy = policy.dependencyPolicy,
                ),
            )

        /*
         * -------------------------------------------------------------
         * Phase 2
         * Context Binding
         * -------------------------------------------------------------
         */

        val contextResult =
            contextBinder.bind(
                DynamicScheduleContextBindingInput(
                    model = dependencyResult.projectedModel,
                    bindings = input.contextBindings,
                    activeContextKeys = input.activeContextKeys,
                    policy = policy.contextPolicy,
                ),
            )

        /*
         * -------------------------------------------------------------
         * Phase 3
         * Attention History Decay
         * -------------------------------------------------------------
         */

        val decayResult =
            decayEngine.decay(
                DynamicScheduleAttentionDecayInput(
                    histories =
                        previous.scheduleHistories,
                    now = now,
                ),
                policy = policy.decayPolicy,
            )

        /*
         * -------------------------------------------------------------
         * Phase 4
         * Dynamic Schedule Priority Adaptation
         * -------------------------------------------------------------
         */

        val adaptationResult =
            priorityAdapter.adaptDecayed(
                DynamicScheduleDecayedPriorityAdaptationInput(
                    model = contextResult.projectedModel,
                    histories = decayResult.histories,
                ),
                policy = policy.adaptationPolicy,
            )

        /*
         * -------------------------------------------------------------
         * Phase 5
         * Apply effective attention priority to a temporary model.
         *
         * IMPORTANT:
         * The original DynamicScheduleModel is NOT mutated.
         * -------------------------------------------------------------
         */

        /*
         * 12.11：Context-aware attention factor。
         *
         * effectivePriority
         *   = basePriority
         *     × globalAttentionAdaptation
         *     × contextAttentionAdaptation
         */
        val contextAttentionFactors:
            Map<
                DynamicScheduleItemId,
                DynamicScheduleContextAttentionResult,
                > =
            input.model.items.associate { item ->

                item.id to
                    contextAttentionMemory.evaluate(
                        DynamicScheduleContextAttentionInput(
                            itemId = item.id,
                            activeContextKeys =
                                input.activeContextKeys,
                            histories =
                                previous.contextAttentionHistories,
                            now = now,
                        ),
                    )
            }

        val adaptedModel =
            applyAttentionPriority(
                model = contextResult.projectedModel,
                adaptationResult = adaptationResult,
                contextAttentionFactors =
                    contextAttentionFactors,
            )

        /*
         * -------------------------------------------------------------
         * Phase 6
         * Schedule -> Attention projection
         * -------------------------------------------------------------
         */

        val attentionProjection =
            attentionProjector.project(
                DynamicScheduleAttentionInput(
                    model = adaptedModel,
                    contextResult = contextResult,
                ),
                policy =
                    policy.attentionProjectionPolicy,
            )

        /*
         * -------------------------------------------------------------
         * Phase 7
         * Attention Conflict Resolution
         *
         * IMPORTANT:
         *
         * Conflict resolution 必须发生在 AttentionRuntime
         * 之前。
         *
         * 否则 hard constraint 只会被记录，
         * 而不会真正影响 Attention selection。
         * -------------------------------------------------------------
         */

        val conflictResults =
            attentionProjection.candidates.associate { projected ->

                val item =
                    adaptedModel.item(
                        projected.itemId,
                    )

                require(item != null) {
                    "Projected attention item does not exist: " +
                        projected.itemId
                }

                val constraints =
                    buildAttentionConstraints(
                        item = item,
                        dependencyResult = dependencyResult,
                        contextResult = contextResult,
                    )

                val signal =
                    DynamicScheduleAttentionSignal(
                        itemId =
                            projected.itemId,

                        /*
                         * Projector 已经把：
                         *
                         * schedule priority
                         * context multiplier
                         * state multiplier
                         *
                         * 应用到了 Recommendation.priority。
                         *
                         * 因此这里把 projector 的最终 priority
                         * 作为 conflict resolver 的输入 priority。
                         *
                         * memory factor = 1
                         * 避免同一个 factor 被重复乘。
                         */
                        schedulePriority =
                            projected
                                .candidate
                                .recommendation
                                .priority,

                        globalMemoryFactor = 1.0,

                        contextMemoryFactor = 1.0,

                        constraints = constraints,
                    )

                projected.itemId to
                    conflictResolver.resolve(
                        DynamicScheduleAttentionConflictInput(
                            signal = signal,
                        ),
                        policy =
                            policy.conflictPolicy,
                    )
            }

        /*
         * -------------------------------------------------------------
         * Phase 8
         * Apply conflict-resolved priority
         * -------------------------------------------------------------
         */

        val resolvedAttentionCandidates =
            attentionProjection.candidates.map { projected ->

                val conflict =
                    conflictResults[
                        projected.itemId
                    ]

                if (conflict == null) {
                    projected.candidate
                } else {

                    val recommendation =
                        projected
                            .candidate
                            .recommendation

                    projected
                        .candidate
                        .copy(
                            recommendation =
                                recommendation.copy(
                                    priority =
                                        conflict.resolvedPriority,
                                    reasons =
                                        recommendation.reasons +
                                            conflict.reasons,
                                ),
                        )
                }
            }

        /*
         * -------------------------------------------------------------
         * Phase 9
         * Existing Attention Runtime
         *
         * 现在 AttentionRuntime 接收到的是：
         *
         *     conflict-resolved priority
         *
         * 而不是原始 priority。
         * -------------------------------------------------------------
         */

        val attentionResult =
            attentionRuntime.evaluate(
                candidates =
                    resolvedAttentionCandidates,

                previous =
                    previous.attentionRuntimeState,

                now =
                    now,

                policy =
                    policy.attentionPolicy,
            )

        /*
         * -------------------------------------------------------------
         * Phase 10
         * Explanation
         *
         * Explanation 只解释已经产生的 Conflict Result。
         * 它不改变任何 decision。
         * -------------------------------------------------------------
         */

        val attentionExplanations =
            attentionProjection.candidates.associate { projected ->

                val item =
                    adaptedModel.item(
                        projected.itemId,
                    )

                require(item != null) {
                    "Projected attention item does not exist: " +
                        projected.itemId
                }

                val conflict =
                    conflictResults[
                        projected.itemId
                    ]

                require(conflict != null) {
                    "Conflict result missing for projected item: " +
                        projected.itemId
                }

                val contextMatch =
                    contextResult.matches.firstOrNull {
                        it.itemId == projected.itemId
                    }

                val adaptation =
                    adaptationResult.adaptations.firstOrNull {
                        it.itemId == projected.itemId
                    }

                projected.itemId to
                    explanationEngine.explain(
                        DynamicScheduleAttentionExplanationInput(
                            item = item,

                            conflictResult =
                                conflict,

                            contextReasons =
                                buildContextExplanationReasons(
                                    contextMatch,
                                ),

                            adaptationReasons =
                                adaptation?.reasons
                                    ?: emptyList(),
                        ),
                        policy =
                            policy.explanationPolicy,
                    )
            }

        /*
         * -------------------------------------------------------------
         * Phase 11
         * Diagnostics
         * -------------------------------------------------------------
         */

        val diagnostics =
            buildList {

                addAll(
                    dependencyResult.warnings,
                )

                addAll(
                    contextResult.warnings,
                )

                addAll(
                    decayResult.diagnostics,
                )

                addAll(
                    adaptationResult.diagnostics,
                )

                addAll(
                    attentionProjection.diagnostics,
                )

                conflictResults.values
                    .flatMap {
                        it.reasons
                    }
                    .forEach {
                        add(
                            "Attention conflict: $it",
                        )
                    }
            }

        return DynamicScheduleIntelligenceResult(
            inputModel =
                input.model,

            dependencyResult =
                dependencyResult,

            contextResult =
                contextResult,

            priorityAdaptation =
                adaptationResult,

            attentionProjection =
                attentionProjection,

            attentionConflicts =
                conflictResults,

            attentionExplanations =
                attentionExplanations,

            attentionRuntime =
                attentionResult,

            nextState =
                previous.copy(
                    attentionRuntimeState =
                        attentionResult.nextState,
                ),

            diagnostics =
                diagnostics,
        )
    }

    override fun applyFeedback(
        state: DynamicScheduleIntelligenceRuntimeState,
        feedback: DynamicScheduleAttentionFeedback,
    ): DynamicScheduleIntelligenceRuntimeState {

        /*
         * First let the general Attention subsystem learn.
         */
        val nextAttentionState =
            attentionRuntime.applyFeedback(
                state = state.attentionRuntimeState,
                feedback = feedback.feedback,
            )

        /*
         * Then maintain the Schedule-specific history.
         */
        val oldHistory =
            state.scheduleHistories[
                feedback.itemId.value
            ]
                ?: DynamicScheduleAttentionHistory(
                    attentionKey =
                        feedback.feedback.attentionKey,
                )

        val nextHistory =
            updateHistory(
                history = oldHistory,
                action = feedback.feedback.action,
                observedAt = feedback.feedback.observedAt,
            )

        return state.copy(
            attentionRuntimeState =
                nextAttentionState,

            scheduleHistories =
                state.scheduleHistories +
                    (
                        feedback.itemId.value to
                            nextHistory
                    ),
        )
    }

    private fun applyAttentionPriority(
        model: DynamicScheduleModel,
        adaptationResult:
            me.moiclathy.dism.schedule.adaptation
                .DynamicSchedulePriorityAdaptationResult,
        contextAttentionFactors:
            Map<
                DynamicScheduleItemId,
                DynamicScheduleContextAttentionResult,
                > = emptyMap(),
    ): DynamicScheduleModel {

        val adaptations =
            adaptationResult.adaptations.associateBy {
                it.itemId
            }

        val nextItems =
            model.items.map { item ->

                val adaptation =
                    adaptations[item.id]

                val basePriority =
                    adaptation?.effectivePriority
                        ?: item.priority

                val contextFactor =
                    contextAttentionFactors[item.id]
                        ?.effectiveFactor
                        ?: 1.0

                val effectivePriority =
                    (basePriority * contextFactor)
                        .coerceIn(0.0, 1.0)

                if (effectivePriority == item.priority) {
                    item
                } else {
                    item.copy(
                        priority = effectivePriority,
                    )
                }
            }

        return if (nextItems == model.items) {
            model
        } else {
            model.copy(
                items = nextItems,
                revision = model.revision + 1,
            )
        }
    }

    private fun updateHistory(
        history: DynamicScheduleAttentionHistory,
        action: AttentionFeedbackAction,
        observedAt: Instant,
    ): DynamicScheduleAttentionHistory {

        return when (action) {

            AttentionFeedbackAction.OPENED,
            AttentionFeedbackAction.COMPLETED -> {
                history.copy(
                    shownCount =
                        history.shownCount + 1,

                    engagedCount =
                        history.engagedCount + 1,

                    lastInteractionAt =
                        observedAt,
                )
            }

            AttentionFeedbackAction.SNOOZED -> {
                history.copy(
                    shownCount =
                        history.shownCount + 1,

                    requestedCount =
                        history.requestedCount + 1,

                    lastInteractionAt =
                        observedAt,
                )
            }

            AttentionFeedbackAction.DISMISSED -> {
                history.copy(
                    shownCount =
                        history.shownCount + 1,

                    dismissedCount =
                        history.dismissedCount + 1,

                    lastInteractionAt =
                        observedAt,
                )
            }

            AttentionFeedbackAction.IGNORED -> {
                history.copy(
                    shownCount =
                        history.shownCount + 1,

                    ignoredCount =
                        history.ignoredCount + 1,

                    lastInteractionAt =
                        observedAt,
                )
            }
        }
    }
private fun buildAttentionConstraints(
    item:
        me.moiclathy.dism.schedule.model.DynamicScheduleItem,

    dependencyResult:
        me.moiclathy.dism.schedule.dependency
            .DynamicScheduleDependencyResolutionResult,

    contextResult:
        me.moiclathy.dism.schedule.context
            .DynamicScheduleContextBindingResult,
): List<DynamicScheduleAttentionConstraint> {

    val constraints =
        mutableListOf<DynamicScheduleAttentionConstraint>()

    /*
     * ---------------------------------------------------------
     * Required Dependency
     * ---------------------------------------------------------
     */

    val blockingDependencies =
        dependencyResult.matches.filter {
            it.dependency.toItemId == item.id &&
                it.blocking
        }

    blockingDependencies
        .filterNot { it.satisfied }
        .forEach { match ->

            constraints +=
                DynamicScheduleAttentionConstraint(
                    type =
                        DynamicScheduleAttentionConstraintType
                            .REQUIRED_DEPENDENCY,

                    strength = 1.0,

                    reason =
                        "Required dependency is not satisfied: " +
                            match.reason,
                )
        }

    /*
     * ---------------------------------------------------------
     * Required Context
     * ---------------------------------------------------------
     */

    val contextMatch =
        contextResult.matches.firstOrNull {
            it.itemId == item.id
        }

    if (
        contextMatch != null &&
        contextMatch.unmatchedRequiredContextKeys.isNotEmpty()
    ) {

        contextMatch
            .unmatchedRequiredContextKeys
            .forEach { contextKey ->

                constraints +=
                    DynamicScheduleAttentionConstraint(
                        type =
                            DynamicScheduleAttentionConstraintType
                                .REQUIRED_CONTEXT,

                        strength = 1.0,

                        reason =
                            "Required context is missing: " +
                                contextKey,
                    )
            }
    }

    /*
     * ---------------------------------------------------------
     * Current ACTIVE state
     *
     * ACTIVE 本身不是 hard constraint。
     *
     * 它只是说明 schedule item 已经进入执行阶段。
     *
     * 因此这里只保留 diagnostic-style soft signal，
     * 不制造 hard constraint。
     * ---------------------------------------------------------
     */

    if (
        item.state ==
            me.moiclathy.dism.schedule.model.DynamicScheduleState.ACTIVE
    ) {

        constraints +=
            DynamicScheduleAttentionConstraint(
                type =
                    DynamicScheduleAttentionConstraintType
                        .NORMAL_PRIORITY,

                strength = 0.8,

                reason =
                    "Schedule item is currently active.",
            )
    }

    return constraints
}

private fun buildContextExplanationReasons(
    match:
        me.moiclathy.dism.schedule.context
            .DynamicScheduleContextMatch?,
): List<String> {

    if (match == null) {
        return emptyList()
    }

    return buildList {

        if (
            match.unmatchedRequiredContextKeys.isNotEmpty()
        ) {
            add(
                "Required context missing: " +
                    match.unmatchedRequiredContextKeys
                        .joinToString(", "),
            )
        }

        if (
            match.matchedExcludedContextKeys.isNotEmpty()
        ) {
            add(
                "Excluded context active: " +
                    match.matchedExcludedContextKeys
                        .joinToString(", "),
            )
        }

        if (
            match.matchedContextKeys.isNotEmpty()
        ) {
            add(
                "Matched context: " +
                    match.matchedContextKeys
                        .joinToString(", "),
            )
        }

        if (
            match.preferredScore > 0.0
        ) {
            add(
                "Context preference score=" +
                    match.preferredScore,
            )
        }
    }
}
}
