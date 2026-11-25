package me.moiclathy.dism.semantic.propagation

import kotlin.uuid.Uuid

data class RequirementRisk(
    val eventId: Uuid,
    val requiredEntityId: Uuid,

    /**
     * 当前最后已知位置。
     *
     * null 表示系统不知道它在哪里，
     * 而不是表示“它没有位置”。
     */
    val knownLocationId: Uuid? = null,

    val confidence: Double,
    val severity: Double,
    val reasons: List<String>,
) {
    init {
        require(confidence in 0.0..1.0)
        require(severity in 0.0..1.0)
    }
}