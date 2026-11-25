package me.moiclathy.dism.integration

import me.moiclathy.dism.context.Context
import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityRegistry
import me.moiclathy.dism.domain.entity.EntityType
import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.semantic.resolution.EntityMention
import me.moiclathy.dism.semantic.resolution.EntityResolutionResult
import me.moiclathy.dism.semantic.resolution.EntityResolver
import me.moiclathy.dism.semantic.resolution.ResolutionContextBuilder
import me.moiclathy.dism.state.model.RelationKey
import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.model.RelationSupport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * 端到端验证：
 *
 * 系统里有 白色充电宝、黑色充电宝；
 * 明早早操 requires 白色充电宝；
 * 用户说 "充电宝我带上了"。
 *
 * 没有 Context 时：白色 0.75 / 黑色 0.75 → Ambiguous。
 * 有 ContextBuilder：白色 + requirement relevance → Resolved。
 */
class EventRequirementResolutionFlowTest {

    @Test
    fun eventRequirement_shouldResolveAmbiguousItemMention() {
        val registry = EntityRegistry()

        val event = Entity(
            type = EntityType.EVENT,
            name = "早操",
        )

        val whitePowerBank = Entity(
            type = EntityType.ITEM,
            name = "白色充电宝",
        )

        val blackPowerBank = Entity(
            type = EntityType.ITEM,
            name = "黑色充电宝",
        )

        val user = Entity(
            type = EntityType.PERSON,
            name = "我",
        )

        registry.registerAll(
            listOf(
                event,
                whitePowerBank,
                blackPowerBank,
                user,
            )
        )

        val now = Instant.parse("2026-09-08T08:00:00Z")

        val requirement = RelationState(
            key = RelationKey(
                subjectId = event.id,
                predicate = Predicates.REQUIRES,
                objectId = whitePowerBank.id,
            ),
            confidence = 1.0,
            supports = setOf(
                RelationSupport(
                    evidenceId = Uuid.random(),
                    semanticConfidence = 1.0,
                )
            ),
            validFrom = now,
        )

        val runtimeContext = Context(
            now = now,
            activeEventIds = setOf(event.id),
        )

        val resolutionContext = ResolutionContextBuilder()
            .build(
                states = listOf(requirement),
                context = runtimeContext,
            )

        val result = EntityResolver()
            .resolve(
                mention = EntityMention(
                    text = "充电宝",
                    expectedType = EntityType.ITEM,
                ),
                registry = registry,
                context = resolutionContext,
            )

        val resolved = assertIs<EntityResolutionResult.Resolved>(result)

        val score = resolved.candidate.score

        assertEquals(
            whitePowerBank.id,
            resolved.candidate.entity.id,
        )

        // 名字本身只给 0.75；
        // 真正让它从 ambiguous → resolved 的是 context +0.20。
        assertEquals(
            0.75,
            score.lexical,
        )

        assertEquals(
            0.0,
            score.alias,
        )

        assertEquals(
            0.20,
            score.context,
        )

        assertEquals(
            0.95,
            score.final,
        )
    }
}
