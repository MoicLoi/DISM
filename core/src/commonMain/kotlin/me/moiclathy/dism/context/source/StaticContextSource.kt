package me.moiclathy.dism.context.source

import me.moiclathy.dism.context.model.ContextClaim

/**
 * 静态 Context Source。
 *
 * 用于测试和确定性场景。
 *
 * 它不会根据 now 自动推导任何东西，
 * 只是返回预先配置好的 claims。
 */
class StaticContextSource(
    override val id: ContextSourceId,
    override val type: ContextSourceType,
    private val claims: List<ContextClaim>,
) : ContextSource {

    override fun collect(
        input: ContextSourceInput,
    ): ContextSourceResult {
        return ContextSourceResult(
            sourceId = id,
            sourceType = type,
            claims = claims,
        )
    }
}
