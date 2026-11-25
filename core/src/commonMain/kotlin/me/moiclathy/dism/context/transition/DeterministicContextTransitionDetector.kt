package me.moiclathy.dism.context.transition

import kotlin.time.Instant
import me.moiclathy.dism.context.model.ContextMode

/**
 * 确定性的 Context Transition Detector。
 *
 * 相同输入始终得到相同输出。
 *
 * 它不：
 * - 修改 ContextState
 * - 访问系统时间
 * - 查询数据库
 * - 调用 LLM
 * - 修改 Attention
 */
class DeterministicContextTransitionDetector :
    ContextTransitionDetector {

    override fun detect(
        previousMode: ContextMode?,
        currentMode: ContextMode?,
        currentConfidence: Double,
        now: Instant,
        policy: ContextTransitionPolicy,
    ): ContextTransitionResult {

        require(currentConfidence in 0.0..1.0) {
            "currentConfidence must be between 0.0 and 1.0."
        }

        /*
         * 当前 Resolution 置信度不足。
         *
         * 重要：
         * 这里不是把 Context 设置成 null。
         *
         * 而是：
         *
         * "证据不足，不进行状态转移。"
         */
        if (
            currentMode != null &&
            currentConfidence <
                policy.minimumTransitionConfidence
        ) {
            return ContextTransitionResult(
                transition = null,
                previousMode = previousMode,
                currentMode = previousMode,
                applied = false,
                evaluatedAt = now,
            )
        }

        /*
         * 没有旧 Context，
         * 当前有 Context：
         *
         * null → IN_CLASS
         */
        if (
            previousMode == null &&
            currentMode != null
        ) {
            val transition =
                ContextTransition(
                    type = ContextTransitionType.ENTERED,
                    from = null,
                    to = currentMode,
                    occurredAt = now,
                    confidence = currentConfidence,
                    reason = "No previous context; current context entered.",
                )

            return ContextTransitionResult(
                transition = transition,
                previousMode = null,
                currentMode = currentMode,
                applied = true,
                evaluatedAt = now,
            )
        }

        /*
         * 有旧 Context，
         * 当前没有 Context。
         *
         * 例如：
         *
         * IN_CLASS → null
         */
        if (
            previousMode != null &&
            currentMode == null
        ) {

            if (!policy.allowExitToUnknown) {
                return ContextTransitionResult(
                    transition = null,
                    previousMode = previousMode,
                    currentMode = previousMode,
                    applied = false,
                    evaluatedAt = now,
                )
            }

            val transition =
                ContextTransition(
                    type = ContextTransitionType.EXITED,
                    from = previousMode,
                    to = null,
                    occurredAt = now,
                    confidence = currentConfidence,
                    reason = "Current context is no longer resolved.",
                )

            return ContextTransitionResult(
                transition = transition,
                previousMode = previousMode,
                currentMode = null,
                applied = true,
                evaluatedAt = now,
            )
        }

        /*
         * 两边都是 null。
         */
        if (
            previousMode == null &&
            currentMode == null
        ) {
            return ContextTransitionResult(
                transition = null,
                previousMode = null,
                currentMode = null,
                applied = false,
                evaluatedAt = now,
            )
        }

        /*
         * 到这里：
         *
         * previous != null
         * current != null
         */

        if (previousMode == currentMode) {

            if (!policy.emitPersisting) {
                return ContextTransitionResult(
                    transition = null,
                    previousMode = previousMode,
                    currentMode = currentMode,
                    applied = false,
                    evaluatedAt = now,
                )
            }

            val transition =
                ContextTransition(
                    type = ContextTransitionType.PERSISTING,
                    from = previousMode,
                    to = currentMode,
                    occurredAt = now,
                    confidence = currentConfidence,
                    reason = "Context remains unchanged.",
                )

            return ContextTransitionResult(
                transition = transition,
                previousMode = previousMode,
                currentMode = currentMode,
                applied = false,
                evaluatedAt = now,
            )
        }

        /*
         * 两个不同的有效 Context：
         *
         * IN_TRANSIT → IN_CLASS
         *
         * 这是 CHANGED，而不是：
         *
         * EXITED + ENTERED
         *
         * 因为对 Runtime 来说，
         * 这是一个原子 Context Transition。
         */
        val transition =
            ContextTransition(
                type = ContextTransitionType.CHANGED,
                from = previousMode,
                to = currentMode,
                occurredAt = now,
                confidence = currentConfidence,
                reason = "Resolved context changed.",
            )

        return ContextTransitionResult(
            transition = transition,
            previousMode = previousMode,
            currentMode = currentMode,
            applied = true,
            evaluatedAt = now,
        )
    }
}
