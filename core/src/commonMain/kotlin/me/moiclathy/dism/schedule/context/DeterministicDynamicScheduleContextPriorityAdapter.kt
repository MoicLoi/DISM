package me.moiclathy.dism.schedule.context

/**
 * 14.7
 *
 * 确定性 Context Priority Adapter。
 *
 * ### 优先级顺序
 *
 * ```text
 * 1. EXCLUDED 激活        → excludedFactor（最强，默认 0.25）
 * 2. REQUIRED 未满足      → requiredMissingFactor（默认 0.50）
 * 3. 其余                 → 1.0 + preferredScore × preferredInfluence
 * ```
 *
 * 因为 Context 是"现在这个场景允不允许"，信息量远大于历史行为，
 * 所以它的影响允许比 14.4–14.6 更强（最低 0.25 vs 最多 ±0.20）。
 *
 * ### 为什么 `reasons` 要拼出具体数值
 *
 * 14.7 是用户第一次真正看到"为什么这条排到后面了"的地方，
 * 所以 reasons 必须自解释，而不是只写 `"context applied"`。
 */
class DeterministicDynamicScheduleContextPriorityAdapter :
    DynamicScheduleContextPriorityAdapter {

    override fun adapt(
        match: DynamicScheduleContextMatch,
        policy: DynamicScheduleContextPriorityPolicy,
    ): DynamicScheduleContextPriorityInfluence {

        /*
         * EXCLUDED 是最强的 Context 信号。
         *
         * 但这里只降低 Priority，
         * 不负责把 Item 改成 BLOCKED。
         */
        if (match.excludedSatisfied) {
            return DynamicScheduleContextPriorityInfluence(
                itemId = match.itemId,
                contextFactor =
                    policy.excludedFactor,
                contextMatch = 0.0,
                excluded = true,
                requiredSatisfied =
                    match.requiredSatisfied,
                matchedContextKeys =
                    match.matchedContextKeys,
                reasons =
                    listOf(
                        "Excluded context is currently active",
                        "Priority reduced by context exclusion",
                    ),
            )
        }

        /*
         * REQUIRED Context 未满足。
         *
         * Context Binding 层已经负责状态投影；
         * 这里仅反映 Priority 层面的影响。
         */
        if (!match.requiredSatisfied) {
            return DynamicScheduleContextPriorityInfluence(
                itemId = match.itemId,
                contextFactor =
                    policy.requiredMissingFactor,
                contextMatch = 0.0,
                excluded = false,
                requiredSatisfied = false,
                matchedContextKeys =
                    match.matchedContextKeys,
                reasons =
                    listOf(
                        "Required context is missing",
                        "Priority reduced until required context is satisfied",
                    ),
            )
        }

        /*
         * REQUIRED 已满足。
         *
         * preferredScore：
         *
         * 0.0 = 没有匹配 Preferred Context
         * 1.0 = Preferred Context 全部匹配
         */
        val preferredInfluence =
            match.preferredScore
                .coerceIn(0.0, 1.0) *
                policy.preferredInfluence *
                policy.matchInfluence

        val contextFactor =
            (
                1.0 +
                    preferredInfluence
                )
                .coerceIn(
                    1.0 - policy.maximumDecrease,
                    1.0 + policy.maximumIncrease,
                )

        return DynamicScheduleContextPriorityInfluence(
            itemId = match.itemId,
            contextFactor = contextFactor,
            contextMatch = match.preferredScore,
            excluded = false,
            requiredSatisfied = true,
            matchedContextKeys =
                match.matchedContextKeys,
            reasons =
                buildReasons(
                    match = match,
                    contextFactor = contextFactor,
                ),
        )
    }

    private fun buildReasons(
        match: DynamicScheduleContextMatch,
        contextFactor: Double,
    ): List<String> {

        return buildList {

            if (match.matchedContextKeys.isNotEmpty()) {
                add(
                    "Matched context: " +
                        match.matchedContextKeys
                            .sorted()
                            .joinToString(", "),
                )
            }

            add(
                "Preferred context score=" +
                    match.preferredScore,
            )

            add(
                "Context priority factor=" +
                    contextFactor,
            )
        }
    }
}
