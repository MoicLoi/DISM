package me.moiclathy.dism.schedule.adaptation.verification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

import me.moiclathy.dism.schedule.adaptation
    .DynamicScheduleAttentionHistory
import me.moiclathy.dism.schedule.adaptation
    .DynamicSchedulePriorityAdaptation
import me.moiclathy.dism.schedule.adaptation
    .DynamicSchedulePriorityAdaptationPolicy
import me.moiclathy.dism.schedule.adaptation.replay
    .DynamicScheduleAdaptationReplayInput
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayTestData

/**
 * 14.13
 *
 * 指纹与 INPUT_CHANGED 这一级。
 *
 * 规格的测试只覆盖了 EXACT_MATCH / POLICY_CHANGED /
 * ENGINE_CHANGED / BEHAVIOR_CHANGED，
 * 但 §十 的分类阶梯里 INPUT_CHANGED 是**第一级**。
 * 如果它坏了，输入已经变了的情况下会误报 BEHAVIOR_CHANGED，
 * 也就是把"数据不是同一份"说成"算法退化"。
 */
class DynamicScheduleAdaptationInputFingerprintTest {

    private fun input(
        ignoredCount: Int = 2,
        policy: DynamicSchedulePriorityAdaptationPolicy =
            DynamicSchedulePriorityAdaptationPolicy(),
    ) =
        DynamicScheduleAdaptationReplayInput(
            model = DynamicScheduleReplayTestData.model(),
            histories =
                mapOf(
                    DynamicScheduleReplayTestData.itemId.value to
                        DynamicScheduleAttentionHistory(
                            attentionKey =
                                DynamicScheduleReplayTestData
                                    .itemId
                                    .value,
                            shownCount = 10,
                            ignoredCount = ignoredCount,
                        ),
                ),
            policy = policy,
        )

    @Test
    fun identicalInputsShareTheSameFingerprint() {

        assertEquals(
            DynamicScheduleAdaptationInputFingerprint.from(input()),
            DynamicScheduleAdaptationInputFingerprint.from(input()),
        )
    }

    @Test
    fun historyChangeChangesTheFingerprint() {

        assertNotEquals(
            DynamicScheduleAdaptationInputFingerprint.from(input()),
            DynamicScheduleAdaptationInputFingerprint
                .from(input(ignoredCount = 9)),
        )
    }

    @Test
    fun inputFingerprintIgnoresPolicy() {

        /*
         * 这是"POLICY_CHANGED 不是死代码"的前提。
         */
        assertEquals(
            DynamicScheduleAdaptationInputFingerprint.from(input()),
            DynamicScheduleAdaptationInputFingerprint
                .from(
                    input(
                        policy =
                            DynamicSchedulePriorityAdaptationPolicy(
                                ignoredPenalty = 0.99,
                            ),
                    ),
                ),
        )
    }

    @Test
    fun policyFingerprintDetectsPolicyChange() {

        assertNotEquals(
            DynamicScheduleAdaptationInputFingerprint.ofPolicy(
                DynamicSchedulePriorityAdaptationPolicy(),
            ),
            DynamicScheduleAdaptationInputFingerprint.ofPolicy(
                DynamicSchedulePriorityAdaptationPolicy(
                    ignoredPenalty = 0.99,
                ),
            ),
        )
    }

    @Test
    fun changedInputProducesInputChanged() {

        val engine =
            DeterministicDynamicScheduleAdaptationVerificationEngine()

        val historical =
            listOf(
                DynamicSchedulePriorityAdaptation(
                    itemId = DynamicScheduleItemId("item-1"),
                    basePriority = 0.5,
                    adaptationFactor = 1.2,
                    effectivePriority = 0.6,
                ),
            )

        val report =
            engine.verify(
                historical = historical,

                /*
                 * 结果一模一样——只有输入不同。
                 */
                replayed = historical,

                historicalEngineVersion = "14.12",
                replayEngineVersion = "14.12",

                historicalPolicyFingerprint = "policy-a",
                replayPolicyFingerprint = "policy-a",

                historicalInputFingerprint =
                    DynamicScheduleAdaptationInputFingerprint
                        .from(input())
                        .value,

                replayInputFingerprint =
                    DynamicScheduleAdaptationInputFingerprint
                        .from(input(ignoredCount = 9))
                        .value,
            )

        assertEquals(
            DynamicScheduleAdaptationVerificationStatus
                .INPUT_CHANGED,
            report.status,
        )

        assertTrue(
            !report.verified,
            "input drift is not a verification pass",
        )
    }

    @Test
    fun inputChangedHasPriorityOverPolicyChanged() {

        val engine =
            DeterministicDynamicScheduleAdaptationVerificationEngine()

        val historical =
            listOf(
                DynamicSchedulePriorityAdaptation(
                    itemId = DynamicScheduleItemId("item-1"),
                    basePriority = 0.5,
                    adaptationFactor = 1.2,
                    effectivePriority = 0.6,
                ),
            )

        val report =
            engine.verify(
                historical = historical,
                replayed = historical,

                historicalEngineVersion = "14.12",
                replayEngineVersion = "14.12",

                historicalPolicyFingerprint = "policy-a",
                replayPolicyFingerprint = "policy-b",

                historicalInputFingerprint = "input-1",
                replayInputFingerprint = "input-2",
            )

        assertEquals(
            DynamicScheduleAdaptationVerificationStatus
                .INPUT_CHANGED,
            report.status,
            "input drift outranks policy drift in the §十 ladder",
        )
    }
}
