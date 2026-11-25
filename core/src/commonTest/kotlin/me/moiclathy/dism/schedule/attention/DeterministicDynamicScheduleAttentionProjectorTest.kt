package me.moiclathy.dism.schedule.attention

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

import me.moiclathy.dism.planning.recommendation.RecommendationKey
import me.moiclathy.dism.planning.recommendation.RecommendationType
import me.moiclathy.dism.schedule.context.DynamicScheduleContextBinding
import me.moiclathy.dism.schedule.context.DynamicScheduleContextBindingId
import me.moiclathy.dism.schedule.context.DynamicScheduleContextBindingMode
import me.moiclathy.dism.schedule.context.DynamicScheduleContextBindingSet
import me.moiclathy.dism.schedule.context.DynamicScheduleContextBindingInput
import me.moiclathy.dism.schedule.context.DynamicScheduleContextBindingPolicy
import me.moiclathy.dism.schedule.context.DeterministicDynamicScheduleContextBinder
import me.moiclathy.dism.schedule.model.DynamicScheduleId
import me.moiclathy.dism.schedule.model.DynamicScheduleItem
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleItemType
import me.moiclathy.dism.schedule.model.DynamicScheduleModel
import me.moiclathy.dism.schedule.model.DynamicScheduleSource
import me.moiclathy.dism.schedule.model.DynamicScheduleState

class DeterministicDynamicScheduleAttentionProjectorTest {

    private val scheduleId =
        DynamicScheduleId("schedule-1")

    private val itemId =
        DynamicScheduleItemId("item-1")

    private val eventId =
        Uuid.random()

    private val entityId =
        Uuid.random()

    private fun model(
        state: DynamicScheduleState =
            DynamicScheduleState.READY,
        priority: Double = 0.8,
        confidence: Double = 1.0,
    ): DynamicScheduleModel {

        return DynamicScheduleModel(
            id = scheduleId,
            items =
                listOf(
                    DynamicScheduleItem(
                        id = itemId,
                        scheduleId = scheduleId,
                        title = "实验课",
                        type = DynamicScheduleItemType.EVENT,
                        source = DynamicScheduleSource.DERIVED,
                        state = state,
                        priority = priority,
                        confidence = confidence,
                    ),
                ),
        )
    }

    private fun projector(
        resolver:
            DynamicScheduleAttentionIdentityResolver =
                DynamicScheduleAttentionIdentityResolver {
                    if (it == itemId) {
                        RecommendationKey(
                            type = RecommendationType.CHECK_INFORMATION,
                            eventId = eventId,
                            requiredEntityId = entityId,
                        )
                    } else {
                        null
                    }
                },
    ): DeterministicDynamicScheduleAttentionProjector {

        return DeterministicDynamicScheduleAttentionProjector(
            identityResolver = resolver,
        )
    }

    @Test
    fun readyItemBecomesAttentionCandidate() {

        val result =
            projector().project(
                DynamicScheduleAttentionInput(
                    model = model(),
                ),
            )

        assertEquals(1, result.candidateCount)

        val candidate =
            result.candidates.single()

        assertEquals(
            itemId,
            candidate.itemId,
        )

        assertEquals(
            0.8,
            candidate.candidate.recommendation.priority,
        )
    }

    @Test
    fun lowPriorityItemIsSkipped() {

        val result =
            projector().project(
                DynamicScheduleAttentionInput(
                    model =
                        model(
                            priority = 0.1,
                        ),
                ),
            )

        assertEquals(0, result.candidateCount)
        assertEquals(1, result.skippedCount)
    }

    @Test
    fun completedItemIsNeverProjected() {

        val result =
            projector().project(
                DynamicScheduleAttentionInput(
                    model =
                        model(
                            state =
                                DynamicScheduleState.COMPLETED,
                        ),
                ),
            )

        assertEquals(0, result.candidateCount)
        assertEquals(1, result.skippedCount)
    }

    @Test
    fun missingStableIdentityIsSkipped() {

        val result =
            projector(
                resolver =
                    DynamicScheduleAttentionIdentityResolver {
                        null
                    },
            ).project(
                DynamicScheduleAttentionInput(
                    model = model(),
                ),
            )

        assertEquals(0, result.candidateCount)
        assertEquals(1, result.skippedCount)

        assertTrue(
            result.diagnostics.any {
                it.contains("RecommendationKey")
            },
        )
    }

    @Test
    fun blockedItemCanBecomeAttentionCandidate() {

        val result =
            projector().project(
                DynamicScheduleAttentionInput(
                    model =
                        model(
                            state =
                                DynamicScheduleState.BLOCKED,
                        ),
                ),
            )

        assertEquals(1, result.candidateCount)
    }

    @Test
    fun contextCanReduceAttentionPriority() {

        val contextResult =
            DeterministicDynamicScheduleContextBinder().bind(
                DynamicScheduleContextBindingInput(
                    model = model(),

                    bindings =
                        DynamicScheduleContextBindingSet(
                            listOf(
                                DynamicScheduleContextBinding(
                                    id =
                                        DynamicScheduleContextBindingId(
                                            "context-1",
                                        ),
                                    itemId = itemId,
                                    contextKey = "DORM",
                                    mode =
                                        DynamicScheduleContextBindingMode
                                            .PREFERRED,
                                    weight = 1.0,
                                ),
                            ),
                        ),

                    activeContextKeys = emptySet(),

                    policy =
                        DynamicScheduleContextBindingPolicy(),
                ),
            )

        val result =
            projector().project(
                DynamicScheduleAttentionInput(
                    model = model(),
                    contextResult = contextResult,
                ),
            )

        assertEquals(1, result.candidateCount)

        val priority =
            result.candidates
                .single()
                .candidate
                .recommendation
                .priority

        assertTrue(priority < 0.8)
    }
}
