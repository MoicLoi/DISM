package me.moiclathy.dism.semantic.observation

import me.moiclathy.dism.context.Context
import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityRegistry
import me.moiclathy.dism.domain.entity.EntityType
import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.semantic.resolution.EntityMention
import me.moiclathy.dism.semantic.resolution.EntityMentionKind
import me.moiclathy.dism.semantic.resolution.EntityResolutionResult
import me.moiclathy.dism.semantic.resolution.ResolutionContext
import me.moiclathy.dism.semantic.resolution.ResolutionContextBuilder
import me.moiclathy.dism.semantic.resolution.ResolutionSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant
import kotlin.uuid.Uuid

class ObservationResolverTest {

    private val resolver = ObservationResolver()

    @Test
    fun resolvableDraft_shouldProduceSemanticObservation() {
        val registry = EntityRegistry()

        val powerBank = Entity(
            type = EntityType.ITEM,
            name = "充电宝",
        )

        val user = Entity(
            type = EntityType.PERSON,
            name = "我",
        )

        registry.registerAll(
            listOf(powerBank, user)
        )

        val evidenceId = Uuid.random()

        val time = Instant.parse("2026-09-08T08:00:00Z")

        val draft = SemanticObservationDraft(
            subjectMention = EntityMention(
                text = "充电宝",
                expectedType = EntityType.ITEM,
            ),
            predicate = Predicates.CARRIED_BY,
            objectMention = EntityMention(
                text = "我",
                expectedType = EntityType.PERSON,
            ),
            confidence = 0.95,
            evidenceIds = setOf(evidenceId),
            observedAt = time,
        )

        val result = resolver.resolve(
            draft = draft,
            registry = registry,
        )

        val resolved = assertIs<ObservationResolutionResult.Resolved>(
            result
        )

        assertEquals(
            powerBank.id,
            resolved.observation.subjectId,
        )

        assertEquals(
            Predicates.CARRIED_BY,
            resolved.observation.predicate,
        )

        assertEquals(
            user.id,
            resolved.observation.objectId,
        )

        assertEquals(
            0.95,
            resolved.observation.confidence,
        )

        assertEquals(
            setOf(evidenceId),
            resolved.observation.evidenceIds,
        )

        assertEquals(
            time,
            resolved.observation.observedAt,
        )
    }

    @Test
    fun ambiguousObject_shouldKeepDraftUnresolved() {
        val registry = EntityRegistry()

        val powerBank = Entity(
            type = EntityType.ITEM,
            name = "充电宝",
        )

        val eastDorm = Entity(
            type = EntityType.LOCATION,
            name = "东区宿舍",
        )

        val westDorm = Entity(
            type = EntityType.LOCATION,
            name = "西区宿舍",
        )

        registry.registerAll(
            listOf(powerBank, eastDorm, westDorm)
        )

        val draft = SemanticObservationDraft(
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
            observedAt = Instant.parse("2026-09-08T08:00:00Z"),
        )

        val result = resolver.resolve(
            draft = draft,
            registry = registry,
        )

        val unresolved = assertIs<ObservationResolutionResult.Unresolved>(
            result
        )

        assertIs<EntityResolutionResult.Resolved>(
            unresolved.subjectResolution
        )

        assertIs<EntityResolutionResult.Ambiguous>(
            unresolved.objectResolution
        )
    }

    @Test
    fun unknownSubject_shouldNotProduceSemanticObservation() {
        val registry = EntityRegistry()

        val user = Entity(
            type = EntityType.PERSON,
            name = "我",
        )

        registry.register(user)

        val draft = SemanticObservationDraft(
            subjectMention = EntityMention(
                text = "充电宝",
                expectedType = EntityType.ITEM,
            ),
            predicate = Predicates.CARRIED_BY,
            objectMention = EntityMention(
                text = "我",
                expectedType = EntityType.PERSON,
            ),
            confidence = 1.0,
            evidenceIds = setOf(Uuid.random()),
            observedAt = Instant.parse("2026-09-08T08:00:00Z"),
        )

        val result = resolver.resolve(
            draft = draft,
            registry = registry,
        )

        val unresolved = assertIs<ObservationResolutionResult.Unresolved>(
            result
        )

        assertIs<EntityResolutionResult.Unresolved>(
            unresolved.subjectResolution
        )

        assertIs<EntityResolutionResult.Resolved>(
            unresolved.objectResolution
        )
    }

