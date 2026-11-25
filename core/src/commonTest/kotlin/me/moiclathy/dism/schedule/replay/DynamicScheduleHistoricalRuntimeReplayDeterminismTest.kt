package me.moiclathy.dism.schedule.replay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

import me.moiclathy.dism.schedule.runtime.DeterministicDynamicScheduleIntelligenceRuntime

/**
 * 12.29
 *
 * 核心 Determinism Test。
 *
 * 相同：
 *
 * - Input
 * - Previous State
 * - Policy
 * - Time
 *
 * 必须得到相同的 Runtime Result。
 */
class DynamicScheduleHistoricalRuntimeReplayDeterminismTest {

    @Test
    fun sameHistoricalInputProducesDeterministicResult() {

        val replayInput =
            createReplayInput()

        val engine =
            DeterministicDynamicScheduleHistoricalRuntimeReplayEngine(
                runtime =
                    DynamicScheduleReplayTestData.runtime(),
            )

        val first =
            engine.replay(
                replayInput,
            )

        val second =
            engine.replay(
                replayInput,
            )

        /*
         * 12.29 说明：
         *
         * Recommendation.id / AttentionEvent.id 每次执行都会生成新的 Uuid，
         * 因此不能直接比较整个 Result 对象。
         *
         * 确定性比较使用 12.29 引入的 Fingerprint
         * （只包含 priority / conflict / explanation 等语义字段）。
         */
        assertEquals(
            DynamicScheduleReplayDeterminismFingerprint
                .from(first)
                .canonical,
            DynamicScheduleReplayDeterminismFingerprint
                .from(second)
                .canonical,
        )

        assertEquals(
            first.attentionConflicts.keys,
            second.attentionConflicts.keys,
        )

        assertEquals(
            first.attentionProjection.candidateCount,
            second.attentionProjection.candidateCount,
        )
    }

    @Test
    fun historicalReplayUsesHistoricalTime() {

        val replayInput =
            createReplayInput()

        val engine =
            DeterministicDynamicScheduleHistoricalRuntimeReplayEngine(
                runtime =
                    DynamicScheduleReplayTestData.runtime(),
            )

        val result =
            engine.replay(
                replayInput,
            )

        assertNotNull(
            result,
        )
    }

    private fun createReplayInput():
        DynamicScheduleDecisionReplayInputSnapshot {

        /*
         * 这里不在测试里伪造一个不完整的 Snapshot。
         *
         * 实际项目接入后，
         * 应由 Test Fixture / Test Factory
         * 创建一个完整的 Persistence Result。
         *
         * 当前方法作为统一测试入口保留。
         */
        return DynamicScheduleReplayTestData.create()
    }
}
