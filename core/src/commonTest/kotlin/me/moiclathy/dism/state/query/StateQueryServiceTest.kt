package me.moiclathy.dism.state.query

import me.moiclathy.dism.domain.relation.Predicate
import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.state.model.RelationKey
import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.model.RelationStatus
import me.moiclathy.dism.state.model.RelationSupport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlin.uuid.Uuid

class StateQueryServiceTest {

    private val service =
        StateQueryService()

    private fun activeState(
        subjectId: Uuid,
        predicate: Predicate,
        objectId: Uuid,
        confidence: Double = 0.9,
        validFrom: Instant =
            Instant.parse("2026-09-08T08:00:00Z"),
    ): RelationState {
        return RelationState(
            key = RelationKey(
                subjectId = subjectId,
                predicate = predicate,
                objectId = objectId,
            ),
            confidence = confidence,
            supports = setOf(
                RelationSupport(
                    evidenceId = Uuid.random(),
                    semanticConfidence = 1.0,
                )
            ),
            validFrom = validFrom,
        )
    }

    @Test
    fun query_shouldReturnMatchingActiveRelation() {
        val powerBankId = Uuid.random()
        val dormId = Uuid.random()

        val state = activeState(
            subjectId = powerBankId,
            predicate = Predicates.LOCATED_AT,
            objectId = dormId,
        )

        val result = service.query(
            states = listOf(state),
            query = RelationQuery(
                subjectId = powerBankId,
                predicate = Predicates.LOCATED_AT,
            ),
        )

        assertEquals(listOf(state), result)
    }

    @Test
    fun query_shouldIgnoreInactiveStatesByDefault() {
        val itemId = Uuid.random()
        val dormId = Uuid.random()

        val oldState = RelationState(
            key = RelationKey(
                subjectId = itemId,
                predicate = Predicates.LOCATED_AT,
                objectId = dormId,
            ),
            confidence = 1.0,
            supports = emptySet(),
            validFrom = Instant.parse("2026-09-08T07:00:00Z"),
            validUntil = Instant.parse("2026-09-08T08:00:00Z"),
            status = RelationStatus.SUPERSEDED,
        )

        val result = service.query(
            states = listOf(oldState),
            query = RelationQuery(
                subjectId = itemId,
            ),
        )

        // Current Reality：默认不返回历史状态
        assertEquals(emptyList(), result)
    }

    @Test
    fun queryWithAllTemporalScope_shouldIncludeHistoricalState() {
        val itemId = Uuid.random()
        val dormId = Uuid.random()

        val oldState = RelationState(
            key = RelationKey(
                subjectId = itemId,
                predicate = Predicates.LOCATED_AT,
                objectId = dormId,
            ),
            confidence = 1.0,
            supports = emptySet(),
            validFrom = Instant.parse("2026-09-08T07:00:00Z"),
            validUntil = Instant.parse("2026-09-08T08:00:00Z"),
            status = RelationStatus.SUPERSEDED,
        )

        val result = service.query(
            states = listOf(oldState),
            query = RelationQuery(
                subjectId = itemId,
                temporalScope = TemporalScope.All,
            ),
        )

        // Historical Reality：显式要求才返回
        assertEquals(listOf(oldState), result)
    }

    @Test
    fun queryAtTime_shouldReturnStateValidAtThatTime() {
        val itemId = Uuid.random()
        val dormId = Uuid.random()

        val state = RelationState(
            key = RelationKey(
                subjectId = itemId,
                predicate = Predicates.LOCATED_AT,
                objectId = dormId,
            ),
            confidence = 1.0,
            supports = emptySet(),
            validFrom = Instant.parse("2026-09-08T07:00:00Z"),
            validUntil = Instant.parse("2026-09-08T08:00:00Z"),
            status = RelationStatus.SUPERSEDED,
        )

        // 07:00–08:00 的状态，07:30 查询应该命中
        val result = service.query(
            states = listOf(state),
            query = RelationQuery(
                subjectId = itemId,
                predicate = Predicates.LOCATED_AT,
                temporalScope = TemporalScope.At(
                    Instant.parse("2026-09-08T07:30:00Z")
                ),
            ),
        )

        assertEquals(listOf(state), result)
    }

    @Test
    fun state_shouldNotBeValidAtItsValidUntilBoundary() {
        val itemId = Uuid.random()
        val dormId = Uuid.random()

        val end = Instant.parse("2026-09-08T08:00:00Z")

        val state = RelationState(
            key = RelationKey(
                subjectId = itemId,
                predicate = Predicates.LOCATED_AT,
                objectId = dormId,
            ),
            confidence = 1.0,
            supports = emptySet(),
            validFrom = Instant.parse("2026-09-08T07:00:00Z"),
            validUntil = end,
            status = RelationStatus.SUPERSEDED,
        )

        // [validFrom, validUntil)：边界时刻本身不再有效
        val result = service.query(
            states = listOf(state),
            query = RelationQuery(
                subjectId = itemId,
                temporalScope = TemporalScope.At(end),
            ),
        )

        assertEquals(emptyList(), result)
    }

    @Test
    fun state_shouldBeValidAtItsValidFromBoundary() {
        val itemId = Uuid.random()
        val dormId = Uuid.random()

        val start = Instant.parse("2026-09-08T07:00:00Z")

        val state = RelationState(
            key = RelationKey(
                subjectId = itemId,
                predicate = Predicates.LOCATED_AT,
                objectId = dormId,
            ),
            confidence = 1.0,
            supports = emptySet(),
            validFrom = start,
            validUntil = Instant.parse("2026-09-08T08:00:00Z"),
            status = RelationStatus.SUPERSEDED,
        )

        // 左闭：validFrom 时刻本身有效
        val result = service.query(
            states = listOf(state),
            query = RelationQuery(
                subjectId = itemId,
                temporalScope = TemporalScope.At(start),
            ),
        )

        assertEquals(listOf(state), result)
    }

