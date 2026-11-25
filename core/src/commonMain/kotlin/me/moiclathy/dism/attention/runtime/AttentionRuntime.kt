package me.moiclathy.dism.attention.runtime

import kotlin.time.Instant
import me.moiclathy.dism.attention.AttentionEngine
import me.moiclathy.dism.attention.AttentionPolicy
import me.moiclathy.dism.attention.adaptation.AttentionAdaptation
import me.moiclathy.dism.attention.adaptation.AttentionAdaptationEngine
import me.moiclathy.dism.attention.adaptation.AttentionFeedback
import me.moiclathy.dism.attention.context.AttentionContext
import me.moiclathy.dism.attention.context.AttentionContextDecision
import me.moiclathy.dism.attention.context.AttentionContextEngine
import me.moiclathy.dism.attention.explanation.AttentionDecisionExplanation
import me.moiclathy.dism.attention.explanation.AttentionExplanationEngine
import me.moiclathy.dism.attention.lifecycle.AttentionDecision
import me.moiclathy.dism.attention.lifecycle.AttentionAction
import me.moiclathy.dism.attention.lifecycle.AttentionKey
import me.moiclathy.dism.attention.lifecycle.AttentionLifecycleReconciler
import me.moiclathy.dism.attention.memory.AttentionMemory
import me.moiclathy.dism.attention.memory.AttentionMemoryEngine
import me.moiclathy.dism.attention.persistence.AttentionEvent
import me.moiclathy.dism.attention.persistence.AttentionEventLog
import me.moiclathy.dism.attention.persistence.AttentionEventType
import kotlin.uuid.Uuid

/**
 * Single orchestration boundary for the Attention subsystem.
 * It coordinates context, adaptive scoring, budget selection, lifecycle,
 * explanation and event emission without sending platform notifications.
 */
