package me.moiclathy.dism.schedule.adaptation

import kotlin.time.Instant

class DeterministicDynamicScheduleAdaptationBoundaryAdapter :
    DynamicScheduleAdaptationBoundaryAdapter {

    override fun apply(
        adaptation:
            DynamicSchedulePriorityAdaptation,

        confidence:
            DynamicScheduleAdaptationConfidence,

        state:
            DynamicScheduleAdaptationBoundaryState,

        now: Instant,

        policy:
            DynamicScheduleAdaptationBoundaryPolicy,
    ): DynamicScheduleAdaptationBoundary {

        val requested =
            adaptation.adaptationFactor - 1.0

        /*
         * Confidence 硬下限。
         */
        if (
            confidence.value <
                policy.hardMinimumConfidence
        ) {
            return disabled(
                adaptation = adaptation,
                confidence = confidence,
                reason =
                    "Confidence below hard minimum",
            )
        }

        /*
         * Confidence 普通下限。
         */
        if (
            confidence.value <
                policy.minimumConfidence
        ) {
            return disabled(
                adaptation = adaptation,
                confidence = confidence,
                reason =
                    "Confidence below automatic adaptation threshold",
            )
        }

        /*
         * 用户明确设置时，自动系统不能覆盖。
         */
        if (
            policy.protectExplicitUserSetting &&
            state.explicitUserSetting
        ) {
            return DynamicScheduleAdaptationBoundary(
                itemId =
                    adaptation.itemId,

                requestedDelta =
                    requested,

                allowedDelta = 0.0,

                confidence =
                    confidence.value,

                limitedBySingleChange = false,

                limitedByCumulativeChange = false,

                limitedByFrequency = false,

                limitedByExplicitUserSetting = true,

                enabled = false,

                reasons =
                    listOf(
                        "Explicit user setting is protected",
                    ),
            )
        }

        /*
         * 1. 单次限制
         */
        val singleLimited =
            requested.coerceIn(
                -policy.maximumSingleDelta,
                policy.maximumSingleDelta,
            )

        val limitedBySingle =
            singleLimited != requested

        /*
         * 2. 累计限制
         */
        val cumulativeRemaining =
            remainingCumulativeAllowance(
                requested = singleLimited,
                cumulativeDelta =
                    state.cumulativeDelta,
                maximum =
                    policy.maximumCumulativeDelta,
            )

        val cumulativeLimited =
            if (singleLimited >= 0.0) {
                singleLimited.coerceAtMost(
                    cumulativeRemaining,
                )
            } else {
                singleLimited.coerceAtLeast(
                    -cumulativeRemaining,
                )
            }

        val limitedByCumulative =
            cumulativeLimited !=
                singleLimited

        /*
         * 3. 频率限制
         */
        val recentChanges =
            state.recentChangeTimes.count {
                now.toEpochMilliseconds() -
                    it.toEpochMilliseconds() <=
                    policy.windowMillis
            }

        val frequencyLimited =
            recentChanges >=
                policy.maximumChangesPerWindow

        val allowed =
            if (frequencyLimited) {
                0.0
            } else {
                cumulativeLimited
            }

        return DynamicScheduleAdaptationBoundary(
            itemId =
                adaptation.itemId,

            requestedDelta =
                requested,

            allowedDelta =
                allowed,

            confidence =
                confidence.value,

            limitedBySingleChange =
                limitedBySingle,

            limitedByCumulativeChange =
                limitedByCumulative,

            limitedByFrequency =
                frequencyLimited,

            limitedByExplicitUserSetting =
                false,

            /*
             * 规格原文这里还要求 `adaptation.enabled`。
             *
             * 但 14.9 消费的是 12.x 的
             * DynamicSchedulePriorityAdaptation
             * （basePriority / adaptationFactor / effectivePriority），
             * 它没有 `enabled` 字段——那是 14.6 delta 模型的字段。
             *
             * 12.x 的等价语义已经由 adaptationFactor 本身表达：
             * factor == 1.0 时 requested == 0.0，自然走到 allowed == 0.0。
             * 所以这里只看 confidence。
             */
            enabled =
                allowed != 0.0 &&
                    confidence.enabled,

            reasons =
                buildReasons(
                    requested =
                        requested,

                    allowed =
                        allowed,

                    limitedBySingle =
                        limitedBySingle,

                    limitedByCumulative =
                        limitedByCumulative,

                    frequencyLimited =
                        frequencyLimited,
                ),
        )
    }

    private fun remainingCumulativeAllowance(
        requested: Double,
        cumulativeDelta: Double,
        maximum: Double,
    ): Double {

        if (requested >= 0.0) {
            return (
                maximum -
                    cumulativeDelta.coerceAtLeast(0.0)
                )
                .coerceAtLeast(0.0)
        }

        return (
            maximum -
                (-cumulativeDelta).coerceAtLeast(0.0)
            )
            .coerceAtLeast(0.0)
    }

    private fun disabled(
        adaptation:
            DynamicSchedulePriorityAdaptation,

        confidence:
            DynamicScheduleAdaptationConfidence,

        reason: String,
    ): DynamicScheduleAdaptationBoundary {

        return DynamicScheduleAdaptationBoundary(
            itemId =
                adaptation.itemId,

            requestedDelta =
                adaptation.adaptationFactor - 1.0,

            allowedDelta = 0.0,

            confidence =
                confidence.value,

            limitedBySingleChange = false,

            limitedByCumulativeChange = false,

            limitedByFrequency = false,

            limitedByExplicitUserSetting = false,

            enabled = false,

            reasons =
                listOf(reason),
        )
    }

    private fun buildReasons(
        requested: Double,
        allowed: Double,
        limitedBySingle: Boolean,
        limitedByCumulative: Boolean,
        frequencyLimited: Boolean,
    ): List<String> {

        return buildList {

            add(
                "Requested adaptation delta=$requested",
            )

            add(
                "Allowed adaptation delta=$allowed",
            )

            if (limitedBySingle) {
                add(
                    "Single-change boundary applied",
                )
            }

            if (limitedByCumulative) {
                add(
                    "Cumulative-change boundary applied",
                )
            }

            if (frequencyLimited) {
                add(
                    "Frequency boundary applied",
                )
            }
        }
    }
}
