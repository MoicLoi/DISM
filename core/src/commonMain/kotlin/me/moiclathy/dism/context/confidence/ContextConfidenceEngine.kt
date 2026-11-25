package me.moiclathy.dism.context.confidence

import kotlin.time.Instant
import me.moiclathy.dism.context.model.ContextMode

/**
 * Context Confidence Engine。
 *
 * 负责：
 *
 * 1. 时间衰减
 * 2. 新证据增强
 * 3. Confidence 上下界
 *
 * 不负责：
 *
 * - Context Resolution
 * - Context Transition
 * - Recommendation
 * - Attention
 */
interface ContextConfidenceEngine {

    /**
     * 对现有 Confidence 进行时间衰减。
     */
    fun decay(
        confidence: ContextConfidence,
        now: Instant,
        policy: ContextConfidencePolicy =
            ContextConfidencePolicy(),
    ): ContextConfidenceResult

    /**
     * 根据新证据更新 Confidence。
     */
    fun applyEvidence(
        confidence: ContextConfidence,
        evidence: ContextConfidenceEvidence,
        now: Instant,
        policy: ContextConfidencePolicy =
            ContextConfidencePolicy(),
    ): ContextConfidenceResult
}
