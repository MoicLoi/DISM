package me.moiclathy.dism.schedule.context

import me.moiclathy.dism.schedule.model.DynamicScheduleModel

/**
 * Context is supplied as opaque keys.
 *
 * This keeps the 12.4 schedule layer independent from the concrete
 * 9.x Context implementation.
 */
data class DynamicScheduleContextBindingInput(
    val model: DynamicScheduleModel,
    val bindings: DynamicScheduleContextBindingSet =
        DynamicScheduleContextBindingSet(),
    val activeContextKeys: Set<String> = emptySet(),
    val policy: DynamicScheduleContextBindingPolicy =
        DynamicScheduleContextBindingPolicy(),
) {
    init {
        require(activeContextKeys.none { it.isBlank() }) {
            "active context keys must not be blank"
        }

        val itemIds = model.items.map { it.id }.toSet()

        require(
            bindings.bindings.all { it.itemId in itemIds },
        ) {
            "every binding must reference an item in the schedule model"
        }
    }
}
