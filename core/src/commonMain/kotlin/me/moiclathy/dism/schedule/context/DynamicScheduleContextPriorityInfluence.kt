package me.moiclathy.dism.schedule.context

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 14.7
 *
 * 当前 Context 对 Schedule Priority 的动态影响。
 *
 * 这里只描述 Context Influence，
 * 不直接修改 `DynamicScheduleItem`。
 *
 * ### 它不是状态阻塞
 *
 * `excluded == true` 或 `requiredSatisfied == false` 在这里
 * **只降低 Priority**，不代表把 Item 变成 BLOCKED。
 * 真正的状态阻塞由 ContextBinding 层负责投影。
 *
 * 这个区分很重要：用户看到"这条被排到后面了"
 * 和"这条被禁用了"是完全不同的两件事。
 */
data class DynamicScheduleContextPriorityInfluence(
    val itemId: DynamicScheduleItemId,

    /**
     * Context 对 Priority 的影响因子。
     *
     * ```text
     * 1.0  = 无影响
     * >1.0 = 当前 Context 提高 Priority
     * <1.0 = 当前 Context 降低 Priority
     * ```
     */
    val contextFactor: Double,

    /**
     * 当前 Context 的匹配程度。
     *
     * `0.0..1.0`
     */
    val contextMatch: Double,

    /**
     * 当前是否存在排斥该 Item 的 Context。
     */
    val excluded: Boolean,

    /**
     * 当前是否满足 REQUIRED Context。
     */
    val requiredSatisfied: Boolean,

    /**
     * 当前实际匹配的 Context。
     */
    val matchedContextKeys: Set<String>,

    /**
     * 解释信息。
     */
    val reasons: List<String> = emptyList(),
) {

    init {
        require(contextFactor >= 0.0)
        require(contextMatch in 0.0..1.0)
    }

    /**
     * 是否真的改变了 priority。
     */
    val isNeutral: Boolean
        get() = contextFactor == 1.0
}
