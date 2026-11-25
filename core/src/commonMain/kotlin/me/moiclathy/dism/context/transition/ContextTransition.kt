package me.moiclathy.dism.context.transition

import kotlin.time.Instant
import me.moiclathy.dism.context.model.ContextMode

/**
 * 一次 Context 状态转移。
 *
 * 这是一个纯事件描述，不会自己修改 ContextState。
 */
data class ContextTransition(
    val type: ContextTransitionType,

    /**
     * 转移前的 Context。
     *
     * ENTERED 时可以为空。
     */
    val from: ContextMode? = null,

    /**
     * 转移后的 Context。
     *
     * EXITED / EXPIRED 时可以为空。
     */
    val to: ContextMode? = null,

    /**
     * Context 转移发生的逻辑时间。
     */
    val occurredAt: Instant,

    /**
     * 当前 Resolution 的 confidence。
     */
    val confidence: Double = 1.0,

    /**
     * 可供 Explain / Event Log 使用的原因。
     */
    val reason: String? = null,
) {

    init {
        require(confidence in 0.0..1.0) {
            "Context transition confidence must be between 0.0 and 1.0."
        }

        when (type) {
            ContextTransitionType.ENTERED -> {
                require(to != null) {
                    "ENTERED transition requires a target ContextMode."
                }
            }

            ContextTransitionType.CHANGED -> {
                require(from != null) {
                    "CHANGED transition requires a source ContextMode."
                }

                require(to != null) {
                    "CHANGED transition requires a target ContextMode."
                }

                require(from != to) {
                    "CHANGED transition requires different ContextMode values."
                }
            }

            ContextTransitionType.EXITED,
            ContextTransitionType.EXPIRED,
            -> {
                require(from != null) {
                    "$type transition requires a source ContextMode."
                }
            }

            ContextTransitionType.PERSISTING -> {
                require(from != null) {
                    "PERSISTING transition requires a current ContextMode."
                }

                require(to == from) {
                    "PERSISTING transition requires from == to."
                }
            }
        }
    }
}
