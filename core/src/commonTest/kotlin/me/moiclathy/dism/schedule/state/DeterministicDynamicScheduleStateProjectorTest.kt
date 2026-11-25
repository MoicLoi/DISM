package me.moiclathy.dism.schedule.state

import kotlinx.datetime.Instant
import me.moiclathy.dism.schedule.model.DynamicScheduleId
import me.moiclathy.dism.schedule.model.DynamicScheduleItem
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleItemType
import me.moiclathy.dism.schedule.model.DynamicScheduleModel
import me.moiclathy.dism.schedule.model.DynamicScheduleSource
import me.moiclathy.dism.schedule.model.DynamicScheduleState
import me.moiclathy.dism.schedule.model.DynamicScheduleTemporal
import kotlin.test.Test
import kotlin.test.assertEquals

class DeterministicDynamicScheduleStateProjectorTest {
    private val scheduleId = DynamicScheduleId("schedule-1")
    private val t08 = Instant.parse("2026-01-01T08:00:00Z")
    private val t09 = Instant.parse("2026-01-01T09:00:00Z")
    private val t10 = Instant.parse("2026-01-01T10:00:00Z")
    private val t11 = Instant.parse("2026-01-01T11:00:00Z")

    private fun item(
        id: String,
        temporal: DynamicScheduleTemporal,
        state: DynamicScheduleState = DynamicScheduleState.UNSCHEDULED,
    ) = DynamicScheduleItem(
        id = DynamicScheduleItemId(id),
        scheduleId = scheduleId,
        title = id,
        type = DynamicScheduleItemType.EVENT,
        source = DynamicScheduleSource.EXTERNAL,
        temporal = temporal,
        state = state,
    )

    @Test
    fun atItemBecomesUpcomingThenActive() {
        val item = item("at", DynamicScheduleTemporal.At(t09))
        val model = DynamicScheduleModel(scheduleId, listOf(item))
        val projector = DeterministicDynamicScheduleStateProjector()

        val before = projector.project(DynamicScheduleStateProjectionInput(model, t08))
        assertEquals(DynamicScheduleState.UPCOMING, before.projectedModel.item(item.id)?.state)

        val atStart = projector.project(DynamicScheduleStateProjectionInput(model, t09))
        assertEquals(DynamicScheduleState.ACTIVE, atStart.projectedModel.item(item.id)?.state)
    }

    @Test
    fun intervalEndsAsExpiredUnlessAlreadyCompleted() {
        val active = item("interval", DynamicScheduleTemporal.Interval(t09, t10))
        val completed = active.copy(
            id = DynamicScheduleItemId("completed"),
            state = DynamicScheduleState.COMPLETED,
        )
        val model = DynamicScheduleModel(scheduleId, listOf(active, completed))

        val result = DeterministicDynamicScheduleStateProjector().project(
            DynamicScheduleStateProjectionInput(model, t11),
        )

        assertEquals(DynamicScheduleState.EXPIRED, result.projectedModel.item(active.id)?.state)
        assertEquals(DynamicScheduleState.COMPLETED, result.projectedModel.item(completed.id)?.state)
    }

    @Test
    fun deadlineProjectsToReadyBeforeDeadline() {
        val item = item("deadline", DynamicScheduleTemporal.Deadline(t10))
        val result = DeterministicDynamicScheduleStateProjector().project(
            DynamicScheduleStateProjectionInput(
                DynamicScheduleModel(scheduleId, listOf(item)),
                t09,
            ),
        )

        assertEquals(DynamicScheduleState.READY, result.projectedModel.item(item.id)?.state)
        assertEquals(DynamicScheduleStateProjectionReason.BEFORE_DEADLINE, result.changes.single().reason)
    }

    @Test
    fun windowIsReadyInsideWindowAndExpiresAfterIt() {
        val item = item("window", DynamicScheduleTemporal.Window(t09, t10))
        val model = DynamicScheduleModel(scheduleId, listOf(item))
        val projector = DeterministicDynamicScheduleStateProjector()

        val inside = projector.project(DynamicScheduleStateProjectionInput(model, t09))
        assertEquals(DynamicScheduleState.READY, inside.projectedModel.item(item.id)?.state)

        val after = projector.project(DynamicScheduleStateProjectionInput(model, t11))
        assertEquals(DynamicScheduleState.EXPIRED, after.projectedModel.item(item.id)?.state)
    }

    @Test
    fun explicitBlockedAndPostponedStatesArePreserved() {
        val blocked = item("blocked", DynamicScheduleTemporal.At(t09), DynamicScheduleState.BLOCKED)
        val postponed = item("postponed", DynamicScheduleTemporal.At(t09), DynamicScheduleState.POSTPONED)
        val model = DynamicScheduleModel(scheduleId, listOf(blocked, postponed))

        val result = DeterministicDynamicScheduleStateProjector().project(
            DynamicScheduleStateProjectionInput(model, t11),
        )

        assertEquals(DynamicScheduleState.BLOCKED, result.projectedModel.item(blocked.id)?.state)
        assertEquals(DynamicScheduleState.POSTPONED, result.projectedModel.item(postponed.id)?.state)
        assertEquals(0, result.changedCount)
        assertEquals(model.revision, result.projectedModel.revision)
    }

    @Test
    fun unchangedProjectionDoesNotIncrementRevision() {
        val item = item("future", DynamicScheduleTemporal.At(t11))
        val model = DynamicScheduleModel(scheduleId, listOf(item))

        val result = DeterministicDynamicScheduleStateProjector().project(
            DynamicScheduleStateProjectionInput(model, t08),
        )

        assertEquals(DynamicScheduleState.UPCOMING, result.projectedModel.item(item.id)?.state)
        assertEquals(1L, result.projectedModel.revision)

        val second = DeterministicDynamicScheduleStateProjector().project(
            DynamicScheduleStateProjectionInput(result.projectedModel, t08),
        )
        assertEquals(1L, second.projectedModel.revision)
        assertEquals(0, second.changedCount)
    }
}
