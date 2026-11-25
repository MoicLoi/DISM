package me.moiclathy.dism.schedule.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DynamicScheduleModelTest {
    @Test
    fun modelKeepsItemsInsideSchedule() {
        val scheduleId = DynamicScheduleId("schedule-1")
        val item = DynamicScheduleItem(
            id = DynamicScheduleItemId("item-1"),
            scheduleId = scheduleId,
            title = "Morning class",
            type = DynamicScheduleItemType.EVENT,
            source = DynamicScheduleSource.EXTERNAL,
            state = DynamicScheduleState.UPCOMING,
        )

        val model = DynamicScheduleModel(scheduleId, listOf(item))

        assertEquals(1, model.size)
        assertEquals(item, model.item(DynamicScheduleItemId("item-1")))
    }

    @Test
    fun upsertIncrementsRevisionOnlyForValidItem() {
        val scheduleId = DynamicScheduleId("schedule-1")
        val item = DynamicScheduleItem(
            id = DynamicScheduleItemId("item-1"),
            scheduleId = scheduleId,
            title = "Morning class",
            type = DynamicScheduleItemType.EVENT,
            source = DynamicScheduleSource.EXTERNAL,
        )

        val model = DynamicScheduleModel(scheduleId).upsertItem(item)
        val updated = item.copy(state = DynamicScheduleState.ACTIVE)
        val next = model.upsertItem(updated)

        assertEquals(1L, model.revision)
        assertEquals(2L, next.revision)
        assertEquals(DynamicScheduleState.ACTIVE, next.item(item.id)?.state)
    }

    @Test
    fun invalidScoresAreRejected() {
        val scheduleId = DynamicScheduleId("schedule-1")

        assertFailsWith<IllegalArgumentException> {
            DynamicScheduleItem(
                id = DynamicScheduleItemId("item-1"),
                scheduleId = scheduleId,
                title = "Invalid",
                type = DynamicScheduleItemType.TASK,
                source = DynamicScheduleSource.USER_INPUT,
                priority = 1.1,
            )
        }
    }
}
