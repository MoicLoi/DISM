package me.moiclathy.dism.context.transition

import kotlin.time.Instant
import me.moiclathy.dism.context.model.ContextMode

/**
 * Context Transition 检测器。
 *
 * 输入：
 *
 * previous Context
 * +
 * current resolved Context
 *
 * 输出：
 *
 * ContextTransition
 *
 * 不修改任何外部状态。
 */
interface ContextTransitionDetector {

    fun detect(
        previousMode: ContextMode?,
        currentMode: ContextMode?,
        currentConfidence: Double,
        now: Instant,
        policy: ContextTransitionPolicy = ContextTransitionPolicy(),
    ): ContextTransitionResult
}
