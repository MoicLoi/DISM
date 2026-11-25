package me.moiclathy.dism.semantic.propagation

import me.moiclathy.dism.context.Context

/**
 * Reconciles two risk snapshots without owning mutable cache state.
 *
 * The caller owns the previous snapshot, which keeps lifecycle handling
 * deterministic and restart-safe.
 */
class RequirementRiskLifecycleReconciler {

    fun reconcile(
        previousRisks: List<RequirementRisk>,
        currentRisks: List<RequirementRisk>,
        context: Context,
    ): List<RequirementRiskLifecycleChange> {
        val previousByKey = previousRisks.associateBy(RequirementRisk::key)
        val currentByKey = currentRisks.associateBy(RequirementRisk::key)

        val changes = buildList {
            currentByKey.forEach { (key, current) ->
                val previous = previousByKey[key]
                add(
                    RequirementRiskLifecycleChange(
                        key = key,
                        lifecycle = if (previous == null) {
                            RequirementRiskLifecycle.CREATED
                        } else {
                            RequirementRiskLifecycle.PERSISTING
                        },
                        previous = previous,
                        current = current,
                    )
                )
            }

            previousByKey.forEach { (key, previous) ->
                if (key in currentByKey) return@forEach

                add(
                    RequirementRiskLifecycleChange(
                        key = key,
                        lifecycle = lifecycleForMissingRisk(
                            key = key,
                            context = context,
                        ),
                        previous = previous,
                        current = null,
                    )
                )
            }
        }

        return changes
    }

    private fun lifecycleForMissingRisk(
        key: RequirementRiskKey,
        context: Context,
    ): RequirementRiskLifecycle {
        val event = context.upcomingEvents.firstOrNull {
            it.eventId == key.eventId
        }

        return if (event?.endAt != null && event.endAt <= context.now) {
            RequirementRiskLifecycle.EXPIRED
        } else {
            RequirementRiskLifecycle.RESOLVED
        }
    }
}
