package me.moiclathy.dism.external.schedule.normalization

import kotlin.test.Test
import kotlin.test.assertEquals

class ExternalScheduleNormalizationPolicyTest {

    @Test
    fun defaultPolicyShouldBeStable() {

        val policy =
            ExternalScheduleNormalizationPolicy()

        assertEquals(
            true,
            policy.trimText,
        )

        assertEquals(
            true,
            policy.collapseWhitespace,
        )

        assertEquals(
            true,
            policy.emptyTextToNull,
        )

        assertEquals(
            true,
            policy.normalizeCourseCode,
        )

        assertEquals(
            true,
            policy.normalizeLocation,
        )

        assertEquals(
            true,
            policy.preserveOriginalMetadata,
        )
    }
}
