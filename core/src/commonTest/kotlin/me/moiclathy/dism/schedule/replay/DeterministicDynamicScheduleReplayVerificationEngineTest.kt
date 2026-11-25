package me.moiclathy.dism.schedule.replay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

import me.moiclathy.dism.schedule.version.DynamicScheduleHistoryEngineVersion
import me.moiclathy.dism.schedule.version.DynamicScheduleHistoryVersionCompatibility
import me.moiclathy.dism.schedule.version.DynamicScheduleHistoryVersionCompatibilityResult

/**
 * 12.29
 *
 * Replay Verification Engine 测试。
 */
class DeterministicDynamicScheduleReplayVerificationEngineTest {

    private val engine =
        DeterministicDynamicScheduleReplayVerificationEngine()

    @Test
    fun exactMatchProducesExactMatchStatus() {

        val replay =
            DynamicScheduleReplayVerificationTestData
                .exactMatch()

        val report =
            engine.verify(
                replay =
                    replay,

                replayEngineVersion =
                    replay.replayEngineVersion,
            )

        assertEquals(
            DynamicScheduleReplayVerificationStatus.EXACT_MATCH,
            report.status,
        )

        assertTrue(
            report.verified,
        )

        assertFalse(
            report.changed,
        )

        assertEquals(
            0,
            report.differenceCount,
        )
    }

    @Test
    fun changedPriorityProducesBehaviorChanged() {

        val replay =
            DynamicScheduleReplayVerificationTestData
                .priorityChanged()

        val report =
            engine.verify(
                replay =
                    replay,

                replayEngineVersion =
                    replay.replayEngineVersion,
            )

        assertEquals(
            DynamicScheduleReplayVerificationStatus
                .BEHAVIOR_CHANGED,
            report.status,
        )

        assertTrue(
            report.changed,
        )

        assertTrue(
            report.differences.any {
                it.factor == "priority"
            },
        )
    }

    @Test
    fun engineVersionMismatchIsReported() {

        val replay =
            DynamicScheduleReplayVerificationTestData
                .exactMatch()

        /*
         * 13.10：版本是否"不匹配"由 Compatibility Registry 的
         * 显式判断决定，而不是裸字符串比较。
         *
         * 这里声明"当前引擎版本是 different-engine，且未注册兼容规则"，
         * 于是仍然必须报 ENGINE_VERSION_MISMATCH。
         */
        val incompatible =
            replay.copy(
                versionCompatibility =
                    DynamicScheduleHistoryVersionCompatibilityResult(
                        historicalVersion =
                            DynamicScheduleHistoryEngineVersion(
                                replay.replayInput.engineVersion,
                            ),

                        currentVersion =
                            DynamicScheduleHistoryEngineVersion(
                                "different-engine",
                            ),

                        compatibility =
                            DynamicScheduleHistoryVersionCompatibility
                                .UNKNOWN,

                        reason =
                            "No compatibility rule registered.",
                    ),
            )

        val report =
            engine.verify(
                replay = incompatible,

                replayEngineVersion =
                    "different-engine",
            )

        assertEquals(
            DynamicScheduleReplayVerificationStatus
                .ENGINE_VERSION_MISMATCH,
            report.status,
        )

        assertFalse(
            report.verified,
        )
    }

    @Test
    fun declaredReplayCompatibleVersionIsNotAnEngineMismatch() {

        /*
         * 13.10 的核心改进：
         *
         * 版本号不同 ≠ 语义不同。
         *
         * 一旦显式注册了 13.1 → 13.10 可重放兼容，
         * Verification 就不应该再把它当成
         * ENGINE_VERSION_MISMATCH 这种"错误状态"。
         */
        val replay =
            DynamicScheduleReplayVerificationTestData
                .exactMatch()

        val compatible =
            replay.copy(
                versionCompatibility =
                    DynamicScheduleHistoryVersionCompatibilityResult(
                        historicalVersion =
                            DynamicScheduleHistoryEngineVersion(
                                replay.replayInput.engineVersion,
                            ),

                        currentVersion =
                            DynamicScheduleHistoryEngineVersion(
                                "13.10",
                            ),

                        compatibility =
                            DynamicScheduleHistoryVersionCompatibility
                                .REPLAY_COMPATIBLE,

                        reason =
                            "Explicitly registered as replay-compatible.",
                    ),
            )

        val report =
            engine.verify(
                replay = compatible,

                replayEngineVersion = "13.10",
            )

        assertEquals(
            DynamicScheduleReplayVerificationStatus
                .EXACT_MATCH,
            report.status,
            "已声明可重放兼容的版本差异不应再报 ENGINE_VERSION_MISMATCH",
        )

        assertTrue(report.verified)
    }

    @Test
    fun incompleteInputIsReported() {

        val replay =
            DynamicScheduleReplayVerificationTestData
                .incomplete()

        val report =
            engine.verify(
                replay =
                    replay,

                replayEngineVersion =
                    replay.replayEngineVersion,
            )

        assertEquals(
            DynamicScheduleReplayVerificationStatus
                .INPUT_INCOMPLETE,
            report.status,
        )

        assertFalse(
            report.verified,
        )
    }
}
