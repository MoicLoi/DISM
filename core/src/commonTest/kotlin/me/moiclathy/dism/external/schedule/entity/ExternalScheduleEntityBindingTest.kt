package me.moiclathy.dism.external.schedule.entity

import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityType
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExternalScheduleEntityBindingTest {

    private val entity =
        Entity(
            type =
                EntityType.COURSE,

            name =
                "MATH101",
        )

    @Test
    fun bindingShouldStoreFields() {

        val binding =
            ExternalScheduleEntityBinding(
                sourceEntryId =
                    ExternalScheduleId(
                        "entry-001"
                    ),

                bindingType =
                    ExternalScheduleEntityBindingType.COURSE,

                entity =
                    entity,

                sourceValue =
                    "MATH101",

                confidence =
                    1.0,

                matchedBy =
                    "exact",
            )

        assertEquals(
            "MATH101",
            binding.sourceValue,
        )

        assertEquals(
            1.0,
            binding.confidence,
        )
    }

    @Test
    fun blankSourceValueShouldFail() {

        assertFailsWith<IllegalArgumentException> {

            ExternalScheduleEntityBinding(
                sourceEntryId =
                    ExternalScheduleId(
                        "entry"
                    ),

                bindingType =
                    ExternalScheduleEntityBindingType.COURSE,

                entity =
                    entity,

                sourceValue =
                    " ",

                confidence =
                    1.0,
            )
        }
    }
}