    @Test
    fun partiallyResolvableDraft_shouldNotPolluteResolutionMemory() {
        val registry = EntityRegistry()

        val powerBank = Entity(
            type = EntityType.ITEM,
            name = "白色充电宝",
        )

        val eastDorm = Entity(
            type = EntityType.LOCATION,
            name = "东区宿舍",
        )

        val westDorm = Entity(
            type = EntityType.LOCATION,
            name = "西区宿舍",
        )

        registry.registerAll(
            listOf(powerBank, eastDorm, westDorm)
        )

        val session = ResolutionSession()

        val resolver = ObservationResolver(
            resolutionSession = session,
        )

        val time = Instant.parse("2026-09-08T08:00:00Z")

        val draft = SemanticObservationDraft(
            subjectMention = EntityMention(
                text = "白色充电宝",
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

        // subject Resolved，但 object Ambiguous
        val result = resolver.resolve(
            draft = draft,
            registry = registry,
            context = ResolutionContext(),
        )

        assertIs<ObservationResolutionResult.Unresolved>(result)

        // Observation 没能成立 → subject 也不许进 memory，避免半成功污染
        assertEquals(
            emptyList(),
            session.memory().recent(now = time),
        )
    }

    @Test
    fun resolvedObservation_shouldCommitBothEntitiesToMemory() {
        val registry = EntityRegistry()

        val powerBank = Entity(
            type = EntityType.ITEM,
            name = "白色充电宝",
        )

        val dorm = Entity(
            type = EntityType.LOCATION,
            name = "宿舍",
        )

        registry.registerAll(
            listOf(powerBank, dorm)
        )

        val session = ResolutionSession()

        val resolver = ObservationResolver(
            resolutionSession = session,
        )

        val time = Instant.parse("2026-09-08T08:00:00Z")

        val result = resolver.resolve(
            draft = SemanticObservationDraft(
                subjectMention = EntityMention(
                    text = "白色充电宝",
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
            ),
            registry = registry,
            context = ResolutionContext(),
        )

        assertIs<ObservationResolutionResult.Resolved>(result)

        val recent = session.memory().recent(now = time)

        val ids = recent.map { it.entityId }.toSet()

        assertEquals(
            setOf(powerBank.id, dorm.id),
            ids,
        )
    }

    @Test
    fun resolvedObservation_shouldSupportFollowingReferenceObservation() {
        val registry = EntityRegistry()

        val powerBank = Entity(
            type = EntityType.ITEM,
            name = "白色充电宝",
        )

        val dorm = Entity(
            type = EntityType.LOCATION,
            name = "宿舍",
        )

        val user = Entity(
            type = EntityType.PERSON,
            name = "我",
        )

        registry.registerAll(
            listOf(powerBank, dorm, user)
        )

        val session = ResolutionSession()

        val resolver = ObservationResolver(
            resolutionSession = session,
        )

        val builder = ResolutionContextBuilder()

        val firstTime = Instant.parse("2026-09-08T08:00:00Z")
        val secondTime = Instant.parse("2026-09-08T08:01:00Z")

        // 08:00 "白色充电宝放宿舍了"
        val firstResult = resolver.resolve(
            draft = SemanticObservationDraft(
                subjectMention = EntityMention(
                    text = "白色充电宝",
                    expectedType = EntityType.ITEM,
                ),
                predicate = Predicates.LOCATED_AT,
                objectMention = EntityMention(
                    text = "宿舍",
                    expectedType = EntityType.LOCATION,
                ),
                confidence = 1.0,
                evidenceIds = setOf(Uuid.random()),
                observedAt = firstTime,
            ),
            registry = registry,
            context = ResolutionContext(),
        )

        assertIs<ObservationResolutionResult.Resolved>(firstResult)

        // 08:01 上下文由第一条 Observation 留下的记忆自动构造
        val secondContext = builder.build(
            states = emptyList(),
            context = Context(now = secondTime),
            memory = session.memory(),
        )

        // 08:01 "那个我带上了"
        // "那个" 是 REFERENCE + expectedType ITEM，
        // expectedType 把宿舍（LOCATION）过滤掉，只剩白色充电宝。
        val secondResult = resolver.resolve(
            draft = SemanticObservationDraft(
                subjectMention = EntityMention(
                    text = "那个",
                    expectedType = EntityType.ITEM,
                    kind = EntityMentionKind.REFERENCE,
                ),
                predicate = Predicates.CARRIED_BY,
                objectMention = EntityMention(
                    text = "我",
                    expectedType = EntityType.PERSON,
                ),
                confidence = 1.0,
                evidenceIds = setOf(Uuid.random()),
                observedAt = secondTime,
            ),
            registry = registry,
            context = secondContext,
        )

        val resolved = assertIs<ObservationResolutionResult.Resolved>(secondResult)

        assertEquals(powerBank.id, resolved.observation.subjectId)
        assertEquals(user.id, resolved.observation.objectId)
    }
}
