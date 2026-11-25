package me.moiclathy.dism.schedule.replay

import kotlin.test.Test
import kotlin.test.assertEquals

class DynamicScheduleReplayDeterminismFingerprintTest {

    @Test
    fun sameResultProducesSameFingerprint() {

        val replay =
            DynamicScheduleReplayVerificationTestData
                .exactMatch()

        val first =
            DynamicScheduleReplayDeterminismFingerprint.from(
                replay.replayedResult,
            )

        val second =
            DynamicScheduleReplayDeterminismFingerprint.from(
                replay.replayedResult,
            )

        assertEquals(
            first.canonical,
            second.canonical,
        )
    }
}
