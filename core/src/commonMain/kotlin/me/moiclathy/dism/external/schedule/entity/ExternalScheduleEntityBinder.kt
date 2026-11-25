package me.moiclathy.dism.external.schedule.entity

import me.moiclathy.dism.domain.entity.EntityRegistry
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.normalization.NormalizedExternalScheduleEntry

/**
 * External Schedule Entity Binding 接口。
 */
interface ExternalScheduleEntityBinder {

    fun bind(
        entry: NormalizedExternalScheduleEntry,
        entityRegistry: EntityRegistry,
    ): ExternalScheduleEntityBindingResult
}
