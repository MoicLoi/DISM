package me.moiclathy.dism.schedule.trace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

class DeterministicDynamicScheduleDecisionTraceBuilderTest {

    private val itemId =
        DynamicScheduleItemId("item-1")

    private val builder =
        DeterministicDynamicScheduleDecisionTraceBuilder()

    @Test
    fun createsEmptyTrace() {

        val trace =
            builder.create(itemId)

        assertEquals(
            itemId,
            trace.itemId,
        )

        assertTrue(
            trace.entries.isEmpty(),
        )
    }

    @Test
    fun appendsEntries() {

        var trace =
            builder.create(itemId)

        trace =
            builder.append(
                trace,
                DynamicScheduleDecisionTraceEntry(
                    stage =
                        DynamicScheduleDecisionTraceStage.INPUT,
                    message =
                        "Schedule item received",
                ),
            )

        trace =
            builder.append(
                trace,
                DynamicScheduleDecisionTraceEntry(
                    stage =
                        DynamicScheduleDecisionTraceStage
                            .CONFLICT_RESOLUTION,
                    message =
                        "Hard dependency preserved attention",
                    decisive = true,
                ),
            )

        assertEquals(
            2,
            trace.entries.size,
        )

        assertEquals(
            1,
            trace.decisiveEntries.size,
        )

        assertTrue(
            trace.stages.contains(
                DynamicScheduleDecisionTraceStage.INPUT,
            ),
        )

        assertTrue(
            trace.stages.contains(
                DynamicScheduleDecisionTraceStage
                    .CONFLICT_RESOLUTION,
            ),
        )
    }
}
