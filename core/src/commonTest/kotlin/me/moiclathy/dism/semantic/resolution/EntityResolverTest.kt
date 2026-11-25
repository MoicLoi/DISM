package me.moiclathy.dism.semantic.resolution

import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityAlias
import me.moiclathy.dism.domain.entity.EntityAliasSource
import me.moiclathy.dism.domain.entity.EntityRegistry
import me.moiclathy.dism.domain.entity.EntityType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class EntityResolverTest {

    private val resolver = EntityResolver()

    @Test
    fun exactNameMatch_shouldResolveEntity() {
        val registry = EntityRegistry()

        val powerBank = Entity(
            type = EntityType.ITEM,
            name = "充电宝",
        )

        registry.register(powerBank)

        val result = resolver.resolve(
            mention = EntityMention(
                text = "充电宝",
                expectedType = EntityType.ITEM,
            ),
            registry = registry,
        )

        val resolved = assertIs<EntityResolutionResult.Resolved>(
            result
        )

        assertEquals(
            powerBank.id,
            resolved.candidate.entity.id,
        )

        val score = resolved.candidate.score

        assertEquals(
            1.0,
            score.lexical,
        )

        assertEquals(
            0.0,
            score.alias,
        )

        assertEquals(
            0.0,
            score.context,
        )

        assertEquals(
            1.0,
            score.final,
        )
    }

    @Test
    fun expectedType_shouldFilterCandidates() {
        val registry = EntityRegistry()

        val location = Entity(
            type = EntityType.LOCATION,
            name = "实验室",
        )

        val organization = Entity(
            type = EntityType.ORGANIZATION,
            name = "实验室",
        )

        registry.registerAll(
            listOf(location, organization)
        )

        val result = resolver.resolve(
            mention = EntityMention(
                text = "实验室",
                expectedType = EntityType.LOCATION,
            ),
            registry = registry,
        )

        val resolved = assertIs<EntityResolutionResult.Resolved>(
            result
        )

        assertEquals(
            location.id,
            resolved.candidate.entity.id,
        )
    }

    @Test
    fun multipleSimilarEntities_shouldBeAmbiguous() {
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

        val result = resolver.resolve(
            mention = EntityMention(
                text = "充电宝",
                expectedType = EntityType.ITEM,
            ),
            registry = registry,
        )

        val ambiguous = assertIs<EntityResolutionResult.Ambiguous>(
            result
        )

        assertEquals(
            2,
            ambiguous.candidates.size,
        )
    }

    @Test
    fun unknownMention_shouldBeUnresolved() {
        val registry = EntityRegistry()

        registry.register(
            Entity(
                type = EntityType.ITEM,
                name = "校园卡",
            )
        )

        val result = resolver.resolve(
            mention = EntityMention(
                text = "充电宝",
                expectedType = EntityType.ITEM,
            ),
            registry = registry,
        )

        assertIs<EntityResolutionResult.Unresolved>(result)
    }

    @Test
    fun exactAliasMatch_shouldResolveEntity() {
        val registry = EntityRegistry()

        val campusCard = Entity(
            type = EntityType.ITEM,
            name = "校园卡",
        )

        registry.register(campusCard)

        registry.registerAlias(
            EntityAlias(
                entityId = campusCard.id,
                value = "一卡通",
                source = EntityAliasSource.USER_DEFINED,
                confidence = 1.0,
            )
        )

        val result = resolver.resolve(
            mention = EntityMention(
                text = "一卡通",
                expectedType = EntityType.ITEM,
            ),
            registry = registry,
        )

        val resolved = assertIs<EntityResolutionResult.Resolved>(
            result
        )

        assertEquals(
            campusCard.id,
            resolved.candidate.entity.id,
        )

        val score = resolved.candidate.score

        assertEquals(
            0.0,
            score.lexical,
        )

        assertEquals(
            0.98,
            score.alias,
        )

        assertEquals(
            0.0,
            score.context,
        )

        assertEquals(
            0.98,
            score.final,
        )
    }

    @Test
    fun lowConfidenceAlias_shouldNotAutomaticallyResolve() {
        val registry = EntityRegistry()

        val campusCard = Entity(
            type = EntityType.ITEM,
            name = "校园卡",
        )

        registry.register(campusCard)

        registry.registerAlias(
            EntityAlias(
                entityId = campusCard.id,
                value = "饭卡",
                source = EntityAliasSource.INFERRED,
                confidence = 0.6,
            )
        )

        val result = resolver.resolve(
            mention = EntityMention(
                text = "饭卡",
                expectedType = EntityType.ITEM,
            ),
            registry = registry,
        )

        assertIs<EntityResolutionResult.Unresolved>(result)
    }

    @Test
    fun strongContext_shouldBreakLexicalTie() {
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

        val context = ResolutionContext(
            entityHints = listOf(
                ContextEntityHint(
                    entityId = whitePowerBank.id,
                    relevance = 1.0,
                )
            )
        )

        val result = resolver.resolve(
            mention = EntityMention(
                text = "充电宝",
                expectedType = EntityType.ITEM,
            ),
            registry = registry,
            context = context,
        )

        val resolved = assertIs<EntityResolutionResult.Resolved>(
            result
        )

        assertEquals(
            whitePowerBank.id,
            resolved.candidate.entity.id,
        )
    }

    @Test
    fun weakContext_shouldRemainAmbiguous() {
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

        val context = ResolutionContext(
            entityHints = listOf(
                ContextEntityHint(
                    entityId = whitePowerBank.id,
                    relevance = 0.4,
                )
            )
        )

        val result = resolver.resolve(
            mention = EntityMention(
                text = "充电宝",
                expectedType = EntityType.ITEM,
            ),
            registry = registry,
            context = context,
        )

        assertIs<EntityResolutionResult.Ambiguous>(result)
    }
}
