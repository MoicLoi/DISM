package me.moiclathy.dism.semantic

import me.moiclathy.dism.context.Context
import me.moiclathy.dism.state.model.RelationState
import kotlin.time.Duration.Companion.hours

class SemanticEvaluator {

    fun evaluate(
        state: RelationState,
        context: Context,
    ): SemanticEvaluation {

        if (!state.isActive) {
            return SemanticEvaluation(
                relevance = 0.0,
                urgency = 0.0,
                confidence = state.confidence,
                consequence = 0.0,
            )
        }

        val urgency = calculateUrgency(context)

        return SemanticEvaluation(
            relevance = 1.0,
            urgency = urgency,
            confidence = state.confidence,
            consequence = 0.5,
        )
    }

    private fun calculateUrgency(
        context: Context,
    ): Double {
        val nearestEvent = context.upcomingEvents
            .filter { it.startAt >= context.now }
            .minByOrNull { it.startAt }
            ?: return 0.0

        val remaining = nearestEvent.startAt - context.now

        return when {
            remaining <= 1.hours -> 1.0
            remaining <= 3.hours -> 0.8
            remaining <= 6.hours -> 0.5
            else -> 0.2
        }
    }
}