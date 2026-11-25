package me.moiclathy.dism.schedule.adaptation.verification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import me.moiclathy.dism.schedule.adaptation
    .DynamicSchedulePriorityAdaptation
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

class DeterministicDynamicScheduleAdaptationVerificationEngineTest {

    private val engine =
        DeterministicDynamicScheduleAdaptationVerificationEngine()

    private val itemId =
        DynamicScheduleItemId("breakfast")

    private fun adaptation(
        factor: Double = 1.20,
        priority: Double = 0.72,
    ) =
        DynamicSchedulePriorityAdaptation(
            itemId = itemId,
            basePriority = 0.60,
            adaptationFactor = factor,
            effectivePriority = priority,
            reasons = listOf("test"),
        )

    @Test
    fun exactMatch() {

        val result =
            engine.verify(
                historical =
                    listOf(
                        adaptation(),
                    ),

                replayed =
                    listOf(
                        adaptation(),
                    ),

                historicalEngineVersion =
                    "14.12",

                replayEngineVersion =
                    "14.12",

                historicalPolicyFingerprint =
                    "policy-a",

                replayPolicyFingerprint =
                    "policy-a",
            )

        assertEquals(
            DynamicScheduleAdaptationVerificationStatus
                .EXACT_MATCH,
            result.status,
        )

        assertTrue(result.verified)
        assertEquals(0, result.differenceCount)
    }

    @Test
    fun adaptationChangeProducesBehaviorChanged() {

        val result =
            engine.verify(
                historical =
                    listOf(
                        adaptation(
                            factor = 1.20,
                            priority = 0.72,
                        ),
                    ),

                replayed =
                    listOf(
                        adaptation(
                            factor = 1.10,
                            priority = 0.66,
                        ),
                    ),

                historicalEngineVersion =
                    "14.12",

                replayEngineVersion =
                    "14.12",

                historicalPolicyFingerprint =
                    "policy-a",

                replayPolicyFingerprint =
                    "policy-a",
            )

        assertEquals(
            DynamicScheduleAdaptationVerificationStatus
                .BEHAVIOR_CHANGED,
            result.status,
        )

        assertTrue(result.changed)
        assertTrue(result.differenceCount >= 2)
    }

    @Test
    fun engineChangeHasPriorityOverBehaviorChange() {

        val result =
            engine.verify(
                historical =
                    listOf(
                        adaptation(
                            factor = 1.20,
                        ),
                    ),

                replayed =
                    listOf(
                        adaptation(
                            factor = 1.10,
                        ),
                    ),

                historicalEngineVersion =
                    "14.12",

                replayEngineVersion =
                    "14.13",

                historicalPolicyFingerprint =
                    "policy-a",

                replayPolicyFingerprint =
                    "policy-a",
            )

        assertEquals(
            DynamicScheduleAdaptationVerificationStatus
                .ENGINE_CHANGED,
            result.status,
        )
    }

    @Test
    fun policyChangeHasPriorityOverBehaviorChange() {

        val result =
            engine.verify(
                historical =
                    listOf(
                        adaptation(
                            factor = 1.20,
                        ),
                    ),

                replayed =
                    listOf(
                        adaptation(
                            factor = 1.10,
                        ),
                    ),

                historicalEngineVersion =
                    "14.12",

                replayEngineVersion =
                    "14.12",

                historicalPolicyFingerprint =
                    "policy-a",

                replayPolicyFingerprint =
                    "policy-b",
            )

        assertEquals(
            DynamicScheduleAdaptationVerificationStatus
                .POLICY_CHANGED,
            result.status,
        )
    }

    @Test
    fun missingItemIsDetected() {

        val result =
            engine.verify(
                historical =
                    listOf(
                        adaptation(),
                    ),

                replayed =
                    emptyList(),

                historicalEngineVersion =
                    "14.12",

                replayEngineVersion =
                    "14.12",

                historicalPolicyFingerprint =
                    "policy-a",

                replayPolicyFingerprint =
                    "policy-a",
            )

        assertEquals(
            DynamicScheduleAdaptationVerificationStatus
                .BEHAVIOR_CHANGED,
            result.status,
        )

        assertTrue(
            result.differences.any {
                it.factor == "item"
            },
        )
    }
}
