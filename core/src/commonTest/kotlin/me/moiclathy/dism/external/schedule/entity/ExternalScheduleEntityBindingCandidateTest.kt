package me.moiclathy.dism.external.schedule.entity

import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityType
import kotlin.test.Test
import kotlin.test.assertEquals

class ExternalScheduleEntityBindingCandidateTest {

    @Test
    fun candidateShouldStoreScore() {

        val entity =
            Entity(
                type =
                    EntityType.COURSE,

                name =
                    "MATH101",
            )

        val candidate =
            ExternalScheduleEntityBindingCandidate(
                entity =
                    entity,

                bindingType =
                    ExternalScheduleEntityBindingType.COURSE,

                sourceValue =
                    "MATH101",

                score =
                    1.0,
            )

        assertEquals(
            1.0,
            candidate.score,
        )
    }
}
