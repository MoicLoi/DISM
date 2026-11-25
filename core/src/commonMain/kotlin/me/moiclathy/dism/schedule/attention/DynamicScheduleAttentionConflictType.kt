package me.moiclathy.dism.schedule.attention

enum class DynamicScheduleAttentionConflictType {

    /**
     * Learned memory wants to suppress,
     * but a hard requirement requires attention.
     */
    LEARNED_SUPPRESSION_VS_HARD_REQUIREMENT,

    /**
     * Multiple hard requirements disagree.
     */
    HARD_CONSTRAINT_CONFLICT,

    /**
     * Context says attention is needed,
     * while learned context memory says it is usually unnecessary.
     */
    CONTEXT_MEMORY_CONFLICT,

    /**
     * User explicitly requested attention.
     */
    EXPLICIT_REQUEST_OVERRIDE,

    /**
     * No actual conflict.
     */
    NONE,
}
