package me.moiclathy.dism.context.source

import me.moiclathy.dism.context.model.ContextClaim

/**
 * ContextSource 的一次采集结果。
 *
 * 这是一个中间结果，而不是最终 ContextState。
 */
data class ContextSourceResult(
    val sourceId: ContextSourceId,
    val sourceType: ContextSourceType,
    val claims: List<ContextClaim> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
) {

    init {
        require(sourceId.value.isNotBlank()) {
            "sourceId must not be blank."
        }
    }

    val isEmpty: Boolean
        get() = claims.isEmpty()
}
