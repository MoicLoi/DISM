package me.moiclathy.dism.schedule.adaptation

import kotlin.math.exp
import kotlin.time.Duration

class DeterministicDynamicScheduleAttentionDecayEngine :
    DynamicScheduleAttentionDecay {

    override fun decay(
        input: DynamicScheduleAttentionDecayInput,
        policy: DynamicScheduleAttentionDecayPolicy,
    ): DynamicScheduleAttentionDecayResult {

        val result = mutableMapOf<
            String,
            DynamicScheduleDecayedAttentionHistory,
        >()

        val diagnostics = mutableListOf<String>()

        input.histories.forEach { (key, history) ->

            if (history.attentionKey != key) {
                diagnostics +=
                    "History key mismatch: mapKey=$key " +
                    "historyKey=${history.attentionKey}"
            }

            val weight = calculateDecayWeight(
                history = history,
                now = input.now,
                policy = policy,
            )

            result[key] =
                DynamicScheduleDecayedAttentionHistory(
                    attentionKey = history.attentionKey,

                    shownWeight =
                        history.shownCount * weight,

                    engagedWeight =
                        history.engagedCount * weight,

                    ignoredWeight =
                        history.ignoredCount * weight,

                    dismissedWeight =
                        history.dismissedCount * weight,

                    requestedWeight =
                        history.requestedCount * weight,

                    feedbackSignal =
                        (history.feedbackSignal * weight)
                            .coerceIn(-1.0, 1.0),

                    evidenceWeight =
                        calculateEvidenceWeight(
                            history = history,
                            decayWeight = weight,
                        ),
                )
        }

        return DynamicScheduleAttentionDecayResult(
            histories = result,
            diagnostics = diagnostics,
        )
    }

    /**
     * 14.1：实际公式已经抽到 [DynamicScheduleRecencyDecay]。
     *
     * 这里只负责"从 history 里取出时间"这一件事，
     * 衰减语义全系统只有一份实现。
     */
    private fun calculateDecayWeight(
        history: DynamicScheduleAttentionHistory,
        now: kotlin.time.Instant,
        policy: DynamicScheduleAttentionDecayPolicy,
    ): Double {

        val lastInteraction =
            history.lastInteractionAt
                ?: return policy.minimumWeight

        return DynamicScheduleRecencyDecay.weight(
            elapsed = now - lastInteraction,
            policy = policy,
        )
    }

    private fun calculateEvidenceWeight(
        history: DynamicScheduleAttentionHistory,
        decayWeight: Double,
    ): Double {

        val total =
            history.shownCount +
                history.engagedCount +
                history.ignoredCount +
                history.dismissedCount +
                history.requestedCount

        return total * decayWeight
    }
}
