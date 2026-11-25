package me.moiclathy.dism.semantic.resolution

import me.moiclathy.dism.context.Context
import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityRegistry
import me.moiclathy.dism.domain.entity.EntityType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant

class ResolutionSessionTest {

    @Test
    fun successfulResolution_shouldBeRecordedInMemory() {
        val registry = EntityRegistry()

        val powerBank = Entity(
            type = EntityType.ITEM,
            name = "白色充电宝",
        )

        registry.register(powerBank)

        val session = ResolutionSession()

        val now = Instant.parse("2026-09-08T08:00:00Z")

        val result = session.resolve(
            mention = EntityMention(
                text = "白色充电宝",
                expectedType = EntityType.ITEM,
            ),
            registry = registry,
            context = ResolutionContext(),
            resolvedAt = now,
        )

        assertIs<EntityResolutionResult.Resolved>(result)

        val recent = session.memory().recent(now = now)

        assertEquals(1, recent.size)
        assertEquals(powerBank.id, recent.single().entityId)
        assertEquals(1.0, recent.single().confidence)
    }

    @Test
    fun ambiguousResolution_shouldNotBeRecorded() {
        val registry = EntityRegistry()

        val whitePowerBank = Entity(
            type = EntityType.ITEM,
            name = "白色充电宝",
        )

        val blackPowerBank = Entity(
            type = EntityType.ITEM,
            name = "黑色充电宝",
        )

        registry.registerAll(
            listOf(whitePowerBank, blackPowerBank)
        )

        val session = ResolutionSession()

        val now = Instant.parse("2026-09-08T08:00:00Z")

        val result = session.resolve(
            mention = EntityMention(
                text = "充电宝",
                expectedType = EntityType.ITEM,
            ),
            registry = registry,
            context = ResolutionContext(),
            resolvedAt = now,
        )

        assertIs<EntityResolutionResult.Ambiguous>(result)

        assertEquals(
            emptyList(),
            session.memory().recent(now = now),
        )
    }

    @Test
    fun resolvedEntity_shouldSupportFollowingReferenceMention() {
        val registry = EntityRegistry()

        val whitePowerBank = Entity(
            type = EntityType.ITEM,
            name = "白色充电宝",
        )

        val blackPowerBank = Entity(
            type = EntityType.ITEM,
            name = "黑色充电宝",
        )

        registry.registerAll(
            listOf(whitePowerBank, blackPowerBank)
        )

        val session = ResolutionSession()
        val builder = ResolutionContextBuilder()

        val firstTime = Instant.parse("2026-09-08T08:00:00Z")
        val secondTime = Instant.parse("2026-09-08T08:00:30Z")

        val firstResult = session.resolve(
            mention = EntityMention(
                text = "白色充电宝",
                expectedType = EntityType.ITEM,
            ),
            registry = registry,
            context = ResolutionContext(),
            resolvedAt = firstTime,
        )

        assertIs<EntityResolutionResult.Resolved>(firstResult)

        // 第二次解析不需要任何手工 memory.record()，
        // 上下文由 Session 的记忆自动构造。
        val secondContext = builder.build(
            states = emptyList(),
            context = Context(now = secondTime),
            memory = session.memory(),
        )

        val secondResult = session.resolve(
            mention = EntityMention(
                text = "那个",
                expectedType = EntityType.ITEM,
                kind = EntityMentionKind.REFERENCE,
            ),
            registry = registry,
            context = secondContext,
            resolvedAt = secondTime,
        )

        val resolved = assertIs<EntityResolutionResult.Resolved>(secondResult)

        assertEquals(whitePowerBank.id, resolved.candidate.entity.id)
    }
}
