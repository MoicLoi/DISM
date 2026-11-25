package me.moiclathy.dism.context.transition

import kotlin.time.Instant
import me.moiclathy.dism.context.model.ContextMode

/**
 * Context Transition Detector 的输出。
 */
data class ContextTransitionResult(
    val transition: ContextTransition?,
    val previousMode: ContextMode?,
    val currentMode: ContextMode?,
    val applied: Boolean,
    val evaluatedAt: Instant,
) {

    /**
     * 是否真的发生了 Context 改变。
     */
    val changed: Boolean
        get() = transition != null &&
            transition.type != ContextTransitionType.PERSISTING

    /**
     * 是否只是保持原 Context。
     */
    val persisting: Boolean
        get() =
            transition?.type ==
                ContextTransitionType.PERSISTING
}