class AttentionRuntime(
    private val attentionEngine: AttentionEngine = AttentionEngine(),
    private val contextEngine: AttentionContextEngine = AttentionContextEngine(),
    private val adaptationEngine: AttentionAdaptationEngine = AttentionAdaptationEngine(),
    private val memoryEngine: AttentionMemoryEngine = AttentionMemoryEngine(),
    private val lifecycleReconciler: AttentionLifecycleReconciler = AttentionLifecycleReconciler(),
    private val explanationEngine: AttentionExplanationEngine = AttentionExplanationEngine(),
) {
    fun evaluate(
        candidates: List<AttentionRuntimeCandidate>,
        context: AttentionContext = AttentionContext(),
        previous: AttentionRuntimeState = AttentionRuntimeState(),
        now: Instant,
        eventLog: AttentionEventLog? = null,
        policy: AttentionPolicy = AttentionPolicy(),
    ): AttentionRuntimeResult {
        require(candidates.map { it.attentionKey }.distinct().size == candidates.size) {
            "AttentionRuntime requires unique attentionKey values per evaluation"
        }
        candidates.forEach { candidate ->
            val recommendationKey = requireNotNull(candidate.recommendation.key) {
                "AttentionRuntime requires a stable RecommendationKey"
            }
            require(AttentionKey(recommendationKey).toString() == candidate.attentionKey) {
                "attentionKey must equal AttentionKey(recommendation.key).toString()"
            }
        }

        val contextDecision = contextEngine.evaluate(context)
        val decayedMemories = previous.memories.mapValues { (_, memory) ->
            memoryEngine.decay(memory, now)
        }
        val effectivePolicy = policy.copy(
            softLimit = minOf(policy.softLimit, contextDecision.effectiveBudget),
            hardLimit = minOf(policy.hardLimit, contextDecision.effectiveBudget),
        )

        val adjusted = candidates.map { candidate ->
            val key = candidate.attentionKey
            val current = previous.adaptations[key] ?: AttentionAdaptation(key)
            val memory = decayedMemories[key]
            val merged = if (memory == null) {
                current
            } else {
                current.copy(suppressionBias = memory.bias).clamp()
            }
            val priority = adaptationEngine.rank(candidate.recommendation.priority, merged)
            candidate.recommendation.copy(priority = priority)
        }

        val adjustedByKey = adjusted.associateBy { it.key?.let { key -> AttentionKey(key).toString() } }
        val contextFiltered = if (contextDecision.allowNonCritical) {
            adjusted
        } else {
            adjusted.filter { it.priority >= effectivePolicy.criticalPriority }
        }

        val selection = attentionEngine.selectWithBudget(contextFiltered, effectivePolicy)
        val selectedKeys = selection.selected.mapNotNull { recommendation ->
            recommendation.key?.let { AttentionKey(it).toString() }
        }.toSet()

        val decisions = candidates.mapNotNull { candidate ->
            val recommendation = candidate.recommendation
            val recommendationKey = recommendation.key ?: return@mapNotNull null
            val attentionKey = AttentionKey(recommendationKey)
            if (candidate.attentionKey in selectedKeys) {
                lifecycleReconciler.evaluateExisting(
                    recommendation = recommendation,
                    record = previous.records[attentionKey],
                    now = now,
                )
            } else {
                AttentionDecision(
                    key = attentionKey,
                    action = AttentionAction.SUPPRESS,
                    reason = if (!contextDecision.allowNonCritical &&
                        (adjustedByKey[candidate.attentionKey]?.priority ?: 0.0) < effectivePolicy.criticalPriority
                    ) {
                        "Current context suppresses non-critical attention"
                    } else {
                        "Attention budget suppressed recommendation"
                    },
                    record = previous.records[attentionKey]
                        ?: me.moiclathy.dism.attention.lifecycle.AttentionRecord(key = attentionKey),
                )
            }
        }

        val explanations = candidates.associate { candidate ->
            val base = candidate.recommendation
            val adapted = adjustedByKey[candidate.attentionKey]
            requireNotNull(adapted) { "Missing adjusted recommendation" }
            candidate.attentionKey to explanationEngine.explain(
                selected = candidate.attentionKey in selectedKeys,
                basePriority = base.priority,
                effectivePriority = adapted.priority,
                minimumPriority = effectivePolicy.minimumPriority,
                criticalPriority = effectivePolicy.criticalPriority,
                budgetLimited = candidate.attentionKey !in selectedKeys,
                contextSuppressed = !contextDecision.allowNonCritical &&
                    adapted.priority < effectivePolicy.criticalPriority,
                nonCriticalSuppressed = !contextDecision.allowNonCritical,
            )
        }

        val events = decisions.map { decision ->
            AttentionEvent(
                id = Uuid.random().toString(),
                attentionKey = decision.key.toString(),
                type = AttentionEventType.DECISION,
                occurredAt = now,
                action = decision.action.name,
                explanation = explanations[decision.key.toString()],
            )
        }
        eventLog?.appendAll(events)

        val nextRecords = previous.records.toMutableMap()
        decisions.forEach { decision ->
            if (decision.action != AttentionAction.SUPPRESS) {
                nextRecords[decision.key] = decision.record
            }
        }

        return AttentionRuntimeResult(
            selection = selection,
            context = contextDecision,
            lifecycleDecisions = decisions,
            explanations = explanations,
            events = events,
            nextState = previous.copy(
                records = nextRecords,
                memories = decayedMemories,
            ),
        )
    }

    fun applyFeedback(
        state: AttentionRuntimeState,
        feedback: AttentionFeedback,
        eventLog: AttentionEventLog? = null,
    ): AttentionRuntimeState {
        val current = state.adaptations[feedback.attentionKey] ?: AttentionAdaptation(feedback.attentionKey)
        val next = adaptationEngine.learn(current, feedback)
        val oldSuppression = current.suppressionBias
        val delta = next.suppressionBias - oldSuppression
        val memory = state.memories[feedback.attentionKey] ?: AttentionMemory()
        val nextMemory = memoryEngine.apply(memory, delta, feedback.observedAt)
        val nextState = state.copy(
            adaptations = state.adaptations + (feedback.attentionKey to next),
            memories = state.memories + (feedback.attentionKey to nextMemory),
        )
        eventLog?.append(
            AttentionEvent(
                id = "runtime:${feedback.attentionKey}:feedback:${feedback.observedAt}:${feedback.action}",
                attentionKey = feedback.attentionKey,
                type = AttentionEventType.FEEDBACK,
                occurredAt = feedback.observedAt,
                feedbackAction = feedback.action,
            )
        )
        return nextState
    }
}
