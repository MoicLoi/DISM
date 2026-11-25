package me.moiclathy.dism.context.source

import kotlin.time.Instant

/**
 * 提供给 ContextSource 的统一输入。
 *
 * ContextSource 不应该自己维护"现在几点"。
 * 当前时间由调用方明确传入，保证测试和回放具有确定性。
 */
data class ContextSourceInput(
    val now: Instant,
    val sourceHint: ContextSourceId? = null,
    val metadata: Map<String, String> = emptyMap(),
)
