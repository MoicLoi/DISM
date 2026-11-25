package me.moiclathy.dism.schedule.adaptation.provenance

/**
 * 14.16
 *
 * 结构化查询的结果。
 *
 * 注意 [provenance] 和 [nodes] 是分开的：
 *
 * ```text
 * provenance  = 完整图
 * nodes       = 这次查询要的投影（可能只要 decisive）
 * ```
 *
 * 这样调用方既能拿到完整上下文，
 * 又能只显示用户关心的那几条。
 */
data class DynamicScheduleDecisionProvenanceQueryResult(
    val provenance:
        DynamicScheduleDecisionProvenance?,

    val nodes:
        List<DynamicScheduleDecisionProvenanceNode> = emptyList(),

    val found:
        Boolean =
        provenance != null,

    val diagnostics:
        List<String> = emptyList(),
)
