package me.moiclathy.dism.schedule.adaptation.provenance

/**
 * 14.16
 *
 * 确定性 Provenance 解释器。
 *
 * ### 它只读，不算
 *
 * ```text
 * Provenance → Explanation
 * ```
 *
 * 它**不会**重新执行 Runtime、不会重新算适配、
 * 不会读数据库。同一个 Provenance 永远得到同一段解释。
 *
 * ### 为什么 summary 里要提"部分完整"
 *
 * 解释最危险的地方是**过度自信**：
 *
 * ```text
 * "该决定包含 1 个决定性因素。"
 * ```
 *
 * 在 Provenance 还缺 Trace / Adaptation 的时候，
 * 这句话会让用户以为"原因就是这一个"，
 * 而真实情况是"我们只看到了一部分"。
 */
class DeterministicDynamicScheduleDecisionProvenanceExplainer {

    fun explain(
        provenance:
            DynamicScheduleDecisionProvenance,
    ): DynamicScheduleDecisionProvenanceExplanation {

        /*
         * INPUT 和 OUTPUT 对所有决策都一样，
         * 它们描述的是"流程的两端"，不是"这次的原因"，
         * 所以不计入因素列表。
         */
        val relevantNodes =
            provenance.nodes
                .filter {
                    it.type !=
                        DynamicScheduleDecisionProvenanceNodeType.INPUT
                }
                .filter {
                    it.type !=
                        DynamicScheduleDecisionProvenanceNodeType.OUTPUT
                }

        val factors =
            relevantNodes.map {
                DynamicScheduleDecisionProvenanceExplanationFactor(
                    type =
                        it.type,

                    message =
                        it.message,

                    decisive =
                        it.decisive,

                    metadata =
                        it.metadata,
                )
            }

        val decisiveFactors =
            factors.filter {
                it.decisive
            }

        val summary =
            buildSummary(
                provenance = provenance,
                decisiveFactors = decisiveFactors,
            )

        return DynamicScheduleDecisionProvenanceExplanation(
            itemId =
                provenance.itemId.toString(),

            summary =
                summary,

            factors =
                factors,

            decisiveFactors =
                decisiveFactors,

            diagnostics =
                provenance.diagnostics,

            complete =
                provenance.complete,
        )
    }

    private fun buildSummary(
        provenance:
            DynamicScheduleDecisionProvenance,

        decisiveFactors:
            List<DynamicScheduleDecisionProvenanceExplanationFactor>,
    ): String {

        if (decisiveFactors.isEmpty()) {
            return "没有检测到明确的决定性因素。"
        }

        return buildString {

            append(
                "该决定包含 "
            )

            append(
                decisiveFactors.size
            )

            append(
                " 个决定性因素。"
            )

            if (!provenance.complete) {
                append(
                    " 当前 Provenance 仍然是部分完整的。"
                )
            }
        }
    }
}
