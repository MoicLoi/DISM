package me.moiclathy.dism.external.schedule.entity

import kotlin.test.Test
import kotlin.test.assertEquals

class ExternalScheduleEntityBindingPolicyTest {

    @Test
    fun defaultPolicyShouldBeConservative() {

        val policy =
            ExternalScheduleEntityBindingPolicy()

        assertEquals(
            true,
            policy.bindCourse,
        )

        assertEquals(
            true,
            policy.bindInstructor,
        )

        assertEquals(
            true,
            policy.bindLocation,
        )

        assertEquals(
            true,
            policy.useAliases,
        )

        assertEquals(
            false,
            policy.allowEntityCreation,
        )
    }
}
