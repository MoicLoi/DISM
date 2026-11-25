package me.moiclathy.dism.domain.relation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PredicateTest {

    @Test
    fun predicatesWithSameValue_shouldBeEqual() {
        assertEquals(
            Predicate("located_at"),
            Predicate("located_at"),
        )
    }

    @Test
    fun blankPredicate_shouldBeRejected() {
        assertFailsWith<IllegalArgumentException> {
            Predicate("")
        }
    }

    @Test
    fun toString_shouldReturnValue() {
        assertEquals(
            "carried_by",
            Predicate("carried_by").toString(),
        )
    }

    @Test
    fun standardPredicates_shouldMatchExplicitConstruction() {
        assertEquals(
            Predicate("located_at"),
            Predicates.LOCATED_AT,
        )

        assertEquals(
            Predicate("inside"),
            Predicates.INSIDE,
        )

        assertEquals(
            Predicate("carried_by"),
            Predicates.CARRIED_BY,
        )

        assertEquals(
            Predicate("requires"),
            Predicates.REQUIRES,
        )
    }
}
