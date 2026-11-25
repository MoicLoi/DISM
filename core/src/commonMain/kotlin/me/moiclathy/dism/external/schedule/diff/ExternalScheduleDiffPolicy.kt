package me.moiclathy.dism.external.schedule.diff

/**
 * External Schedule Diff 策略。
 */
data class ExternalScheduleDiffPolicy(

    /**
     * 比较标题。
     */
    val compareTitle: Boolean = true,

    /**
     * 比较开始时间。
     */
    val compareStartAt: Boolean = true,

    /**
     * 比较结束时间。
     */
    val compareEndAt: Boolean = true,

    /**
     * 比较地点 Entity。
     */
    val compareLocationEntity: Boolean = true,

    /**
     * 比较教师 Entity。
     */
    val compareInstructorEntity: Boolean = true,

    /**
     * 比较课程 Entity。
     */
    val compareCourseEntity: Boolean = true,

    /**
     * 比较 fingerprint。
     *
     * 如果 fingerprint 存在，可以作为快速变化检测。
     */
    val compareFingerprint: Boolean = true,

    /**
     * 是否把 active 状态变化视为 CHANGED。
     */
    val compareActive: Boolean = true,
)
