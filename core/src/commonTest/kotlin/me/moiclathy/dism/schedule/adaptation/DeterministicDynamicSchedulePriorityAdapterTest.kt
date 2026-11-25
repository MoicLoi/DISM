package me.moiclathy.dism.schedule.adaptation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

import me.moiclathy.dism.schedule.model.DynamicScheduleId
import me.moiclathy.dism.schedule.model.DynamicScheduleItem
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleItemType
import me.moiclathy.dism.schedule.model.DynamicScheduleModel
import me.moiclathy.dism.schedule.model.DynamicScheduleSource
import me.moiclathy.dism.schedule.model.DynamicScheduleState

class DeterministicDynamicSchedulePriorityAdapterTest {

    private val scheduleId =
        DynamicScheduleId("schedule-1")

    private val itemId =
        DynamicScheduleItemId("item-1")

    private fun model(
        priority: Double = 0.8,
    ): DynamicScheduleModel {

        return DynamicScheduleModel(
            id = scheduleId,
            items = listOf(
                DynamicScheduleItem(
                    id = itemId,
                    scheduleId = scheduleId,
                    title = "实验课",
                    type = DynamicScheduleItemType.EVENT,
                    source = DynamicScheduleSource.DERIVED,
                    state = DynamicScheduleState.READY,
                    priority = priority,
                    confidence = 1.0,
                ),
            ),
        )
    }

    @Test
    fun noHistoryKeepsPriorityUnchanged() {

        val result =
            DeterministicDynamicSchedulePriorityAdapter()
                .adapt(
                    DynamicSchedulePriorityAdaptationInput(
                        model = model(),
                    ),
                )

        val adaptation =
            result.adaptations.single()

        assertEquals(
            0.8,
            adaptation.basePriority,
        )

        assertEquals(
            1.0,
            adaptation.adaptationFactor,
        )

        assertEquals(
            0.8,
            adaptation.effectivePriority,
        )
    }

    @Test
    fun repeatedIgnoredAttentionReducesPriority() {

        val result =
            DeterministicDynamicSchedulePriorityAdapter()
                .adapt(
                    DynamicSchedulePriorityAdaptationInput(
                        model = model(),
                        histories = mapOf(
                            itemId.value to
                                DynamicScheduleAttentionHistory(
                                    attentionKey = itemId.value,
                                    shownCount = 10,
                                    ignoredCount = 5,
                                ),
                        ),
                    ),
                )

        val adaptation =
            result.adaptations.single()

        assertTrue(
            adaptation.effectivePriority < 0.8,
        )

        assertTrue(
            adaptation.adaptationFactor < 1.0,
        )
    }

    @Test
    fun engagementCanIncreasePriority() {

        val result =
            DeterministicDynamicSchedulePriorityAdapter()
                .adapt(
                    DynamicSchedulePriorityAdaptationInput(
                        model = model(),
                        histories = mapOf(
                            itemId.value to
                                DynamicScheduleAttentionHistory(
                                    attentionKey = itemId.value,
                                    shownCount = 5,
                                    engagedCount = 5,
                                    requestedCount = 1,
                                ),
                        ),
                    ),
                )

        val adaptation =
            result.adaptations.single()

        assertTrue(
            adaptation.effectivePriority > 0.8,
        )
    }

    @Test
    fun explicitNegativeFeedbackReducesPriority() {

        val result =
            DeterministicDynamicSchedulePriorityAdapter()
                .adapt(
                    DynamicSchedulePriorityAdaptationInput(
                        model = model(),
                        histories = mapOf(
                            itemId.value to
                                DynamicScheduleAttentionHistory(
                                    attentionKey = itemId.value,
                                    feedbackSignal = -1.0,
                                ),
                        ),
                    ),
                )

        val adaptation =
            result.adaptations.single()

        assertTrue(
            adaptation.effectivePriority < 0.8,
        )
    }

    @Test
    fun priorityIsAlwaysClamped() {

        val result =
            DeterministicDynamicSchedulePriorityAdapter()
                .adapt(
                    DynamicSchedulePriorityAdaptationInput(
                        model = model(priority = 1.0),
                        histories = mapOf(
                            itemId.value to
                                DynamicScheduleAttentionHistory(
                                    attentionKey = itemId.value,
                                    requestedCount = 100,
                                    engagedCount = 100,
                                ),
                        ),
                    ),
                )

        val adaptation =
            result.adaptations.single()

        assertTrue(
            adaptation.effectivePriority in 0.0..1.0,
        )
    }
}
