package me.moiclathy.dism.schedule.attention

import me.moiclathy.dism.attention.lifecycle.AttentionKey
import me.moiclathy.dism.attention.runtime.AttentionRuntimeCandidate
import me.moiclathy.dism.planning.recommendation.Recommendation
import me.moiclathy.dism.planning.recommendation.RecommendationKey
import me.moiclathy.dism.planning.recommendation.RecommendationMessage
import me.moiclathy.dism.planning.recommendation.RecommendationType
import me.moiclathy.dism.schedule.model.DynamicScheduleItem
import me.moiclathy.dism.schedule.model.DynamicScheduleModel

class DeterministicDynamicScheduleAttentionProjector(
    private val identityResolver:
        DynamicScheduleAttentionIdentityResolver,
) : DynamicScheduleAttentionProjector {

    override fun project(
        input: DynamicScheduleAttentionInput,
        policy: DynamicScheduleAttentionPolicy,
    ): DynamicScheduleAttentionProjectionResult {

        val candidates =
            mutableListOf<DynamicScheduleAttentionCandidate>()

        val skipped =
            mutableListOf<String>()

        val diagnostics =
            mutableListOf<String>()

        input.model.items.forEach { item ->

            if (!policy.acceptsState(item.state)) {
                skipped += item.id.value
                diagnostics +=
                    "Item ${item.id} skipped because state=${item.state}"
                return@forEach
            }

            if (item.priority < policy.minimumPriority) {
                skipped += item.id.value
                diagnostics +=
                    "Item ${item.id} skipped because priority=${item.priority}"
                return@forEach
            }

            if (item.confidence < policy.minimumConfidence) {
                skipped += item.id.value
                diagnostics +=
                    "Item ${item.id} skipped because confidence=${item.confidence}"
                return@forEach
            }

            val recommendationKey =
                identityResolver.resolve(item.id)

            if (recommendationKey == null) {
                skipped += item.id.value
                diagnostics +=
                    "Item ${item.id} skipped because no stable RecommendationKey exists"
                return@forEach
            }

            val recommendation =
                createRecommendation(
                    item = item,
                    key = recommendationKey,
                    contextResult = input.contextResult,
                )

            /*
             * 12.10 修正：
             *
             * 现有 AttentionRuntime 要求 attentionKey 必须是
             * AttentionKey(recommendationKey).toString()，
             * 否则 7.x runtime 中的一致性 require 会直接失败。
             */
            val attentionKey =
                AttentionKey(recommendationKey).toString()

            candidates +=
                DynamicScheduleAttentionCandidate(
                    itemId = item.id,
                    candidate =
                        AttentionRuntimeCandidate(
                            recommendation = recommendation,
                            attentionKey = attentionKey,
                        ),
                    schedulePriority = item.priority,
                    scheduleConfidence = item.confidence,
                )
        }

        return DynamicScheduleAttentionProjectionResult(
            candidates = candidates,
            skippedItemIds = skipped,
            diagnostics = diagnostics,
        )
    }

    private fun createRecommendation(
        item: DynamicScheduleItem,
        key: RecommendationKey,
        contextResult:
            me.moiclathy.dism.schedule.context
                .DynamicScheduleContextBindingResult?,
    ): Recommendation {

        val contextMatch =
            contextResult
                ?.matches
                ?.firstOrNull { it.itemId == item.id }

        val contextMultiplier =
            contextMultiplier(contextMatch)

        val stateMultiplier =
            stateMultiplier(item)

        val priority =
            (
                item.priority *
                    contextMultiplier *
                    stateMultiplier
                ).coerceIn(0.0, 1.0)

        return Recommendation(
            type = RecommendationType.CHECK_INFORMATION,
            message = RecommendationMessage.Generic(
                key = buildMessageKey(item),
            ),
            priority = priority,
            confidence = item.confidence,
            reasons = buildReasons(
                item = item,
                contextMatch = contextMatch,
            ),
            key = key,
        )
    }

    private fun contextMultiplier(
        match:
            me.moiclathy.dism.schedule.context
                .DynamicScheduleContextMatch?,
    ): Double {

        if (match == null) {
            return 1.0
        }

        if (!match.contextSatisfied) {
            return 0.0
        }

        return match.preferredScore
            .coerceAtLeast(0.25)
            .coerceIn(0.0, 1.0)
    }

    private fun stateMultiplier(
        item: DynamicScheduleItem,
    ): Double {
        return when (item.state) {
            me.moiclathy.dism.schedule.model.DynamicScheduleState.ACTIVE ->
                1.0

            me.moiclathy.dism.schedule.model.DynamicScheduleState.READY ->
                1.0

            me.moiclathy.dism.schedule.model.DynamicScheduleState.BLOCKED ->
                1.0

            me.moiclathy.dism.schedule.model.DynamicScheduleState.UPCOMING ->
                0.85

            me.moiclathy.dism.schedule.model.DynamicScheduleState.POSTPONED ->
                0.65

            else ->
                0.0
        }
    }

    private fun buildMessageKey(
        item: DynamicScheduleItem,
    ): String {
        return "dynamic_schedule:${item.id.value}"
    }

    private fun buildReasons(
        item: DynamicScheduleItem,
        contextMatch:
            me.moiclathy.dism.schedule.context
                .DynamicScheduleContextMatch?,
    ): List<String> {

        return buildList {

            add("Schedule item: ${item.title}")
            add("Schedule state: ${item.state}")

            if (item.isScheduled) {
                add("Item has temporal information")
            }

            if (item.state ==
                me.moiclathy.dism.schedule.model.DynamicScheduleState.BLOCKED
            ) {
                add("Schedule item is currently blocked")
            }

            if (contextMatch != null) {

                if (contextMatch.contextSatisfied) {
                    add("Current context satisfies schedule requirements")
                }

                if (contextMatch.unmatchedRequiredContextKeys.isNotEmpty()) {
                    add(
                        "Required context missing: " +
                            contextMatch.unmatchedRequiredContextKeys
                                .joinToString(", "),
                    )
                }

                if (contextMatch.matchedExcludedContextKeys.isNotEmpty()) {
                    add(
                        "Excluded context active: " +
                            contextMatch.matchedExcludedContextKeys
                                .joinToString(", "),
                    )
                }
            }
        }
    }
}
