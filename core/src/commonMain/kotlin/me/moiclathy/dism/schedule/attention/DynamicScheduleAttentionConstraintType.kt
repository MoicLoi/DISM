package me.moiclathy.dism.schedule.attention

enum class DynamicScheduleAttentionConstraintType {

    /**
     * 用户明确要求关注。
     */
    EXPLICIT_USER_REQUEST,

    /**
     * 必须满足的 Schedule Dependency。
     */
    REQUIRED_DEPENDENCY,

    /**
     * 必须满足的 Context。
     */
    REQUIRED_CONTEXT,

    /**
     * 即将到达硬时间锚点。
     */
    HARD_TEMPORAL_ANCHOR,

    /**
     * 普通 Schedule priority。
     */
    NORMAL_PRIORITY,

    /**
     * Attention Memory / learned preference。
     */
    LEARNED_PREFERENCE,
}
