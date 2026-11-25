package me.moiclathy.dism.event.constraint

/**
 * 单个时间约束违反信息。
 */
data class TimeConstraintViolation(
    val constraint: TimeConstraint,
    val message: String,
)
