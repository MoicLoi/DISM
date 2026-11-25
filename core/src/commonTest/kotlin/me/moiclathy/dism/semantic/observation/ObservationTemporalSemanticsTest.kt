package me.moiclathy.dism.semantic.observation

import me.moiclathy.dism.domain.entity.EntityType
import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.semantic.resolution.EntityMention
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * 锁定两时间轴基础：
 * Knowledge Time（observedAt）vs World Time（effectiveAt）。
 */
class ObservationTemporalSemanticsTest {

    @Test
    fun observation_shouldPreserveObservedAndEffectiveTimeSeparately() {
        val observedAt =
            Instant.parse("2026-09-08T10:00:00Z")

        val effectiveAt =
            Instant.parse("2026-09-07T22:47:00Z")

        val observation =
            SemanticObservation(
                subjectId = Uuid.random(),
                predicate = Predicates.LOCATED_AT,
                objectId = Uuid.random(),
                confidence = 1.0,
                evidenceIds = setOf(Uuid.random()),
                observedAt = observedAt,
                effectiveAt = effectiveAt,
            )

        assertEquals(observedAt, observation.observedAt)
        assertEquals(effectiveAt, observation.effectiveAt)
    }

    @Test
    fun draft_shouldDefaultEffectiveAtToObservedAt() {
        val time = Instant.parse("2026-09-08T10:00:00Z")

        val draft =
            SemanticObservationDraft(
                subjectMention = EntityMention(
                    text = "充电宝",
                    expectedType = EntityType.ITEM,
                ),
                predicate = Predicates.LOCATED_AT,
                objectMention = EntityMention(
                    text = "宿舍",
                    expectedType = EntityType.LOCATION,
                ),
                confidence = 1.0,
                evidenceIds = setOf(Uuid.random()),
                observedAt = time,
            )

        // 大多数即时陈述：effectiveAt 默认等于 observedAt
        assertEquals(time, draft.effectiveAt)
    }

    @Test
    fun processor_shouldCarryEffectiveAtIntoTransition() {
        val observedAt =
            Instant.parse("2026-09-08T10:00:00Z")

        val effectiveAt =
            Instant.parse("2026-09-07T22:47:00Z")

        val observation =
            SemanticObservation(
                subjectId = Uuid.random(),
                predicate = Predicates.LOCATED_AT,
                objectId = Uuid.random(),
                confidence = 1.0,
                evidenceIds = setOf(Uuid.random()),
                observedAt = observedAt,
                effectiveAt = effectiveAt,
            )

        val transition = ObservationProcessor()
            .process(observation)
            .transitions
            .single()

        // Transition.effectiveAt 记录"什么时候改变现实状态"
        assertEquals(effectiveAt, transition.effectiveAt)
    }
}
