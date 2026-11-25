package me.moiclathy.dism.semantic.observation

import me.moiclathy.dism.semantic.resolution.EntityResolutionResult

/**
 * Observation Draft 的 Entity Resolution 结果。
 *
 * 保留 subject 和 object 各自的 resolution result，
 * 让上层可以看到具体是哪一侧失败、失败原因是什么。
 *
 * 例如：
 * subject = Resolved
 * object = Ambiguous
 */
sealed interface ObservationResolutionResult {

    data class Resolved(
        val observation: SemanticObservation,
    ) : ObservationResolutionResult

    data class Unresolved(
        val subjectResolution: EntityResolutionResult,
        val objectResolution: EntityResolutionResult,
    ) : ObservationResolutionResult
}
