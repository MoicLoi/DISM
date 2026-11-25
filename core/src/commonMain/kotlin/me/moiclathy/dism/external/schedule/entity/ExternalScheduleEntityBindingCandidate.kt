package me.moiclathy.dism.external.schedule.entity

import me.moiclathy.dism.domain.entity.Entity

/**
 * Entity Binding 候选。
 *
 * 注意：
 * Candidate 不代表已经绑定。
 */
data class ExternalScheduleEntityBindingCandidate(
    val entity: Entity,
    val bindingType: ExternalScheduleEntityBindingType,
    val sourceValue: String,
    val score: Double,
) {

    init {
        require(sourceValue.isNotBlank()) {
            "sourceValue must not be blank."
        }

        require(score in 0.0..1.0) {
            "score must be between 0.0 and 1.0."
        }
    }
}
