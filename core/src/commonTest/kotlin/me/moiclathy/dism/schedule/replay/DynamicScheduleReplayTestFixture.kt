package me.moiclathy.dism.schedule.replay

import kotlin.test.assertNotNull

import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceInput
import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceRuntimePolicy
import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceRuntimeState
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId

/**
 * 12.29
 *
 * Replay 测试统一 Fixture。
 *
 * 目的：
 * 避免每一个测试自己构造一套略有差异的 Runtime Input。
 */
object DynamicScheduleReplayTestFixture {

    suspend fun requireReplayInput(
        snapshotId:
            DynamicScheduleDecisionSnapshotId,
        store:
            DynamicScheduleDecisionReplayInputSnapshotStore,
    ):
        DynamicScheduleDecisionReplayInputSnapshot {

        val input =
            store.get(
                snapshotId,
            )

        return assertNotNull(
            input,
            "Replay input must exist for snapshot $snapshotId",
        )
    }

    fun copyInput(
        input:
            DynamicScheduleDecisionReplayInputSnapshot,

        runtimeInput:
            DynamicScheduleIntelligenceInput =
            input.input,

        previous:
            DynamicScheduleIntelligenceRuntimeState =
            input.previousState,

        policy:
            DynamicScheduleIntelligenceRuntimePolicy =
            input.policy,

        evaluatedAt:
            kotlin.time.Instant =
            input.evaluatedAt,

        engineVersion:
            String =
            input.engineVersion,
    ):
        DynamicScheduleDecisionReplayInputSnapshot {

        return input.copy(
            input =
                runtimeInput,

            previousState =
                previous,

            policy =
                policy,

            evaluatedAt =
                evaluatedAt,

            engineVersion =
                engineVersion,
        )
    }
}
