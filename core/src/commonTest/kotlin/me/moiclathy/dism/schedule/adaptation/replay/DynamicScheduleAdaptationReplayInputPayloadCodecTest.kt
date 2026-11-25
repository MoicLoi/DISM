package me.moiclathy.dism.schedule.adaptation.replay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

import me.moiclathy.dism.schedule.adaptation
    .DynamicScheduleAttentionHistory
import me.moiclathy.dism.schedule.adaptation
    .DynamicSchedulePriorityAdaptationPolicy
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayTestData

/**
 * 14.12
 *
 * Replay Input 必须能**无损**地过一遍 JSON。
 *
 * 特别是 policy：
 *
 * ```text
 * 历史 factor = 1.25
 * 重放 factor = 1.10
 * ```
 *
 * 如果 policy 在编解码里丢了默认值，重放会得到
 * "算法变了"的假警报。
 */
class DynamicScheduleAdaptationReplayInputPayloadCodecTest {

    private fun input() =
        DynamicScheduleAdaptationReplayInput(
            model = DynamicScheduleReplayTestData.model(),

            histories =
                mapOf(
                    "item-1" to
                        DynamicScheduleAttentionHistory(
                            attentionKey = "item-1",
                            shownCount = 10,
                            engagedCount = 3,
                            ignoredCount = 2,
                            dismissedCount = 1,
                            requestedCount = 1,
                            feedbackSignal = 0.25,
                        ),
                ),

            policy =
                DynamicSchedulePriorityAdaptationPolicy(
                    ignoredPenalty = 0.11,
                    engagedBonus = 0.22,
                    feedbackWeight = 0.33,
                ),
        )

    @Test
    fun replayInputRoundTripsThroughJson() {

        val original = input()

        val encoded =
            DynamicScheduleAdaptationReplayInputPayloadCodec
                .encode(original)

        val decoded =
            DynamicScheduleAdaptationReplayInputPayloadCodec
                .decode(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun nonDefaultPolicySurvivesTheRoundTrip() {

        val original = input()

        val decoded =
            DynamicScheduleAdaptationReplayInputPayloadCodec
                .decode(
                    DynamicScheduleAdaptationReplayInputPayloadCodec
                        .encode(original),
                )

        /*
         * 逐字段比较 policy，而不是只比较对象本身——
         * 这样失败信息能直接指出是哪个权重丢了。
         */
        assertEquals(
            original.policy.ignoredPenalty,
            decoded.policy.ignoredPenalty,
            absoluteTolerance = 0.000001,
        )

        assertEquals(
            original.policy.engagedBonus,
            decoded.policy.engagedBonus,
            absoluteTolerance = 0.000001,
        )

        assertEquals(
            original.policy.feedbackWeight,
            decoded.policy.feedbackWeight,
            absoluteTolerance = 0.000001,
        )

        assertEquals(
            original.histories,
            decoded.histories,
        )
    }

    @Test
    fun unsupportedSchemaVersionIsRejected() {

        val payload =
            """{"input":""" +
                DynamicScheduleAdaptationReplayInputPayloadCodec
                    .encode(input())
                    .substringAfter("\"input\":")
                    .substringBefore(",\"schemaVersion\"") +
                ""","schemaVersion":99}"""

        assertFailsWith<IllegalArgumentException> {
            DynamicScheduleAdaptationReplayInputPayloadCodec
                .decode(payload)
        }
    }
}
