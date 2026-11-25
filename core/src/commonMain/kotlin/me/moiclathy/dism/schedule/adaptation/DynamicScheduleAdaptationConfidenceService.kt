package me.moiclathy.dism.schedule.adaptation

import me.moiclathy.dism.schedule.context.DynamicScheduleContextPriorityInfluence

class DynamicScheduleAdaptationConfidenceService(
    private val adapter:
        DynamicScheduleAdaptationConfidenceAdapter =
            DeterministicDynamicScheduleAdaptationConfidenceAdapter(),
) {

    fun calculate(
        adaptations:
            List<DynamicSchedulePriorityAdaptation>,

        historicalEvidenceByItem:
            Map<String, Double> = emptyMap(),

        contextInfluences:
            List<DynamicScheduleContextPriorityInfluence> =
                emptyList(),

        stabilityByItem:
            Map<String, Double> = emptyMap(),

        sampleCountByItem:
            Map<String, Int> = emptyMap(),

        policy:
            DynamicScheduleAdaptationConfidencePolicy =
                DynamicScheduleAdaptationConfidencePolicy(),
    ): List<DynamicScheduleAdaptationConfidence> {

        val contextByItem =
            contextInfluences.associateBy {
                it.itemId
            }

        return adaptations
            .sortedBy {
                it.itemId.value
            }
            .map { adaptation ->

                val key =
                    adaptation.itemId.value

                adapter.calculate(
                    input =
                        DynamicScheduleAdaptationConfidenceInput(
                            priorityAdaptation =
                                adaptation,

                            historicalEvidence =
                                historicalEvidenceByItem[
                                    key
                                ] ?: 0.0,

                            contextInfluence =
                                contextByItem[
                                    adaptation.itemId
                                ],

                            stabilityEvidence =
                                stabilityByItem[
                                    key
                                ] ?: 0.0,

                            sampleCount =
                                sampleCountByItem[
                                    key
                                ] ?: 0,
                        ),

                    policy =
                        policy,
                )
            }
    }
}
