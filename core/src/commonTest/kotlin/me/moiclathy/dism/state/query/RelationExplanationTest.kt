package me.moiclathy.dism.state.query

import me.moiclathy.dism.context.Context
import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityType
import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.evidence.Evidence
import me.moiclathy.dism.evidence.EvidenceType
import me.moiclathy.dism.semantic.engine.SemanticStateEngine
import me.moiclathy.dism.semantic.observation.SemanticObservationDraft
import me.moiclathy.dism.semantic.resolution.EntityMention
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class RelationExplanationTest {

    @Test
    fun explain_shouldExposeEstablishingTransitionAndEvidence() {
        val engine = SemanticStateEngine()
        val item = Entity(type = EntityType.ITEM, name = "充电宝")
        val dorm = Entity(type = EntityType.LOCATION, name = "宿舍")
        engine.entities.saveAll(listOf(item, dorm))

        val at = Instant.parse("2026-09-10T08:00:00Z")
        val evidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = at,
            confidence = 1.0,
        )
        engine.evidence.save(evidence)

        engine.observe(
            draft = SemanticObservationDraft(
                subjectMention = EntityMention("充电宝", EntityType.ITEM),
                predicate = Predicates.LOCATED_AT,
                objectMention = EntityMention("宿舍", EntityType.LOCATION),
                confidence = 1.0,
                evidenceIds = setOf(evidence.id),
                observedAt = at,
            ),
            context = Context(now = at),
        )

        val state = assertNotNull(engine.locatedAt(item.id))
        val explanation = engine.explain(state)

        assertEquals(listOf(evidence), explanation.evidence)
        assertNotNull(explanation.establishedBy)
        assertEquals(at, explanation.establishedBy!!.record.effectiveAt)
        assertEquals(listOf(evidence), explanation.establishedBy!!.evidence)
        assertNull(explanation.endedBy)
        assertEquals(listOf(evidence), explanation.allEvidence)
    }

    @Test
    fun explain_shouldExposeEndingCauseForSupersededState() {
        val engine = SemanticStateEngine()
        val item = Entity(type = EntityType.ITEM, name = "充电宝")
        val dorm = Entity(type = EntityType.LOCATION, name = "宿舍")
        val lab = Entity(type = EntityType.LOCATION, name = "实验楼")
        engine.entities.saveAll(listOf(item, dorm, lab))

        val first = Instant.parse("2026-09-10T08:00:00Z")
        val second = Instant.parse("2026-09-10T10:00:00Z")

        val firstEvidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = first,
            confidence = 1.0,
        )
        val secondEvidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = second,
            confidence = 1.0,
        )
        engine.evidence.saveAll(listOf(firstEvidence, secondEvidence))

        fun observe(
            location: String,
            at: Instant,
            evidenceId: kotlin.uuid.Uuid,
        ) {
            engine.observe(
                draft = SemanticObservationDraft(
                    subjectMention = EntityMention("充电宝", EntityType.ITEM),
                    predicate = Predicates.LOCATED_AT,
                    objectMention = EntityMention(location, EntityType.LOCATION),
                    confidence = 1.0,
                    evidenceIds = setOf(evidenceId),
                    observedAt = at,
                ),
                context = Context(now = at),
            )
        }

        observe("宿舍", first, firstEvidence.id)
        observe("实验楼", second, secondEvidence.id)

        val oldState = engine.historyOf(item.id).single {
            it.key.objectId == dorm.id
        }

        val explanation = engine.explain(oldState)

        assertEquals(listOf(firstEvidence), explanation.evidence)
        assertNotNull(explanation.establishedBy)
        assertEquals(first, explanation.establishedBy!!.record.effectiveAt)
        assertNotNull(explanation.endedBy)
        assertEquals(second, explanation.endedBy!!.record.effectiveAt)
        assertEquals(listOf(secondEvidence), explanation.endedBy!!.evidence)
        assertEquals(
            listOf(firstEvidence, secondEvidence),
            explanation.allEvidence,
        )
    }

    @Test
    fun explain_shouldSeparateHistoricalTruthFromCurrentEndingCause() {
        val engine = SemanticStateEngine()
        val item = Entity(type = EntityType.ITEM, name = "充电宝")
        val dorm = Entity(type = EntityType.LOCATION, name = "宿舍")
        val lab = Entity(type = EntityType.LOCATION, name = "实验楼")
        engine.entities.saveAll(listOf(item, dorm, lab))

        val first = Instant.parse("2026-09-10T08:00:00Z")
        val correction = Instant.parse("2026-09-10T09:00:00Z")
        val observedLater = Instant.parse("2026-09-10T12:00:00Z")

        val originalEvidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = first,
            confidence = 1.0,
        )
        val correctionEvidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = observedLater,
            confidence = 1.0,
        )
        engine.evidence.saveAll(listOf(originalEvidence, correctionEvidence))

        engine.observe(
            draft = SemanticObservationDraft(
                subjectMention = EntityMention("充电宝", EntityType.ITEM),
                predicate = Predicates.LOCATED_AT,
                objectMention = EntityMention("宿舍", EntityType.LOCATION),
                confidence = 1.0,
                evidenceIds = setOf(originalEvidence.id),
                observedAt = first,
                effectiveAt = first,
            ),
            context = Context(now = first),
        )

        // 今天 12:00 才知道，但明确纠正“09:00 已经在实验楼”。
        engine.observe(
            draft = SemanticObservationDraft(
                subjectMention = EntityMention("充电宝", EntityType.ITEM),
                predicate = Predicates.LOCATED_AT,
                objectMention = EntityMention("实验楼", EntityType.LOCATION),
                confidence = 1.0,
                evidenceIds = setOf(correctionEvidence.id),
                observedAt = observedLater,
                effectiveAt = correction,
            ),
            context = Context(now = observedLater),
        )

        val oldState = engine.historyOf(item.id).single {
            it.key.objectId == dorm.id
        }
        val explanation = engine.explain(oldState)

        // 原状态的 evidence 解释“为什么当时在宿舍”。
        assertEquals(listOf(originalEvidence), explanation.evidence)
        // ending cause 解释“为什么 09:00 之后不再成立”。
        assertEquals(listOf(correctionEvidence), explanation.endedBy!!.evidence)
        assertEquals(correction, explanation.endedBy!!.record.effectiveAt)
    }
}
