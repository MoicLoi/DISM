package me.moiclathy.dism.schedule.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

import me.moiclathy.dism.attention.lifecycle.AttentionKey
import me.moiclathy.dism.planning.recommendation.RecommendationKey
import me.moiclathy.dism.planning.recommendation.RecommendationType
import me.moiclathy.dism.schedule.attention.DeterministicDynamicScheduleAttentionProjector
import me.moiclathy.dism.schedule.attention.DynamicScheduleAttentionIdentityResolver
import me.moiclathy.dism.schedule.model.DynamicScheduleId
import me.moiclathy.dism.schedule.model.DynamicScheduleItem
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleItemType
import me.moiclathy.dism.schedule.model.DynamicScheduleModel
import me.moiclathy.dism.schedule.model.DynamicScheduleSource
import me.moiclathy.dism.schedule.model.DynamicScheduleState
import me.moiclathy.dism.attention.adaptation.AttentionFeedback
import me.moiclathy.dism.attention.adaptation.AttentionFeedbackAction

class DeterministicDynamicScheduleIntelligenceRuntimeTest {

    private val scheduleId =
        DynamicScheduleId("schedule-1")

    private val itemId =
        DynamicScheduleItemId("item-1")

    private val eventId =
        Uuid.random()

    private val entityId =
        Uuid.random()

    private val recommendationKey =
        RecommendationKey(
            type =
                RecommendationType.CHECK_INFORMATION,
            eventId = eventId,
            requiredEntityId = entityId,
        )

    private val attentionKey =
        AttentionKey(
            recommendationKey,
        ).toString()

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

    private fun runtime():
        DeterministicDynamicScheduleIntelligenceRuntime {

        val projector =
            DeterministicDynamicScheduleAttentionProjector(
                identityResolver =
                    DynamicScheduleAttentionIdentityResolver {
                        if (it == itemId) {
                            recommendationKey
                        } else {
                            null
                        }
                    },
            )

        return DeterministicDynamicScheduleIntelligenceRuntime(
            attentionProjector = projector,
        )
    }

    @Test
    fun completePipelineProducesAttentionCandidate() {

        val result =
            runtime().evaluate(
                input =
                    DynamicScheduleIntelligenceInput(
                        model = model(),
                    ),
                now =
                    Instant.parse(
                        "2026-09-21T10:00:00Z",
                    ),
            )

        assertEquals(
            1,
            result.attentionProjection.candidateCount,
        )

        assertNotNull(
            result.attentionRuntime,
        )

        assertEquals(
            1,
            result.attentionRuntime.selection.selected.size,
        )
    }

    @Test
    fun completedScheduleDoesNotReachAttention() {

        val result =
            runtime().evaluate(
                input =
                    DynamicScheduleIntelligenceInput(
                        model =
                            model().copy(
                                items =
                                    listOf(
                                        model()
                                            .items
                                            .single()
                                            .copy(
                                                state =
                                                    DynamicScheduleState.COMPLETED,
                                            ),
                                    ),
                            ),
                    ),
                now =
                    Instant.parse(
                        "2026-09-21T10:00:00Z",
                    ),
            )

        assertEquals(
            0,
            result.attentionProjection.candidateCount,
        )
    }

    @Test
    fun feedbackUpdatesScheduleHistory() {

        val initial =
            DynamicScheduleIntelligenceRuntimeState()

        val next =
            runtime().applyFeedback(
                state = initial,
                feedback =
                    DynamicScheduleAttentionFeedback(
                        itemId = itemId,
                        feedback =
                            AttentionFeedback(
                                attentionKey =
                                    attentionKey,
                                action =
                                    AttentionFeedbackAction.IGNORED,
                                observedAt =
                                    Instant.parse(
                                        "2026-09-21T10:00:00Z",
                                    ),
                            ),
                    ),
            )

        val history =
            next.scheduleHistories[
                itemId.value
            ]

        assertNotNull(history)

        assertEquals(
            1,
            history.ignoredCount,
        )

        assertEquals(
            1,
            history.shownCount,
        )
    }

    @Test
    fun feedbackAlsoUpdatesAttentionRuntimeState() {

        val initial =
            DynamicScheduleIntelligenceRuntimeState()

        val next =
            runtime().applyFeedback(
                state = initial,
                feedback =
                    DynamicScheduleAttentionFeedback(
                        itemId = itemId,
                        feedback =
                            AttentionFeedback(
                                attentionKey =
                                    attentionKey,
                                action =
                                    AttentionFeedbackAction.DISMISSED,
                                observedAt =
                                    Instant.parse(
                                        "2026-09-21T10:00:00Z",
                                    ),
                            ),
                    ),
            )

        assertTrue(
            next.attentionRuntimeState
                .adaptations
                .containsKey(attentionKey),
        )

        assertTrue(
            next.attentionRuntimeState
                .memories
                .containsKey(attentionKey),
        )
    }
}
