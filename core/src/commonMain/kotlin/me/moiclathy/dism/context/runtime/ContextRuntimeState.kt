package me.moiclathy.dism.context.runtime

import kotlin.time.Instant
import me.moiclathy.dism.context.confidence.ContextConfidence
import me.moiclathy.dism.context.model.ContextMode

/**
 * 当前 Context Runtime 的可恢复状态。
 *
 * 注意：
 *
 * ContextRuntimeState 是 Projection，
 * 不是 Event Log。
 */
data class ContextRuntimeState(
    val activeMode: ContextMode? = null,

    val confidence: ContextConfidence? = null,

    val records: Map<ContextMode, ContextRuntimeRecord> =
        emptyMap(),

    val restoredAt: Instant? = null,

    val eventCount: Int = 0,
)