    @Test
    fun state_shouldNotBeValidBeforeValidFrom() {
        val itemId = Uuid.random()
        val dormId = Uuid.random()

        val state = RelationState(
            key = RelationKey(
                subjectId = itemId,
                predicate = Predicates.LOCATED_AT,
                objectId = dormId,
            ),
            confidence = 1.0,
            supports = emptySet(),
            validFrom = Instant.parse("2026-09-08T07:00:00Z"),
            status = RelationStatus.ACTIVE,
        )

        val result = service.query(
            states = listOf(state),
            query = RelationQuery(
                subjectId = itemId,
                temporalScope = TemporalScope.At(
                    Instant.parse("2026-09-08T06:59:59Z")
                ),
            ),
        )

        assertEquals(emptyList(), result)
    }

    @Test
    fun temporalQuery_shouldSwitchExactlyAtTransitionTime() {
        val itemId = Uuid.random()
        val dormId = Uuid.random()
        val userId = Uuid.random()

        val transitionTime = Instant.parse("2026-09-08T08:00:00Z")

        val located = RelationState(
            key = RelationKey(
                subjectId = itemId,
                predicate = Predicates.LOCATED_AT,
                objectId = dormId,
            ),
            confidence = 1.0,
            supports = emptySet(),
            validFrom = Instant.parse("2026-09-08T07:00:00Z"),
            validUntil = transitionTime,
            status = RelationStatus.SUPERSEDED,
        )

        val carried = RelationState(
            key = RelationKey(
                subjectId = itemId,
                predicate = Predicates.CARRIED_BY,
                objectId = userId,
            ),
            confidence = 1.0,
            supports = emptySet(),
            validFrom = transitionTime,
        )

        val beforeSwitch = service.query(
            states = listOf(located, carried),
            query = RelationQuery(
                subjectId = itemId,
                temporalScope = TemporalScope.At(
                    Instant.parse("2026-09-08T07:59:59Z")
                ),
            ),
        )

        assertEquals(listOf(located), beforeSwitch)

        val atSwitch = service.query(
            states = listOf(located, carried),
            query = RelationQuery(
                subjectId = itemId,
                temporalScope = TemporalScope.At(
                    transitionTime
                ),
            ),
        )

        assertEquals(listOf(carried), atSwitch)
    }

    @Test
    fun query_shouldFilterByMinimumConfidence() {
        val powerBankId = Uuid.random()
        val dormId = Uuid.random()

        val lowConfidenceState = activeState(
            subjectId = powerBankId,
            predicate = Predicates.LOCATED_AT,
            objectId = dormId,
            confidence = 0.6,
        )

        // 只问 80% 以上把握的状态 → 0.6 被过滤
        assertEquals(
            emptyList(),
            service.query(
                states = listOf(lowConfidenceState),
                query = RelationQuery(
                    subjectId = powerBankId,
                    minimumConfidence = 0.8,
                ),
            ),
        )

        // 放宽到 50% → 命中
        assertEquals(
            listOf(lowConfidenceState),
            service.query(
                states = listOf(lowConfidenceState),
                query = RelationQuery(
                    subjectId = powerBankId,
                    minimumConfidence = 0.5,
                ),
            ),
        )
    }

    @Test
    fun query_shouldFilterByObjectIdAndPredicate() {
        val userId = Uuid.random()
        val powerBankId = Uuid.random()
        val campusCardId = Uuid.random()

        val carried = activeState(
            subjectId = powerBankId,
            predicate = Predicates.CARRIED_BY,
            objectId = userId,
        )

        val located = activeState(
            subjectId = campusCardId,
            predicate = Predicates.LOCATED_AT,
            objectId = userId,
        )

        // 只问“由用户携带”的实体
        assertEquals(
            listOf(carried),
            service.query(
                states = listOf(carried, located),
                query = RelationQuery(
                    predicate = Predicates.CARRIED_BY,
                    objectId = userId,
                ),
            ),
        )
    }

    @Test
    fun changesAt_shouldReportStartedAndEndedAtInstant() {
        val itemId = Uuid.random()
        val classroomId = Uuid.random()
        val dormId = Uuid.random()

        val transitionTime =
            Instant.parse("2026-09-08T22:47:00Z")

        val classroom = RelationState(
            key = RelationKey(
                subjectId = itemId,
                predicate = Predicates.LOCATED_AT,
                objectId = classroomId,
            ),
            confidence = 1.0,
            supports = emptySet(),
            validFrom = Instant.parse("2026-09-08T22:00:00Z"),
            validUntil = transitionTime,
            status = RelationStatus.SUPERSEDED,
        )

        val dorm = RelationState(
            key = RelationKey(
                subjectId = itemId,
                predicate = Predicates.LOCATED_AT,
                objectId = dormId,
            ),
            confidence = 1.0,
            supports = emptySet(),
            validFrom = transitionTime,
        )

        val changes = service.changesAt(
            states = listOf(classroom, dorm),
            instant = transitionTime,
        )

        assertEquals(2, changes.size)

        val ended = changes
            .filterIsInstance<TemporalStateChange.Ended>()
            .single()

        assertEquals(classroomId, ended.state.key.objectId)

        val started = changes
            .filterIsInstance<TemporalStateChange.Started>()
            .single()

        assertEquals(dormId, started.state.key.objectId)
    }
}
