package me.moiclathy.dism.schedule.context

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import kotlinx.serialization.Serializable

@Serializable
data class DynamicScheduleContextBindingSet(
    val bindings: List<DynamicScheduleContextBinding> = emptyList(),
) {
    init {
        require(bindings.map { it.id }.distinct().size == bindings.size) {
            "context binding ids must be unique"
        }
    }

    val activeBindings: List<DynamicScheduleContextBinding>
        get() = bindings.filter { it.active }

    fun forItem(
        itemId: DynamicScheduleItemId,
    ): List<DynamicScheduleContextBinding> =
        activeBindings.filter { it.itemId == itemId }

    fun forContext(
        contextKey: String,
    ): List<DynamicScheduleContextBinding> =
        activeBindings.filter { it.contextKey == contextKey }

    fun add(
        binding: DynamicScheduleContextBinding,
    ): DynamicScheduleContextBindingSet =
        copy(
            bindings = bindings + binding,
        )

    fun replace(
        binding: DynamicScheduleContextBinding,
    ): DynamicScheduleContextBindingSet {
        require(bindings.any { it.id == binding.id }) {
            "binding does not exist: ${binding.id}"
        }

        return copy(
            bindings = bindings.map {
                if (it.id == binding.id) {
                    binding
                } else {
                    it
                }
            },
        )
    }
}
