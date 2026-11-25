package me.moiclathy.dism.external.schedule.ics

import kotlin.test.Test
import kotlin.test.assertEquals

class IcsParserPolicyTest {

    @Test
    fun defaultPolicyShouldBeStable() {

        val policy =
            IcsParserPolicy()

        assertEquals(
            true,
            policy.requireVersion,
        )

        assertEquals(
            false,
            policy.requireEventUid,
        )

        assertEquals(
            true,
            policy.requireSummary,
        )

        assertEquals(
            true,
            policy.allowMissingDateTime,
        )

        assertEquals(
            true,
            policy.allowDateOnly,
        )
    }
}
