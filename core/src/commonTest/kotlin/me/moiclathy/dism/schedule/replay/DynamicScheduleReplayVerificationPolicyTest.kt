package me.moiclathy.dism.schedule.replay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DynamicScheduleReplayVerificationPolicyTest {

    @Test
    fun defaultPolicyRequiresSameEngineVersion() {

        val policy =
            DynamicScheduleReplayVerificationPolicy()

        assertTrue(
            policy.requireSameEngineVersion,
        )
    }

    @Test
    fun defaultPolicyAllowsSmallPriorityFloatingPointError() {

        val policy =
            DynamicScheduleReplayVerificationPolicy()

        assertTrue(
            policy.priorityTolerance > 0.0,
        )
    }

    @Test
    fun explanationParityIsEnabledByDefault() {

        val policy =
            DynamicScheduleReplayVerificationPolicy()

        assertTrue(
            policy.requireExplanationParity,
        )
    }

    @Test
    fun conflictParityIsEnabledByDefault() {

        val policy =
            DynamicScheduleReplayVerificationPolicy()

        assertTrue(
            policy.requireConflictParity,
        )
    }
}
