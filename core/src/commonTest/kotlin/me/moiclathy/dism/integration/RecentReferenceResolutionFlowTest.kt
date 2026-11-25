package me.moiclathy.dism.integration

import me.moiclathy.dism.context.Context
import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityRegistry
import me.moiclathy.dism.domain.entity.EntityType
import me.moiclathy.dism.semantic.resolution.EntityMention
import me.moiclathy.dism.semantic.resolution.EntityMentionKind
import me.moiclathy.dism.semantic.resolution.EntityResolutionResult
import me.moiclathy.dism.semantic.resolution.EntityResolver
import me.moiclathy.dism.semantic.resolution.ResolutionContextBuilder
import me.moiclathy.dism.semantic.resolution.ResolutionMemory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant

/**
 * 端到端验证"连续语境"：
 *
 * 系统里有 白色充电宝、黑色充电宝；
 * 刚刚明确解析过 白色充电宝（recent reference）；
 * 用户说 "那个我带上了"。
 *
 * "那个" 不能靠字符串匹配，但可以靠最近解析历史解析到白色充电宝。
 */
class RecentReferenceResolutionFlowTest {

    @Test
    fun recentReference_shouldResolvePronounLikeMention() {
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
            listOf(
                whitePowerBank,
                blackPowerBank,
            )
        )

        val now = Instant.parse("2026-09-08T08:05:00Z")

        val memory = ResolutionMemory()
        memory.record(
            entityId = whitePowerBank.id,
            referencedAt = Instant.parse("2026-09-08T08:04:30Z"),
        )

        val resolutionContext = ResolutionContextBuilder()
            .build(
                states = emptyList(),
                context = Context(now = now),
                memory = memory,
            )

        val result = EntityResolver()
            .resolve(
                mention = EntityMention(
                    text = "那个",
                    expectedType = EntityType.ITEM,
                    kind = EntityMentionKind.REFERENCE,
                ),
                registry = registry,
                context = resolutionContext,
            )

        val resolved = assertIs<EntityResolutionResult.Resolved>(result)

        assertEquals(whitePowerBank.id, resolved.candidate.entity.id)

        val score = resolved.candidate.score

        // 完全靠 recent context，lexical / alias 均为 0
        assertEquals(0.0, score.lexical)
        assertEquals(0.0, score.alias)
        assertEquals(0.95, score.context)
        assertEquals(0.95, score.final)
    }

    @Test
    fun namedUnknownMention_shouldNotResolveViaRecentContext() {
        val registry = EntityRegistry()

        val powerBank = Entity(
            type = EntityType.ITEM,
            name = "充电宝",
        )

        registry.register(powerBank)

        val now = Instant.parse("2026-09-08T08:05:00Z")

        val memory = ResolutionMemory()
        memory.record(
            entityId = powerBank.id,
            referencedAt = Instant.parse("2026-09-08T08:04:30Z"),
        )

        val resolutionContext = ResolutionContextBuilder()
            .build(
                states = emptyList(),
                context = Context(now = now),
                memory = memory,
            )

        // "冰箱" 是 NAMED 但没有任何 lexical 匹配，
        // 即使 recent context 很强，也不允许 context-only 强行解析。
        val result = EntityResolver()
            .resolve(
                mention = EntityMention(
                    text = "冰箱",
                    expectedType = EntityType.ITEM,
                    kind = EntityMentionKind.NAMED,
                ),
                registry = registry,
                context = resolutionContext,
            )

        assertIs<EntityResolutionResult.Unresolved>(result)
    }
}
