package me.moiclathy.dism.planning.recommendation

import me.moiclathy.dism.semantic.propagation.RequirementRiskLifecycle
import me.moiclathy.dism.semantic.propagation.RequirementRiskLifecycleChange
import me.moiclathy.dism.semantic.propagation.RequirementRiskKey

/**
 * Reconciles recommendation snapshots into lifecycle changes.
 *
 * The reconciler does not own mutable cache state. The caller supplies the
 * previous snapshot, which keeps the result deterministic and restart-safe.
 */
class RecommendationLifecycleReconciler {

    fun reconcile(
        previousRecommendations: List<Recommendation>,
        currentRecommendations: List<Recommendation>,
        riskLifecycleChanges: List<RequirementRiskLifecycleChange> = emptyList(),
    ): List<RecommendationLifecycleChange> {
        val previousByKey = previousRecommendations
            .mapNotNull { recommendation ->
                recommendation.key?.let { it to recommendation }
            }
            .toMap()

        val currentByKey = currentRecommendations
            .mapNotNull { recommendation ->
                recommendation.key?.let { it to recommendation }
            }
            .toMap()

        val riskLifecycleByKey = riskLifecycleChanges
            .associateBy(
                keySelector = { it.key },
            )

        return buildList {
            currentByKey.forEach { (key, current) ->
                val previous = previousByKey[key]

                when {
                    previous == null -> add(
                        RecommendationLifecycleChange(
                            key = key,
                            lifecycle = RecommendationLifecycle.CREATED,
                            previous = null,
                            current = current,
                        )
                    )

                    semanticallyChanged(previous, current) -> add(
                        RecommendationLifecycleChange(
                            key = key,
                            lifecycle = RecommendationLifecycle.UPDATED,
                            previous = previous,
                            current = current,
                        )
                    )
                }
            }

            previousByKey.forEach { (key, previous) ->
                if (key in currentByKey) return@forEach

                val riskLifecycle = riskLifecycleByKey[
                    RequirementRiskKey(
                        eventId = key.eventId,
                        requiredEntityId = key.requiredEntityId,
                    )
                ]
                val lifecycle = when (riskLifecycle?.lifecycle) {
                    RequirementRiskLifecycle.EXPIRED ->
                        RecommendationLifecycle.EXPIRED

                    RequirementRiskLifecycle.RESOLVED ->
                        RecommendationLifecycle.RESOLVED

                    else ->
                        // Recommendation lifecycle is driven by the absence of
                        // its source risk. If no explicit risk lifecycle was
                        // supplied, disappearance is conservatively treated as
                        // resolution rather than expiry.
                        RecommendationLifecycle.RESOLVED
                }

                add(
                    RecommendationLifecycleChange(
                        key = key,
                        lifecycle = lifecycle,
                        previous = previous,
                        current = null,
                    )
                )
            }
        }
    }

    private fun semanticallyChanged(
        previous: Recommendation,
        current: Recommendation,
    ): Boolean {
        // Recommendation.id is an instance identifier and changes whenever a
        // recommendation is regenerated. It must not by itself cause UPDATED.
        return previous.copy(id = current.id) != current
    }
}
