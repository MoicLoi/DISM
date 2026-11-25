package me.moiclathy.dism.context.confidence

import kotlin.time.Instant
import me.moiclathy.dism.context.model.ContextMode

/**
 * 一次 Confidence 更新结果。
 */
data class ContextConfidenceResult(
    val confidence: ContextConfidence,
    val change: ContextConfidenceChange?,
    val acceptedEvidence: List<ContextConfidenceEvidence> = emptyList(),
    val rejectedEvidence: List<ContextConfidenceEvidence> = emptyList(),
    val evaluatedAt: Instant,
)
