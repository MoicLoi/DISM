package me.moiclathy.dism.schedule.provenance

import kotlin.uuid.Uuid

/**
 * 13.12.3
 */
data class DynamicScheduleDecisionProvenanceId(
    val value: Uuid = Uuid.random(),
) {

    override fun toString(): String =
        value.toString()
}
