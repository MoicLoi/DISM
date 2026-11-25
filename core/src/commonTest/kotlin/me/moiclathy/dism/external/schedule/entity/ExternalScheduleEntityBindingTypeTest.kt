package me.moiclathy.dism.external.schedule.entity

import kotlin.test.Test
import kotlin.test.assertEquals

class ExternalScheduleEntityBindingTypeTest {

    @Test
    fun bindingTypesShouldBeStable() {

        assertEquals(
            listOf(
                ExternalScheduleEntityBindingType.COURSE,
                ExternalScheduleEntityBindingType.INSTRUCTOR,
                ExternalScheduleEntityBindingType.LOCATION,
            ),
            ExternalScheduleEntityBindingType.entries,
        )
    }
}
