package me.moiclathy.dism.attention

import me.moiclathy.dism.planning.recommendation.Recommendation

class AttentionEngine {

    fun select(
        recommendations: List<Recommendation>,
        policy: AttentionPolicy = AttentionPolicy(),
    ): List<Recommendation> {
        return selectWithBudget(
            recommendations = recommendations,
            policy = policy,
        ).selected
    }

    /**
     * Applies the user's attention budget.
     *
     * Normal recommendations consume the soft budget. Critical ones may use
     * the overflow budget only when explicitly enabled, and can never exceed
     * hardLimit. This keeps "tell me more" separate from "the system is
     * allowed to interrupt me more".
     */
    fun selectWithBudget(
        recommendations: List<Recommendation>,
        policy: AttentionPolicy = AttentionPolicy(),
    ): AttentionSelectionResult {
        val candidates = recommendations
            .asSequence()
            .filter { it.priority >= policy.minimumPriority }
            .sortedByDescending { it.priority }
            .toList()

        if (policy.hardLimit == 0 || candidates.isEmpty()) {
            return AttentionSelectionResult(
                selected = emptyList(),
                suppressed = candidates,
                softLimitReached = policy.softLimit == 0 && candidates.isNotEmpty(),
                hardLimitReached = policy.hardLimit == 0 && candidates.isNotEmpty(),
            )
        }

        val normalCount = minOf(policy.softLimit, candidates.size)
        val selected = candidates.take(normalCount).toMutableList()

        if (
            policy.allowCriticalOverflow &&
            selected.size < policy.hardLimit &&
            candidates.size > selected.size
        ) {
            candidates
                .drop(selected.size)
                .asSequence()
                .filter { it.priority >= policy.criticalPriority }
                .take(policy.hardLimit - selected.size)
                .forEach { selected += it }
        }

        val selectedSet = selected.toSet()
        val suppressed = candidates.filterNot { it in selectedSet }

        return AttentionSelectionResult(
            selected = selected,
            suppressed = suppressed,
            softLimitReached = candidates.size > policy.softLimit,
            hardLimitReached = selected.size >= policy.hardLimit && candidates.size > selected.size,
        )
    }
}
