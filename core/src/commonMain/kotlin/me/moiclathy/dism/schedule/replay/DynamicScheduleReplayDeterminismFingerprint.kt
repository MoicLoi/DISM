package me.moiclathy.dism.schedule.replay

import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceResult

/**
 * 12.29
 *
 * 用于确定性测试的轻量 Fingerprint。
 *
 * 注意：
 *
 * 这不是安全哈希，也不是持久化 ID。
 *
 * 只用于：
 *
 * - Determinism Test
 * - Debug
 * - Replay Verification
 */
data class DynamicScheduleReplayDeterminismFingerprint(
    val values:
        List<String>,
) {

    val canonical:
        String
        get() =
            values.joinToString("|")

    companion object {

        fun from(
            result:
                DynamicScheduleIntelligenceResult,
        ):
            DynamicScheduleReplayDeterminismFingerprint {

            val priorities =
                result
                    .attentionProjection
                    .candidates
                    .sortedBy {
                        it.itemId.toString()
                    }
                    .map {
                        "priority:" +
                            it.itemId +
                            "=" +
                            it.candidate
                                .recommendation
                                .priority
                    }

            val conflicts =
                result
                    .attentionConflicts
                    .entries
                    .sortedBy {
                        it.key.toString()
                    }
                    .map {
                        "conflict:" +
                            it.key +
                            "=" +
                            (it.value.conflict != null)
                    }

            val explanations =
                result
                    .attentionExplanations
                    .keys
                    .sortedBy {
                        it.toString()
                    }
                    .map {
                        "explanation:$it"
                    }

            return DynamicScheduleReplayDeterminismFingerprint(
                values =
                    priorities +
                        conflicts +
                        explanations,
            )
        }
    }
}
