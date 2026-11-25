package me.moiclathy.dism.semantic.resolution

/**
 * Entity Resolution 的结果。
 *
 * 区分三种情况：
 * - Resolved: 确定性地映射到一个 Entity
 * - Ambiguous: 多个候选都合理，需要进一步推理或询问
 * - Unresolved: 没有找到任何合理候选
 *
 * 不把 Ambiguous 和 Unresolved 混成 Entity?，
 * 因为后续处理逻辑完全不同。
 */
sealed interface EntityResolutionResult {

    data class Resolved(
        val candidate: EntityResolutionCandidate,
    ) : EntityResolutionResult

    data class Ambiguous(
        val candidates: List<EntityResolutionCandidate>,
    ) : EntityResolutionResult

    data object Unresolved : EntityResolutionResult
}
