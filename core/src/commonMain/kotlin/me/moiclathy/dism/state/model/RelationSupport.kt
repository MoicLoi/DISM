package me.moiclathy.dism.state.model

import kotlin.uuid.Uuid

/**
 * 一份 Evidence 对某个 RelationState 的语义支持。
 *
 * evidenceId:
 *   指向原始 Evidence。
 *
 * semanticConfidence:
 *   表示"从该 Evidence 中得出这条 Relation"有多可信。
 *
 * 注意：
 * Evidence 自己还有 Evidence.confidence（来源可信度），
 * 两者不是同一个概念。
 *
 * 例如：
 * 群里的官方通知 Evidence confidence = 1.0
 * LLM 判断它意味着：实验课 start_at 16:00
 * semantic confidence = 0.85
 * 这两个数字都不会丢。
 */
data class RelationSupport(
    val evidenceId: Uuid,
    val semanticConfidence: Double,
) {
    init {
        require(semanticConfidence in 0.0..1.0) {
            "Semantic confidence must be between 0.0 and 1.0"
        }
    }
}
