package me.moiclathy.dism.external.schedule.entity

import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.external.schedule.ExternalScheduleId

/**
 * 一个 External Schedule 字段与 DISM Entity 的绑定。
 */
data class ExternalScheduleEntityBinding(
    val sourceEntryId: ExternalScheduleId,
    val bindingType: ExternalScheduleEntityBindingType,
    val entity: Entity,
    val sourceValue: String,
    val confidence: Double = 1.0,
    val matchedBy: String = "exact",
) {

    init {
        require(sourceValue.isNotBlank()) {
            "sourceValue must not be blank."
        }

        require(confidence in 0.0..1.0) {
            "confidence must be between 0.0 and 1.0."
        }

        require(matchedBy.isNotBlank()) {
            "matchedBy must not be blank."
        }
    }
}
