package me.moiclathy.dism.context.transition

/**
 * Context Transition 策略。
 */
data class ContextTransitionPolicy(

    /**
     * Resolution confidence 低于此值时，
     * 不允许触发 Context Transition。
     *
     * 这样可以避免：
     *
     * IN_CLASS 0.31
     *
     * 这种低置信度结果导致 Context 来回抖动。
     */
    val minimumTransitionConfidence: Double = 0.50,

    /**
     * 是否允许 UNKNOWN / null Context
     * 触发 EXITED。
     */
    val allowExitToUnknown: Boolean = true,

    /**
     * 如果前后 Context 相同，
     * 是否产生 PERSISTING。
     */
    val emitPersisting: Boolean = false,
) {

    init {
        require(
            minimumTransitionConfidence in 0.0..1.0
        ) {
            "minimumTransitionConfidence must be between 0.0 and 1.0."
        }
    }
}
