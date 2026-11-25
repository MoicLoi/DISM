package me.moiclathy.dism.schedule.model

import me.moiclathy.dism.api.DismPublicApi
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Immutable schedule-level model for the 12.x intelligence pipeline.
 */
@Serializable
@DismPublicApi
data class DynamicScheduleModel(
    val id: DynamicScheduleId,
    val items: List<DynamicScheduleItem> = emptyList(),
    val generatedAt: Instant? = null,
    val revision: Long = 0L,
) {
    init {
        require(revision >= 0) { "revision must be >= 0" }
        require(items.all { it.scheduleId == id }) {
            "every item must belong to the schedule"
        }
        require(items.map { it.id }.distinct().size == items.size) {
            "schedule item ids must be unique"
        }
    }

    val size: Int
        get() = items.size

    val scheduledItems: List<DynamicScheduleItem>
        get() = items.filter { it.isScheduled }

    val activeItems: List<DynamicScheduleItem>
        get() = items.filter { it.isActive }

    val pendingItems: List<DynamicScheduleItem>
        get() = items.filterNot { it.isTerminal }

    fun item(id: DynamicScheduleItemId): DynamicScheduleItem? =
        items.firstOrNull { it.id == id }

    fun replaceItem(item: DynamicScheduleItem): DynamicScheduleModel {
        require(item.scheduleId == id) { "replacement item must belong to the schedule" }
        val replaced = items.map { if (it.id == item.id) item else it }
        require(replaced.any { it.id == item.id }) { "item does not exist: ${item.id}" }
        return copy(items = replaced, revision = revision + 1)
    }

    fun upsertItem(item: DynamicScheduleItem): DynamicScheduleModel {
        require(item.scheduleId == id) { "item must belong to the schedule" }
        val exists = items.any { it.id == item.id }
        val nextItems = if (exists) {
            items.map { if (it.id == item.id) item else it }
        } else {
            items + item
        }
        return copy(items = nextItems, revision = revision + 1)
    }
}
