package me.moiclathy.dism.schedule.adaptation.provenance

/**
 * 14.16
 *
 * 面向产品层的 Provenance 解释。
 *
 * ### 为什么要有这一层
 *
 * 产品层**不应该**接触：
 *
 * ```text
 * Snapshot
 * Trace
 * ProvenanceNode
 * ```
 *
 * 它们只需要：
 *
 * ```kotlin
 * val explanation = session.explainProvenance(itemId, now)
 *
 * explanation?.summary
 * explanation?.decisiveFactors
 * ```
 *
 * 这是 DISM 解释体系的分层点：
 *
 * ```text
 * 14.15 Provenance   → 结构化因果图（机器可读）
 * 14.16 Explanation  → 人可读的一句话 + 因素列表
 * ```
 */
data class DynamicScheduleDecisionProvenanceExplanation(
    val itemId: String,

    val summary: String,

    val factors:
        List<DynamicScheduleDecisionProvenanceExplanationFactor>,

    val decisiveFactors:
        List<DynamicScheduleDecisionProvenanceExplanationFactor>,

    val diagnostics:
        List<String> = emptyList(),

    val complete:
        Boolean,
) {

    val factorCount: Int
        get() = factors.size
}

/**
 * 14.16
 *
 * 单个解释因素。
 *
 * 它刻意**不包含节点 id 与父链**：
 * 那是 14.15 的图该表达的东西。
 * 这里只保留"用户能读懂的那部分"。
 */
data class DynamicScheduleDecisionProvenanceExplanationFactor(
    val type:
        DynamicScheduleDecisionProvenanceNodeType,

    val message:
        String,

    val decisive:
        Boolean,

    val metadata:
        Map<String, String> = emptyMap(),
)
