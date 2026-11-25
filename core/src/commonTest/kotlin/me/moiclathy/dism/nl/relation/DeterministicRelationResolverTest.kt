package me.moiclathy.dism.nl.relation

import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.nl.SemanticMention
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeterministicRelationResolverTest {
    private val resolver = DeterministicRelationResolver()

    @Test
    fun chineseLocationAliasResolves() {
        val m = RelationMention(SemanticMention("实验服"), "放在", SemanticMention("宿舍"))
        val result = resolver.resolve(m)
        assertTrue(result.resolved)
        assertEquals(Predicates.LOCATED_AT, result.predicate)
    }

    @Test
    fun canonicalRequiresResolves() {
        val m = RelationMention(SemanticMention("实验课"), "requires", SemanticMention("实验服"))
        val result = resolver.resolve(m)
        assertEquals(Predicates.REQUIRES, result.predicate)
        assertEquals(1.0, result.confidence)
    }

    @Test
    fun confidencePropagates() {
        val m = RelationMention(SemanticMention("实验课"), "需要", SemanticMention("实验服"), confidence = 0.6)
        val result = resolver.resolve(m)
        assertEquals(Predicates.REQUIRES, result.predicate)
        assertEquals(0.6, result.confidence)
    }

    @Test
    fun unknownPredicateRemainsUnresolved() {
        val m = RelationMention(SemanticMention("实验课"), "随便", SemanticMention("实验服"))
        val result = resolver.resolve(m)
        assertFalse(result.resolved)
        assertEquals(null, result.predicate)
    }
}
