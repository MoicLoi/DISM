package me.moiclathy.dism.state.query

import me.moiclathy.dism.evidence.Evidence
import me.moiclathy.dism.state.model.RelationState

/**
 * 一条状态的可解释性结果。
 *
 * explanation 不改变 Core 的事实模型，只把已有的：
 * RelationState → TransitionRecord → Evidence
 * 组织成可供 UI / LLM / debug 使用的因果链。
 */
data class RelationExplanation(
    val state: RelationState,
    /** 直接支持该状态成立的 Evidence。保持历史 API 语义。 */
    val evidence: List<Evidence>,
    /** 该状态为什么在 validFrom 开始成立。 */
    val establishedBy: TransitionCause? = null,
    /** 如果该状态已经结束，为什么在 validUntil 结束。 */
    val endedBy: TransitionCause? = null,
) {
    /** explanation 中出现过的全部证据，去重且保持首次出现顺序。 */
    val allEvidence: List<Evidence>
        get() = buildList {
            val seen = mutableSetOf<kotlin.uuid.Uuid>()
            (evidence +
                (establishedBy?.evidence ?: emptyList()) +
                (endedBy?.evidence ?: emptyList()))
                .forEach { item ->
                    if (seen.add(item.id)) add(item)
                }
        }
}
